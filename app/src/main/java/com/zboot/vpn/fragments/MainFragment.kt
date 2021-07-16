package com.zboot.vpn.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.*
import android.graphics.Color
import android.os.*
import android.provider.Settings
import android.view.View
import android.view.animation.Animation
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.lifecycleScope
import androidx.multidex.BuildConfig
import com.dzboot.vpn.helpers.AdsHelper
import com.github.ybq.android.spinkit.sprite.Sprite
import com.github.ybq.android.spinkit.style.DoubleBounce
import com.github.ybq.android.spinkit.style.Wave
import com.google.android.play.core.review.ReviewManagerFactory
import com.zboot.vpn.R
import com.zboot.vpn.activities.MainActivity
import com.zboot.vpn.adapters.ServersAdapter
import com.zboot.vpn.base.BaseFragment
import com.zboot.vpn.custom.ReverseInterpolator
import com.zboot.vpn.databinding.FragmentMainBinding
import com.zboot.vpn.db.ServersDatabase
import com.zboot.vpn.helpers.*
import com.zboot.vpn.helpers.Utils.isOrientationLandscape
import com.zboot.vpn.helpers.Utils.isRunningOnTV
import com.zboot.vpn.models.Server
import com.zboot.vpn.remote.NetworkCaller
import com.zboot.vpn.vpn.VPNLauncherImpl
import com.zboot.vpn.vpn.VPNService
import de.blinkt.openvpn.VPNLauncher
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.VpnStatus
import de.blinkt.openvpn.core.services.OpenVPNService
import de.blinkt.openvpn.custom.DataCleanManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


class MainFragment : BaseFragment<MainActivity, FragmentMainBinding>(TAG), VPNLauncher.VPNLauncherCallbacks,
	VpnStatus.StateListener {

	override fun getPageTitle() = 0 //not used for MainFragment
	override fun initializeBinding(): FragmentMainBinding = FragmentMainBinding.inflate(requireActivity().layoutInflater)

	companion object {

		const val TAG = "MainFragment"
		const val PROMPT_DISCONNECT = "prompt_disconnect"
		private const val SELECTED_SERVER_KEY = "selected_server"
		private const val CONNECTED_SERVER_KEY = "connected_server"
		private const val RECONNECT_TIMEOUT = 8000L
	}

	var isListVisible = false

	//selectedLocation and connectLocation are different in Auto mode
	private var selectedServer: Server = Server.auto()
	private var connectServer: Server? = null
	private var service: VPNService? = null

	//	private var isConnectedToInternet = false
	private var isServiceBound = false
	private var retryConnectingToVPN = false

	private val reviewManager by lazy { ReviewManagerFactory.create(requireContext()) }
	private val animator = ValueAnimator.ofFloat(0f, 1f)
	private val serversDao = ServersDatabase.getInstance().serversDao()
	private var currentStatus = ConnectionStatus.LEVEL_DISCONNECTED


	private val reconnectTimer = object : CountDownTimer(RECONNECT_TIMEOUT, RECONNECT_TIMEOUT) {
		override fun onFinish() {
			if (currentStatus == ConnectionStatus.LEVEL_CONNECTED)
				return

			//retry
			retryConnectingToVPN = true
			stopVPN()
		}

		override fun onTick(millisUntilFinished: Long) {
		}
	}

	private val vpnLauncher: VPNLauncher<VPNService> by lazy {
		VPNLauncherImpl(this, this, VPNService::class.java)
	}

	private val connection: ServiceConnection = object : ServiceConnection {
		override fun onServiceConnected(className: ComponentName, binder: IBinder) {
			this@MainFragment.service = (binder as VPNService.LocalBinder).service
		}

		override fun onServiceDisconnected(arg0: ComponentName) {
			service = null
		}
	}

	private val serversAdapter = ServersAdapter { location ->

		if (currentStatus == ConnectionStatus.LEVEL_DISCONNECTED) {
			changeSelectedLocation(location)
			return@ServersAdapter
		}

		DialogHelper.disconnect(requireContext(), R.string.server_change_disconnect_alert_message) {
			stopVPN()
			changeSelectedLocation(location)
		}
	}

	private val startVPNForResult =
			registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
				vpnLauncher.onActivityResult(result.resultCode)
			}

	override fun startVPNIntent(intent: Intent) = startVPNForResult.launch(intent)

	private fun changeSelectedLocation(server: Server) {
		AdsHelper.showInterstitialAd(activity)
		selectedServer = server
		hideServers()
		setSelectedLocation()
	}

	private fun connectBtnClicked() {
		//don't show ad first time to connect
		if (PrefsHelper.isFirstConnect())
			PrefsHelper.disableFirstConnect()
		else
			AdsHelper.showInterstitialAd(activity)

		if (currentStatus != ConnectionStatus.LEVEL_DISCONNECTED) {
			currentStatus = ConnectionStatus.LEVEL_DISCONNECTED
			stopVPN()

			//show rating dialog
			reviewManager.requestReviewFlow().addOnCompleteListener { request ->
				if (isAdded && request.isSuccessful)
					reviewManager.launchReviewFlow(requireActivity(), request.result)
			}
			return
		}

		if (!Utils.isConnected(requireContext())) {
			Toast.makeText(requireContext(), R.string.log_no_internet, Toast.LENGTH_SHORT).show()
			return
		}

		binding.tvLog.text = ""

		if (serversAdapter.itemCount == 1) {
			Toast.makeText(requireContext(), R.string.no_servers, Toast.LENGTH_SHORT).show()
			return
		}

		binding.tvStatus?.setText(R.string.connecting)
		binding.connect?.setText(R.string.connecting)

		startAnim()

		viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
			DataCleanManager.cleanCache(requireContext())

			if (selectedServer.isAuto()) {
				try {
					val bestServersResponse = NetworkCaller.instance.getBestServer(BuildConfig.VERSION_CODE)
					val bestServers = bestServersResponse.body()?.servers

					if (bestServersResponse.isSuccessful && bestServers != null && !bestServers.isNullOrEmpty()) {
						connectServer = bestServers[0]
						fetchProfileAndStartVPN()
					} else
						throw Exception("Error while loading best server")
				} catch (e: Exception) {
					connectServer = serversDao.getBestServer()
					fetchProfileAndStartVPN()
				}
			} else {
				connectServer = selectedServer
				fetchProfileAndStartVPN()
			}
		}
	}

	//region Fragment lifecycle
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)



		if (requireContext().isRunningOnTV())
		//connect button only visible for TV
			binding.connect?.visible()

		binding.serversList.hasFixedSize()
		binding.serversList.adapter = serversAdapter

		binding.connectLayout.setOnClickListener { connectBtnClicked() }
		binding.connect?.setOnClickListener { connectBtnClicked() }
		binding.closeList?.setOnClickListener { if (isListVisible) hideServers() }
		binding.currentLocationLayout?.setOnClickListener { showServers() }
		binding.refreshList.setOnClickListener { updateServers() }


		if (savedInstanceState != null) {
			selectedServer = savedInstanceState.getSerializable(SELECTED_SERVER_KEY) as Server
			connectServer = savedInstanceState.getSerializable(CONNECTED_SERVER_KEY) as Server?
			setSelectedLocation()
		}

		viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
			loadFromDatabase(true)
			try {
				val lastUpdateResponse = NetworkCaller.instance.getSetting("last_update")
				val lastUpdateBody = lastUpdateResponse.body()
				if (lastUpdateResponse.isSuccessful && lastUpdateBody != null) {
					val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
					val lastUpdateCalendar = Calendar.getInstance()
					val savedLastUpdateCalendar = Calendar.getInstance()
					sdf.parse(lastUpdateBody.value)?.let { lastUpdateCalendar.time = it }
					PrefsHelper.getLastUpdate()?.let { savedLastUpdate ->
						sdf.parse(savedLastUpdate)?.let { savedLastUpdateCalendar.time = it }
					}

					if (lastUpdateCalendar <= savedLastUpdateCalendar)
						return@launch
				}

				val serversResponse = NetworkCaller.instance.getServers(BuildConfig.VERSION_CODE)
				val responseBody = serversResponse.body()
				if (serversResponse.isSuccessful && responseBody != null) {
					PrefsHelper.saveLastUpdate(responseBody.lastUpdate)
					serversDao.deleteServers()
					serversDao.insertServers(responseBody.servers)
					loadFromDatabase()
				} else throw Exception("Error while loading servers list")
			} catch (e: Exception) {
				Timber.w(e)
			}
		}
	}

	override fun onStart() {
		super.onStart()

		viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
			val connectedCode = PrefsHelper.getSavedServer()
			if (connectedCode != -1)
				connectServer = serversDao.getServer(connectedCode)
		}
	}

	override fun onResume() {
		super.onResume()
		VpnStatus.addStateListener(this)

		val intent = Intent(requireContext(), VPNService::class.java)
		intent.action = OpenVPNService.START_SERVICE
		requireContext().bindService(intent, connection, Context.BIND_AUTO_CREATE)
		isServiceBound = true

		when (currentStatus) {
			ConnectionStatus.LEVEL_CONNECTED -> arguments?.let {
				if (it.getBoolean(PROMPT_DISCONNECT, false)) {
					it.remove(PROMPT_DISCONNECT)
					DialogHelper.disconnect(
							requireContext(),
							R.string.apps_using_change_disconnect_alert_message
					) { stopVPN() }
				}
			}
			ConnectionStatus.LEVEL_DISCONNECTED -> {
				setUIToDisconnected()
				viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
					//save IP if not connected
					try {
						val ipResponse = NetworkCaller.instance.getIp()
						val ip = ipResponse.body()
						if (ipResponse.isSuccessful && ip != null) {
							PrefsHelper.saveOriginalIP(ip.value)
						}
					} catch (ignore: Exception) {
						PrefsHelper.saveOriginalIP(null)
					}
				}
			}
			else -> {
			}
		}
	}

	override fun onPause() {
		super.onPause()
		VpnStatus.removeStateListener(this)

		if (isServiceBound) {
			requireContext().unbindService(connection)
			isServiceBound = false
		}
	}

	override fun onStop() {
		super.onStop()
		PrefsHelper.saveServer(connectServer?.id ?: -1)
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putSerializable(SELECTED_SERVER_KEY, selectedServer)
		outState.putSerializable(CONNECTED_SERVER_KEY, connectServer)
	}
	//endregion

	//region Functions Related to OpenVPN
	private fun fetchProfileAndStartVPN() {
		currentStatus = ConnectionStatus.LEVEL_PREPARING
		connectServer?.let {
			//TODO this is retrying only twice??!!
			ProfileFetcher.getProfile(requireContext(), it, object : ProfileFetcher.ConnectCallback {
				override fun connect() {
					startVPN()
				}

				override fun error(message: String?) {
					Timber.e(message)
					viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
						stopVPN()
						//TODO extract method
						retryConnectingToVPN = false
						binding.tvStatus?.setText(R.string.reconnecting)
						binding.connect?.setText(R.string.reconnecting)
						reconnectTimer.start()
						startAnim()
					}
					fetchProfileAndStartVPN()
				}
			})
		}
	}

	private fun startVPN() {
		reconnectTimer.start()
		try {
			vpnLauncher.startVPN(connectServer?.getProfileName())
		} catch (ex: Exception) {
			ex.printStackTrace()
		}
	}

	private fun stopVPN() {
		reconnectTimer.cancel()
		if (!retryConnectingToVPN) {
			animator.end()
			connectServer = null
		}

		try {
			service?.stopVPN()
		} catch (e: RemoteException) {
			Timber.e(e)
		}
	}

	override fun updateState(
			state: String?, logMessage: String?, localizedResId: Int, level: ConnectionStatus, Intent: Intent?
	) {
		currentStatus = level

		if (isAdded)
			activity?.runOnUiThread {
				if (retryConnectingToVPN) {
					retryConnectingToVPN = false
					binding.tvStatus?.setText(R.string.reconnecting)
					binding.connect?.setText(R.string.reconnecting)
					reconnectTimer.start()
					startAnim()
					viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
						//retry with random server to avoid retrying connection to the same server if it is down
						connectServer = serversDao.getRandomServer()
						startVPN()
					}
				} else if (state != "NOPROCESS") when (currentStatus) {
					ConnectionStatus.LEVEL_CONNECTED -> setUIToConnected()
					ConnectionStatus.LEVEL_DISCONNECTED -> setUIToDisconnected()
					ConnectionStatus.LEVEL_INVALID_CERTIFICATE -> {
						setUIToDisconnected()
						DialogHelper.invalidCertificate(requireContext())
					}
					else -> {
						//connecting
					}
				}
			}
	}

	override fun setConnectedVPN(uuid: String?) {
	}
	//endregion

	//region Update UI
	private fun setUIToConnected() {
		if (!isAdded)
			return
		animator.end()
		binding.tvLog.setTextColor(getColorCompat(R.color.primary))
		binding.tvLog.text = getString(R.string.connected_to, connectServer?.getLocationName(requireContext()))
		binding.tvLog.setCompoundDrawablesRelativeWithIntrinsicBounds(
				null,
				null,
				connectServer?.getFlagResId(requireContext())?.let {
					Utils.resizeDrawable(requireContext(), it, 24, 20)
				},
				null
		)
		binding.tvStatus?.setText(R.string.connected)
		binding.connect?.setText(R.string.disconnect)
		binding.ivStatusBackground.background = getDrawableCompat(R.drawable.ic_shield_on)
		DrawableCompat.setTint(
				DrawableCompat.wrap(binding.ivStatusForeground.background),
				getColorCompat(R.color.background)
		)
		object : CountDownTimer(3000, 1000) {
			override fun onTick(millisUntilFinished: Long) {
			}
			override fun onFinish() {
				AdsHelper.showInterstitialAd(activity)
			}
		}.start()
	}

	private fun setUIToDisconnected() {
		animator.end()
		binding.tvLog.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null)
		//		if (isConnectedToInternet)
		binding.tvLog.text = ""
		binding.tvStatus?.setText(R.string.not_connected)
		binding.connect?.setText(R.string.connect)
		binding.ivStatusBackground.background = getDrawableCompat(R.drawable.ic_shield_off)
		DrawableCompat.setTint(
				DrawableCompat.wrap(binding.ivStatusForeground.background),
				getColorCompat(R.color.background)
		)
	}

	private fun resetTvLog() {
		if (isAdded) {
			binding.tvLog.setTextColor(Color.RED)
			binding.tvLog.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null)
		}
	}
	//endregion

	private fun updateServers() {
		binding.loadingServers.visible()
		binding.serversList.gone()
		binding.locationFlag.gone()
		binding.locationArrow?.gone()
		binding.locationTitle.text = getString(R.string.loading)

		viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
			try {
				val serversResponse = NetworkCaller.instance.getServers(BuildConfig.VERSION_CODE)
				val responseBody = serversResponse.body()

				if (serversResponse.isSuccessful && responseBody != null) {
					PrefsHelper.saveLastUpdate(responseBody.lastUpdate)
					serversDao.deleteServers()
					serversDao.insertServers(responseBody.servers)
					setServersList(responseBody.servers)
				} else throw Exception("Error while loading servers list")
			} catch (e: Exception) {
				loadFromDatabase()
			}
		}
	}

	/**
	 * updateIfEmpty is used to retry if local servers tables has no rows. This typically happens after the
	 * database schema changes and users update the app do to fallbackToDestructiveMigration()
	 */
	private suspend fun loadFromDatabase(updateIfEmpty: Boolean = false) {
		setServersList(serversDao.getServers(), updateIfEmpty)
	}

	private suspend fun setServersList(servers: List<Server>, updateIfEmpty: Boolean = false) {
		withContext(Dispatchers.Main) {
			binding.loadingServers.gone()
			binding.serversList.visible()
			binding.locationArrow?.visible()
			binding.locationFlag.visible()

			setSelectedLocation()
			if (servers.isEmpty()) {
				if (updateIfEmpty)
					updateServers()
				else
					onDownloadingServersError()
			} else {
				if (currentStatus != ConnectionStatus.LEVEL_CONNECTED)
				//don't change text if connected
					binding.tvLog.text = ""
				serversAdapter.setServers(servers)
			}

			//Request focus for TV only
			binding.connect?.let { it.post { it.requestFocus() } }
		}
	}

	private fun onDownloadingServersError(@StringRes errorMessage: Int = R.string.load_servers_error) {
		resetTvLog()
		binding.locationTitle.setText(R.string.click_retry)
		binding.tvLog.setText(errorMessage)
		binding.loadingServers.gone()
		binding.locationArrow?.visible()
		binding.locationFlag.visible()
		binding.locationFlag.setImageDrawable(
				ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_error_24, null)
		)
	}

	private fun setSelectedLocation() {
		binding.locationFlag.setImageDrawable(selectedServer.getFlagDrawable(requireContext()))
		binding.locationTitle.text = selectedServer.getLocationName(requireContext())

		with(selectedServer.city) {
			if (isBlank()) {
				binding.locationSubtitle.visibility = View.GONE
			} else {
				binding.locationSubtitle.visibility = View.VISIBLE
				binding.locationSubtitle.text = this
			}
		}
	}

	private fun showServers() {
//		AdsHelper.showInterstitialAd(activity)
		isListVisible = true
		binding.serversListLayout?.visible()
		binding.currentLocationLayout?.gone()
	}

	fun hideServers() {
		if (requireContext().isOrientationLandscape())
			return

		isListVisible = false
		binding.serversListLayout?.gone()
		binding.currentLocationLayout?.visible()
	}

	override fun onVPNLaunchError() {}

	override fun onVPNLaunchCancel() {}

	@RequiresApi(Build.VERSION_CODES.N)
	override fun onAlwaysConnectChecked() {
		AlertDialog.Builder(requireContext())
				.setMessage(R.string.nought_alwayson_warning)
				.setPositiveButton(R.string.open_settings) { _: DialogInterface?, _: Int ->
					try {
						startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
						return@setPositiveButton
					} catch (ignore: ActivityNotFoundException) {
					}
				}
				.setNegativeButton(android.R.string.cancel, null)
				.setCancelable(false)
				.show()
	}

	@Suppress("DEPRECATION")
	private fun startAnim() {

		val progressBar = binding.spinKit
		val doubleBounce: Sprite = Wave()
		progressBar?.setIndeterminateDrawable(doubleBounce)
		progressBar?.visibility = View.VISIBLE

	animator.duration = 1000
		animator.repeatMode = ValueAnimator.REVERSE
		animator.repeatCount = Animation.INFINITE

		val hsv = FloatArray(3)
		val from = FloatArray(3)
		val to = FloatArray(3)
		context?.resources?.getColor(R.color.background)?.let { Color.colorToHSV(it, from) }
		context?.resources?.getColor(R.color.primary)?.let { Color.colorToHSV(it, to) }

		val drawable = DrawableCompat.wrap(binding.ivStatusForeground.background)
		animator.addUpdateListener { animation ->
			hsv[0] = (from[0] + to[0] * animation.animatedFraction)
			hsv[1] = (from[1] + to[1] * animation.animatedFraction)
			hsv[2] = (from[2] + to[2] * animation.animatedFraction)
			DrawableCompat.setTint(drawable, Color.HSVToColor(hsv))
		}

		animator.addListener(object : AnimatorListenerAdapter() {
			override fun onAnimationEnd(animation: Animator) {
				progressBar?.visibility = View.GONE
				animation.removeListener(this)
				animation.duration = 0
				animation.interpolator = ReverseInterpolator()
				animation.start()

			}
		})

		animator.start()
	}

	//	override fun onInternetConnectivityChanged(isConnected: Boolean) {
	//		isConnectedToInternet = isConnected
	//		if (!isAdded)
	//			return
	//
	//		if (currentStatus == ConnectionStatus.LEVEL_CONNECTED && !isConnected)
	//			stopVPN()
	//
	//		if (currentStatus != ConnectionStatus.LEVEL_CONNECTED)
	//			updateLogText()
	//	}
	//
	//	private fun updateLogText() {
	//		if (!isAdded)
	//			return
	//
	//		resetTvLog()
	//		if (isConnectedToInternet) {
	//			binding.tvLog.text = ""
	//			binding.connectLayout.isEnabled = true
	//		} else {
	//			binding.tvLog.text = getString(R.string.log_no_internet)
	//			binding.connectLayout.isEnabled = false
	//		}
	//	}
}