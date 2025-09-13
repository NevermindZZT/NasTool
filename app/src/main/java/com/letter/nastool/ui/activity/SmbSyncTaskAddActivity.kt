package com.letter.nastool.ui.activity

import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.letter.nastool.R
import com.letter.nastool.databinding.ActivitySmbSyncTaskAddBinding
import com.letter.nastool.ui.dialog.FileChooseDialogBuilder
import com.letter.nastool.viewmodel.SmbSyncTaskAddViewModel

class SmbSyncTaskAddActivity : AppCompatActivity(), View.OnClickListener, Toolbar.OnMenuItemClickListener {

    private lateinit var binding: ActivitySmbSyncTaskAddBinding
    private val model by lazy {
        ViewModelProvider(this)[SmbSyncTaskAddViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySmbSyncTaskAddBinding.inflate(layoutInflater)
        initBinding()
        initModel()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    private fun initBinding() {
        binding.let {
            it.lifecycleOwner = this
            it.model = model
            it.onClick = this
            it.materialToolbar.setOnMenuItemClickListener(this)
        }
    }

    private fun initModel() {
        val id = intent.getIntExtra("id", -1)
        model.apply {
            load(id)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.savaButton -> {
                val dialog = MaterialAlertDialogBuilder(this)
                    .setView(R.layout.layout_dialog_smb_sync_task_add_checking)
                    .setCancelable(false)
                    .create().apply {
                        show()
                    }
                model.save {
                    if (it) {
                        dialog.dismiss()
                        finish()
                    } else {
                        dialog.dismiss()
                        Toast.makeText(
                            this,
                            R.string.activity_smb_sync_task_add_toast_check_failed,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            R.id.chooseRemoteButton -> {
                FileChooseDialogBuilder(this)
                    .setRootDir("/")
                    .setShowProgressWhenList(true)
                    .setOnFileList { path ->
                        model.listSmbFiles(this, path)
                    }
                    .setOnFileChosen { dialog, path ->
                        model.remotePath.value = path
                        dialog.dismiss()
                    }
                    .setTitle(R.string.activity_smb_sync_task_add_dialog_choose_remote_title)
                    .create().apply {
                        show()
                    }
            }
            R.id.chooseLocalButton ->{
                FileChooseDialogBuilder(this)
                    .setRootDir("/sdcard/")
                    .setOnFileList { path ->
                        model.listLocalFiles(path)
                    }
                    .setOnFileChosen { dialog, path ->
                        model.localPath.value = path
                        dialog.dismiss()
                    }
                    .setTitle(R.string.activity_smb_sync_task_add_dialog_choose_local_title)
                    .create().apply {
                        show()
                    }
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.delete -> {
                if (model.task == null) {
                    Toast.makeText(
                        this,
                        R.string.activity_smb_sync_task_add_toast_delete_no_such_task,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.activity_smb_sync_task_add_dialog_delete_title)
                        .setMessage(R.string.activity_smb_sync_task_add_dialog_delete_message)
                        .setPositiveButton(R.string.activity_smb_sync_task_add_dialog_delete_positive_button) { dialog, _ ->
                            model.delete()
                            dialog.dismiss()
                            finish()
                        }
                        .setNegativeButton(R.string.activity_smb_sync_task_add_dialog_delete_negative_button) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .create()
                        .show()
                }
            }
        }
        return true
    }
}