package com.zboot.vpn.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.zboot.vpn.R
import com.zboot.vpn.activities.MainActivity
import de.blinkt.openvpn.core.VpnStatus
import java.util.*


object NotifsHelper {

	private const val DEFAULT_NOTIFICATION_CHANNEL_ID = "persistent_notif"
	const val STATUS_CHANNEL_ID = "status_notif"
	const val USER_REQ_CHANNEL_ID = "user_req_notif"
	const val DEFAULT_NOTIF_ID = 6


	fun cancelPersistentNotification(context: Context) {
		if (!VpnStatus.isVPNActive())
			NotificationManagerCompat.from(context).cancel(DEFAULT_NOTIF_ID)
	}

	fun showPersistentNotification(context: Context) {
		if (!PrefsHelper.isPersistentNotif()) return

		val notifBuilder = NotificationCompat.Builder(context, DEFAULT_NOTIFICATION_CHANNEL_ID)
		val intent = Intent(context, MainActivity::class.java)
		val pIntent = PendingIntent.getActivity(context, 0, intent, 0)
		notifBuilder.setContentTitle(context.getString(R.string.persistent_notif_title))
				.setContentText(context.getString(R.string.persistent_notif_text))
				.setSmallIcon(R.drawable.ic_baseline_notifications_24)
				.setContentIntent(pIntent)
				.setOngoing(true)
		NotificationManagerCompat.from(context).notify(DEFAULT_NOTIF_ID, notifBuilder.build())
	}

	fun showNotification(
			context: Context,
			channelId: String,
			contentTitle: String,
			contentText: String,
			largeIcon: Int,
			intent: Intent? = null
	): Notification {
		val notifBuilder = NotificationCompat.Builder(context, channelId)
		notifBuilder.setContentTitle(contentTitle)
				.setContentText(contentText)
				.setSmallIcon(R.drawable.ic_baseline_notifications_24)
				.setLargeIcon(BitmapFactory.decodeResource(context.resources, largeIcon))
				.setOnlyAlertOnce(true)
				.setOngoing(true)
				.setContentIntent(
						PendingIntent.getActivity(
								context,
								0,
								intent ?: Intent(context, MainActivity::class.java),
								PendingIntent.FLAG_UPDATE_CURRENT
						)
				)

		return notifBuilder.build().also {
			NotificationManagerCompat.from(context).notify(DEFAULT_NOTIF_ID, it)
		}
	}

	fun showNotification(
			context: Context,
			channelId: String,
			contentTitle: String?,
			contentText: String?,
			intent: Intent? = null
	): Notification {
		val notifBuilder = NotificationCompat.Builder(context, channelId)
		notifBuilder.setContentTitle(contentTitle)
				.setContentText(contentText)
				.setSmallIcon(R.drawable.ic_baseline_notifications_24)
				.setOnlyAlertOnce(true)
				.setOngoing(true)
				.setContentIntent(
						PendingIntent.getActivity(
								context,
								0,
								intent ?: Intent(context, MainActivity::class.java),
								PendingIntent.FLAG_UPDATE_CURRENT
						)
				)

		return notifBuilder.build().also {
			NotificationManagerCompat.from(context).notify(DEFAULT_NOTIF_ID, it)
		}
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	fun createNotificationChannels(context: Context) {
		val channels: MutableList<NotificationChannel> = ArrayList()
		var channel = NotificationChannel(
				DEFAULT_NOTIFICATION_CHANNEL_ID,
				context.getString(R.string.persistent_notification),
				NotificationManager.IMPORTANCE_LOW
		)
		channel.description = context.getString(R.string.persistent_notification_summary)
		channel.enableLights(false)
		channel.setSound(null, null)
		channel.enableVibration(false)
		channels.add(channel)

		// Connection status change messages
		channel = NotificationChannel(
				STATUS_CHANNEL_ID,
				context.getString(R.string.channel_name_status),
				NotificationManager.IMPORTANCE_LOW
		)
		channel.description = context.getString(R.string.channel_description_status)
		channel.enableLights(false)
		channel.enableVibration(false)
		channels.add(channel)

		// Urgent requests, e.g. two factor auth
		channel = NotificationChannel(
				USER_REQ_CHANNEL_ID,
				context.getString(R.string.channel_name_user_req),
				NotificationManager.IMPORTANCE_HIGH
		)
		channel.description = context.getString(R.string.channel_description_user_req)
		channel.enableLights(false)
		channel.enableVibration(false)
		channels.add(channel)

		NotificationManagerCompat.from(context).createNotificationChannels(channels)
	}
}