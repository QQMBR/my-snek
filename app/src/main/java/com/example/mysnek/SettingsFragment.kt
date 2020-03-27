package com.example.mysnek

import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.game_settings, rootKey)

        val startSizePreference : EditTextPreference? = findPreference("start_size")
        val isGridPreference : SwitchPreferenceCompat? = findPreference("square_grid")
        val gridWidthPreference : EditTextPreference? = findPreference("grid_width")
        val gridHeightPreference : EditTextPreference? = findPreference("grid_height")
        val startSlowness : EditTextPreference? = findPreference("snake_start_slowness")
        val minSlowness : EditTextPreference? = findPreference("snake_min_slowness")
        val expFactor : EditTextPreference? = findPreference("snake_exp_factor")
        val switchThemePreference: SwitchPreferenceCompat? = findPreference("switch_theme")

        isGridPreference?.also {
            updateWidthHeight(gridWidthPreference, gridHeightPreference, it.isChecked)
        }

        setInputType(startSizePreference, InputType.TYPE_CLASS_NUMBER)
        setInputType(gridHeightPreference, InputType.TYPE_CLASS_NUMBER)
        setInputType(gridWidthPreference, InputType.TYPE_CLASS_NUMBER)
        setInputType(startSlowness, InputType.TYPE_CLASS_NUMBER)
        setInputType(minSlowness, InputType.TYPE_CLASS_NUMBER)

        setInputType(expFactor, InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)

        setNumberInputSummary(startSizePreference) { x -> "Size: $x"}
        setNumberInputSummary(gridWidthPreference)
        setNumberInputSummary(gridHeightPreference)
        setNumberInputSummary(startSlowness) { x -> "$x ms/move"}
        setNumberInputSummary(minSlowness) { x -> "$x ms/move"}

        setFloatInputSummary(expFactor)

        isGridPreference?.apply {
            setOnPreferenceChangeListener { _, isGrid ->
                when (isGrid) {
                    is Boolean -> {

                        updateWidthHeight(gridWidthPreference, gridHeightPreference, isGrid)
                    }
                }

                true
            }
        }

        switchThemePreference?.apply {
            setOnPreferenceChangeListener { preference, alternativePref ->
                when (alternativePref) {
                    is Boolean -> {
                        if (alternativePref) {
                            Log.d("Settings", "Using AppTheme")
                        }
                        else {
                            Log.d("Settings", "Using StrongTheme")
                        }
                    }
                }

                true
            }
        }

    }

    private fun updateWidthHeight(widthPreference: EditTextPreference?, heightPreference: EditTextPreference?, square: Boolean) {
        widthPreference?.also { width ->
            heightPreference?.also { height ->
                if (square) {
                    width.title = "Side lengths"
                    height.isVisible = false

                    width.setOnPreferenceChangeListener { preference, newValue ->
                        when (preference) {
                            is EditTextPreference -> {
                                when (newValue) {
                                    is String -> {
                                        if (newValue.toIntOrNull() != null) {
                                            height.text = newValue
                                            Log.d("Settings", "Updated height to $newValue")
                                        }
                                    }
                                }
                            }
                        }

                        true
                    }
                }
                else {
                    width.title = "Width"
                    height.isVisible = true

                    width.onPreferenceChangeListener = null
                }
            }
        }
    }
}
