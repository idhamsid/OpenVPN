package com.zboot.vpn.activities

import android.os.Bundle
import androidx.fragment.app.Fragment
/*import com.appodeal.ads.Appodeal
import com.appodeal.ads.utils.Log
import com.zboot.vpn.BuildConfig*/
import com.zboot.vpn.R
import com.zboot.vpn.base.BaseActivity
import com.zboot.vpn.base.BaseFragmentInterface
import com.zboot.vpn.databinding.ActivityIntroBinding
import com.zboot.vpn.fragments.DisplayFragment
import com.zboot.vpn.fragments.FirstLoadFragment
import com.zboot.vpn.fragments.IntroFragment
import timber.log.Timber


class IntroActivity : BaseActivity<ActivityIntroBinding>() {

	private var currentFragment: BaseFragmentInterface = IntroFragment()

	override fun initializeBinding() = ActivityIntroBinding.inflate(layoutInflater)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)


		supportFragmentManager.beginTransaction()
				.add(R.id.fragment_container, currentFragment as Fragment, currentFragment.toString())
				.addToBackStack(null)
				.commit()
	}

	override fun changeFragment(newFragment: BaseFragmentInterface) {
		currentFragment = newFragment
		supportFragmentManager.beginTransaction()
				.replace(R.id.fragment_container, currentFragment as Fragment, currentFragment.toString())
				.commit()
	}

	override fun onBackPressed() {
		if (currentFragment is IntroFragment || currentFragment is FirstLoadFragment)
			finish()
		else
			changeFragment(supportFragmentManager.findFragmentByTag(IntroFragment.TAG) as IntroFragment)
	}

	fun changeToDisplayFragment(type: String) {
		changeFragment(getDisplayFragment(type))
	}

	private fun getDisplayFragment(type: String): DisplayFragment<IntroActivity> {
		val fragment = DisplayFragment<IntroActivity>()
		val args = Bundle()
		args.putString(DisplayFragment.TYPE, type)
		fragment.arguments = args
		return fragment
	}
}