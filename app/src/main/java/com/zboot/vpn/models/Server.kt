package com.zboot.vpn.models

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.flags.CountryUtils
import com.google.gson.annotations.SerializedName
import com.zboot.vpn.R
import com.zboot.vpn.helpers.PrefsHelper
import java.io.Serializable


@Entity(tableName = "servers")
class Server : Serializable {

	//TODO this should not be here?
	annotation class ConnectionProtocol {

		companion object {

			var UDP = "udp"
			var TCP = "tcp"
		}
	}

	companion object {

		const val AUTO = "auto"
		fun auto(): Server {
			return Server()
		}
	}

	@PrimaryKey(autoGenerate = true)
	var id = 0

	@SerializedName("country_code")
	var countryCode = AUTO

	var order = 0

	var ip = ""
	var port = 0
	var city: String = ""

	@ConnectionProtocol
	var protocol: String? = null

	@SerializedName("connected_devices")
	var connectedDevices = 0

	@SerializedName("use_file")
	var useFile = false

	@DrawableRes
	fun getFlagResId(context: Context): Int =
			if (isAuto()) R.drawable.ic_auto
			else CountryUtils.getFlagResIDFromCountryCode(context, countryCode).let {
				if (it == 0)
					R.drawable.ic_drawer
				else
					it
			}

	fun getFlagDrawable(context: Context): Drawable = ResourcesCompat.getDrawable(
			context.resources,
			getFlagResId(context),
			null
	)!!

	fun getLocationName(context: Context): String =
			if (isAuto()) context.getString(R.string.auto)
			else CountryUtils.getLocalizedNameFromCountryCode(PrefsHelper.getLanguage(), countryCode)

	fun isAuto(): Boolean = countryCode == AUTO

	fun getProfileName(): String = countryCode + order

	//	override fun equals(other: Any?): Boolean {
	//		if (other !is Server)
	//			return false
	//
	//		return countryCode == other.countryCode
	//	}
	//
	//	override fun hashCode(): Int {
	//		return countryCode.hashCode()
	//	}
}
