package com.example.blinkit.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.blinkit.R
import com.example.blinkit.databinding.ItemViewOrdersBinding
import com.example.blinkit.models.OrderItems

class AdapterOrders(
    val context: Context,
    val onOrderItemViewClicked: (OrderItems) -> Unit
) : RecyclerView.Adapter<AdapterOrders.OrdersViewHolder>() {
    class OrdersViewHolder(val binding: ItemViewOrdersBinding) : ViewHolder(binding.root)

    val diffUtil = object : DiffUtil.ItemCallback<OrderItems>() {
        override fun areItemsTheSame(oldItem: OrderItems, newItem: OrderItems): Boolean {
            return oldItem.orderId == newItem.orderId
        }

        override fun areContentsTheSame(oldItem: OrderItems, newItem: OrderItems): Boolean {
            return oldItem == newItem
        }

    }

    val differ = AsyncListDiffer(this, diffUtil)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrdersViewHolder {
        return OrdersViewHolder(
            ItemViewOrdersBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }


    override fun onBindViewHolder(holder: OrdersViewHolder, position: Int) {
        val order = differ.currentList[position]
        holder.binding.apply {
            tvOrderTitle.text = order.itemTitle
            tvOrderDate.text = order.itemDate
            tvOrderAmount.text = "₹" + order.itemPrice.toString()
            when (order.itemStatus) {
                0 -> {

                    tvOrderStatus.text = "Ordered"
                    tvOrderStatus.backgroundTintList =
                        ContextCompat.getColorStateList(context, R.color.grayish_blue)


                }

                1 -> {

                    tvOrderStatus.text = "Recived"
//                    tvOrderStatus.setBackgroundColor(R.color.grayish_blue)
                    tvOrderStatus.backgroundTintList =
                        ContextCompat.getColorStateList(context, R.color.blue)

                }

                2 -> {

                    tvOrderStatus.text = "Dispatched"
//                    tvOrderStatus.setBackgroundColor(R.color.diya)
                    tvOrderStatus.backgroundTintList =
                        ContextCompat.getColorStateList(context, R.color.diya)

                }

                3 -> {

                    tvOrderStatus.text = "Delivered"
//                    tvOrderStatus.setBackgroundColor(R.color.green)
                    tvOrderStatus.backgroundTintList =
                        ContextCompat.getColorStateList(context, R.color.green)

                }
            }

        }
        holder.itemView.setOnClickListener {
            onOrderItemViewClicked(order)
        }
    }

}