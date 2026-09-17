#include <jni.h>
#include <malloc.h>
#include <string.h>
#include <android/log.h>
#include <map>
#include <memory>
#include <mutex>
#include <new>
#include <unrar/rar.hpp>
#include "common.h"

// RAR 展開の JNI.
// 以前はバッファと展開器をプロセス全体で 1 組だけ持っていたため、
// サムネイル取得とビューアが同時に RAR を開くとデータが混ざり、解放済みメモリにも触れていた.
// ハンドルごとに状態を持たせ、各呼び出しはハンドルのロックを取ってから処理する.

#define RAR_LOG_TAG "callUnrar"
#define RAR_LOGE(...) __android_log_print(ANDROID_LOG_ERROR, RAR_LOG_TAG, __VA_ARGS__)

// Java 側 (CallJniLibrary) と同じ値にすること
#define RAR_OK                    0
#define RAR_ERROR_INVALID_HANDLE  -1
#define RAR_ERROR_INVALID_SIZE    -2
#define RAR_ERROR_UNPACK          -3
#define RAR_ERROR_INCOMPLETE      -4
#define RAR_ERROR_TRUNCATED       -5

namespace {

struct RarContext {
	std::mutex lock;
	bool closed = false;

	byte *fromBuff = nullptr;
	byte *toBuff = nullptr;
	int maxCompLen = 0;
	int maxOrigLen = 0;

	int compLen = 0;
	int origLen = 0;
	int fromPos = 0;
	int toPos = 0;
	int rarVersion = 0;
	uint64 winSize = 0;
	bool noCompress = false;

	ComprDataIO dataIO;
	Unpack *unpack = nullptr;

	void freeBuffers() {
		free(fromBuff);
		fromBuff = nullptr;
		free(toBuff);
		toBuff = nullptr;
		maxCompLen = 0;
		maxOrigLen = 0;
		compLen = 0;
		origLen = 0;
	}

	~RarContext() {
		freeBuffers();
		delete unpack;
	}
};

std::mutex gRegistryLock;
std::map<jlong, std::shared_ptr<RarContext>> gContexts;
jlong gNextHandle = 1;

// ハンドルに対応するコンテキストを取得する. 閉じられていれば nullptr
std::shared_ptr<RarContext> findContext(jlong handle) {
	std::lock_guard<std::mutex> guard(gRegistryLock);
	auto it = gContexts.find(handle);
	if (it == gContexts.end()) {
		return nullptr;
	}
	return it->second;
}

bool validRange(JNIEnv *env, jbyteArray array, jint offset, jint size) {
	if (array == nullptr || offset < 0 || size < 0) {
		return false;
	}
	jsize length = env->GetArrayLength(array);
	return offset <= length && size <= length - offset;
}

}  // namespace

extern "C" {

/*
 * 展開用のハンドルを作成する. 失敗時は 0.
 */
JNIEXPORT jlong JNICALL Java_src_comitton_jni_CallJniLibrary_rarOpen(JNIEnv *env, jclass clazz)
{
	std::shared_ptr<RarContext> ctx(new (std::nothrow) RarContext());
	if (!ctx) {
		return 0;
	}
	std::lock_guard<std::mutex> guard(gRegistryLock);
	jlong handle = gNextHandle++;
	gContexts[handle] = ctx;
	return handle;
}

/*
 * 圧縮データと展開データのバッファを確保する (既存のバッファは解放する).
 */
JNIEXPORT jint JNICALL Java_src_comitton_jni_CallJniLibrary_rarAlloc(JNIEnv *env, jclass clazz, jlong handle, jint cmplen, jint orglen)
{
	std::shared_ptr<RarContext> ctx = findContext(handle);
	if (!ctx) {
		return RAR_ERROR_INVALID_HANDLE;
	}
	std::lock_guard<std::mutex> guard(ctx->lock);
	if (ctx->closed) {
		return RAR_ERROR_INVALID_HANDLE;
	}
	if (cmplen < 0 || orglen < 0) {
		return RAR_ERROR_INVALID_SIZE;
	}

	ctx->freeBuffers();
	// 0 バイトの malloc は nullptr を返し得るので最低 1 バイト確保する
	ctx->fromBuff = (byte *) malloc(cmplen > 0 ? cmplen : 1);
	ctx->toBuff = (byte *) malloc(orglen > 0 ? orglen : 1);
	if (ctx->fromBuff == nullptr || ctx->toBuff == nullptr) {
		ctx->freeBuffers();
		return ERROR_CODE_MALLOC_FAILURE;
	}
	ctx->maxCompLen = cmplen;
	ctx->maxOrigLen = orglen;

	if (ctx->unpack == nullptr) {
		ctx->unpack = new (std::nothrow) Unpack(&ctx->dataIO);
		if (ctx->unpack == nullptr) {
			ctx->freeBuffers();
			return ERROR_CODE_MALLOC_FAILURE;
		}
	}
	return RAR_OK;
}

/*
 * 1 エントリ分の展開を始める.
 * winsize はエントリの辞書サイズ (バイト). 0 以下なら 4MB とみなす.
 */
JNIEXPORT jint JNICALL Java_src_comitton_jni_CallJniLibrary_rarInit(JNIEnv *env, jclass clazz, jlong handle, jint cmplen, jint orglen, jint rarver, jlong winsize, jboolean nocomp)
{
	std::shared_ptr<RarContext> ctx = findContext(handle);
	if (!ctx) {
		return RAR_ERROR_INVALID_HANDLE;
	}
	std::lock_guard<std::mutex> guard(ctx->lock);
	if (ctx->closed || ctx->fromBuff == nullptr || ctx->toBuff == nullptr) {
		return RAR_ERROR_INVALID_HANDLE;
	}
	if (cmplen < 0 || orglen < 0 || ctx->maxCompLen < cmplen || ctx->maxOrigLen < orglen) {
		return RAR_ERROR_INVALID_SIZE;
	}

	ctx->compLen = cmplen;
	ctx->origLen = orglen;
	ctx->rarVersion = rarver;
	ctx->winSize = winsize > 0 ? (uint64) winsize : (uint64) UNPACK_MAX_WRITE;
	ctx->noCompress = nocomp;
	ctx->fromPos = 0;
	ctx->toPos = 0;
	return RAR_OK;
}

/*
 * 圧縮データを追記する. 書き込んだバイト数を返す.
 */
JNIEXPORT jint JNICALL Java_src_comitton_jni_CallJniLibrary_rarWrite(JNIEnv *env, jclass clazz, jlong handle, jbyteArray cmpArray, jint offset, jint size)
{
	std::shared_ptr<RarContext> ctx = findContext(handle);
	if (!ctx) {
		return RAR_ERROR_INVALID_HANDLE;
	}
	std::lock_guard<std::mutex> guard(ctx->lock);
	if (ctx->closed || ctx->fromBuff == nullptr) {
		return RAR_ERROR_INVALID_HANDLE;
	}
	if (!validRange(env, cmpArray, offset, size)) {
		return RAR_ERROR_INVALID_SIZE;
	}

	if (ctx->compLen - ctx->fromPos < size) {
		// バッファサイズまでしか書き込まない
		size = ctx->compLen - ctx->fromPos;
	}
	if (size > 0) {
		env->GetByteArrayRegion(cmpArray, offset, size, (jbyte *) &ctx->fromBuff[ctx->fromPos]);
		ctx->fromPos += size;
	}
	return size;
}

/*
 * 書き込まれた圧縮データを展開する. 成功時 0、失敗時は負の値.
 */
JNIEXPORT jint JNICALL Java_src_comitton_jni_CallJniLibrary_rarDecomp(JNIEnv *env, jclass clazz, jlong handle)
{
	std::shared_ptr<RarContext> ctx = findContext(handle);
	if (!ctx) {
		return RAR_ERROR_INVALID_HANDLE;
	}
	std::lock_guard<std::mutex> guard(ctx->lock);
	if (ctx->closed || ctx->fromBuff == nullptr || ctx->toBuff == nullptr || ctx->unpack == nullptr) {
		return RAR_ERROR_INVALID_HANDLE;
	}
	if (ctx->fromPos < ctx->compLen) {
		// 圧縮データを最後まで受け取れていない (ファイルが途中で切れている)
		RAR_LOGE("rarDecomp: truncated input. read=%d, expected=%d", ctx->fromPos, ctx->compLen);
		return RAR_ERROR_TRUNCATED;
	}

	if (ctx->noCompress) {
		// 無圧縮の場合はそのままコピー. ヘッダの値が矛盾していれば読み越さない
		if (ctx->origLen > ctx->compLen) {
			RAR_LOGE("rarDecomp: stored entry is larger than its data. orig=%d, comp=%d", ctx->origLen, ctx->compLen);
			return RAR_ERROR_INVALID_SIZE;
		}
		memcpy(ctx->toBuff, ctx->fromBuff, ctx->origLen);
		return RAR_OK;
	}

	try {
		ComprDataIO &io = ctx->dataIO;
		io.CurUnpRead = 0;
		io.CurUnpWrite = 0;
		io.UnpVolume = false;
		io.NextVolumeMissing = false;
		io.SetPackedSizeToRead(ctx->compLen);
		io.SetUnpackFromMemory(ctx->fromBuff, ctx->compLen);
		io.SetUnpackToMemory(ctx->toBuff, ctx->origLen);

		// エントリごとの辞書サイズで初期化する (固定 4MB だと大きな辞書の RAR5/RAR7 で壊れた画像になる)
		ctx->unpack->Init(ctx->winSize, false);
		ctx->unpack->SetDestSize(ctx->origLen);
		ctx->unpack->DoUnpack(ctx->rarVersion, false);

		if (io.CurUnpWrite != (int64) ctx->origLen) {
			RAR_LOGE("rarDecomp: incomplete. written=%lld, expected=%d, version=%d",
					(long long) io.CurUnpWrite, ctx->origLen, ctx->rarVersion);
			return RAR_ERROR_INCOMPLETE;
		}
	} catch (const std::bad_alloc &) {
		RAR_LOGE("rarDecomp: out of memory. winsize=%llu", (unsigned long long) ctx->winSize);
		return ERROR_CODE_MALLOC_FAILURE;
	} catch (...) {
		RAR_LOGE("rarDecomp: unpack failed. version=%d", ctx->rarVersion);
		return RAR_ERROR_UNPACK;
	}
	return RAR_OK;
}

/*
 * 展開済みデータを読み出す. 読み出したバイト数を返す (終端では 0).
 */
JNIEXPORT jint JNICALL Java_src_comitton_jni_CallJniLibrary_rarRead(JNIEnv *env, jclass clazz, jlong handle, jbyteArray orgArray, jint offset, jint size)
{
	std::shared_ptr<RarContext> ctx = findContext(handle);
	if (!ctx) {
		return RAR_ERROR_INVALID_HANDLE;
	}
	std::lock_guard<std::mutex> guard(ctx->lock);
	if (ctx->closed || ctx->toBuff == nullptr) {
		return RAR_ERROR_INVALID_HANDLE;
	}
	if (!validRange(env, orgArray, offset, size)) {
		return RAR_ERROR_INVALID_SIZE;
	}

	if (ctx->origLen - ctx->toPos < size) {
		// バッファサイズまでしか読み込まない
		size = ctx->origLen - ctx->toPos;
	}
	if (size > 0) {
		env->SetByteArrayRegion(orgArray, offset, size, (const jbyte *) &ctx->toBuff[ctx->toPos]);
		ctx->toPos += size;
	}
	return size;
}

/*
 * 展開済みデータの読み出し位置を先頭に戻す.
 */
JNIEXPORT jint JNICALL Java_src_comitton_jni_CallJniLibrary_rarInitSeek(JNIEnv *env, jclass clazz, jlong handle)
{
	std::shared_ptr<RarContext> ctx = findContext(handle);
	if (!ctx) {
		return RAR_ERROR_INVALID_HANDLE;
	}
	std::lock_guard<std::mutex> guard(ctx->lock);
	if (ctx->closed || ctx->toBuff == nullptr) {
		return RAR_ERROR_INVALID_HANDLE;
	}
	ctx->toPos = 0;
	return RAR_OK;
}

/*
 * ハンドルを閉じる. 処理中の呼び出しがあれば終わるのを待ってから解放する.
 */
JNIEXPORT void JNICALL Java_src_comitton_jni_CallJniLibrary_rarClose(JNIEnv *env, jclass clazz, jlong handle)
{
	std::shared_ptr<RarContext> ctx;
	{
		std::lock_guard<std::mutex> guard(gRegistryLock);
		auto it = gContexts.find(handle);
		if (it == gContexts.end()) {
			return;
		}
		ctx = it->second;
		gContexts.erase(it);
	}
	std::lock_guard<std::mutex> guard(ctx->lock);
	ctx->closed = true;
	ctx->freeBuffers();
	// 本体は最後の参照が外れたときに解放される
}

}
