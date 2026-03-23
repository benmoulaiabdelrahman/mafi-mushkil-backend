package com.vardash.mafimushkil.auth

import android.util.Log
import com.google.gson.annotations.SerializedName
import com.vardash.mafimushkil.BuildConfig
import com.vardash.mafimushkil.models.Payment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.HttpException
import kotlin.math.roundToInt

object ChargilyManager {

    private const val TAG = "ChargilyManager"
    private const val BASE_URL = "https://pay.chargily.net/test/api/v2/"

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
        userId: String? = null
    ): CheckoutSession = withContext(Dispatchers.IO) {
        Log.d(TAG, "Creating checkout for order: $orderId, amount: ${payment.amount}")
        
        check(BuildConfig.CHARGILY_SECRET_KEY.isNotBlank()) {
            "Missing Chargily secret key in local.properties."
        }

        val amount = payment.amount.roundToInt().coerceAtLeast(1)
        require(amount <= 1_000_000) {
            "Checkout amount $amount DA exceeds the safety cap of 1,000,000 DA."
        }
        Log.d(TAG, "Validated checkout amount for order $orderId: $amount DA")

        val request = CheckoutRequest(
            amount = amount,
            currency = "dzd",
            paymentMethod = payment.method.lowercase().ifBlank { "edahabia" },
            successUrl = "https://mafi-mushkil-backend.onrender.com/payment/success?orderId=$orderId",
            failureUrl = "https://mafi-mushkil-backend.onrender.com/payment/failure?orderId=$orderId",
            webhookEndpoint = BuildConfig.CHARGILY_WEBHOOK_URL.takeIf { it.isHttpUrl() },
            description = payment.title.ifBlank { "MafiMushkil - Order $orderId" },
            locale = "ar",
            metadata = buildMap {
                put("orderId", orderId)
                put("paymentId", payment.id)
                userId?.let { put("userId", it) }
            }
        )

        val response = try {
            api.createCheckout(
                authorization = "Bearer ${BuildConfig.CHARGILY_SECRET_KEY}",
                request = request
            )
        } catch (error: HttpException) {
            val errorBody = error.response()?.errorBody()?.string() ?: ""
            Log.e(TAG, "Chargily API Error: $errorBody")
            throw IllegalStateException(
                errorBody.ifBlank { "Chargily request failed with HTTP ${error.code()}" },
                error
            )
        } catch (e: Exception) {
            Log.e(TAG, "Network Error: ${e.message}", e)
            throw e
        }

        Log.d(TAG, "Checkout created successfully: ${response.checkoutUrl}")
        return@withContext CheckoutSession(
            checkoutId = response.id,
            checkoutUrl = response.checkoutUrl,
            reference = response.id,
            publicKey = BuildConfig.CHARGILY_PUBLIC_KEY
        )
    }

    private fun String.isHttpUrl(): Boolean {
        val normalized = trim()
        return normalized.startsWith("http://") || normalized.startsWith("https://")
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
        val metadata: Map<String, String>
    )

    private data class CheckoutResponse(
        val id: String,
        @SerializedName("checkout_url")
        val checkoutUrl: String
    )
}
