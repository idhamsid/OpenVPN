package com.zboot.vpn.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import com.zboot.vpn.BuildConfig
import com.zboot.vpn.R
import com.zboot.vpn.activities.MainActivity
import com.zboot.vpn.adapters.AppsUsingAdapter
import com.zboot.vpn.base.BaseFragment
import com.zboot.vpn.databinding.FragmentAllowedAppsBinding
import com.zboot.vpn.helpers.PrefsHelper
import com.zboot.vpn.helpers.gone
import com.zboot.vpn.helpers.visible
import com.zboot.vpn.models.AppModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AllowedAppsFragment : BaseFragment<MainActivity, FragmentAllowedAppsBinding>(TAG),
	AppsUsingAdapter.CountChangeListener, TextWatcher {

	companion object {

		private const val TAG = "AllowedAppsFragment"
	}

	private val adapter: AppsUsingAdapter = AppsUsingAdapter(this)


	override fun getPageTitle() = R.string.allowed_apps

	override fun initializeBinding(): FragmentAllowedAppsBinding =
			FragmentAllowedAppsBinding.inflate(requireActivity().layoutInflater)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		binding.rvAppsList.setHasFixedSize(true)
		binding.rvAppsList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
		binding.reset.setOnClickListener { adapter.activateAll() }
		binding.searchBar.addTextChangeListener(this)

		CoroutineScope(Dispatchers.Default).launch {
			onAppsLoaded(getApps())
		}
	}

	private suspend fun onAppsLoaded(apps: ArrayList<AppModel>) {
		if (activity == null || !isAdded)
			return

		withContext(Dispatchers.Main) {
			adapter.setApps(apps)
			binding.rvAppsList.adapter = adapter
			binding.pbLoading.gone()
			binding.tvLoading.gone()
			binding.container.visible()
			binding.sActiveApps.text = getString(R.string.active_apps_count, adapter.appsUsingCount, apps.size)
		}
	}

	override fun onCountChange(newCount: Int) {
		binding.sActiveApps.text = getString(R.string.active_apps_count, newCount, adapter.allItemsCount)
	}

	fun hasConfigChanged() = adapter.hasConfigChanged()

	private fun getApps(): ArrayList<AppModel> {
		val pm = activity?.packageManager
		val apps: ArrayList<AppModel> = ArrayList()

		for (packageInfo in pm!!.getInstalledPackages(PackageManager.GET_PERMISSIONS)) {
			if (packageInfo.packageName == BuildConfig.APPLICATION_ID || packageInfo.requestedPermissions == null) continue
			for (permission in packageInfo.requestedPermissions) {
				if (TextUtils.equals(permission, Manifest.permission.INTERNET)) {
					val name = pm.getApplicationLabel(packageInfo.applicationInfo).toString()
					val icon = pm.getApplicationIcon(packageInfo.applicationInfo)
					apps.add(AppModel(name, packageInfo.packageName, icon))
					break
				}
			}
		}

		apps.sortWith { o1, o2 -> o1.name.compareTo(o2.name) }
		val inactiveApps: Set<String> = PrefsHelper.getAppsNotUsing()
		if (inactiveApps.isEmpty())
			return apps

		for (app in apps)
			if (inactiveApps.contains(app.packageName))
				app.isActive = false

		return apps
	}

	override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
	}

	override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = adapter.filter.filter(s)

	override fun afterTextChanged(s: Editable?) {
	}
}