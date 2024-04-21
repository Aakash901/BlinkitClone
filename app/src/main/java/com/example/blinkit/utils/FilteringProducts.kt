package com.example.blinkit.utils

import android.util.Log
import android.widget.Filter
import com.example.blinkit.adapters.AdapterProduct
import com.example.blinkit.models.Product
import java.util.Locale

class FilteringProducts(
    val adapter: AdapterProduct,
    val filter: ArrayList<Product>
) : Filter() {
    override fun performFiltering(constraint: CharSequence?): FilterResults {
        val result = FilterResults()
        if (!constraint.isNullOrEmpty()) {

            Log.d("check", "1 " + constraint)
            val filteredList = ArrayList<Product>()
            val query = constraint.toString().trim().uppercase(Locale.getDefault()).split(" ")

            for (products in filter) {
                if (query.any {
                        products.productTitle?.uppercase(Locale.getDefault())
                            ?.contains(it) == true ||
                                products.productPrice?.toString()?.uppercase(Locale.getDefault())
                                    ?.contains(it) == true ||
                                products.productCategory?.uppercase(Locale.getDefault())
                                    ?.contains(it) == true ||
                                products.productType?.uppercase(Locale.getDefault())
                                    ?.contains(it) == true
                    }) {
                    filteredList.add(products)
                }
            }

            Log.d("check", "2 " + filteredList.size.toString())
            result.values = filteredList
            result.count = filteredList.size
        } else {

            Log.d("check", "3 " + filter.size.toString())
            result.values = filter
            result.count = filter.size
        }

        return result
    }

    override fun publishResults(p0: CharSequence?, result: FilterResults?) {
        adapter.differ.submitList(result?.values as ArrayList<Product>)
    }

}

