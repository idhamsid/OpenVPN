package com.zboot.vpn.helpers

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.dzboot.vpn.helpers.AdsHelper
import com.zboot.vpn.R


object DialogHelper {

	fun newUpdateRequired(context: Context, okBtnClicked: () -> Unit) {
		AlertDialog.Builder(context)
				.setTitle(R.string.update_required)
				.setMessage(R.string.update_required_message)
				.setCancelable(false)
				.setPositiveButton(android.R.string.ok) { _, _ -> okBtnClicked() }
				.setNegativeButton(android.R.string.cancel, null)
				.show()
	}

	fun invalidCertificate(context: Context) {
		AlertDialog.Builder(context)
				.setTitle(R.string.disconnect_alert_title)
				.setMessage(R.string.certificate_outdated_update_prompt)
				.setCancelable(false)
				.setPositiveButton(android.R.string.ok) { _, _ -> Utils.openPlayStoreAppPage(context) }
				.setNegativeButton(android.R.string.cancel, null)
				.show()
	}

	fun disconnect(context: Context, @StringRes messageResId: Int, okBtnClicked: () -> Unit) {
		AlertDialog.Builder(context)
				.setTitle(R.string.disconnect_alert_title)
				.setMessage(messageResId)
				.setCancelable(false)
				.setPositiveButton(android.R.string.ok) { _, _ -> okBtnClicked() }
				.setNegativeButton(android.R.string.cancel, null)
				.show()
	}

	fun unlicensed(activity: AppCompatActivity) {
		AlertDialog.Builder(activity)
				.setTitle(R.string.illegal_copy)
				.setMessage(R.string.unlicensed_copy)
				.setCancelable(false)
				.setPositiveButton(R.string.download) { _, _ ->
					//TODO toast showing twice??
					Toast.makeText(activity, R.string.uninstall_toast, Toast.LENGTH_SHORT).show()
					Utils.openPlayStoreAppPage(activity)
					activity.finishAffinity()
				}
				.setNegativeButton(R.string.close_app) { _, _ -> activity.finishAffinity() }
				.show()
	}

	fun supportUs(activity: AppCompatActivity) {
		AlertDialog.Builder(activity)
				.setTitle(R.string.support_us_title)
				.setMessage(R.string.support_us_prompt)
				.setCancelable(false)
				.setPositiveButton(android.R.string.ok) { _, _ -> AdsHelper.showRewardedAd(activity) }
				.setNegativeButton(android.R.string.cancel, null)
				.show()
	}
}