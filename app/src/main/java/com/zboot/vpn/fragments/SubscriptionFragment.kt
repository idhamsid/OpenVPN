package com.zboot.vpn.fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.SkuDetails
import com.zboot.vpn.R
import com.zboot.vpn.activities.MainActivity
import com.zboot.vpn.base.BaseFragment
import com.zboot.vpn.custom.Constants
import com.zboot.vpn.databinding.FragmentProductsBinding
import com.zboot.vpn.helpers.invisible
import com.zboot.vpn.helpers.visible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SubscriptionFragment : BaseFragment<MainActivity, FragmentProductsBinding>(TAG), BillingClientStateListener {

   companion object {

      const val TAG = "SubscriptionFragment"
   }

   override fun initializeBinding(): FragmentProductsBinding =
         FragmentProductsBinding.inflate(requireActivity().layoutInflater)

   override fun getPageTitle(): Int = R.string.subscription

   private lateinit var monthlySku: SkuDetails
   private lateinit var yearlySku: SkuDetails

   override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      super.onViewCreated(view, savedInstanceState)

      activity?.billingClient?.let {
         if (!it.isReady)
            it.startConnection(this)
         else
            requestSkuDetails()
      }

      binding.monthlySubscribe.setOnClickListener { activity?.launchPurchaseFlow(monthlySku) }
      binding.yearlySubscribe.setOnClickListener { activity?.launchPurchaseFlow(yearlySku) }
   }

   private fun requestSkuDetails() {
      CoroutineScope(Dispatchers.IO).launch {
         val skuDetailsResult = activity?.querySkuDetails()
         val skuDetailsList = skuDetailsResult?.skuDetailsList

         if (skuDetailsResult?.billingResult?.responseCode == BillingClient.BillingResponseCode.OK
             && !skuDetailsList.isNullOrEmpty()) {

            for (skuDetail in skuDetailsList) {
               when (skuDetail.sku) {
                  Constants.MONTHLY_SUBSCRIPTION -> monthlySku = skuDetail
                  Constants.YEARLY_SUBSCRIPTION -> yearlySku = skuDetail
               }
            }

            withContext(Dispatchers.Main) {
               if (!isAdded)
                  return@withContext

               binding.subscribePrompt.text = getString(R.string.subscribe_prompt)
               binding.loading.invisible()
               if (this@SubscriptionFragment::monthlySku.isInitialized) {
                  binding.monthlySubscribe.text = getString(R.string.monthly_price, monthlySku.price)
                  binding.monthlySubscribe.visible()
                  binding.monthlyPrompt.visible()
               }

               if (this@SubscriptionFragment::yearlySku.isInitialized) {
                  binding.yearlySubscribe.text = getString(R.string.yearly_price, yearlySku.price)
                  binding.yearlySubscribe.visible()
               }
            }
         } else withContext(Dispatchers.Main) {
            if (!isAdded)
               return@withContext

            binding.subscribePrompt.text = getString(R.string.unknown_error)
            binding.loading.invisible()
         }
      }
   }

   override fun onBillingSetupFinished(result: BillingResult) {
      if (!isAdded)
         return

      if (result.responseCode == BillingClient.BillingResponseCode.OK) {
         requestSkuDetails()
      } else {
         Toast.makeText(requireContext(), R.string.unknown_error, Toast.LENGTH_SHORT).show()
         activity?.switchToMainFragment()
      }
   }

   override fun onBillingServiceDisconnected() {
   }
}