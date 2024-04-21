package com.example.blinkit.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.blinkit.R
import com.example.blinkit.adapters.AdapterOrders
import com.example.blinkit.databinding.FragmentOrdersBinding
import com.example.blinkit.models.OrderItems
import com.example.blinkit.viewmodels.UserViewmodel
import kotlinx.coroutines.launch


class OrdersFragment : Fragment() {

    private lateinit var binding: FragmentOrdersBinding
    private val viewmodel: UserViewmodel by viewModels()
    private lateinit var adapterOrder: AdapterOrders

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentOrdersBinding.inflate(layoutInflater)
        getAllOrders()
        onBackButtonClicked()
        return binding.root
    }

    private fun getAllOrders() {
        binding.shimmerViewContainerOrders.visibility = View.VISIBLE
        binding.tvText.visibility = View.GONE
        lifecycleScope.launch {
            viewmodel.getAllOrders().collect { orderList ->
                if (orderList.isNotEmpty()) {
                    val orderedList = ArrayList<OrderItems>()
                    for (orders in orderList) {

                        val title = StringBuilder()
                        var totalPrice = 0

                        for (products in orders.orderList!!) {
                            val price = products.productPrice?.substring(1)?.toInt()
                            val itemCount = products.productCount!!
                            totalPrice += (price?.times(itemCount)!!)
                            title.append("${products.productCategory}")
                        }

                        val orderedItems = OrderItems(
                            orders.orderId,
                            orders.orderDate,
                            orders.orderStatus,
                            title.toString(),
                            totalPrice
                        )
                        orderedList.add(orderedItems)
                    }

                    adapterOrder = AdapterOrders(requireContext(), ::onOrderItemViewClicked)
                    binding.rvOrders.adapter = adapterOrder
                    adapterOrder.differ.submitList(orderedList)
                    binding.shimmerViewContainerOrders.visibility = View.GONE

                } else {

                    binding.shimmerViewContainerOrders.visibility = View.GONE
                    binding.tvText.visibility = View.VISIBLE

                }
            }
        }

    }

    fun onOrderItemViewClicked(orderItems: OrderItems) {
        val bundle = Bundle()
        bundle.putInt("status", orderItems.itemStatus!!.toInt())
        bundle.putString("orderId", orderItems.orderId)

        findNavController().navigate(R.id.action_ordersFragment_to_orderDetailFragment, bundle)


    }

    private fun onBackButtonClicked() {
        binding.tbSearchFragment.setNavigationOnClickListener {
            findNavController().navigate(R.id.action_ordersFragment_to_profileFragment)
        }
    }

}