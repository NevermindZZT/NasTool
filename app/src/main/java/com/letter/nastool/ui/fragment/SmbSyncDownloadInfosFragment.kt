package com.letter.nastool.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.letter.nastool.R
import com.letter.nastool.adapter.BindingViewAdapter
import com.letter.nastool.adapter.OnItemViewClick
import com.letter.nastool.data.local.SmbSyncDownloadInfo
import com.letter.nastool.databinding.FragmentSmbSyncDownloadInfosBinding
import com.letter.nastool.databinding.LayoutSmbSyncDownloadInfoItemBinding
import com.letter.nastool.viewmodel.SymSyncDownloadInfosViewModel

private const val TAG = "SmbSyncDownloadInfosFragment"

class SmbSyncDownloadInfosFragment: Fragment(), OnItemViewClick {

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
                    binding.onItemViewClick = this@SmbSyncDownloadInfosFragment

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

    override fun onClick(position: Int, view: View?) {
        when (view?.id) {
            R.id.itemView -> {
                val info = model.infos.value?.get(position) ?: return
                Log.i(TAG, "onClick: itemView $position, info: $info")
                if (info.state == SmbSyncDownloadInfo.STATE_FAILED) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.fragment_smb_sync_download_dialog_failed_info_title)
                        .setMessage(info.info)
                        .setPositiveButton(R.string.fragment_smb_sync_download_dialog_failed_info_positive_button) { dialog, _ ->
                            dialog.dismiss()
                        }.apply {
                            show()
                        }
                }
            }
        }
    }
}