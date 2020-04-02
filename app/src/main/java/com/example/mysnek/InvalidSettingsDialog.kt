package com.example.mysnek

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class InvalidSettingsDialog: DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return requireActivity().let {
            val builder = AlertDialog.Builder(it)

            builder
                .setMessage(R.string.dialog_invalid_settings)
                .setPositiveButton(R.string.dialog_invalid_settings_play) { _, _ ->
                }
                .setNegativeButton(R.string.dialog_invalid_settings_back) { _, _ ->  }

            builder.create()
        }
    }
}