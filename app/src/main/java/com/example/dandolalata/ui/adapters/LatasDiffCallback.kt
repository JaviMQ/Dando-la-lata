package com.example.dandolalata.ui.adapters

import androidx.recyclerview.widget.DiffUtil
import com.example.dandolalata.data.entities.Lata

class LatasDiffCallback(
    private val oldList: List<Lata>,
    private val newList: List<Lata>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
