package com.example.blinkit.fragment

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.blinkit.utils.CartListener
import com.example.blinkit.utils.Constants
import com.example.blinkit.R
import com.example.blinkit.utils.Utils
import com.example.blinkit.adapters.AdapterCategory
import com.example.blinkit.adapters.AdapterProduct
import com.example.blinkit.adapters.BestSellerAdapter
import com.example.blinkit.databinding.BsSeeAllBinding
import com.example.blinkit.databinding.FragmentHomeBinding
import com.example.blinkit.databinding.ItemViewProductsBinding
import com.example.blinkit.models.BestSeller
import com.example.blinkit.models.Category
import com.example.blinkit.models.Product
import com.example.blinkit.roomdb.CartProducts
import com.example.blinkit.viewmodels.UserViewmodel
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapterBestSeller: BestSellerAdapter
    private lateinit var adapterProduct: AdapterProduct

    private var cartListener: CartListener? = null
    val viewModel: UserViewmodel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater)
        setStatusBarColor()
        setAllCategories()
        navigatingToSearchFragment()
        onProfileBtnClicked()
        fetchBestSeller()
        return binding.root
    }

    private fun fetchBestSeller() {
        binding.shimmerViewContainer.visibility=View.VISIBLE
        lifecycleScope.launch {
            viewModel.fetchProductType().collect{
                adapterBestSeller=BestSellerAdapter(::seeAllBtnClicked)
                binding.rvBestselers.adapter=adapterBestSeller
                adapterBestSeller.differ.submitList(it)
                binding.shimmerViewContainer.visibility=View.GONE
            }

        }
    }

    private fun onProfileBtnClicked() {
        binding.ivProfile.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
        }
    }

    private fun navigatingToSearchFragment() {
        binding.searchCv.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
        }
    }

    private fun setAllCategories() {
        val categoriesList = ArrayList<Category>()


        for (i in 0 until Constants.allProductsCategory.size) {
            categoriesList.add(
                Category(
                    Constants.allProductsCategory[i],
                    Constants.allProductsCategoryIcon[i]
                )
            )
        }

        binding.rvCategories.adapter = AdapterCategory(categoriesList,::onCategoryClicked)

    }

    fun seeAllBtnClicked(productType:BestSeller){
            val bsSeeAllBinding=BsSeeAllBinding.inflate(LayoutInflater.from(requireContext()))
        val bs=BottomSheetDialog(requireContext())
        bs.setContentView(bsSeeAllBinding.root)

        adapterProduct= AdapterProduct(::onAddButtonClick,::onIncrementButtonClicked,::onDecrementButtonClicked)
        bsSeeAllBinding.rvProducts.adapter=adapterProduct
        adapterProduct.differ.submitList(productType.products)

        bs.show()
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

    fun onCategoryClicked(category: Category){
        val bundle=Bundle()
        bundle.putString("category",category.title)
       findNavController().navigate(R.id.action_homeFragment_to_categoryFragment,bundle)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is CartListener) {
            cartListener = context
        } else {
            throw ClassCastException("please implement cart listener")
        }
    }



    private fun setStatusBarColor() {
        activity?.window?.apply {
            val statusBarColors = ContextCompat.getColor(requireContext(), R.color.diyaFlame)
            statusBarColor = statusBarColors
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }

}