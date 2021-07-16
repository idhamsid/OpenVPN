package com.zboot.vpn.vpn

import android.content.Intent
import android.os.Binder
import android.os.CountDownTimer
import android.os.IBinder
import com.flags.CountryUtils
import com.google.firebase.installations.FirebaseInstallations
import com.zboot.vpn.R
import com.zboot.vpn.helpers.NotifsHelper
import com.zboot.vpn.helpers.PrefsHelper
import com.zboot.vpn.helpers.Utils
import com.zboot.vpn.remote.NetworkCaller
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.VpnStatus
import de.blinkt.openvpn.core.services.OpenVPNService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class VPNService : OpenVPNService(), VpnStatus.ByteCountListener {

	companion object {

		private const val CONNECT_DURATION = 3 * 60 * 60 * 1000L
	}

	private var timerStarted = false

	private val mBinder = LocalBinder()
	private val timer: CountDownTimer = object : CountDownTimer(CONNECT_DURATION, 60 * 60 * 1000L) {
		override fun onTick(millisUntilFinished: Long) {}
		override fun onFinish() {
			stopVPN()
		}
	}

	override fun onBind(intent: Intent?): IBinder {
		return mBinder
	}

	override fun showNotification(status: ConnectionStatus, intent: Intent?) {
		startForeground(
				NotifsHelper.DEFAULT_NOTIF_ID,
				NotifsHelper.showNotification(
						this,
						NotifsHelper.STATUS_CHANNEL_ID,
						getNotificationTitle(status),
						getNotificationText(status),
						getLargeNotificationIcon(),
						intent
				)
		)
	}

	override fun showNotification(contentTitle: String, contentText: String, intent: Intent?) {
		NotifsHelper.showNotification(
				this,
				NotifsHelper.USER_REQ_CHANNEL_ID,
				contentTitle,
				contentText,
				R.mipmap.ic_launcher
		)
	}

	inner class LocalBinder : Binder() {

		val service: VPNService get() = this@VPNService
	}

	private fun getLargeNotificationIcon() = CountryUtils.getFlagResIDFromCountryCode(this, profile.name).let {
		if (it == 0)
			R.drawable.ic_drawer
		else
			it
	}

	override fun onVPNConnected() {
		VpnStatus.addByteCountListener(this)
		logConnect()

		//start timer
		if (timerStarted)
			return

		timer.start()
		timerStarted = true
	}

	override fun onVPNDisconnected() {
		VpnStatus.removeByteCountListener(this)
		NotifsHelper.showPersistentNotification(this)
		logDisconnect()

		//stop timer
		if (!timerStarted)
			return

		timer.cancel()
		timerStarted = false
	}

	private fun logConnect() {
		PrefsHelper.getOriginalIP()?.let { ip ->
			FirebaseInstallations.getInstance().id.addOnSuccessListener { firebaseId ->
				profile?.id?.let { profileId ->
					NetworkCaller.instance.logConnect(profileId, firebaseId, ip)
							.enqueue(object : Callback<Void> {
								override fun onResponse(call: Call<Void>, response: Response<Void>) {
								}

								override fun onFailure(call: Call<Void>, t: Throwable) {
								}
							})
				}
			}
		}
	}

	private fun logDisconnect() {
		FirebaseInstallations.getInstance().id.addOnSuccessListener {
			NetworkCaller.instance.logDisconnect(it)
					.enqueue(object : Callback<Void> {
						override fun onResponse(call: Call<Void>, response: Response<Void>) {
//							Timber.d("logDisconnect success")
						}

						override fun onFailure(call: Call<Void>, t: Throwable) {
//							Timber.d("logDisconnect fail: ${t.message}")
						}
					})
		}
	}

	override fun updateByteCount(ins: Long, outs: Long, diffIn: Long, diffOut: Long) {
		val netStats = String.format(
				getString(R.string.notif_rate),
				Utils.humanReadableByteCount(ins, false, resources),
				Utils.humanReadableByteCount(outs, false, resources)
		)

		NotifsHelper.showNotification(
				this,
				NotifsHelper.STATUS_CHANNEL_ID,
				getNotificationTitle(ConnectionStatus.LEVEL_CONNECTED),
				netStats,
				getLargeNotificationIcon(),
				null
		)
	}

	private fun getNotificationTitle(status: ConnectionStatus) = getString(
			when (status) {
				ConnectionStatus.LEVEL_CONNECTED -> R.string.connected_to
				//TODO what to show when disconnected?
				//					ConnectionStatus.LEVEL_DISCONNECTED -> R.string.state_connected
				else -> R.string.connecting_to
			}, CountryUtils.getLocalizedNameFromCountryCode(PrefsHelper.getLanguage(), profile.mCountryCode)
	)

	private fun getNotificationText(status: ConnectionStatus) = getString(
			when (status) {
				ConnectionStatus.LEVEL_CONNECTED -> R.string.state_connected
				ConnectionStatus.LEVEL_DISCONNECTED -> R.string.state_disconnected
				else -> R.string.state_connecting
			}
	)
}