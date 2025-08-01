package com.zboot.vpn.fragments

import android.os.Bundle
import android.view.View
import androidx.viewbinding.ViewBinding
import com.zboot.vpn.R
import com.zboot.vpn.activities.IntroActivity
import com.zboot.vpn.base.BaseActivity
import com.zboot.vpn.base.BaseFragment
import com.zboot.vpn.databinding.FragmentDisplayBinding
import com.zboot.vpn.helpers.visible

class DisplayFragment<D : BaseActivity<out ViewBinding>> : BaseFragment<D, FragmentDisplayBinding>(TAG) {

	annotation class DisplayType {
		companion object {

			const val PRIVACY_POLICY = "privacy"
			const val TERMS = "terms"
		}
	}

	companion object {

		const val TYPE = "type"
		const val TAG = "DisplayFragment"
	}

	private val type by lazy { arguments?.get(TYPE) }

	override fun initializeBinding(): FragmentDisplayBinding = FragmentDisplayBinding.inflate(requireActivity().layoutInflater)

	override fun getPageTitle(): Int = when (type) {
		DisplayType.PRIVACY_POLICY -> R.string.privacy_policy
		DisplayType.TERMS -> R.string.terms_of_service
		else -> 0
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		if (activity is IntroActivity) {
			binding.back.visible()
			binding.back.setOnClickListener { activity?.onBackPressed() }
		}

		val str = activity?.assets?.open("$type.html")?.bufferedReader().use { it?.readText() }
			?.replace("[DEV]", getString(R.string.developer_name))
			?.replace("[APP_NAME]", getString(R.string.app_name))
			?.replace("[SUPPORT_EMAIL]", getString(R.string.support_email))
			?.replace("[WEBSITE]", getString(R.string.website))

		str?.let { binding.webview.loadData(it, "text/html; charset=utf-8", "UTF-8") }
	}
}