package com.example.blinkit.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.blinkit.R
import com.example.blinkit.adapters.AdapterCartProducts
import com.example.blinkit.databinding.FragmentOrderDetailBinding
import com.example.blinkit.viewmodels.UserViewmodel
import kotlinx.coroutines.launch


class OrderDetailFragment : Fragment() {

    private lateinit var binding: FragmentOrderDetailBinding
    private lateinit var adapterCartProducts: AdapterCartProducts
    private val viewmodel: UserViewmodel by viewModels()
    private var status = 0
    private var orderId = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOrderDetailBinding.inflate(layoutInflater)

        getValues()
        settingStatus()
        onBackButtonClicked()
        lifecycleScope.launch {
            getOrderedProducts()
        }
        return binding.root
    }

    suspend private fun getOrderedProducts() {
        viewmodel.getOrderedProducts(orderId).collect { cartProductList ->

            adapterCartProducts = AdapterCartProducts()
            binding.rvProductItems.adapter = adapterCartProducts
            adapterCartProducts.differ.submitList(cartProductList)

        }
    }

    private fun settingStatus() {
        val statusToViews = mapOf(
            0 to listOf(binding.iv1),
            1 to listOf(binding.iv1, binding.iv2, binding.view1),
            2 to listOf(binding.iv1, binding.iv2, binding.view1, binding.iv3, binding.view2),
            3 to listOf(
                binding.iv1,
                binding.iv2,
                binding.view1,
                binding.iv3,
                binding.view2,
                binding.iv4,
                binding.view3
            )
        )

        val viewsToTint = statusToViews.getOrDefault(status, emptyList())
        for (view in viewsToTint) {
            view.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.blue)
        }
    }

    private fun getValues() {
        val bundle = arguments
        status = bundle?.getInt("status")!!
        orderId = bundle.getString("orderId").toString()

    }

    private fun onBackButtonClicked() {
        binding.tbOrderDetailFragment.setNavigationOnClickListener {
            findNavController().navigate(R.id.action_ordersFragment_to_orderDetailFragment)
        }
    }
}