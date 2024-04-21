package com.example.blinkit.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.blinkit.utils.CartListener
import com.example.blinkit.R
import com.example.blinkit.utils.Utils
import com.example.blinkit.adapters.AdapterProduct
import com.example.blinkit.databinding.FragmentCategoryBinding
import com.example.blinkit.databinding.ItemViewProductsBinding
import com.example.blinkit.models.Product
import com.example.blinkit.roomdb.CartProducts
import com.example.blinkit.viewmodels.UserViewmodel
import kotlinx.coroutines.launch

class CategoryFragment : Fragment() {

    private lateinit var binding: FragmentCategoryBinding
    val viewModel: UserViewmodel by viewModels()
    private lateinit var adapterProduct: AdapterProduct
    private var cartListener: CartListener? = null
    private var category: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCategoryBinding.inflate(layoutInflater)

        getProductCategory()
        fetchCategoryProduct()
        onsearchMenuClick()
        onNavigationItemClicked()
        return binding.root
    }

    private fun onNavigationItemClicked() {
        binding.tbSearchFragment.setNavigationOnClickListener {
            findNavController().navigate(R.id.action_categoryFragment_to_homeFragment)

        }
    }

    private fun onsearchMenuClick() {
        binding.tbSearchFragment.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.searchMenu -> {
                    findNavController().navigate(R.id.action_categoryFragment_to_searchFragment)
                    true
                }

                else -> false
            }

        }
    }

    private fun fetchCategoryProduct() {
        binding.shimmerViewContainer.visibility = View.VISIBLE
        lifecycleScope.launch {
            viewModel.getCategoryProducts(category!!).collect {
                if (it.isEmpty()) {
                    binding.rvProducts.visibility = View.GONE
                    binding.tvText.visibility = View.VISIBLE
                } else {
                    binding.rvProducts.visibility = View.VISIBLE
                    binding.tvText.visibility = View.GONE
                }

                adapterProduct = AdapterProduct(
                    ::onAddButtonClick,
                    ::onIncrementButtonClicked,
                    ::onDecrementButtonClicked
                )
                binding.rvProducts.adapter = adapterProduct
                adapterProduct.differ.submitList(it)
                adapterProduct.originalList = it as ArrayList<Product>
                binding.shimmerViewContainer.visibility = View.GONE
            }
        }
    }

    private fun getProductCategory() {
        val bundle = arguments
        category = bundle?.getString("category")
        binding.tbSearchFragment.title = category
    }

    private fun onAddButtonClick(product: Product, productsBinding: ItemViewProductsBinding) {
        productsBinding.tvAdd.visibility = View.GONE
        productsBinding.llProductCount.visibility = View.VISIBLE

        var itemCount = productsBinding.tvProductCount.text.toString().toInt()
        itemCount++
        productsBinding.tvProductCount.text = itemCount.toString()

        cartListener?.showCartLayout(1)

        product.itemCount = itemCount

        lifecycleScope.launch {
            ///saving item in shared pref
            cartListener?.savingCartItemCount(1)
            //saving item in room db
            savedProductInRoomDb(product)
            viewModel.updateItemCount(product, itemCount)

        }


    }

    private fun savedProductInRoomDb(product: Product) {

        val cartProducts = CartProducts(
            productId = product.productRandomId!!,
            productTitle = product.productTitle,
            productQuantity = product.productQuantity.toString() + product.productUnit.toString(),
            productPrice = "₹${product.productPrice}",
            productCount = product.itemCount,
            productStock = product.productStock,
            productImages = product.productImagesUris?.get(0)!!,
            productCategory = product.productCategory,
            adminUid = product.adminUid,
            productType = product.productType
        )

        lifecycleScope.launch {
            viewModel.insertCartProducts(cartProducts)
        }

    }

    private fun onIncrementButtonClicked(
        product: Product,
        productsBinding: ItemViewProductsBinding
    ) {
        var itemCountIncrement = productsBinding.tvProductCount.text.toString().toInt()
        itemCountIncrement++

        if (product.productStock!! + 1 > itemCountIncrement) {
            productsBinding.tvProductCount.text = itemCountIncrement.toString()

            cartListener?.showCartLayout(1)

            product.itemCount = itemCountIncrement

            lifecycleScope.launch {
                ///saving item in shared pref
                cartListener?.savingCartItemCount(1)
                //saving item in room db
                savedProductInRoomDb(product)
                viewModel.updateItemCount(product, itemCountIncrement)


            }
        } else {
            Utils.showToast(requireContext(), "Can't add more item of this")
        }


    }

    private fun onDecrementButtonClicked(
        product: Product,
        productsBinding: ItemViewProductsBinding
    ) {
        var itemCountDecrement = productsBinding.tvProductCount.text.toString().toInt()

        itemCountDecrement--

        product.itemCount = itemCountDecrement

        lifecycleScope.launch {
            ///saving item in shared pref
            cartListener?.savingCartItemCount(-1)
            //saving item in room db
            savedProductInRoomDb(product)
            viewModel.updateItemCount(product, itemCountDecrement)


        }
        if (itemCountDecrement > 0) {

            productsBinding.tvProductCount.text = itemCountDecrement.toString()

        } else {
            lifecycleScope.launch {
                viewModel.deleteCartProduct(product.productRandomId!!)
            }
            productsBinding.tvAdd.visibility = View.VISIBLE
            productsBinding.llProductCount.visibility = View.GONE
            productsBinding.tvProductCount.text = "0"
        }
        cartListener?.showCartLayout(-1)


    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is CartListener) {
            cartListener = context
        } else {
            throw ClassCastException("please implement cart listener")
        }
    }


}