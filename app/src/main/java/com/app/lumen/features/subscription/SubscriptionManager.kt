package com.app.lumen.features.subscription

import android.util.Log
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.NumberFormat
import java.util.Currency

object SubscriptionManager {
    private const val TAG = "RevenueCat"
    private const val ENTITLEMENT_ID = "lumen-premium"

    private val _availablePackages = MutableStateFlow<List<Package>>(emptyList())
    val availablePackages: StateFlow<List<Package>> = _availablePackages.asStateFlow()

    private val _hasProAccess = MutableStateFlow(false)
    val hasProAccess: StateFlow<Boolean> = _hasProAccess.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val monthlyPackage: Package?
        get() = _availablePackages.value.firstOrNull { it.packageType == PackageType.MONTHLY }

    val yearlyPackage: Package?
        get() = _availablePackages.value.firstOrNull { it.packageType == PackageType.ANNUAL }

    fun fetchOfferings() {
        _isLoading.value = true
        Log.d(TAG, "Fetching offerings...")
        Purchases.sharedInstance.getOfferingsWith(
            onError = { error ->
                Log.e(TAG, "Error fetching offerings: ${error.code} - ${error.message}")
                Log.e(TAG, "  underlyingErrorMessage: ${error.underlyingErrorMessage}")
                _isLoading.value = false
            },
            onSuccess = { offerings ->
                Log.d(TAG, "Offerings fetched successfully")
                Log.d(TAG, "  Current offering: ${offerings.current?.identifier ?: "null"}")
                Log.d(TAG, "  All offering keys: ${offerings.all.keys}")
                offerings.current?.let { current ->
                    Log.d(TAG, "  Available packages: ${current.availablePackages.size}")
                    current.availablePackages.forEach { pkg ->
                        Log.d(TAG, "    Package: ${pkg.identifier} (${pkg.packageType})")
                        Log.d(TAG, "      Product: ${pkg.product.id}")
                        Log.d(TAG, "      Price: ${pkg.product.price.formatted}")
                        Log.d(TAG, "      SubscriptionOptions: ${pkg.product.subscriptionOptions}")
                    }
                }
                offerings.current?.availablePackages?.let { packages ->
                    _availablePackages.value = packages
                }
                _isLoading.value = false
            },
        )
    }

    fun checkProAccess() {
        Log.d(TAG, "Checking pro access...")
        Purchases.sharedInstance.getCustomerInfo(
            callback = object : com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    val hasAccess = customerInfo.entitlements[ENTITLEMENT_ID]?.isActive == true
                    Log.d(TAG, "Pro access: $hasAccess")
                    Log.d(TAG, "  Entitlements: ${customerInfo.entitlements.all.keys}")
                    _hasProAccess.value = hasAccess
                }
                override fun onError(error: PurchasesError) {
                    Log.e(TAG, "Error checking pro access: ${error.message}")
                }
            }
        )
    }

    fun purchase(
        activity: android.app.Activity,
        pkg: Package,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancelled: () -> Unit,
    ) {
        Log.d(TAG, "Purchasing package: ${pkg.identifier}")
        Purchases.sharedInstance.purchaseWith(
            purchaseParams = com.revenuecat.purchases.PurchaseParams.Builder(activity, pkg).build(),
            onError = { error, userCancelled ->
                if (userCancelled) {
                    Log.d(TAG, "Purchase cancelled by user")
                    onCancelled()
                } else {
                    Log.e(TAG, "Purchase error: ${error.message}")
                    onError(error.message)
                }
            },
            onSuccess = { _, customerInfo ->
                Log.d(TAG, "Purchase successful")
                _hasProAccess.value = customerInfo.entitlements[ENTITLEMENT_ID]?.isActive == true
                onSuccess()
            },
        )
    }

    fun restorePurchases(
        onSuccess: (Boolean) -> Unit,
        onError: (String) -> Unit,
    ) {
        Log.d(TAG, "Restoring purchases...")
        Purchases.sharedInstance.restorePurchasesWith(
            onError = { error ->
                Log.e(TAG, "Restore error: ${error.message}")
                onError(error.message)
            },
            onSuccess = { customerInfo ->
                val hasAccess = customerInfo.entitlements[ENTITLEMENT_ID]?.isActive == true
                Log.d(TAG, "Restore result - hasAccess: $hasAccess")
                _hasProAccess.value = hasAccess
                onSuccess(hasAccess)
            },
        )
    }

    fun formatWeeklyPrice(pkg: Package, divisor: Int): String? {
        val price = pkg.product.price
        val weeklyMicros = price.amountMicros / divisor
        val weeklyAmount = weeklyMicros / 1_000_000.0

        return try {
            val format = NumberFormat.getCurrencyInstance()
            format.currency = Currency.getInstance(price.currencyCode)
            format.maximumFractionDigits = 2
            format.format(weeklyAmount)
        } catch (e: Exception) {
            null
        }
    }
}
