package com.zboot.vpn.fragments

import android.os.Bundle
import android.view.View
import com.zboot.vpn.activities.IntroActivity
import com.zboot.vpn.base.BaseFragment
import com.zboot.vpn.databinding.FragmentIntroBinding


class IntroFragment : BaseFragment<IntroActivity, FragmentIntroBinding>(TAG) {

	companion object {

		const val TAG = "IntroFragment"
	}

	override fun initializeBinding() = FragmentIntroBinding.inflate(requireActivity().layoutInflater)

	override fun getPageTitle() = 0

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		binding.continueToApp.setOnClickListener { activity?.changeFragment(FirstLoadFragment()) }

		binding.privacyInnerLayout.setOnClickListener {
			activity?.changeToDisplayFragment(DisplayFragment.DisplayType.PRIVACY_POLICY)
		}

		binding.termsInnerLayout.setOnClickListener {
			activity?.changeToDisplayFragment(DisplayFragment.DisplayType.TERMS)
		}

		binding.close.setOnClickListener { activity?.finishAffinity() }
	}
}