package com.zboot.vpn.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.zboot.vpn.BuildConfig
import com.zboot.vpn.R
import com.zboot.vpn.activities.IntroActivity
import com.zboot.vpn.activities.MainActivity
import com.zboot.vpn.base.BaseFragment
import com.zboot.vpn.databinding.FragmentFirstLoadBinding
import com.zboot.vpn.db.ServersDatabase
import com.zboot.vpn.helpers.PrefsHelper
import com.zboot.vpn.helpers.gone
import com.zboot.vpn.helpers.visible
import com.zboot.vpn.remote.NetworkCaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class FirstLoadFragment : BaseFragment<IntroActivity, FragmentFirstLoadBinding>(TAG) {

	companion object {

		const val TAG = "FirstLoadFragment"
	}

	override fun initializeBinding() = FragmentFirstLoadBinding.inflate(requireActivity().layoutInflater)

	override fun getPageTitle() = 0

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		binding.retry.setOnClickListener { loadServers() }
		loadServers()
	}

	private fun loadServers() {
		binding.loading.visible()
		binding.retry.gone()
		binding.tvLog.text = getString(R.string.first_time_load)

		viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
			try {
				val serversResponse = NetworkCaller.instance.getServers(BuildConfig.VERSION_CODE)
				val responseBody = serversResponse.body()

				if (serversResponse.isSuccessful && responseBody != null) {
					PrefsHelper.saveLastUpdate(responseBody.lastUpdate)
					ServersDatabase.getInstance().serversDao().insertServers(responseBody.servers)

					startActivity(Intent(requireContext(), MainActivity::class.java))
					PrefsHelper.disableFirstRun()
					activity?.finish()
				} else throw Exception("Error while loading servers list")
			} catch (e: Exception) {
				withContext(Dispatchers.Main) {
					binding.loading.gone()
					binding.retry.visible()
					binding.tvLog.text = getString(R.string.load_servers_error)
				}
			}
		}
	}
}