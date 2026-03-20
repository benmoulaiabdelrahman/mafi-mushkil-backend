package com.vardash.mafimushkil.auth

import com.google.gson.annotations.SerializedName
import com.vardash.mafimushkil.BuildConfig
import com.vardash.mafimushkil.models.Payment
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

object ChargilyManager {

    private const val BASE_URL = "https://pay.chargily.net/test/api/v2/"
    private const val SUCCESS_URL = "https://mafimushkil.app/payments/success"
    private const val FAILURE_URL = "https://mafimushkil.app/payments/failure"

    private val api by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ChargilyApi::class.java)
    }

    suspend fun createCheckout(
        orderId: String,
        payment: Payment,
        customerId: String? = null
    ): CheckoutSession {
        check(BuildConfig.CHARGILY_SECRET_KEY.isNotBlank()) {
            "Missing Chargily secret key in local.properties."
        }
        check(BuildConfig.CHARGILY_PUBLIC_KEY.isNotBlank()) {
            "Missing Chargily public key in local.properties."
        }

        val amountInMinorUnits = (payment.amount * 100).toInt()
        val request = CheckoutRequest(
            amount = amountInMinorUnits,
            currency = "dzd",
            paymentMethod = payment.method.lowercase().ifBlank { "edahabia" },
            successUrl = "$SUCCESS_URL?orderId=$orderId&paymentId=${payment.id}",
            failureUrl = "$FAILURE_URL?orderId=$orderId&paymentId=${payment.id}",
            webhookEndpoint = BuildConfig.CHARGILY_WEBHOOK_URL.ifBlank { null },
            description = payment.title.ifBlank { "Order payment" },
            locale = "en",
            customerId = customerId,
            metadata = mapOf(
                "orderId" to orderId,
                "paymentId" to payment.id,
                "publicKey" to BuildConfig.CHARGILY_PUBLIC_KEY
            )
        )

        val response = api.createCheckout(
            authorization = "Bearer ${BuildConfig.CHARGILY_SECRET_KEY}",
            request = request
        )

        return CheckoutSession(
            checkoutId = response.id,
            checkoutUrl = response.checkoutUrl,
            reference = response.id,
            publicKey = BuildConfig.CHARGILY_PUBLIC_KEY
        )
    }

    data class CheckoutSession(
        val checkoutId: String,
        val checkoutUrl: String,
        val reference: String,
        val publicKey: String
    )

    private interface ChargilyApi {
        @POST("checkouts")
        suspend fun createCheckout(
            @Header("Authorization") authorization: String,
            @Body request: CheckoutRequest
        ): CheckoutResponse
    }

    private data class CheckoutRequest(
        val amount: Int,
        val currency: String,
        @SerializedName("payment_method")
        val paymentMethod: String,
        @SerializedName("success_url")
        val successUrl: String,
        @SerializedName("failure_url")
        val failureUrl: String,
        @SerializedName("webhook_endpoint")
        val webhookEndpoint: String?,
        val description: String,
        val locale: String,
        @SerializedName("customer_id")
        val customerId: String?,
        val metadata: Map<String, String>
    )

    private data class CheckoutResponse(
        val id: String,
        @SerializedName("checkout_url")
        val checkoutUrl: String
    )
}
