package com.zboot.vpn.helpers

import android.content.Context
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.zboot.vpn.custom.Config
import com.zboot.vpn.models.Server
import com.zboot.vpn.remote.NetworkCaller
import de.blinkt.openvpn.core.Connection
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.core.VpnProfile
import java.io.InputStreamReader


object ProfileFetcher {

	interface ConnectCallback {
		fun connect()
		fun error(message: String?)
	}

	fun getProfile(context: Context, server: Server, connectCallback: ConnectCallback) {

		val profile: VpnProfile? = if (server.useFile) {
			val request = Request.Builder()
					.url(NetworkCaller.getBaseUrl() + "ovpn_files/${server.id}.ovpn")
					.build()

			val response = OkHttpClient().newCall(request).execute()
			if (!response.isSuccessful) {
				connectCallback.error(response.message())
				null
			} else {
				val configParser = ConfigParser()
				configParser.parseConfig(InputStreamReader(response.body().byteStream()))
				configParser.convertProfile()
			}
		} else {
			getDefaultProfile(server)
		}

		profile?.let {
			it.mName = server.getProfileName()
			it.id = server.id
			it.mCountryCode = server.countryCode
			it.mCity = server.city
			it.mAllowedAppsVpnAreDisallowed = true
			it.mAllowedAppsVpn = PrefsHelper.getAppsNotUsing()
			ProfileManager.getInstance(context).saveProfile(it)
			connectCallback.connect()
		}
	}

	private fun getDefaultProfile(server: Server): VpnProfile {
		val profile = VpnProfile()
		profile.clearDefaults()
		profile.mUsePull = true
		profile.mUseTLSAuth = true
		profile.mTLSAuthDirection = "1"
		profile.mCipher = "AES-256-GCM"
		profile.mAuth = "SHA256"
		profile.mAuthenticationType = VpnProfile.TYPE_CERTIFICATES
		profile.mVerb = "3"
		profile.mNobind = true
		profile.mPersistTun = true
		profile.mExpectTLSCert = true
		profile.mUsername = null
		profile.mPassword = null

		profile.mClientKeyFilename = "[[INLINE]]${Config.CLIENT_KEY}"
		profile.mCaFilename = "[[INLINE]]${Config.CA}"
		profile.mClientCertFilename = "[[INLINE]]${Config.CERT}"
		profile.mTLSAuthFilename = "[[INLINE]]${Config.TA}"

		with(Connection()) {
			mServerName = server.ip
			mServerPort = server.port.toString()
			mUseUdp = server.protocol == Server.ConnectionProtocol.UDP
			profile.mConnections = arrayOf(this)
		}
		return profile
	}
}