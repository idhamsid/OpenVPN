package com.zboot.vpn.helpers

import android.app.UiModeManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.Uri
import android.util.TypedValue
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.zboot.vpn.R
import kotlin.math.ln
import kotlin.math.pow


object Utils {

	fun resizeDrawable(
			context: Context, @DrawableRes drawable: Int, widthDp: Int, heightDp: Int
	): Drawable {
		val b = (ContextCompat.getDrawable(context, drawable) as BitmapDrawable).bitmap
		val bitmapResized =
				Bitmap.createScaledBitmap(b, dpToPx(context, widthDp.toFloat()), dpToPx(context, heightDp.toFloat()), false)
		return BitmapDrawable(context.resources, bitmapResized)
	}

	private fun dpToPx(context: Context, dp: Float) =
			TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics).toInt()

	fun Context.isOrientationLandscape(): Boolean {
		return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
	}

	fun Context.isRunningOnTV() =
			(getSystemService(Context.UI_MODE_SERVICE) as UiModeManager).currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

	fun humanReadableByteCount(bytes: Long, speed: Boolean, res: Resources): String {
		val b = if (speed) 8 * bytes else bytes
		val unit = if (speed) 1000 else 1024
		val exp = 0.coerceAtLeast((ln(b.toDouble()) / ln(unit.toDouble())).toInt().coerceAtMost(3))
		val bytesUnit = (b / unit.toDouble().pow(exp.toDouble())).toFloat()
		return if (speed) when (exp) {
			0 -> res.getString(R.string.bits_per_second, bytesUnit)
			1 -> res.getString(R.string.kbits_per_second, bytesUnit)
			2 -> res.getString(R.string.mbits_per_second, bytesUnit)
			else -> res.getString(R.string.gbits_per_second, bytesUnit)
		} else when (exp) {
			0 -> res.getString(R.string.volume_byte, bytesUnit)
			1 -> res.getString(R.string.volume_kbyte, bytesUnit)
			2 -> res.getString(R.string.volume_mbyte, bytesUnit)
			else -> res.getString(R.string.volume_gbyte, bytesUnit)
		}
	}

	fun shareApp(context: Context) = with(context) {
		val sendIntent = Intent()
		sendIntent.action = Intent.ACTION_SEND
		sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message, packageName))
		sendIntent.type = "text/plain"
		try {
			startActivity(sendIntent)
		} catch (anfe: ActivityNotFoundException) {
			Toast.makeText(this, R.string.no_app_event, Toast.LENGTH_SHORT).show()
		}
	}

	fun openPlayStoreAppPage(context: Context) = with(context) {
		try {
			startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
		} catch (anfe: ActivityNotFoundException) {
			try {
				startActivity(
						Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
				)
			} catch (anfe: ActivityNotFoundException) {
				Toast.makeText(this, R.string.no_app_event, Toast.LENGTH_SHORT).show()
			}
		}
	}

	fun isConnected(context: Context): Boolean {
		//no easy fast way to check for internet than this right now
		return try {
			val cm = ContextCompat.getSystemService(context, ConnectivityManager::class.java)
			val nInfo = cm?.activeNetworkInfo
			nInfo != null && nInfo.isAvailable && nInfo.isConnected
		} catch (ignore: Exception) {
			false
		}
	}
}