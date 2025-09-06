package com.letter.nastool.ui.fragment

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.letter.nastool.R
import com.letter.nastool.viewmodel.SettingsViewModel

class SettingsFragment: PreferenceFragmentCompat() {

    private val model by lazy {
        ViewModelProvider(this)[SettingsViewModel::class.java]
    }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?
    ) {
        setPreferencesFromResource(R.xml.settings_preference, rootKey)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            "clear_file_sync_infos" -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.fragment_settings_dialog_clear_file_sync_infos_title)
                    .setMessage(R.string.fragment_settings_dialog_clear_file_sync_infos_message)
                    .setPositiveButton(R.string.fragment_settings_dialog_clear_file_sync_infos_positive_button) { dialog, _ ->
                        model.clearFileSyncInfos()
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.fragment_settings_dialog_clear_file_sync_infos_negative_button) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }
        }
        return super.onPreferenceTreeClick(preference)
    }
}