package com.letter.nastool.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.letter.nastool.R
import com.letter.nastool.adapter.BindingViewAdapter
import com.letter.nastool.databinding.FragmentSmbSyncDownloadInfosBinding
import com.letter.nastool.databinding.LayoutSmbSyncDownloadInfoItemBinding
import com.letter.nastool.viewmodel.SymSyncDownloadInfosViewModel

private const val TAG = "SmbSyncDownloadInfosFragment"

class SmbSyncDownloadInfosFragment: Fragment() {

    private lateinit var binding: FragmentSmbSyncDownloadInfosBinding

    private val model by lazy {
        ViewModelProvider(this)[SymSyncDownloadInfosViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSmbSyncDownloadInfosBinding.inflate(inflater)
        initBinding()
        initModel()
        return binding.root
    }

    private fun initBinding() {
        binding.let {
            it.lifecycleOwner = this
            it.list = model.infos.value
        }
    }

    private fun initModel() {
        model.apply {
            infos.observe(this@SmbSyncDownloadInfosFragment.viewLifecycleOwner) {
                val adapter = BindingViewAdapter(
                    this@SmbSyncDownloadInfosFragment.requireContext(),
                    R.layout.layout_smb_sync_download_info_item,
                    it
                ) { binding, item, position ->
                    binding as LayoutSmbSyncDownloadInfoItemBinding
                    binding.model = model
                    binding.item = item
                    binding.position = position

                    item.onInfoChanged = {
                        binding.invalidateAll()
                    }
                }
                binding.recyclerView.apply {
                    this.adapter = adapter
                    layoutManager = LinearLayoutManager(this@SmbSyncDownloadInfosFragment.requireContext())
                }
            }
        }
    }
}