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

package com.sample.subscriptionscodelab.repository

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.sample.subscriptionscodelab.billing.BillingClientWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map


/**
 * The [SubscriptionDataRepository] processes and tranforms the [StateFlow] data received from
 * the [BillingClientWrapper] into [Flow] data available to the viewModel.
 *
 */
class SubscriptionDataRepository(billingClientWrapper: BillingClientWrapper) {

    val hasAdFree: Flow<Boolean> = billingClientWrapper.purchases.map { purchaseList ->
        purchaseList.any { purchase ->
            purchase.products.contains(AD_FREE)
        }
    }

    val adFreeProductDetails: Flow<ProductDetails> =
        billingClientWrapper.productWithProductDetails.filter {
            it.containsKey(
                AD_FREE
            )
        }.map { it[AD_FREE]!! }

    // List of current purchases returned by the Google PLay Billing client library.
    val purchases: Flow<List<Purchase>> = billingClientWrapper.purchases

    // Set to true when a purchase is acknowledged.
    val isNewPurchaseAcknowledged: Flow<Boolean> = billingClientWrapper.isNewPurchaseAcknowledged

    companion object {
        private const val AD_FREE = "ad_free"
    }
}
