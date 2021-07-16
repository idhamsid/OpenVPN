package com.zboot.vpn.helpers

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.zboot.vpn.BuildConfig
import com.zboot.vpn.base.BaseApplication


object PrefsHelper {

	private const val FIRST_RUN = "first_run"
	private const val FIRST_CONNECT = "first_connect"
	private const val APPS_NOT_USING = "not_allowed_apps"
	private const val LAST_SERVERS_UPDATE = "last_servers_update"
	private const val CONNECT_SERVER_ID = "connect_server_id"
	private const val ORIGINAL_IP = "original_ip"

	private val sp: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(BaseApplication.instance) }

	private fun getEditor(): SharedPreferences.Editor = sp.edit()

	fun isFirstRun() = sp.getBoolean(FIRST_RUN, true)
	fun setIsFirstRun() = getEditor().putBoolean(FIRST_RUN, true).apply()
	fun disableFirstRun() = getEditor().putBoolean(FIRST_RUN, false).apply()

	fun isFirstConnect() = sp.getBoolean(FIRST_CONNECT, true)
	fun disableFirstConnect() = getEditor().putBoolean(FIRST_CONNECT, false).apply()


	fun getLastUpdate() = sp.getString(LAST_SERVERS_UPDATE, null)
	fun saveLastUpdate(lastUpdate: String) = getEditor().putString(LAST_SERVERS_UPDATE, lastUpdate).apply()

	fun setAllAppsUsing() = getEditor().putStringSet(APPS_NOT_USING, null).apply()
	fun saveAppsNotUsing(inactiveApps: HashSet<String>) = getEditor().putStringSet(APPS_NOT_USING, inactiveApps).apply()
	fun getAppsNotUsing(): HashSet<String> = sp.getStringSet(APPS_NOT_USING, HashSet<String>()) as HashSet<String>

	fun getDisplayMode() = sp.getString(BuildConfig.DISPLAY_MODE_KEY, "default")
	fun getLanguage() = sp.getString(BuildConfig.LANG_KEY, "default")

	fun isPersistentNotif() = sp.getBoolean(BuildConfig.PERSISTENT_NOTIF_KEY, true)

	fun getSavedServer() = sp.getInt(CONNECT_SERVER_ID, -1)
	fun saveServer(id: Int) = getEditor().putInt(CONNECT_SERVER_ID, id).apply()

	fun saveOriginalIP(ip: String?) = sp.edit().putString(ORIGINAL_IP, ip).apply()
	fun getOriginalIP() = sp.getString(ORIGINAL_IP, null)
}