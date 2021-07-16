package com.dzboot.vpn.helpers

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.widget.FrameLayout
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
//import com.appodeal.ads.Appodeal
import com.google.ads.consent.*
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.zboot.vpn.BuildConfig
import com.zboot.vpn.activities.MainActivity
import com.zboot.vpn.base.BaseApplication
import com.zboot.vpn.fragments.SubscriptionFragment
import com.zboot.vpn.helpers.PrefsHelper
import com.zboot.vpn.helpers.Utils.isRunningOnTV
import com.zboot.vpn.helpers.gone
import com.zboot.vpn.helpers.visible
import io.grpc.InternalChannelz.id
import timber.log.Timber
import java.net.MalformedURLException
import java.net.URL


//TODO memory leak here
@SuppressLint("StaticFieldLeak")
object AdsHelper : ActivityLifecycleCallbacks, LifecycleObserver {

	private const val DISABLE_ADS = !BuildConfig.SHOW_ADS
	private const val RELOAD_AD_PERIOD = 10000L

	//TODO need to add GDPR consent reset ability
	//disable GDPR Consent at developing stage
	private const val DISABLE_GDPR_CONSENT = !BuildConfig.SHOW_ADS

	//OPPO test device id
	private const val TEST_DEVICE_ID = "F40D8D0517577C9DFC3EDD5973A08B2B"

	//Ads test devices
	private val TEST_DEVICES = listOf(TEST_DEVICE_ID)

	//Admob publisher id
	private const val ADMOB_PUB_ID = "ca-app-pub-3940256099942544"

	private const val APP_OPEN_AD_VALIDITY_HOURS = 4

	private var isShowingAd = true
	private var AppOpenAdLoadTime: Long = 0
	private var appOpenAd: AppOpenAd? = null
	private var currentActivity: Activity? = null

	private lateinit var consentForm: ConsentForm
	private var interstitialAd: InterstitialAd? = null
	private var bannerAd: AdView? = null
	private var rewardedAd: RewardedAd? = null

	private var showAdCount = 0
	var showAds = false

	private fun fetchAppOpenAd() {
		if (isAppOpenAdAvailable()) return

		val loadCallback = object : AppOpenAd.AppOpenAdLoadCallback() {
			override fun onAdLoaded(appOpenAd: AppOpenAd) {
				super.onAdLoaded(appOpenAd)
				AdsHelper.appOpenAd = appOpenAd
				AppOpenAdLoadTime = System.currentTimeMillis()
			}

			override fun onAdFailedToLoad(loadAdError: LoadAdError) {
				super.onAdFailedToLoad(loadAdError)
				Timber.w("AppOpen ad failed to load: %s", loadAdError.message)
				Handler(Looper.getMainLooper()).postDelayed({ fetchAppOpenAd() }, RELOAD_AD_PERIOD)
			}
		}

		//TODO use GDPR
		val request = AdRequest.Builder().build()
		AppOpenAd.load(
				BaseApplication.instance,
				BuildConfig.ADMOB_APP_OPEN_KEY,
				request,
				AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
				loadCallback
		)
	}

	/**
	 * Utility method to check if ad was loaded more than n hours ago.
	 */
	private fun isAppOpenAdValid() = System.currentTimeMillis() - AppOpenAdLoadTime < 3600000 * APP_OPEN_AD_VALIDITY_HOURS

	/**
	 * Utility method that checks if ad exists and can be shown.
	 */
	private fun isAppOpenAdAvailable() = appOpenAd != null && isAppOpenAdValid()

	/**
	 * Shows the ad if one isn't already showing.
	 */
	private fun showAppOpenAdIfAvailable() {
		if (showAds && !isShowingAd && isAppOpenAdAvailable()) {
			appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
				override fun onAdDismissedFullScreenContent() {
					// Set the reference to null so isAdAvailable() returns false.
					appOpenAd = null
					isShowingAd = false
					fetchAppOpenAd()
				}

				override fun onAdFailedToShowFullScreenContent(adError: AdError) {}
				override fun onAdShowedFullScreenContent() {
					isShowingAd = true
				}
			}
			currentActivity?.let { appOpenAd?.show(it) }
		} else {
			Timber.d("Can not show ad.")
			fetchAppOpenAd()
		}
	}

	override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
	override fun onActivityStarted(activity: Activity) {
		currentActivity = activity
	}

	override fun onActivityResumed(activity: Activity) {
		currentActivity = activity
	}

	override fun onActivityPaused(activity: Activity) {}
	override fun onActivityStopped(activity: Activity) {}
	override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
	override fun onActivityDestroyed(activity: Activity) {
		currentActivity = null
	}

	private fun getGDPRConsent(activity: MainActivity, adViewLayout: FrameLayout) {
		if (DISABLE_GDPR_CONSENT) {
			initializeAds(activity, adViewLayout)
			return
		}

		val consentInfo = ConsentInformation.getInstance(activity)
		if (BuildConfig.DEBUG) {
			consentInfo.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
			consentInfo.addTestDevice(TEST_DEVICE_ID)
			consentInfo.debugGeography = DebugGeography.DEBUG_GEOGRAPHY_EEA
		}

		consentInfo.requestConsentInfoUpdate(arrayOf(ADMOB_PUB_ID), object : ConsentInfoUpdateListener {
			override fun onFailedToUpdateConsentInfo(reason: String?) {
				Timber.w("Failed to update consent info: $reason")
				initializeAds(activity, adViewLayout)
			}

			override fun onConsentInfoUpdated(consentStatus: ConsentStatus?) {
				if (consentInfo.isRequestLocationInEeaOrUnknown)
					when (consentStatus) {
						ConsentStatus.PERSONALIZED -> initializeAds(activity, adViewLayout)
						ConsentStatus.NON_PERSONALIZED -> initializeAds(activity, adViewLayout, false)
						else -> displayConsentForm(activity, adViewLayout)
					}
				else
					initializeAds(activity, adViewLayout)
			}
		})
	}

	private fun displayConsentForm(activity: MainActivity, adViewLayout: FrameLayout) {
		var privacyUrl: URL? = null
		try {
			privacyUrl = URL("http://www.google.com")
		} catch (e: MalformedURLException) {
			Timber.e(e, "Error processing privacy policy url")
		}

		consentForm = ConsentForm.Builder(activity, privacyUrl)
				.withListener(object : ConsentFormListener() {
					override fun onConsentFormLoaded() {
						// Consent form loaded successfully.
						if (!activity.isFinishing && !activity.isDestroyed)
							consentForm.show()
					}

					override fun onConsentFormOpened() {
						// Consent form was displayed.
					}

					override fun onConsentFormClosed(consentStatus: ConsentStatus, userPrefersAdFree: Boolean?) {
						initializeAds(activity, adViewLayout)
					}

					override fun onConsentFormError(errorDescription: String) {
						// Consent form error. This usually happens if the user is not in the EU.
						Timber.e("Error loading consent form: $errorDescription")
						initializeAds(activity, adViewLayout)
					}
				})
				.withPersonalizedAdsOption()
				.withNonPersonalizedAdsOption()
				.build()
		consentForm.load()
	}

	private fun initializeAds(activity: MainActivity, adViewLayout: FrameLayout, isPersonalized: Boolean = true) {
		val isRunningOnTV = activity.isRunningOnTV()
		val outMetrics = DisplayMetrics()
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
			activity.display?.getMetrics(outMetrics)
		} else {
			activity.windowManager.defaultDisplay.getMetrics(outMetrics)
		}
		val width = (adViewLayout.width.toFloat().let {
			if (it == 0f)
				outMetrics.widthPixels.toFloat()
			else
				it
		} / outMetrics.density).toInt()

		bannerAd = AdView(activity)
		bannerAd?.apply {
			adUnitId = BuildConfig.ADMOB_BANNER_KEY
			adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, width)
			adListener = object : AdListener() {
				override fun onAdClosed() {
					super.onAdClosed()
					loadBannerAd(isPersonalized, isRunningOnTV)
				}

				override fun onAdClicked() {
					super.onAdClicked()
					loadBannerAd(isPersonalized, isRunningOnTV)
				}

				override fun onAdLoaded() {
					super.onAdLoaded()
					Timber.d("Banner ad loaded successfully")

//					Appodeal.hide(activity,Appodeal.BANNER)
				}

				override fun onAdFailedToLoad(loadAdError: LoadAdError) {
					super.onAdFailedToLoad(loadAdError)
					Timber.w("Banner Ad failed to load: ${loadAdError.message}")



					Handler(Looper.getMainLooper()).postDelayed({
						loadBannerAd(isPersonalized, isRunningOnTV) },RELOAD_AD_PERIOD
					)


				}
			}
		}

		adViewLayout.removeAllViews()
		adViewLayout.addView(bannerAd)
		loadInterstitialAd(activity, isPersonalized)
		loadBannerAd(isPersonalized, isRunningOnTV)
	}

	private fun getAdRequest(isPersonalized: Boolean): AdRequest {
		return with(AdRequest.Builder()) {
			if (isPersonalized) {
				build()
			} else {
				val extras = Bundle()
				extras.putString("npa", "1")
				addNetworkExtrasBundle(AdMobAdapter::class.java, extras).build()
			}
		}
	}

	private fun loadInterstitialAd(activity: MainActivity, isPersonalized: Boolean) {
		if (DISABLE_ADS || !showAds) {
			Timber.d("Won't load interstitial ad")
			return
		}

		InterstitialAd.load(
				activity,
				BuildConfig.ADMOB_INTERSTITIAL_KEY,
				getAdRequest(isPersonalized),
				object : InterstitialAdLoadCallback() {
					override fun onAdLoaded(ad: InterstitialAd) {
						Timber.d("Interstitial ad loaded successfully")
						ad.fullScreenContentCallback = object : FullScreenContentCallback() {
							override fun onAdDismissedFullScreenContent() {
								super.onAdDismissedFullScreenContent()
								loadInterstitialAd(activity, isPersonalized)

							}

							override fun onAdFailedToShowFullScreenContent(adError: AdError) {
								super.onAdFailedToShowFullScreenContent(adError)

								Timber.d("Admob Inters failed ${adError.message}")
								loadInterstitialAd(activity, isPersonalized)



							}

							override fun onAdShowedFullScreenContent() {
								super.onAdShowedFullScreenContent()
								loadInterstitialAd(activity, isPersonalized)
							}
						}
						interstitialAd = ad
					}

					override fun onAdFailedToLoad(loadAdError: LoadAdError) {
						super.onAdFailedToLoad(loadAdError)
						Timber.w("Interstitial Ad failed to load: ${loadAdError.message}")
//						Appodeal.show(activity, Appodeal.INTERSTITIAL)
						Handler(Looper.getMainLooper()).postDelayed(
							{ loadInterstitialAd(activity, isPersonalized) },
								RELOAD_AD_PERIOD
						)
					}
				})
	}

	private fun loadBannerAd(isPersonalized: Boolean, isRunningOnTV: Boolean) {
		if (DISABLE_ADS || !showAds || isRunningOnTV) {
			Timber.d("Won't load banner ad")
			return
		}

		bannerAd?.loadAd(getAdRequest(isPersonalized))
	}

	fun init() {
		if (DISABLE_ADS)
			return

		MobileAds.initialize(BaseApplication.instance) {
			val configuration = RequestConfiguration.Builder()
					.setTestDeviceIds(TEST_DEVICES)
					.build()
			MobileAds.setRequestConfiguration(configuration)


		}



		BaseApplication.instance.registerActivityLifecycleCallbacks(this)
		ProcessLifecycleOwner.get().lifecycle.addObserver(this)
	}

	fun showInterstitialAd(activity: Activity?) {
		if (interstitialAd != null) {

			if (!showAds || showAdCount++ == 2) {
				//only show ads twice out of 3 times
				showAdCount = 0
				Timber.d("Ad count  $showAdCount")
				return
			}

			activity?.let { interstitialAd?.show(it) }
		} else {
			Timber.d("Inters admob null")
//			activity?.let {Appodeal.show(it,Appodeal.INTERSTITIAL)}
		}

	}

	fun showRewardedAd(activity: Activity) {
		rewardedAd?.show(activity) { }
	}

	/**
	 * LifecycleObserver methods
	 */
	@OnLifecycleEvent(Lifecycle.Event.ON_START)
	fun onResume() {
		//prevent showing ad on app first start
		if (!PrefsHelper.isFirstRun()) showAppOpenAdIfAvailable()
	}

	fun deliverContent(activity: MainActivity, adViewLayout: FrameLayout, subscribed: Boolean, canSubscribe: Boolean) {
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
				activity.showPurchase()

			getGDPRConsent(activity, adViewLayout)
		}
	}

	fun destroy() {
		bannerAd?.destroy()
	}
}