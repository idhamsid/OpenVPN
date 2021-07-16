package com.dzboot.vpn.helpers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.FrameLayout
import com.facebook.ads.*
import com.zboot.vpn.BuildConfig
import com.zboot.vpn.activities.MainActivity
import com.zboot.vpn.fragments.SubscriptionFragment
import com.zboot.vpn.helpers.Utils.isRunningOnTV
import com.zboot.vpn.helpers.gone
import com.zboot.vpn.helpers.visible
import timber.log.Timber


object AdsHelper : AudienceNetworkAds.InitListener {

	private const val DISABLE_ADS = !BuildConfig.SHOW_ADS

	private var isRunningOnTV = false
	private var interstitialAd: InterstitialAd? = null
	private var bannerAdView: AdView? = null

	private val interstitialAdListener = object : InterstitialAdListener {

		override fun onError(p0: Ad?, loadAdError: AdError?) {
			Timber.w("Interstitial Ad failed to load: ${loadAdError?.errorCode}-> ${loadAdError?.errorMessage}")
			Handler(Looper.getMainLooper()).postDelayed({ loadInterstitialAd() }, 5000)
		}

		override fun onAdLoaded(p0: Ad?) {
			Timber.d("Interstitial loaded successfully")
		}

		override fun onAdClicked(p0: Ad?) {
			loadInterstitialAd()
		}

		override fun onLoggingImpression(p0: Ad?) {
		}

		override fun onInterstitialDisplayed(p0: Ad?) {
			loadInterstitialAd()
		}

		override fun onInterstitialDismissed(p0: Ad?) {
			loadInterstitialAd()
		}
	}
	private val bannerAdListener = object : AdListener {

		override fun onError(p0: Ad?, loadAdError: AdError?) {
			Timber.w("Banner Ad failed to load: ${loadAdError?.errorMessage}->${loadAdError?.errorMessage}")
			Handler(Looper.getMainLooper()).postDelayed({ loadBannerAd() }, 5000)
		}

		override fun onAdLoaded(p0: Ad?) {
			Timber.d("banner loaded successfully")
		}

		override fun onAdClicked(p0: Ad?) {
			loadBannerAd()
		}

		override fun onLoggingImpression(p0: Ad?) {
		}
	}

	private var showAdCount = 0
	var showAds = false


	override fun onInitialized(result: AudienceNetworkAds.InitResult?) {
		Timber.d(result?.message)
	}

	fun init(context: Context) {
		if (DISABLE_ADS)
			return

		if (!AudienceNetworkAds.isInitialized(context)) {
			if (BuildConfig.DEBUG) {
				AdSettings.turnOnSDKDebugger(context)
				AdSettings.addTestDevice("c6545850-8771-44e8-a9fd-a2248aca786c")
			}

			AudienceNetworkAds.buildInitSettings(context)
					.withInitListener(this)
					.initialize()
		}
	}

	fun deliverContent(
			activity: MainActivity,
			adViewLayout: FrameLayout,
			subscribed: Boolean,
			canSubscribe: Boolean,
			showPurchase: () -> Unit
	) {
		if (subscribed) {
			showAds = false
			adViewLayout.gone()

			if (activity.currentFragment is SubscriptionFragment)
				activity.switchToMainFragment()
		} else {
			showAds = true

			if (!activity.isRunningOnTV())
			//don't show banner on TV
				adViewLayout.visible()

			if (canSubscribe)
				showPurchase()

			initializeAds(activity, adViewLayout)
		}
	}

	fun destroy() {
		bannerAdView?.destroy()
		interstitialAd?.destroy()
	}

	fun showInterstitialAd() {
		if (!showAds || showAdCount++ % 2 != 0)
			return

		interstitialAd?.let {
			if (it.isAdLoaded && !it.isAdInvalidated)
				it.show()
		}
	}

	private fun initializeAds(activity: MainActivity, adViewLayout: FrameLayout) {
		isRunningOnTV = activity.isRunningOnTV()

		bannerAdView = AdView(activity, BuildConfig.FAN_BANNER_KEY, AdSize.BANNER_HEIGHT_50)
		interstitialAd = InterstitialAd(activity, BuildConfig.FAN_INTERSTITIAL_KEY)

		adViewLayout.removeAllViews()
		adViewLayout.addView(bannerAdView)
		loadInterstitialAd()
		loadBannerAd()
	}

	private fun loadInterstitialAd() {
		if (DISABLE_ADS || !showAds) {
			Timber.d("Won't load interstitial ad")
			return
		}

		interstitialAd?.loadAd(
				interstitialAd?.buildLoadAdConfig()
						?.withAdListener(interstitialAdListener)
						?.build()
		)
	}

	private fun loadBannerAd() {
		if (DISABLE_ADS || !showAds || isRunningOnTV) {
			Timber.d("Won't load banner ad")
			return
		}

		bannerAdView?.loadAd(
				bannerAdView?.buildLoadAdConfig()
						?.withAdListener(bannerAdListener)
						?.build()
		)
	}
}