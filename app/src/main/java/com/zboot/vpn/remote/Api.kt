package com.zboot.vpn.remote

import com.zboot.vpn.models.ServersResponse
import com.zboot.vpn.models.Setting
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

//TODO need to use server side check with each request (BaseActivity line 48)
interface Api {

	@GET("get-setting")
	suspend fun getSetting(@Query("key") key: String): Response<Setting>

	@GET("list")
	suspend fun getServers(@Query("v") version: Int): Response<ServersResponse>

	@GET("best")
	suspend fun getBestServer(@Query("v") version: Int): Response<ServersResponse>

	@GET("request-ip")
	suspend fun getIp(): Response<Setting>

	@FormUrlEncoded
	@POST("log-connect")
	fun logConnect(
			@Field("server_id") serverId: Int,
			@Field("device_id") deviceId: String,
			@Field("ip") ip: String
	): Call<Void>

	@FormUrlEncoded
	@POST("log-disconnect")
	fun logDisconnect(@Field("device_id") deviceId: String): Call<Void>
}