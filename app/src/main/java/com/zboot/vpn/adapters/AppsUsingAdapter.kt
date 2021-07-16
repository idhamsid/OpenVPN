package com.zboot.vpn.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.zboot.vpn.R
import com.zboot.vpn.custom.ContextViewHolder
import com.zboot.vpn.helpers.PrefsHelper
import com.zboot.vpn.models.AppModel
import org.jetbrains.annotations.Contract
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet


class AppsUsingAdapter(private val countChangeListener: CountChangeListener) :
	RecyclerView.Adapter<AppsUsingAdapter.ViewHolder>(), Filterable {

	private var apps: ArrayList<AppModel> = arrayListOf()
	private var allApps: ArrayList<AppModel> = arrayListOf()
	private var configChanged = false
	var appsUsingCount = 0
		private set
	private val filter: Filter = object : Filter() {
		@Contract(pure = true)
		override fun performFiltering(constraint: CharSequence): FilterResults {
			val query = constraint.toString().toLowerCase(Locale.ROOT).trim()
			var temp: MutableList<AppModel>? = ArrayList()
			if (query.isEmpty()) {
				temp = allApps
			} else {
				for (app in allApps) if (app.name.toLowerCase(Locale.ROOT).contains(query)) temp!!.add(app)
			}
			val results = FilterResults()
			results.values = temp
			return results
		}

		override fun publishResults(constraint: CharSequence, results: FilterResults) {
			apps.clear()
			apps.addAll((results.values as List<AppModel>))
			notifyDataSetChanged()
		}
	}

	override fun getFilter(): Filter {
		return filter
	}

	fun setApps(apps: ArrayList<AppModel>) {
		this.apps = apps
		allApps = ArrayList(apps)
		for (app in apps) if (app.isActive) appsUsingCount++
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_apps_using, parent, false))
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val app = apps[position]
		holder.icon.setImageDrawable(app.icon)
		holder.activate.text = app.name
		holder.activate.isChecked = app.isActive
	}

	override fun getItemCount(): Int {
		return apps.size
	}

	val allItemsCount: Int
		get() = allApps.size

	fun hasConfigChanged() = configChanged

	fun activateAll() {
		for (app in apps) app.isActive = true
		notifyDataSetChanged()
		PrefsHelper.setAllAppsUsing()
		appsUsingCount = apps.size
		countChangeListener.onCountChange(appsUsingCount)
	}

	inner class ViewHolder(itemView: View) : ContextViewHolder(itemView), View.OnClickListener {

		val icon: ImageView = itemView.findViewById(R.id.icon)
		val activate: SwitchCompat = itemView.findViewById(R.id.activate)

		override fun onClick(v: View) {
			var inactiveApps: HashSet<String>? = PrefsHelper.getAppsNotUsing()
			if (inactiveApps == null) inactiveApps = HashSet()
			val pos = adapterPosition
			val app = apps[pos]
			if ((v as SwitchCompat).isChecked) {
				app.isActive = true
				inactiveApps.remove(app.packageName)
				countChangeListener.onCountChange(++appsUsingCount)
			} else {
				app.isActive = false
				inactiveApps.add(app.packageName)
				countChangeListener.onCountChange(--appsUsingCount)
			}
			configChanged = true
			PrefsHelper.saveAppsNotUsing(inactiveApps)
			notifyItemChanged(pos)
		}

		init {
			activate.setOnClickListener(this)
		}
	}

	interface CountChangeListener {

		fun onCountChange(newCount: Int)
	}
}