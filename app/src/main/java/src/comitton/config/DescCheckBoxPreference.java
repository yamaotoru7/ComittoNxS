package src.comitton.config;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.CheckBoxPreference;

/**
 * XML の summary を説明文として保持し、実行時に setSummary() で渡された現在値を
 * 「説明 + 現在値」の形で表示する CheckBoxPreference。
 */
public class DescCheckBoxPreference extends CheckBoxPreference {
	private final CharSequence mDescription;

	public DescCheckBoxPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		mDescription = PreferenceSummary.pickDescription(super.getSummary());
		if (mDescription == null) {
			super.setSummary(null);
		}
	}

	@Override
	public void setSummary(CharSequence summary) {
		super.setSummary(PreferenceSummary.compose(getContext(), mDescription, summary));
	}
}
