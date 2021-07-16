package com.zboot.vpn.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.android.billingclient.api.*
//import com.appodeal.ads.Appodeal
import com.dzboot.vpn.helpers.AdsHelper
import com.google.android.material.navigation.NavigationView
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.zboot.vpn.BuildConfig
import com.zboot.vpn.R
import com.zboot.vpn.base.BaseActivity
import com.zboot.vpn.base.BaseFragmentInterface
import com.zboot.vpn.custom.Constants
import com.zboot.vpn.databinding.ActivityMainBinding
import com.zboot.vpn.fragments.*
import com.zboot.vpn.helpers.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : BaseActivity<ActivityMainBinding>(), NavigationView.OnNavigationItemSelectedListener,
	PurchasesUpdatedListener, BillingClientStateListener {

	companion object {

		private const val SAVED_FRAGMENT = "saved_fragment"
		private const val UPDATE_REQUEST_CODE = 63
	}

	private val drawerToggle: ActionBarDrawerToggle by lazy {
		ActionBarDrawerToggle(this, binding.drawerLayout, R.string.open_drawer, R.string.close_drawer)
	}

	val billingClient: BillingClient by lazy {
		BillingClient.newBuilder(this).enablePendingPurchases().setListener(this).build()
	}

	private val appUpdateManager by lazy { AppUpdateManagerFactory.create(this) }
	var currentFragment: BaseFragmentInterface = MainFragment()


	override fun initializeBinding(): ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)

	//region Lifecycle
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

/*		val appKey = BuildConfig.APPODEAL
		Appodeal.setTesting(true)
		Appodeal.setLogLevel(com.appodeal.ads.utils.Log.LogLevel.debug)
		Appodeal.initialize(this,appKey, Appodeal.INTERSTITIAL or Appodeal.BANNER)
		Timber.d("appodeal inisialisasi")

		Appodeal.setBannerViewId(R.id.appodealBanner)
		Appodeal.show(this, Appodeal.BANNER_VIEW)
		Appodeal.cache(this, Appodeal.INTERSTITIAL);*/

		if (PrefsHelper.isFirstRun()) {
			startActivity(Intent(this, IntroActivity::class.java))
			finish()
			return
		}

		checkAppUpdate()

		billingClient.startConnection(this)

		binding.drawerLayout.addDrawerListener(drawerToggle)
		drawerToggle.syncState()
		binding.navView.setCheckedItem(R.id.nav_home)
		binding.navView.setNavigationItemSelectedListener(this)
		binding.share.setOnClickListener { Utils.shareApp(this) }
		binding.rate.setOnClickListener { Utils.openPlayStoreAppPage(this) }
		binding.purchase.setOnClickListener { changeFragment(SubscriptionFragment()) }

		binding.navView.getHeaderView(0).findViewById<TextView>(R.id.version).text =
				getString(R.string.version_1_s, BuildConfig.VERSION_NAME)

		binding.drawerButton.setOnClickListener {
			if (currentFragment is MainFragment)
				binding.drawerLayout.openDrawer(GravityCompat.START, true)
			else
				backFromAllowedAppsFragment()
		}

		if (savedInstanceState == null) {
			addMainFragment()
		} else {
			changeFragment(supportFragmentManager.getFragment(savedInstanceState, SAVED_FRAGMENT) as BaseFragmentInterface)
		}


	}

	private fun addMainFragment() {
		val transaction = supportFragmentManager.beginTransaction()
		currentFragment = MainFragment()
		//			supportFragmentManager.findFragmentByTag(PreferencesFragment.TAG)?.let { transaction.remove(it) }
		transaction.add(R.id.fragment_container, currentFragment as MainFragment, currentFragment.toString())
				.addToBackStack(null)
				.commit()
	}

	fun switchToMainFragment(promptDisconnect: Boolean = false) {
		val mainFragment = supportFragmentManager.findFragmentByTag(MainFragment.TAG) as MainFragment?
		if (mainFragment == null) {
			addMainFragment()
		} else {
			val bundle = Bundle()
			bundle.putBoolean(MainFragment.PROMPT_DISCONNECT, promptDisconnect)
			mainFragment.arguments = bundle
			changeFragment(mainFragment)
		}
	}

	override fun onResume() {
		super.onResume()

		appUpdateManager.appUpdateInfo.addOnSuccessListener {
			if (it.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
				try {
					appUpdateManager.startUpdateFlowForResult(it, AppUpdateType.IMMEDIATE, this, UPDATE_REQUEST_CODE)
				} catch (ignore: Exception) {
					//TODO need to send custom log to crashlytics
				}
			}
		}
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		if (requestCode == UPDATE_REQUEST_CODE) {
			if (resultCode != RESULT_OK) {
				DialogHelper.newUpdateRequired(this) { checkAppUpdate() }
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data)
		}
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)

		supportFragmentManager.putFragment(outState, SAVED_FRAGMENT, currentFragment as Fragment)
	}

	override fun onBackPressed() {
		if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
			binding.drawerLayout.closeDrawer(GravityCompat.START)
			return
		}

		if (currentFragment is MainFragment) {
			if ((currentFragment as MainFragment).isListVisible)
				(currentFragment as MainFragment).hideServers()
			else
				finish()
		} else
			backFromAllowedAppsFragment()
	}

	override fun onDestroy() {
		AdsHelper.destroy()
		super.onDestroy()
	}
	//endregion

	//region Billing
	suspend fun querySkuDetails(): SkuDetailsResult {
		val skuList = ArrayList<String>()
		skuList.add(Constants.MONTHLY_SUBSCRIPTION)
		skuList.add(Constants.YEARLY_SUBSCRIPTION)
		val params = SkuDetailsParams.newBuilder()
		params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS)
		return billingClient.querySkuDetails(params.build())
	}

	fun launchPurchaseFlow(skuDetails: SkuDetails) {
		val flowParams = BillingFlowParams.newBuilder().setSkuDetails(skuDetails).build()
		if (billingClient.launchBillingFlow(this, flowParams).responseCode != BillingClient.BillingResponseCode.OK)
			Toast.makeText(this, R.string.purchase_flow_fail, Toast.LENGTH_SHORT).show()
	}

	override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
		if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED)
			return

		var subscribed = false
		if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !purchases.isNullOrEmpty()) {
			for (purchase in purchases) {
				if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
					subscribed = true
					if (!purchase.isAcknowledged) {
						val ackParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken)
						CoroutineScope(Dispatchers.IO).launch { billingClient.acknowledgePurchase(ackParams.build()) }
					}
				}
			}
		}

		AdsHelper.deliverContent(this, binding.adViewLayout, subscribed, true)
	}

	override fun onBillingSetupFinished(billingResult: BillingResult) {
		var subscribed = false
		val canSubscribe = (if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
			val purchases = billingClient.queryPurchases(BillingClient.SkuType.SUBS).purchasesList
			if (!purchases.isNullOrEmpty())
				subscribed = true
			true
		} else false) && (billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS).responseCode
				!= BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED)

		AdsHelper.deliverContent(this, binding.adViewLayout, subscribed, canSubscribe)
	}

	override fun onBillingServiceDisconnected() {
		//TODO restart connection!!
	}

	fun showPurchase() = binding.purchase.visible()
	//endregion

	fun updateLocale(locale: Locale) = localeDelegate.setLocale(this, locale)

	override fun onNavigationItemSelected(item: MenuItem): Boolean {
		AdsHelper.showInterstitialAd(this)
		val newFragment = when (item.itemId) {
			R.id.nav_allowed_apps -> AllowedAppsFragment()
			R.id.nav_settings -> PreferencesFragment()
			R.id.nav_privacy -> getDisplayFragment(DisplayFragment.DisplayType.PRIVACY_POLICY)
			R.id.nav_terms -> getDisplayFragment(DisplayFragment.DisplayType.TERMS)
			//         R.id.nav_purchase -> SubscriptionFragment()
			else -> MainFragment()
		}

		binding.drawerLayout.closeDrawer(GravityCompat.START, true)
		changeFragment(newFragment)
		return true
	}

	private fun getDisplayFragment(type: String): DisplayFragment<MainActivity> {
		val fragment = DisplayFragment<MainActivity>()
		val args = Bundle()
		args.putString(DisplayFragment.TYPE, type)
		fragment.arguments = args
		return fragment
	}

	override fun changeFragment(newFragment: BaseFragmentInterface) {
		if (currentFragment == newFragment)
			return

		currentFragment = newFragment

		if (currentFragment is MainFragment) {
			binding.pageTitle.text = ""
			binding.share.visible()
			binding.rate.visible()
			binding.drawerButton.setImageDrawable(getDrawableCompat(R.drawable.ic_baseline_menu_24))
			binding.drawerButton.contentDescription = getString(R.string.open_drawer)

			if (AdsHelper.showAds) binding.purchase.visible() else binding.purchase.gone()
		} else {
			binding.pageTitle.setText(currentFragment.getPageTitle())
			binding.share.gone()
			binding.rate.gone()
			binding.purchase.gone()
			binding.drawerButton.setImageDrawable(getDrawableCompat(R.drawable.ic_baseline_arrow_back_24))
			binding.drawerButton.contentDescription = getString(R.string.go_back)
		}

		supportFragmentManager.beginTransaction()
				.replace(R.id.fragment_container, currentFragment as Fragment, currentFragment.toString())
				.commit()
	}

	private fun backFromAllowedAppsFragment() {
		if (currentFragment is AllowedAppsFragment)
			switchToMainFragment((currentFragment as AllowedAppsFragment).hasConfigChanged())
		else
			switchToMainFragment()
	}

	private fun checkAppUpdate() {
		appUpdateManager.appUpdateInfo.addOnSuccessListener {
			if (it.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
			    && it.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE))
				appUpdateManager.startUpdateFlowForResult(it, AppUpdateType.IMMEDIATE, this, UPDATE_REQUEST_CODE)
		}
	}
}