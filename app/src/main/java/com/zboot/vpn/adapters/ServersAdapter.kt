package com.zboot.vpn.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.flags.CountryUtils
import com.zboot.vpn.R
import com.zboot.vpn.custom.ContextViewHolder
import com.zboot.vpn.helpers.PrefsHelper
import com.zboot.vpn.helpers.gone
import com.zboot.vpn.helpers.visible
import com.zboot.vpn.models.Server


class ServersAdapter(private val onServerSelect: (Server) -> Unit) : RecyclerView.Adapter<ServersAdapter.ViewHolder>() {

	private val servers = ArrayList<Server>()


	fun setServers(servers: List<Server>) {
		this.servers.clear()
		this.servers.add(0, Server.auto())
		this.servers.addAll(servers)
		notifyDataSetChanged()
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_server_layout, parent, false))
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		with(servers[position]) {
			holder.ivFlag.setImageDrawable(getFlagDrawable(holder.context))

			if (isAuto()) {
				holder.tvConnectedDevices.gone()
				holder.tvTitle.text = holder.getString(R.string.auto)
			} else {
				holder.tvConnectedDevices.visible()
				holder.tvConnectedDevices.text = connectedDevices.toString()
				holder.tvTitle.text =
						CountryUtils.getLocalizedNameFromCountryCode(PrefsHelper.getLanguage(), countryCode)
			}

			holder.tvSubtitle.visibility = if (city.isBlank()) View.GONE else View.VISIBLE
			holder.tvSubtitle.text = city.trim()
		}
	}

	override fun getItemCount() = servers.size

	inner class ViewHolder(itemView: View) : ContextViewHolder(itemView) {

		val ivFlag: AppCompatImageView = itemView.findViewById(R.id.flag)
		val tvTitle: TextView = itemView.findViewById(R.id.title)
		val tvSubtitle: TextView = itemView.findViewById(R.id.subtitle)
		val tvConnectedDevices: TextView = itemView.findViewById(R.id.connected_devices)

		init {
			itemView.setOnClickListener { onServerSelect(servers[adapterPosition]) }
		}
	}
}