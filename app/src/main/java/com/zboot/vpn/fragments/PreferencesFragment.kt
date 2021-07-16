package com.zboot.vpn.fragments

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.zboot.vpn.BuildConfig
import com.zboot.vpn.R
import com.zboot.vpn.activities.MainActivity
import com.zboot.vpn.base.BaseFragmentInterface
import com.zboot.vpn.helpers.LocalesHelper
import com.zboot.vpn.helpers.NotifsHelper
import com.zboot.vpn.helpers.PrefsHelper
import com.zboot.vpn.helpers.ThemeHelper


class PreferencesFragment : PreferenceFragmentCompat(), BaseFragmentInterface,
	SharedPreferences.OnSharedPreferenceChangeListener {

	companion object {

		const val TAG = "PreferencesFragment"
	}

	private val displayPref by lazy { findPreference<ListPreference>(BuildConfig.DISPLAY_MODE_KEY) }

	override fun getPageTitle() = R.string.settings
	override fun toString() = TAG


	override fun onResume() {
		super.onResume()
		preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
	}

	override fun onPause() {
		super.onPause()
		preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
	}

	private fun setDisplayModeSummary(themeMode: String) {
		displayPref?.summary =
				getString(R.string.display_mode_summary, getString(ThemeHelper.getDisplayModeResId(themeMode)))
	}

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		setPreferencesFromResource(R.xml.preferences, rootKey)

		val langPref = findPreference<ListPreference>(BuildConfig.LANG_KEY)
		langPref?.entries = LocalesHelper.getLanguagesEntries(requireContext())
		langPref?.entryValues = LocalesHelper.getLanguagesValues()
		langPref?.summary = getString(R.string.lang_summary, LocalesHelper.getDefaultLanguage(requireContext()))

		displayPref?.entries = ThemeHelper.getLanguagesEntries(requireContext())
		displayPref?.entryValues = ThemeHelper.getDisplayValues()
		PrefsHelper.getDisplayMode()?.let { setDisplayModeSummary(it) }
	}

	override fun onSharedPreferenceChanged(sp: SharedPreferences, key: String) {

		when (key) {
			BuildConfig.LANG_KEY -> sp.getString(key, "default")?.let { setLanguage(it) }
			BuildConfig.DISPLAY_MODE_KEY -> sp.getString(key, "default")?.let { setTheme(it) }
			BuildConfig.PERSISTENT_NOTIF_KEY -> setPersistentNotifications(sp.getBoolean(key, true))
		}
	}

	private fun setLanguage(value: String) =
			(activity as MainActivity).updateLocale(LocalesHelper.getLocaleFromLanguageCode(value))

	private fun setTheme(value: String) {
		setDisplayModeSummary(value)
		ThemeHelper.applyTheme(value)
	}

	private fun setPersistentNotifications(value: Boolean) {
		if (value)
			NotifsHelper.showPersistentNotification(requireContext())
		else
			NotifsHelper.cancelPersistentNotification(requireContext())
	}
}