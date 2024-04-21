package com.example.blinkit.viewmodels

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.blinkit.utils.Constants
import com.example.blinkit.utils.Utils
import com.example.blinkit.api.ApiUtilities
import com.example.blinkit.models.BestSeller
import com.example.blinkit.models.Notification
import com.example.blinkit.models.NotificationData
import com.example.blinkit.models.Orders
import com.example.blinkit.models.Product
import com.example.blinkit.roomdb.CartProducts
import com.example.blinkit.roomdb.CartProductsDao
import com.example.blinkit.roomdb.CartProductsDatabase
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
        cartProductsDao.insertCartProducts(products)
    }

    suspend fun updateCartProducts(products: CartProducts) {
        cartProductsDao.updateCartProducts(products)
    }

    suspend fun deleteCartProduct(productsId: String) {
        cartProductsDao.deleteCartProduct(productsId)
    }

    fun getAll(): LiveData<List<CartProducts>> {
        return cartProductsDao.getAllCartProducts()
    }

    suspend fun deleteCartProducts() {
        cartProductsDao.deleteCartProducts()
    }


    //Firebase call
    fun fetchAllTheProducts(): Flow<List<Product>> = callbackFlow {
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
        FirebaseDatabase.getInstance().getReference("AllUsers").child("Users")
            .child(Utils.getCurrentUserId()!!).child("userAddress").setValue(address)
    }

    fun getUserAddress(callbacks: (String?) -> Unit) {
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

    fun saveAddress(address:String){
      FirebaseDatabase.getInstance().getReference("AllUsers").child("Users")
            .child(Utils.getCurrentUserId()!!).child("userAddress").setValue(address)

    }

    fun logOutUser(){
      FirebaseAuth.getInstance().signOut()
    }

    fun saveOrderProducts(orders: Orders) {
        FirebaseDatabase.getInstance().getReference("Admins").child("Orders")
            .child(orders.orderId!!).setValue(orders)

    }

    fun fetchProductType():Flow<List<BestSeller>> = callbackFlow {
        val db= FirebaseDatabase.getInstance().getReference("Admins/ProductType")

        val eventListener=object:ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
               val productTypeList=ArrayList<BestSeller>()
                for(productType in snapshot.children){
                    val productTypeName =productType.key
                    val productList =ArrayList<Product>()

                    for (products in productType.children){
                        val product=products.getValue(Product::class.java)
                        productList.add(product!!)
                    }
                    val bestSeller =BestSeller(productType=productTypeName, products = productList)
                    productTypeList.add(bestSeller)
                }
                trySend(productTypeList)
            }


            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        }
        db.addValueEventListener(eventListener)
        awaitClose{db.removeEventListener(eventListener)}
    }

    //Shared preferences
    fun savingCartItemCount(itemCount: Int) {
        sharedPreferences.edit().putInt("itemCount", itemCount).apply()
    }

    fun fetchTotalCartItemCount(): MutableLiveData<Int> {
        val totalItemCount = MutableLiveData<Int>()
        totalItemCount.value = sharedPreferences.getInt("itemCount", 0)
        return totalItemCount
    }

    fun saveAddressStatus() {
        sharedPreferences.edit().putBoolean("addressStatus", true).apply()
    }

    fun getAddressStatus(): MutableLiveData<Boolean> {
        val status = MutableLiveData<Boolean>()
        status.value = sharedPreferences.getBoolean("addressStatus", false)
        return status
    }

    fun getUserMobileNumber(): String? {
        val currentUser = Utils.getAUthInstance().currentUser
        return currentUser?.phoneNumber
    }
    //retrofit
    suspend fun checkPaymentStatus(headers: Map<String, String>) {
        val res = ApiUtilities.statusAPi.checkStatus(
            headers, Constants.MERCHANT_ID, Constants.merchantTransactionId
        )

        Log.d("UserViewModel", "Payment Status Response: ${res.body()?.success}")
        _paymentStatus.value = res.body() != null && res.body()!!.success
    }

     fun sendNotification(adminUid: String, title: String, message: String) {
         Log.d("GGG", "inside sendNotification ")
        val getToken = FirebaseDatabase.getInstance().getReference("Admins").child("AdminInfo").child(adminUid)
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
                    }else{
                        Log.d("GGG", "failed "+response.toString())
                    }
                }

                override fun onFailure(call: Call<Notification>, t: Throwable) {
                    TODO("Not yet implemented")
                    Log.d("GGG", "failed "+t.message.toString())
                }

            })


        }.addOnFailureListener {
            Log.d("GGG", "inside failure listener ")
        }

    }


}