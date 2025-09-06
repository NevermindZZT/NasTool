package com.letter.nastool.repository

import android.util.Log
import jcifs.CIFSContext
import jcifs.smb.SmbFile
import java.io.File
import java.io.FileOutputStream
import java.util.Properties

private const val TAG = "SmbRepo"

class SmbRepo(val serverIp: String, val username: String, val password: String) {

    val baseContext: CIFSContext by lazy {
        val props = Properties()
        props["jcifs.smb.client.disablePlainTextPasswords"] = "false"
        val base = jcifs.config.PropertyConfiguration(props)
        val baseContext = jcifs.context.BaseContext(base)
        baseContext.withCredentials(jcifs.smb.NtlmPasswordAuthenticator(null, username, password))
    }

    fun close() {
        try {
            baseContext.close()
        } catch (e: Exception) {
            Log.e(TAG, "", e)
        }
    }

    private fun getUrl(path: String): String {
        return if (path.startsWith("smb://")) {
            path
        } else {
            "smb://$serverIp/$path"
        }
    }

    fun checkConnection(path: String): Boolean {
        return try {
            val smbFile = SmbFile(getUrl(path), baseContext)
            smbFile.connect()
            smbFile.close()
            true
        } catch (e: Exception) {
            Log.i(TAG, "", e)
            false
        }
    }

    fun listFiles(path: String): Array<SmbFile> {
        try {
            val smbFile = SmbFile(getUrl(path), baseContext)
            val files = smbFile.listFiles()
            return files
        } catch (e: Exception) {
            Log.i(TAG, "", e)
        }
        return emptyArray()
    }

    fun download(smbFile: SmbFile, localPath: String): String? {
        try {
            smbFile.getInputStream().use { input ->
                val dest = if (File(localPath).isDirectory) {
                    if (localPath.endsWith(File.separator)) {
                        localPath + smbFile.name
                    } else {
                        "$localPath${File.separator}${smbFile.name}"
                    }
                } else {
                    localPath
                }
                val parentFile = File(dest).parentFile
                if (parentFile != null && !parentFile.exists()) {
                    parentFile.mkdirs()
                }
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
                return dest
            }
        } catch (e: Exception) {
            Log.e(TAG, "", e)
        }
        return null
    }
}