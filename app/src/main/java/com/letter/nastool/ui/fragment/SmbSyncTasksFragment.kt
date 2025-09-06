package com.letter.nastool.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.letter.nastool.R
import com.letter.nastool.adapter.BindingViewAdapter
import com.letter.nastool.adapter.OnItemViewClick
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
                    binding.onItemViewClick = this@SmbSyncTasksFragment
                    model.getTaskInfo(position) { info ->
                        binding.infoText.text = info
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
                model.run(position)
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