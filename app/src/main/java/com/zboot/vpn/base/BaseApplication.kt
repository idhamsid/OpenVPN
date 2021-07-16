package com.zboot.vpn.base

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import androidx.multidex.MultiDex
//import com.appodeal.ads.Appodeal
//import com.appodeal.ads.AppodealNetworks.APPODEAL
import com.dzboot.vpn.helpers.AdsHelper
import com.google.android.play.core.missingsplits.MissingSplitsManagerFactory
import com.google.firebase.messaging.FirebaseMessaging
import com.zboot.vpn.BuildConfig
import com.zboot.vpn.helpers.NotifsHelper
import com.zeugmasolutions.localehelper.LocaleHelper
import com.zeugmasolutions.localehelper.LocaleHelperApplicationDelegate
import de.blinkt.openvpn.OpenVPNApplication
import timber.log.Timber


class BaseApplication : OpenVPNApplication() {

	companion object {

		lateinit var instance: BaseApplication
	}


	//region LocaleHelper
	private val localeAppDelegate = LocaleHelperApplicationDelegate()

	override fun attachBaseContext(baseContext: Context) {
		super.attachBaseContext(localeAppDelegate.attachBaseContext(baseContext))
		MultiDex.install(this)
	}

	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		localeAppDelegate.onConfigurationChanged(this)
	}

	override fun getApplicationContext(): Context = LocaleHelper.onAttach(super.getApplicationContext())
	//endregion

	override fun onCreate() {

		//check for missing components (sideloaded apps)
		if (MissingSplitsManagerFactory.create(this).disableAppIfMissingRequiredSplits()) {
			return
		}

		super.onCreate()
		instance = this

		if (BuildConfig.DEBUG) {
			FirebaseMessaging.getInstance().subscribeToTopic("debug")
			Timber.plant(Timber.DebugTree())
//			enableStrictModes()
		} else {
			FirebaseMessaging.getInstance().subscribeToTopic("all")
		}

		AdsHelper.init()



		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			NotifsHelper.createNotificationChannels(this)

		NotifsHelper.showPersistentNotification(this)
	}

	private fun enableStrictModes() {
		val policy = VmPolicy.Builder()
				.detectAll()
				.penaltyLog() //            .penaltyDeath()
				.build()
		StrictMode.setVmPolicy(policy)
	}
}