package com.example.blinkit.viewmodels

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.blinkit.api.ApiUtilities
import com.example.blinkit.models.BestSeller
import com.example.blinkit.models.Notification
import com.example.blinkit.models.NotificationData
import com.example.blinkit.models.Orders
import com.example.blinkit.models.Product
import com.example.blinkit.roomdb.CartProducts
import com.example.blinkit.roomdb.CartProductsDao
import com.example.blinkit.roomdb.CartProductsDatabase
import com.example.blinkit.utils.Constants
import com.example.blinkit.utils.Utils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserViewmodel(application: Application) : AndroidViewModel(application) {

    //Initialization
    val sharedPreferences: SharedPreferences =
        application.getSharedPreferences("My_Pref", MODE_PRIVATE)
    val cartProductsDao: CartProductsDao =
        CartProductsDatabase.getDatabaseInstance(application).cartProductsDao()

    private val _paymentStatus = MutableStateFlow<Boolean>(false)
    val paymentStatus = _paymentStatus

    //roomDb
    suspend fun insertCartProducts(products: CartProducts) {
        Log.d("ViewModel", "inside  insertCartProducts")
        cartProductsDao.insertCartProducts(products)
    }

    suspend fun updateCartProducts(products: CartProducts) {

        Log.d("ViewModel", "inside  updateCartProducts")
        cartProductsDao.updateCartProducts(products)
    }

    suspend fun deleteCartProduct(productsId: String) {

        Log.d("ViewModel", "inside  deleteCartProduct")
        cartProductsDao.deleteCartProduct(productsId)
    }

    fun getAll(): LiveData<List<CartProducts>> {

        Log.d("ViewModel", "inside  deleteCartProduct")
        return cartProductsDao.getAllCartProducts()
    }

    suspend fun deleteCartProducts() {

        Log.d("ViewModel", "inside  deleteCartProduct")
        cartProductsDao.deleteCartProducts()
    }


    //Firebase call
    fun fetchAllTheProducts(): Flow<List<Product>> = callbackFlow {

        Log.d("ViewModel", "inside  deleteCartProduct")
        val db = FirebaseDatabase.getInstance().getReference("Admins").child("AllProducts")

        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val products = ArrayList<Product>()
                for (product in snapshot.children) {
                    val prod = product.getValue(Product::class.java)

                    products.add(prod!!)

                }
                trySend(products)
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        }
        db.addValueEventListener(eventListener)

        awaitClose {
            db.removeEventListener(eventListener)
        }
    }

    fun getAllOrders(): Flow<List<Orders>> = callbackFlow {

        Log.d("ViewModel", "inside  getAllOrders")
        val db = FirebaseDatabase.getInstance().getReference("Admins").child("Orders")
            .orderByChild("orderStatus")


        val eventListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                val orderList = ArrayList<Orders>()
                for (orders in snapshot.children) {
                    val order = orders.getValue(Orders::class.java)

                    if (order?.orderingUserId == Utils.getCurrentUserId()) {
                        orderList.add(order!!)

                    }

                    Log.d("checkrUser", "orderingId ; " + order?.orderingUserId.toString())
                    Log.d("checkrUser", "orderId ; " + order?.orderId.toString())
                    Log.d("checkrUser", "uId ; " + Utils.getCurrentUserId().toString())
                    Log.d("checkrUser", "" + orderList.toString())
                }
                trySend(orderList)
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        }
        db.addValueEventListener(eventListener)
        awaitClose { db.removeEventListener(eventListener) }
    }

    fun getCategoryProducts(category: String): Flow<List<Product>> = callbackFlow {

        Log.d("ViewModel", "inside  getCategoryProducts")
        val db = FirebaseDatabase.getInstance().getReference("Admins")
            .child("ProductCategory/${category}")

        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val products = ArrayList<Product>()
                for (product in snapshot.children) {
                    val prod = product.getValue(Product::class.java)

                    products.add(prod!!)

                }
                trySend(products)
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }


        }

        db.addValueEventListener(eventListener)

        awaitClose {
            db.removeEventListener(eventListener)
        }
    }

    fun getOrderedProducts(orderId: String): Flow<List<CartProducts>> = callbackFlow {

        Log.d("ViewModel", "inside  getOrderedProducts")
        val db =
            FirebaseDatabase.getInstance().getReference("Admins").child("Orders").child(orderId)
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val order = snapshot.getValue(Orders::class.java)
                trySend(order?.orderList!!)
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        }
        db.addValueEventListener(eventListener)
        awaitClose { db.removeEventListener(eventListener) }

    }

    fun updateItemCount(product: Product, itemCount: Int) {

        Log.d("ViewModel", "inside  updateItemCount")
        FirebaseDatabase.getInstance().getReference("Admins")
            .child("AllProducts/${product.productRandomId}").child("itemCount").setValue(itemCount)
        FirebaseDatabase.getInstance().getReference("Admins")
            .child("ProductCategory/${product.productCategory}/${product.productRandomId}")
            .child("itemCount").setValue(itemCount)
        FirebaseDatabase.getInstance().getReference("Admins")
            .child("ProductType/${product.productType}/${product.productRandomId}")
            .child("itemCount").setValue(itemCount)
    }

    fun saveProductsAfterOrder(stock: Int, product: CartProducts) {

        Log.d("ViewModel", "inside  saveProductsAfterOrder")
        FirebaseDatabase.getInstance().getReference("Admins")
            .child("AllProducts/${product.productId}").child("itemCount").setValue(0)
        FirebaseDatabase.getInstance().getReference("Admins")
            .child("ProductCategory/${product.productCategory}/${product.productId}")
            .child("itemCount").setValue(0)
        FirebaseDatabase.getInstance().getReference("Admins")
            .child("ProductType/${product.productType}/${product.productId}").child("itemCount")
            .setValue(stock)


        FirebaseDatabase.getInstance().getReference("Admins")
            .child("AllProducts/${product.productId}").child("productStock").setValue(stock)
        FirebaseDatabase.getInstance().getReference("Admins")
            .child("ProductCategory/${product.productCategory}/${product.productId}")
            .child("productStock").setValue(stock)
        FirebaseDatabase.getInstance().getReference("Admins")
            .child("ProductType/${product.productType}/${product.productId}").child("productStock")
            .setValue(stock)
    }

    fun saveUserAddress(address: String) {

        Log.d("ViewModel", "inside  saveUserAddress")
        FirebaseDatabase.getInstance().getReference("AllUsers").child("Users")
            .child(Utils.getCurrentUserId()!!).child("userAddress").setValue(address)
    }

    fun getUserAddress(callbacks: (String?) -> Unit) {

        Log.d("ViewModel", "inside  getUserAddress")
        val db = FirebaseDatabase.getInstance().getReference("AllUsers").child("Users")
            .child(Utils.getCurrentUserId()!!).child("userAddress")

        db.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val address = snapshot.getValue(String::class.java)
                    callbacks(address)
                } else {
                    callbacks(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callbacks(null)
            }

        })
    }

    fun saveAddress(address: String) {

        Log.d("ViewModel", "inside  saveAddress")
        FirebaseDatabase.getInstance().getReference("AllUsers").child("Users")
            .child(Utils.getCurrentUserId()!!).child("userAddress").setValue(address)

    }

    fun logOutUser() {

        Log.d("ViewModel", "inside  logOutUser")
        FirebaseAuth.getInstance().signOut()
    }

    fun saveOrderProducts(orders: Orders) {

        Log.d("ViewModel", "inside  saveOrderProducts")
        FirebaseDatabase.getInstance().getReference("Admins").child("Orders")
            .child(orders.orderId!!).setValue(orders)

    }

    fun fetchProductType(): Flow<List<BestSeller>> = callbackFlow {
        val db = FirebaseDatabase.getInstance().getReference("Admins/ProductType")

        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val productTypeList = ArrayList<BestSeller>()

                for (productType in snapshot.children) {
                    val productTypeName = productType.key
                    val productList = ArrayList<Product>()

                    for (products in productType.children) {
                        try {
                            // check if child is a map (object) and not just string
                            if (products.value is Map<*, *>) {
                                val product = products.getValue(Product::class.java)
                                if (product != null) {
                                    productList.add(product)
                                }
                            } else {
                                // just a string or unexpected type → skip
                                Log.w(
                                    "FirebaseMapping",
                                    "Skipping invalid product node: ${products.key}"
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("FirebaseMapping", "Error parsing product ${products.key}", e)
                        }
                    }

                    val bestSeller = BestSeller(
                        productType = productTypeName,
                        products = productList
                    )
                    productTypeList.add(bestSeller)
                }
                trySend(productTypeList).isSuccess
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseDB", "Database error: ${error.message}")
                trySend(emptyList()) // avoid crash
            }
        }

        db.addValueEventListener(eventListener)
        awaitClose { db.removeEventListener(eventListener) }
    }

    //Shared preferences
    fun savingCartItemCount(itemCount: Int) {

        Log.d("ViewModel", "inside  savingCartItemCount")
        sharedPreferences.edit().putInt("itemCount", itemCount).apply()
    }

    fun fetchTotalCartItemCount(): MutableLiveData<Int> {

        Log.d("ViewModel", "inside  fetchTotalCartItemCount")
        val totalItemCount = MutableLiveData<Int>()
        totalItemCount.value = sharedPreferences.getInt("itemCount", 0)
        return totalItemCount
    }

    fun saveAddressStatus() {

        Log.d("ViewModel", "inside  saveAddressStatus")
        sharedPreferences.edit().putBoolean("addressStatus", true).apply()
    }

    fun getAddressStatus(): MutableLiveData<Boolean> {

        Log.d("ViewModel", "inside  getAddressStatus")
        val status = MutableLiveData<Boolean>()
        status.value = sharedPreferences.getBoolean("addressStatus", false)
        return status
    }

    fun getUserMobileNumber(): String? {

        Log.d("ViewModel", "inside  getUserMobileNumber")
        val currentUser = Utils.getAUthInstance().currentUser
        return currentUser?.phoneNumber
    }

    //retrofit
    suspend fun checkPaymentStatus(headers: Map<String, String>) {

        Log.d("ViewModel", "inside  checkPaymentStatus")
        val res = ApiUtilities.statusAPi.checkStatus(
            headers, Constants.MERCHANT_ID, Constants.merchantTransactionId
        )

        Log.d("UserViewModel", "Payment Status Response: ${res.body()?.success}")
        _paymentStatus.value = res.body() != null && res.body()!!.success
    }

    fun sendNotification(adminUid: String, title: String, message: String) {

        Log.d("ViewModel", "inside  sendNotification")

        val getToken =
            FirebaseDatabase.getInstance().getReference("Admins").child("AdminInfo").child(adminUid)
                .child("adminToken").get()

        getToken.addOnCompleteListener { task ->

            val token = task.result.getValue(String::class.java)

            val notification = Notification(token, NotificationData(title, message))
            ApiUtilities.notificationApi.sendNotification(notification).enqueue(object :
                Callback<Notification> {
                override fun onResponse(
                    call: Call<Notification>,
                    response: Response<Notification>
                ) {
                    Log.d("GGG", "inside on success response ")
                    if (response.isSuccessful) {
                        Log.d("GGG", "sent")
                    } else {
                        Log.d("GGG", "failed " + response.toString())
                    }
                }

                override fun onFailure(call: Call<Notification>, t: Throwable) {
                    TODO("Not yet implemented")
                    Log.d("GGG", "failed " + t.message.toString())
                }

            })


        }.addOnFailureListener {
            Log.d("GGG", "inside failure listener ")
        }

    }


}