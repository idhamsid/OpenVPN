package com.zboot.vpn.custom

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.zboot.vpn.helpers.NotifsHelper
import timber.log.Timber


class Messaging : FirebaseMessagingService() {

	override fun onMessageReceived(remoteMessage: RemoteMessage) {
		super.onMessageReceived(remoteMessage)
		Timber.d("Notification received")

		remoteMessage.notification?.let {
			NotifsHelper.showNotification(this, NotifsHelper.STATUS_CHANNEL_ID, it.title, it.body)
		}
	}

	override fun onNewToken(token: String) {
		super.onNewToken(token)
		Timber.d("Token changed")
	}
}