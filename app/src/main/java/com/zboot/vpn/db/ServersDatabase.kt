package com.zboot.vpn.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zboot.vpn.base.BaseApplication
import com.zboot.vpn.models.Server


@Database(entities = [Server::class], version = 3, exportSchema = false)
abstract class ServersDatabase : RoomDatabase() {

	abstract fun serversDao(): ServersDao

	companion object {

		private const val DATABASE = "servers"

		// For Singleton instantiation
		@Volatile
		private var instance: ServersDatabase? = null

		fun getInstance(): ServersDatabase {
			return instance ?: synchronized(this) {
				instance ?: Room.databaseBuilder(BaseApplication.instance, ServersDatabase::class.java, DATABASE)
						.fallbackToDestructiveMigration()
						.build()
						.also { instance = it }
			}
		}	}
}