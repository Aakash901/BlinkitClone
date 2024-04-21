package com.example.blinkit.activity

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.blinkit.utils.CartListener
import com.example.blinkit.utils.Constants
import com.example.blinkit.R
import com.example.blinkit.utils.Utils
import com.example.blinkit.adapters.AdapterCartProducts
import com.example.blinkit.databinding.ActivityOrderPlacedBinding
import com.example.blinkit.databinding.AddressLayoutBinding
import com.example.blinkit.models.Orders
import com.example.blinkit.viewmodels.UserViewmodel
import com.phonepe.intent.sdk.api.B2BPGRequest
import com.phonepe.intent.sdk.api.B2BPGRequestBuilder
import com.phonepe.intent.sdk.api.PhonePe
import com.phonepe.intent.sdk.api.PhonePeInitException
import com.phonepe.intent.sdk.api.models.PhonePeEnvironment
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.nio.charset.Charset
import java.security.MessageDigest


class OrderPlacedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderPlacedBinding
    val viewModel: UserViewmodel by viewModels()
    private lateinit var adapterCartProducts: AdapterCartProducts
    private lateinit var b2BPGRequest: B2BPGRequest

    private var cartListener: CartListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderPlacedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getAllProducts()
        setStatusBarColor()
        backToMainActivity()
        onPlaceOrderClicked()
        initializePhonePay()

    }

    private fun initializePhonePay() {

        Log.d("PhonePe", "Initialization")

        val data = JSONObject()
        PhonePe.init(this, PhonePeEnvironment.UAT, Constants.MERCHANT_ID, "")

        data.put("merchantId", Constants.MERCHANT_ID)
        data.put("merchantTransactionId", Constants.merchantTransactionId)
        data.put("amount", 20000)
        data.put("mobileNumber", "9369616560")
        data.put("callbackUrl", "https://webhook.site/callback-url")

        val paymentInstrument = JSONObject()
        paymentInstrument.put("type", "UPI_INTENT")
        paymentInstrument.put("targetApp", "com.phonepe.simulator")
        data.put("paymentInstrument", paymentInstrument)

        val deviceContext = JSONObject()
        deviceContext.put("deviceOS", "ANDROID")
        data.put("deviceContext", deviceContext)

        Log.d("PhonePe", "data is " + data.toString())

        val payloadBase64 = Base64.encodeToString(
            data.toString().toByteArray(Charset.defaultCharset()), Base64.NO_WRAP
        )


        val checksum = sha256(payloadBase64 + Constants.apiEndPoints + Constants.SALT_KEY) + "###1"

        b2BPGRequest = B2BPGRequestBuilder()
            .setData(payloadBase64)
            .setChecksum(checksum)
            .setUrl(Constants.apiEndPoints)
            .build()
    }

    private fun sha256(input: String): String {
        val bytes = input.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it ->
            str + "%02x".format(it)
        }
    }

    private fun onPlaceOrderClicked() {
        binding.btnNext.setOnClickListener {
            viewModel.getAddressStatus().observe(this) { status ->
                if (status) {
                    getPaymentView()
                } else {
                    val addressLayoutBinding =
                        AddressLayoutBinding.inflate(LayoutInflater.from(this))

                    val alertDialog = AlertDialog.Builder(this).setView(addressLayoutBinding.root)
                        .create()
                    alertDialog.show()

                    addressLayoutBinding.btnAddAddress.setOnClickListener {
                        saveAddress(alertDialog, addressLayoutBinding)
                    }
                }
            }
        }
    }


    private fun checkStatus() {

        Log.d("PhonePe", "checking status")
        val xVerify =
            sha256("/pg/v1/status/${Constants.MERCHANT_ID}/${Constants.merchantTransactionId}${Constants.SALT_KEY}") + "###1"
        val headers = mapOf(
            "Content-Type" to "application/json",
            "X-VERIFY" to xVerify,
            "X-MERCHANT" to Constants.MERCHANT_ID
        )

        lifecycleScope.launch {
            viewModel.checkPaymentStatus(headers)
            viewModel.paymentStatus.collect() { status ->
                if (status) {
                    Utils.showToast(this@OrderPlacedActivity, "Payment done ")
                    saveOrder()
                    viewModel.deleteCartProducts()
                    viewModel.savingCartItemCount(0)
                    cartListener?.hideCartLayout()

                    startActivity(Intent(this@OrderPlacedActivity, UsersMainActivity::class.java))
                    finish()
                } else {

                    Utils.showToast(this@OrderPlacedActivity, "Payment failed ")

                }
            }
        }

    }


    private fun saveOrder() {
        viewModel.getAll().observe(this) { cartProductList ->

            if (cartProductList.isNotEmpty()) {
                viewModel.getUserAddress { address ->
                    val orders = Orders(
                        orderId = Utils.getRandomId(),
                        orderList = cartProductList,
                        userAddress = address,
                        orderStatus = 0,
                        orderDate = Utils.getCurrentDate(),
                        orderingUserId = Utils.getCurrentUserId()
                    )
                    viewModel.saveOrderProducts(orders)
                    //notification
                    lifecycleScope.launch {
                        Log.d("GGG", "uid:"+cartProductList[0].adminUid.toString())
                        viewModel.sendNotification(
                            cartProductList[0].adminUid!!,
                            "Ordered",
                            "Some products has been ordered"
                        )
                    }


                }
                for (products in cartProductList) {
                    val count = products.productCount
                    val stock = products.productStock?.minus(count!!)
                    viewModel.saveProductsAfterOrder(stock!!, products)

                }
            }

        }
    }

    val phonePayView = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            checkStatus()
        }
    }

    private fun getPaymentView() {

        try {
            PhonePe.getImplicitIntent(this, b2BPGRequest, "com.phonepe.simulator")
                .let {
                    phonePayView.launch(it)

                    Log.d("PhonePe", "in Payment view")
                }
        } catch (e: PhonePeInitException) {
            Utils.showToast(this, "" + e.message.toString())
        }
    }

    private fun saveAddress(alertDialog: AlertDialog?, addressLayoutBinding: AddressLayoutBinding) {
        Utils.showDialog(this, "Processing....")
        val userPinCode = addressLayoutBinding.etPinCode.text.toString()
        val userPhoneNumber = addressLayoutBinding.etPhoneNo.text.toString()
        val userState = addressLayoutBinding.etState.text.toString()
        val userDistrict = addressLayoutBinding.etDistrict.text.toString()
        val userAddress = addressLayoutBinding.etAddress.text.toString()

        val address = "$userPinCode, $userDistrict($userState), $userAddress, $userPhoneNumber"


        lifecycleScope.launch {
            viewModel.saveUserAddress(address)
            viewModel.saveAddressStatus()
        }

        alertDialog?.dismiss()
        Utils.hideDialog()
        getPaymentView()


    }

    private fun backToMainActivity() {
        binding.tbOrderFragment.setOnClickListener {
            startActivity(Intent(this, UsersMainActivity::class.java))
            finish()
        }
    }

    private fun getAllProducts() {
        viewModel.getAll().observe(this) { cartProductsList ->
            adapterCartProducts = AdapterCartProducts()
            binding.rvProductsItem.adapter = adapterCartProducts
            adapterCartProducts.differ.submitList(cartProductsList)

            var totalPrice = 0

            for (products in cartProductsList) {
                val price = products.productPrice?.substring(1)?.toInt()
                val itemCount = products.productCount!!
                totalPrice += price!! * itemCount
            }

            binding.tvSubTotal.text = "₹" + totalPrice.toString()

            if (totalPrice < 200) {
                binding.tvDeliveryCharge.text = "₹40"
                totalPrice += 40;
            }

            binding.tvFinalTotal.text = "₹" + totalPrice.toString()

        }
    }

    private fun setStatusBarColor() {
        window?.apply {
            val statusBarColors =
                ContextCompat.getColor(this@OrderPlacedActivity, R.color.diyaFlame)
            statusBarColor = statusBarColors
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }
}