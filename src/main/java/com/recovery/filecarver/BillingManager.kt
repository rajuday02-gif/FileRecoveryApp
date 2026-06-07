package com.recovery.filecarver

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode

class BillingManager(private val context: Context) {

    private lateinit var billingClient: BillingClient
    private var purchaseListener: ((String) -> Unit)? = null

    fun initialize(activity: Activity) {
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            }
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                Log.d("BillingManager", "Billing service disconnected")
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    Log.d("BillingManager", "Billing setup finished")
                    queryProducts()
                }
            }
        })
    }

    private fun queryProducts() {
        val skuList = listOf(
            "recovery_app_pro_monthly",
            "recovery_app_pro_yearly",
            "recovery_app_premium_monthly",
            "recovery_app_premium_yearly"
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(skuList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                Log.d("BillingManager", "Products queried: ${productDetailsList.size}")
            }
        }
    }

    fun launchBillingFlow(activity: Activity, productId: String) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(productId))
            .build()

        billingClient.queryProductDetailsAsync(params) { _, productDetailsList ->
            if (productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .build()
                        )
                    )
                    .build()

                billingClient.launchBillingFlow(activity, billingFlowParams)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        Log.d("BillingManager", "Purchase: ${purchase.products}")

        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Verify the purchase
            acknowledgePurchase(purchase)
            updateTier(purchase.products[0])
            purchaseListener?.invoke(purchase.products[0])
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                Log.d("BillingManager", "Purchase acknowledged")
            }
        }
    }

    private fun updateTier(productId: String) {
        val tier = when {
            productId.contains("pro") -> "PRO"
            productId.contains("premium") -> "PREMIUM"
            else -> "FREE"
        }

        val prefs = context.getSharedPreferences("FileRecovery", Context.MODE_PRIVATE)
        prefs.edit().putString("tier", tier).apply()
        Log.d("BillingManager", "Tier updated to: $tier")
    }

    fun setOnPurchaseListener(listener: (String) -> Unit) {
        purchaseListener = listener
    }

    fun destroy() {
        if (::billingClient.isInitialized) {
            billingClient.endConnection()
        }
    }
}
