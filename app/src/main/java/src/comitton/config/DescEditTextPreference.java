package src.comitton.config;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.EditTextPreference;

/**
 * XML の summary を説明文として保持し、実行時に setSummary() で渡された現在値を
 * 「説明 + 現在値」の形で表示する EditTextPreference。
 */
public class DescEditTextPreference extends EditTextPreference {
	private final CharSequence mDescription;

	public DescEditTextPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		mDescription = PreferenceSummary.pickDescription(super.getSummary(), getDialogMessage());
		if (PreferenceSummary.isPlaceholder(super.getSummary()) && mDescription != null) {
			super.setSummary(mDescription);
		}
	}

	@Override
	public void setSummary(CharSequence summary) {
		super.setSummary(PreferenceSummary.compose(getContext(), mDescription, summary));
	}
}
