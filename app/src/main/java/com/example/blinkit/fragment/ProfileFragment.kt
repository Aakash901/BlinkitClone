package com.example.blinkit.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.blinkit.R
import com.example.blinkit.utils.Utils
import com.example.blinkit.activity.AuthMainActivity
import com.example.blinkit.databinding.AddressBookLayoutBinding
import com.example.blinkit.databinding.FragmentProfileBinding
import com.example.blinkit.viewmodels.UserViewmodel


class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding

    val viewModel: UserViewmodel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentProfileBinding.inflate(layoutInflater)

        onBackButtonClicked()
        onOrdersLayoutClick()
        onAddressBookClicked()
        onLogOut()
        setUserMobileNo()
        return binding.root
    }

    private fun setUserMobileNo() {
        binding.etMob.text=viewModel.getUserMobileNumber()
    }

    private fun onLogOut() {
        binding.llLogOut.setOnClickListener {
            val bulider = AlertDialog.Builder(requireContext())
            val alertDialog = bulider.create()
            bulider.setTitle("Log out")
                .setMessage("Do you want to log out ?")
                .setPositiveButton("yes") { _, _ ->
                    viewModel.logOutUser()
                    startActivity(Intent(requireContext(),AuthMainActivity::class.java))
                    requireActivity().finish()
                }
                .setNegativeButton("No") { _, _ ->
                    alertDialog.dismiss()

                }
                .show()
                .setCancelable(false)
        }
    }

    private fun onAddressBookClicked() {
        binding.llAddress.setOnClickListener {
            val addressLayoutBinding: AddressBookLayoutBinding =
                AddressBookLayoutBinding.inflate(LayoutInflater.from(requireContext()))
            viewModel.getUserAddress { address ->
                addressLayoutBinding.etAddress.setText(address.toString())
            }
            val alertDialog = AlertDialog.Builder(requireContext())
                .setView(addressLayoutBinding.root)
                .create()
            alertDialog.show()
            addressLayoutBinding.btnEdit.setOnClickListener {
                addressLayoutBinding.etAddress.isEnabled = true
            }
            addressLayoutBinding.btnSave.setOnClickListener {
                viewModel.saveAddress(addressLayoutBinding.etAddress.text.toString().trim())
                alertDialog.dismiss()
                Utils.showToast(requireContext(), "Address updated !!")
            }
        }
    }

    private fun onOrdersLayoutClick() {
        binding.llOrders.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_ordersFragment)
        }
    }

    private fun onBackButtonClicked() {
        binding.tbSearchFragment.setNavigationOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_homeFragment)
        }
    }


}