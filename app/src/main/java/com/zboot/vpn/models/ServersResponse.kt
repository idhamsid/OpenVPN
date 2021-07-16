package com.zboot.vpn.models

import com.google.gson.annotations.SerializedName


data class ServersResponse(
		@SerializedName("last_update") val lastUpdate: String,
		val servers: List<Server>
)