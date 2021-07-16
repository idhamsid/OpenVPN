@file:Suppress("UNCHECKED_CAST")

package com.zboot.vpn.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding


abstract class BaseFragment<D : BaseActivity<out ViewBinding>, T : ViewBinding>(private val TAG: String) : Fragment(),
	BaseFragmentInterface {

	protected var activity: D? = null
	protected val binding: T by lazy { initializeBinding() }


	abstract fun initializeBinding(): T

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		return binding.root
	}

	override fun onAttach(context: Context) {
		super.onAttach(context)
		activity = context as D
	}

	override fun onDetach() {
		super.onDetach()
		activity = null
	}

	override fun toString(): String {
		return TAG
	}

	fun getDrawableCompat(@DrawableRes resId: Int) = ResourcesCompat.getDrawable(resources, resId, null)

	fun getColorCompat(@ColorRes resId: Int) = ResourcesCompat.getColor(resources, resId, null)
}