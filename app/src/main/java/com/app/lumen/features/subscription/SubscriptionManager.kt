package com.app.lumen.features.subscription

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.app.lumen.services.AnalyticsManager
import com.app.lumen.widget.VerseWidgetData
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

object SubscriptionManager {
    private const val TAG = "RevenueCat"
    // No specific entitlement ID — any active entitlement grants pro access

    private const val PREFS_NAME = "subscription_prefs"
    private const val KEY_IS_PREMIUM = "is_premium"
    private const val KEY_EXPIRATION_DATE = "expiration_date"

    private lateinit var prefs: SharedPreferences
    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Track last known premium status to avoid redundant widget refreshes
    private var lastKnownPremium: Boolean? = null

    private val _availablePackages = MutableStateFlow<List<Package>>(emptyList())
    val availablePackages: StateFlow<List<Package>> = _availablePackages.asStateFlow()

    private val _hasProAccess = MutableStateFlow(false)
    val hasProAccess: StateFlow<Boolean> = _hasProAccess.asStateFlow()

    private val _expirationDate = MutableStateFlow<Date?>(null)
    val expirationDate: StateFlow<Date?> = _expirationDate.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val monthlyPackage: Package?
        get() = _availablePackages.value.firstOrNull { it.packageType == PackageType.MONTHLY }

    val yearlyPackage: Package?
        get() = _availablePackages.value.firstOrNull { it.packageType == PackageType.ANNUAL }

    /**
     * Initialize with context to load cached state and listen for updates.
     * Call this from Application.onCreate() after Purchases.configure().
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Seed from cache immediately to prevent UI flicker
        _hasProAccess.value = prefs.getBoolean(KEY_IS_PREMIUM, false)
        val cachedExpiry = prefs.getLong(KEY_EXPIRATION_DATE, 0L)
        if (cachedExpiry > 0L) {
            _expirationDate.value = Date(cachedExpiry)
        }
        lastKnownPremium = _hasProAccess.value
        Log.d(TAG, "Initialized from cache - premium: ${_hasProAccess.value}, expiry: ${_expirationDate.value}")

        // Listen for RevenueCat customer info updates (purchases, renewals, etc.)
        // This is the single source of truth — no need to also call checkProAccess()
        // since the listener fires immediately with cached data and again on network refresh.
        Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener { customerInfo ->
            Log.d(TAG, "Customer info updated via listener")
            updateFromCustomerInfo(customerInfo)
        }

        // Pre-fetch offerings so the paywall opens instantly
        fetchOfferings()
    }

    private fun updateFromCustomerInfo(customerInfo: CustomerInfo) {
        val activeEntitlement = customerInfo.entitlements.active.values.firstOrNull()
        val hasAccess = customerInfo.entitlements.active.isNotEmpty()
        val expiry = activeEntitlement?.expirationDate

        _hasProAccess.value = hasAccess
        _expirationDate.value = expiry

        // Persist to local cache
        prefs.edit()
            .putBoolean(KEY_IS_PREMIUM, hasAccess)
            .putLong(KEY_EXPIRATION_DATE, expiry?.time ?: 0L)
            .apply()

        // Set Mixpanel premium user property (matching iOS)
        AnalyticsManager.setUserPremiumProperty(hasAccess)

        Log.d(TAG, "Updated state - premium: $hasAccess, expiry: $expiry")
        Log.d(TAG, "  Entitlements: ${customerInfo.entitlements.all.keys}")

        // Only refresh widgets when premium status actually changes
        if (::appContext.isInitialized && hasAccess != lastKnownPremium) {
            Log.d(TAG, "Premium status changed ($lastKnownPremium -> $hasAccess), refreshing widgets")
            lastKnownPremium = hasAccess
            VerseWidgetData.savePremiumStatus(appContext, hasAccess)
            scope.launch(Dispatchers.IO) {
                VerseWidgetData.updateAllWidgets(appContext)
                Log.d(TAG, "Widgets refreshed successfully")
            }
        }
    }

    fun fetchOfferings() {
        // Skip if already fetching
        if (_isLoading.value) return
        // Only show loading spinner if packages aren't cached yet
        _isLoading.value = _availablePackages.value.isEmpty()
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

                // Use current offering, or fall back to first available offering
                val offering = offerings.current ?: offerings.all.values.firstOrNull()
                if (offering != null && offering !== offerings.current) {
                    Log.d(TAG, "  Current offering unavailable, falling back to: ${offering.identifier}")
                }

                offering?.let { selected ->
                    Log.d(TAG, "  Available packages: ${selected.availablePackages.size}")
                    selected.availablePackages.forEach { pkg ->
                        Log.d(TAG, "    Package: ${pkg.identifier} (${pkg.packageType})")
                        Log.d(TAG, "      Product: ${pkg.product.id}")
                        Log.d(TAG, "      Price: ${pkg.product.price.formatted}")
                    }
                    _availablePackages.value = selected.availablePackages
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
                    updateFromCustomerInfo(customerInfo)
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
                updateFromCustomerInfo(customerInfo)
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
                updateFromCustomerInfo(customerInfo)
                onSuccess(_hasProAccess.value)
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

    fun formatExpirationDate(date: Date): String {
        val formatter = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        return formatter.format(date)
    }
}
