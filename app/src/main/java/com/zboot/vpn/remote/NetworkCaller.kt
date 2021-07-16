package com.zboot.vpn.remote

import com.google.gson.GsonBuilder
import com.zboot.vpn.BuildConfig
import com.zboot.vpn.adapters.BooleanTypeAdapter
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory


class NetworkCaller {

	companion object {

		//convenient constant to avoid syncing gradle on every change
		private const val LOCAL_BASE_URL = "http://192.168.42.203/"
		private const val API = "api/v1/"

		fun getBaseUrl() = if (BuildConfig.BASE_URL.isEmpty()) LOCAL_BASE_URL else BuildConfig.BASE_URL

		val instance: Api
			get() {
				val logging = HttpLoggingInterceptor()
				logging.level =
						if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
						else HttpLoggingInterceptor.Level.NONE
				val client = OkHttpClient.Builder().addInterceptor(logging).build()

				val gsonBuilder = GsonBuilder()
				gsonBuilder.registerTypeAdapter(Boolean::class.java, BooleanTypeAdapter())

				return Retrofit.Builder()
						.baseUrl("${getBaseUrl()}$API")
						.addConverterFactory(ScalarsConverterFactory.create())
						.addConverterFactory(GsonConverterFactory.create(gsonBuilder.create()))
						.client(client)
						.build()
						.create(Api::class.java)
			}
	}
}