package com.letter.nastool.ui.dialog

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.databinding.ObservableArrayList
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.letter.nastool.R
import com.letter.nastool.adapter.BindingViewAdapter
import com.letter.nastool.adapter.OnItemViewClick
import com.letter.nastool.data.local.FileChooseInfo
import com.letter.nastool.databinding.LayoutDialogFiileChooseBinding
import com.letter.nastool.databinding.LayoutDialogFileChooseItemBinding
import com.letter.nastool.utils.ext.joinPath

class FileChooseDialogBuilder(context: Context): MaterialAlertDialogBuilder(context), View.OnClickListener, OnItemViewClick {

    private var binding: LayoutDialogFiileChooseBinding = LayoutDialogFiileChooseBinding.inflate(LayoutInflater.from(context))

    private var onFileChosen: ((dialog: AlertDialog, path: String) -> Unit)? = null

    private var onFileList: ((path: String) -> List<FileChooseInfo>)? = null

    private var currentDir = "/"

    private val files = ObservableArrayList<FileChooseInfo>()

    private var dialog: AlertDialog? = null

    private var showProgressWhenFresh = false

    private var progressing = false

    private val handler = Handler(Looper.getMainLooper())

    init {
        initBinding()
        setView(binding.root)
    }

    private fun initBinding() {
        binding.let {
            it.onClick = this
        }
    }

    private fun freshFileList() {
        if (showProgressWhenFresh) {
            binding.progressBar.visibility = View.VISIBLE
            progressing = true
        }
        Thread {
            val list =  onFileList?.invoke(currentDir)
            handler.post {
                files.clear()
                if (list != null) {
                    this.files.addAll(list)
                }
                val adapter = BindingViewAdapter(
                    context,
                    R.layout.layout_dialog_file_choose_item,
                    files
                ) { binding, item, position ->
                    binding as LayoutDialogFileChooseItemBinding
                    binding.item = item
                    binding.position = position
                    binding.onItemViewClick = this
                }
                binding.recyclerView.apply {
                    this.adapter = adapter
                    layoutManager = LinearLayoutManager(context)
                }
                if (showProgressWhenFresh) {
                    binding.progressBar.visibility = View.GONE
                    progressing = false
                }
            }
        }.start()
    }

    fun setRootDir(path: String): FileChooseDialogBuilder {
        currentDir = path
        return this
    }

    fun setOnFileList(onFileList: (path: String) -> List<FileChooseInfo>): FileChooseDialogBuilder {
        this.onFileList = onFileList
        return this
    }

    fun setShowProgressWhenList(show: Boolean): FileChooseDialogBuilder {
        this.showProgressWhenFresh = show
        return this
    }

    fun setOnFileChosen(onFileChosen: (dialog: AlertDialog, path: String) -> Unit): FileChooseDialogBuilder {
        this.onFileChosen = onFileChosen
        return this
    }

    override fun create(): AlertDialog {
        binding.root.post {
            freshFileList()
        }
        dialog = super.create()
        return dialog!!
    }

    override fun onClick(view: View?) {
        if (progressing) {
            return
        }
        when (view?.id) {
            R.id.chooseCurrentDir -> {
                onFileChosen?.invoke(dialog!!, currentDir)
            }
            R.id.backToParentDir -> {
                if (currentDir != "/") {
                    if (currentDir.endsWith("/")) {
                        currentDir = currentDir.dropLast(1)
                    }
                    currentDir = currentDir.substringBeforeLast('/', "/")
                    freshFileList()
                }
            }
        }
    }

    override fun onClick(position: Int, view: View?) {
        if (progressing) {
            return
        }
        val file = files[position]
        if (file.isDirectory) {
            currentDir = currentDir.joinPath(file.name)
            freshFileList()
        } else {
            onFileChosen?.invoke(dialog!!, currentDir.joinPath(file.name))
        }
    }
}