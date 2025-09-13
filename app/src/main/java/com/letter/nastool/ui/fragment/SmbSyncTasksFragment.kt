package com.letter.nastool.ui.fragment

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.letter.nastool.R
import com.letter.nastool.adapter.BindingViewAdapter
import com.letter.nastool.adapter.OnItemViewClick
import com.letter.nastool.data.local.SmbSyncTaskInfo
import com.letter.nastool.databinding.FragmentSmbSyncTasksBinding
import com.letter.nastool.databinding.LayoutSmbSyncTaskItemBinding
import com.letter.nastool.ui.activity.SmbSyncTaskAddActivity
import com.letter.nastool.viewmodel.SmbSyncTaskListsViewModel

private const val TAG = "SmbSyncTasksFragment"

class SmbSyncTasksFragment: Fragment(), View.OnClickListener, OnItemViewClick {

    private lateinit var binding: FragmentSmbSyncTasksBinding

    private val model by lazy {
        ViewModelProvider(this)[SmbSyncTaskListsViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSmbSyncTasksBinding.inflate(inflater)
        initBinding()
        initModel()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        model.loadTasks()
    }

    private fun initBinding() {
        binding.let {
            it.lifecycleOwner = this
            it.onClick = this
        }
    }

    private fun initModel() {
        model.apply {
            tasks.observe(this@SmbSyncTasksFragment.viewLifecycleOwner) { it ->
                val adapter = BindingViewAdapter(
                    this@SmbSyncTasksFragment.requireContext(),
                    R.layout.layout_smb_sync_task_item,
                    it
                ) { binding, item, position ->
                    binding as LayoutSmbSyncTaskItemBinding
                    binding.model = model
                    binding.item = item
                    binding.position = position
                    binding.syncing = false
                    binding.onItemViewClick = this@SmbSyncTasksFragment
                    model.getTaskInfo(position) { info ->
                        Log.i(TAG, "initModel: info=$info")
                        val stringBuilder = StringBuilder()
                        stringBuilder.append(
                            getString(R.string.fragment_smb_sync_tasks_task_info_total).format(info.total)
                        )
                        stringBuilder.append("\n")
                        stringBuilder.append(
                            getString(R.string.fragment_smb_sync_tasks_task_info_synced).format(info.synced)
                        )
                        if (info.updates != 0) {
                            stringBuilder.append("\n")
                            stringBuilder.append(
                                getString(R.string.fragment_smb_sync_tasks_task_info_updates).format(info.updates)
                            )
                        }
                        if (info.state == SmbSyncTaskInfo.STATE_SYNCING) {
                            stringBuilder.append("\n")
                            stringBuilder.append(
                                getString(R.string.fragment_smb_sync_tasks_task_info_syncing)
                            )
                        }
                        binding.root.post {
                            binding.syncing = info.state == SmbSyncTaskInfo.STATE_SYNCING
                            binding.infoText.text = stringBuilder.toString()
                        }
                    }
                }
                binding.recyclerView.apply {
                    this.adapter = adapter
                    layoutManager = LinearLayoutManager(this@SmbSyncTasksFragment.requireContext())
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.floatingActionButton -> {
                startActivity(Intent(requireContext(), SmbSyncTaskAddActivity::class.java))
            }
        }
    }

    override fun onClick(position: Int, view: View?) {
        when (view?.id) {
            R.id.runButton -> {
                if (model.isSyncing(position)) {
                    model.stop(position)
                } else {
                    model.run(position)
                }
            }
            R.id.itemView -> {
                startActivity(
                    Intent(
                        requireContext(),
                        SmbSyncTaskAddActivity::class.java
                    ).apply {
                        putExtra("id", model.tasks.value?.get(position)?.id)
                    }
                )
            }
        }
    }
}