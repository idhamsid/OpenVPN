package com.zboot.vpn.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zboot.vpn.models.Server


@Dao
interface ServersDao {

	@Query("SELECT * FROM servers ORDER BY connectedDevices ASC LIMIT 1")
	suspend fun getBestServer(): Server

	@Query("SELECT * FROM servers WHERE id IN (SELECT id FROM servers ORDER BY RANDOM() LIMIT 1)")
	suspend fun getRandomServer(): Server

	@Query("SELECT * FROM servers WHERE id = :id")
	suspend fun getServer(id: Int): Server

	@Query("SELECT * FROM servers ORDER BY `order` ASC")
	suspend fun getServers(): List<Server>

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertServers(servers: List<Server>)

	@Query("DELETE FROM servers WHERE 1")
	suspend fun deleteServers()
}