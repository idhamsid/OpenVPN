package com.zboot.vpn.helpers

import android.content.Context
import android.content.res.Resources
import androidx.core.os.ConfigurationCompat
import com.zboot.vpn.R
import java.util.*


object LocalesHelper {

	private val LOCALES = arrayListOf(
			"default", "en", "ar", "fr", "fa", "ru", "tk", "zh", "tr", "xx"
	)

	private fun getDisplayLanguage(context: Context, code: String?): String =
			if (code.isNullOrBlank() || code == "default")
				context.getString(R.string.default_mode)
			else
				Locale(code).getDisplayLanguage(Locale(code))

	fun getLanguagesEntries(context: Context) = LOCALES.map { getDisplayLanguage(context, it) }.toTypedArray()

	fun getLanguagesValues() = LOCALES.toTypedArray()

	fun getDefaultLanguage(context: Context) = getDisplayLanguage(context, PrefsHelper.getLanguage())

	fun getLocaleFromLanguageCode(code: String?): Locale =
			if (code.isNullOrBlank())
				ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0]
			else
				Locale(code)
}