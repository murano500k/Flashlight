/*
 * Copyright 2022 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stc.flashlight

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.sample.subscriptionscodelab.billing.BillingClientWrapper
import com.sample.subscriptionscodelab.repository.SubscriptionDataRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The [AndroidViewModel] implementation combines all flows from the repo into a single one
 * that is collected in the Composable.
 *
 * It, also, has helper methods that are used to launch the Google Play Billing purchase flow.
 *
 */
class MainViewModel(application: Application) :
    AndroidViewModel(application) {
    var billingClient: BillingClientWrapper = BillingClientWrapper(application)
    private var repo: SubscriptionDataRepository =
        SubscriptionDataRepository(billingClientWrapper = billingClient)
    private val _billingConnectionState = MutableLiveData(false)
    val billingConnectionState: LiveData<Boolean> = _billingConnectionState

    private val _destinationScreen = MutableLiveData<DestinationScreen>()
    val destinationScreen: LiveData<DestinationScreen> = _destinationScreen

    // Start the billing connection when the viewModel is initialized.
    init {
        billingClient.startBillingConnection(billingConnectionState = _billingConnectionState)
    }

    // The productsForSaleFlows object combines all the Product flows into one for emission.


    // The userCurrentSubscriptionFlow object combines all the possible subscription flows into one
    // for emission.


    // Current purchases.


    val productsForSaleFlows = repo.adFreeProductDetails.map {
        adFreeProductDetails ->
        MainState(
            adFreeProductDetails = adFreeProductDetails
        )
    }

    // The userCurrentSubscriptionFlow object combines all the possible subscription flows into one
// for emission.
    private val userCurrentInAppFlow = repo.hasAdFree.map {
        hasAdFree ->
        MainState(
            hasAdFree = hasAdFree
        )
    }

    // Current purchases.
    val currentPurchasesFlow = repo.purchases

    init {
        viewModelScope.launch {
            userCurrentInAppFlow.collectLatest { collectedInApp ->
                when {
                    collectedInApp.hasAdFree == true -> {
                        _destinationScreen.postValue(DestinationScreen.BASIC_RENEWABLE_PROFILE)
                    }
                    else -> {
                        _destinationScreen.postValue(DestinationScreen.SUBSCRIPTIONS_OPTIONS_SCREEN)
                    }
                }
            }

        }
    }

    // When an activity is destroyed the viewModel's onCleared is called, so we terminate the
    // billing connection.
    private fun retrieveEligibleOffers(
        offerDetails: MutableList<ProductDetails.SubscriptionOfferDetails>,
        tag: String
    ): List<ProductDetails.SubscriptionOfferDetails> {
        val eligibleOffers = emptyList<ProductDetails.SubscriptionOfferDetails>().toMutableList()
        offerDetails.forEach { offerDetail ->
            if (offerDetail.offerTags.contains(tag)) {
                eligibleOffers.add(offerDetail)
            }
        }

        return eligibleOffers
    }

    private fun leastPricedOfferToken(
        offerDetails: List<ProductDetails.SubscriptionOfferDetails>
    ): String {
        var offerToken = String()
        var leastPricedOffer: ProductDetails.SubscriptionOfferDetails
        var lowestPrice = Int.MAX_VALUE

        if (!offerDetails.isNullOrEmpty()) {
            for (offer in offerDetails) {
                for (price in offer.pricingPhases.pricingPhaseList) {
                    if (price.priceAmountMicros < lowestPrice) {
                        lowestPrice = price.priceAmountMicros.toInt()
                        leastPricedOffer = offer
                        offerToken = leastPricedOffer.offerToken
                    }
                }
            }
        }
        return offerToken
    }

    private fun upDowngradeBillingFlowParamsBuilder(
        productDetails: ProductDetails,
        offerToken: String,
        oldToken: String
    ): BillingFlowParams {
        return BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )
        ).setSubscriptionUpdateParams(
            BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                .setOldPurchaseToken(oldToken)
                .setReplaceProrationMode(
                    BillingFlowParams.ProrationMode.IMMEDIATE_AND_CHARGE_FULL_PRICE
                )
                .build()
        ).build()
    }


    private fun billingFlowParamsBuilder(
        productDetails: ProductDetails,
        offerToken: String
    ): BillingFlowParams.Builder {
        return BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )
        )
    }

    fun buy(
   productDetails: ProductDetails,
   currentPurchases: List<Purchase>?,
   activity: Activity,
   tag: String
) {
   val offers =
       productDetails.subscriptionOfferDetails?.let {
           retrieveEligibleOffers(
               offerDetails = it,
               tag = tag.lowercase()
           )
       }
   val offerToken = offers?.let { leastPricedOfferToken(it) }
   val oldPurchaseToken: String

   // Get current purchase. In this app, a user can only have one current purchase at
   // any given time.
   if (!currentPurchases.isNullOrEmpty() &&
       currentPurchases.size == MAX_CURRENT_PURCHASES_ALLOWED
   ) {
       // This either an upgrade, downgrade, or conversion purchase.
       val currentPurchase = currentPurchases.first()

       // Get the token from current purchase.
       oldPurchaseToken = currentPurchase.purchaseToken

       val billingParams = offerToken?.let {
           upDowngradeBillingFlowParamsBuilder(
               productDetails = productDetails,
               offerToken = it,
               oldToken = oldPurchaseToken
           )
       }

       if (billingParams != null) {
           billingClient.launchBillingFlow(
               activity,
               billingParams
           )
       }
   } else if (currentPurchases == null) {
       // This is a normal purchase.
       val billingParams = offerToken?.let {
           billingFlowParamsBuilder(
               productDetails = productDetails,
               offerToken = it
           )
       }

       if (billingParams != null) {
           billingClient.launchBillingFlow(
               activity,
               billingParams.build()
           )
       }
   } else if (!currentPurchases.isNullOrEmpty() &&
       currentPurchases.size > MAX_CURRENT_PURCHASES_ALLOWED
   ) {
       // The developer has allowed users  to have more than 1 purchase, so they need to
       /// implement a logic to find which one to use.
       Log.d(TAG, "User has more than 1 current purchase.")
   }
}

    /**
     * Enum representing the various screens a user can be redirected to.
     */
    enum class DestinationScreen {
        SUBSCRIPTIONS_OPTIONS_SCREEN,
        BASIC_PREPAID_PROFILE_SCREEN,
        BASIC_RENEWABLE_PROFILE,
        PREMIUM_PREPAID_PROFILE_SCREEN,
        PREMIUM_RENEWABLE_PROFILE;
    }

    companion object {
        private const val TAG: String = "MainViewModel"

        private const val MAX_CURRENT_PURCHASES_ALLOWED = 1
    }
}
