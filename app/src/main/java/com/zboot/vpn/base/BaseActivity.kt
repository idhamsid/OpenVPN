package com.zboot.vpn.base

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.viewbinding.ViewBinding
import com.zboot.vpn.R
import com.zboot.vpn.helpers.PrefsHelper
import com.zboot.vpn.helpers.ThemeHelper
import com.zeugmasolutions.localehelper.LocaleHelper
import com.zeugmasolutions.localehelper.LocaleHelperActivityDelegateImpl


abstract class BaseActivity<T : ViewBinding> : AppCompatActivity() {

	protected val localeDelegate = LocaleHelperActivityDelegateImpl()
	protected val binding: T by lazy { initializeBinding() }

	abstract fun initializeBinding(): T

	abstract fun changeFragment(newFragment: BaseFragmentInterface)

	override fun attachBaseContext(newBase: Context) {
		super.attachBaseContext(localeDelegate.attachBaseContext(newBase))
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		//TODO splash screen always follow system display mode
		ThemeHelper.applyTheme(PrefsHelper.getDisplayMode())
		super.onCreate(savedInstanceState)
		setTheme(R.style.AppTheme_Main)
		localeDelegate.onCreate(this)

		setContentView(binding.root)
	}

	override fun getApplicationContext() = localeDelegate.getApplicationContext(super.getApplicationContext())
	override fun getDelegate() = localeDelegate.getAppCompatDelegate(super.getDelegate())
	fun getDrawableCompat(@DrawableRes resId: Int) = ResourcesCompat.getDrawable(resources, resId, theme)
	fun getColorCompat(@ColorRes resId: Int) = ResourcesCompat.getColor(resources, resId, theme)

	override fun onResume() {
		super.onResume()
		localeDelegate.onResumed(this)
	}

	override fun onPause() {
		super.onPause()
		localeDelegate.onPaused()
	}

	override fun createConfigurationContext(overrideConfiguration: Configuration): Context {
		return LocaleHelper.onAttach(super.createConfigurationContext(overrideConfiguration))
	}
}