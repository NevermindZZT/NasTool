package com.letter.nastool.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableList
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

class BindingViewAdapter<T> constructor(private val context: Context,
                                        @LayoutRes private val layoutRes: Int,
                                        private val list: ObservableList<T>?,
                                        private val onBinding: ((binding: ViewDataBinding, item: T, position: Int) -> Unit)? = null)
    : RecyclerView.Adapter<BindingViewAdapter.BindingViewHolder<ViewDataBinding>>() {

    init {
        list?.addOnListChangedCallback(object : ObservableList.OnListChangedCallback<ObservableList<T>>() {
            override fun onChanged(sender: ObservableList<T>?) {
                notifyDataSetChanged()
            }

            override fun onItemRangeRemoved(
                sender: ObservableList<T>?,
                positionStart: Int,
                itemCount: Int
            ) {
                notifyItemRangeRemoved(positionStart, itemCount)
            }

            override fun onItemRangeMoved(
                sender: ObservableList<T>?,
                fromPosition: Int,
                toPosition: Int,
                itemCount: Int
            ) {
                notifyItemMoved(fromPosition, toPosition)
            }

            override fun onItemRangeInserted(
                sender: ObservableList<T>?,
                positionStart: Int,
                itemCount: Int
            ) {
                notifyItemRangeInserted(positionStart, itemCount)
            }

            override fun onItemRangeChanged(
                sender: ObservableList<T>?,
                positionStart: Int,
                itemCount: Int
            ) {
                notifyItemRangeChanged(positionStart, itemCount)
            }

        })
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = BindingViewHolder(
        DataBindingUtil.inflate<ViewDataBinding>(
            LayoutInflater.from(context),
            layoutRes,
            parent,
            false
        )
    )

    override fun onBindViewHolder(
        holder: BindingViewHolder<ViewDataBinding>,
        position: Int
    ) {
        onBinding?.invoke(holder.binding, list!![position], position)
        holder.binding.executePendingBindings()
    }

    override fun getItemCount() = list?.size ?: 0

    /**
     * DataBinding 列表适配器ViewHolder
     * @param T : ViewDataBinding 数据绑定类型
     * @property binding T 数据绑定对象
     * @constructor 构造一个ViewHolder
     *
     * @author Letter(nevermindzzt@gmail.com)
     * @since 1.0.0
     */
    class BindingViewHolder<out T : ViewDataBinding>
    constructor(val binding : T)
        : RecyclerView.ViewHolder(binding.root)
}