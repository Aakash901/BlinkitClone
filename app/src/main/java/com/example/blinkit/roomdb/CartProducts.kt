package com.example.blinkit.roomdb

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "CartProducts")
class CartProducts (
    @PrimaryKey
    var productId: String = "random",
    var productTitle: String? = null,
    var productQuantity: String? = null,
    var productPrice: String? = null,
    var productCount: Int? = null,
    var productStock: Int? = null,
    var productImages: String? = null,
    var productCategory: String? = null,
    var adminUid: String? = null,
    var productType: String? = null,

)
