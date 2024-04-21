package com.example.blinkit.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.blinkit.utils.CartListener
import com.example.blinkit.adapters.AdapterCartProducts
import com.example.blinkit.databinding.ActivityUsersBinding
import com.example.blinkit.databinding.BsCartProductsBinding
import com.example.blinkit.roomdb.CartProducts
import com.example.blinkit.viewmodels.UserViewmodel
import com.google.android.material.bottomsheet.BottomSheetDialog

class UsersMainActivity : AppCompatActivity(), CartListener {

    private lateinit var binding: ActivityUsersBinding
    val viewModel: UserViewmodel by viewModels()
    private lateinit var cartProductsList :List<CartProducts>
    private lateinit var adapterCartProducts: AdapterCartProducts

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getTotalItemCount()
        onCartClicked()
        getAllProducts()
        onNextButtonClicked()
    }

    private fun onNextButtonClicked() {
       binding.btnNext.setOnClickListener {
           startActivity(Intent(this,OrderPlacedActivity::class.java))
       }
    }

    private fun getAllProducts() {
       viewModel.getAll().observe(this){
           cartProductsList=it
       }
    }

    private fun onCartClicked() {
        binding.llItemCart.setOnClickListener {
            val bsCartProductBinding =  BsCartProductsBinding.inflate(LayoutInflater.from(this))

            val bs = BottomSheetDialog(this)
            bs.setContentView(bsCartProductBinding.root)


            bsCartProductBinding.tvNumberOfProductCount.text=binding.tvNumberOfProductCount.text

            bsCartProductBinding.btnNext.setOnClickListener {
                startActivity(Intent(this,OrderPlacedActivity::class.java))
            }

            adapterCartProducts=AdapterCartProducts()
            bsCartProductBinding.rvProductItems.adapter=adapterCartProducts
            adapterCartProducts.differ.submitList(cartProductsList)

            bs.show()
        }
    }

    private fun getTotalItemCount() {
        viewModel.fetchTotalCartItemCount().observe(this) {
            if (it > 0) {
                binding.llCart.visibility = View.VISIBLE
                binding.tvNumberOfProductCount.text = it.toString()
            } else {
                binding.llCart.visibility = View.GONE
            }
        }
    }

    override fun showCartLayout(itemCount: Int) {
        val previousCount = binding.tvNumberOfProductCount.text.toString().toInt()
        val updatedCount = previousCount + itemCount
        if (updatedCount > 0) {
            binding.llCart.visibility = View.VISIBLE
            binding.tvNumberOfProductCount.text = updatedCount.toString()
        } else {

            binding.llCart.visibility = View.GONE
            binding.tvNumberOfProductCount.text = "0"
        }
    }

    override fun savingCartItemCount(itemCount: Int) {
        viewModel.fetchTotalCartItemCount().observe(this) {
            viewModel.savingCartItemCount(it + itemCount)
        }

    }

    override fun hideCartLayout() {
       binding.llCart.visibility=View.GONE
        binding.tvNumberOfProductCount.text="0"
    }
}