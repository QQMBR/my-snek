package com.example.mysnek

import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.preference.DropDownPreference
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
        val startSpeed : EditTextPreference? = findPreference("snake_start_slowness")
        val maxSpeed : EditTextPreference? = findPreference("snake_min_slowness")
        val countsToMax : EditTextPreference? = findPreference("counts_to_max")
        val switchThemePreference: SwitchPreferenceCompat? = findPreference("switch_theme")
        val accelModePreference: DropDownPreference? = findPreference("accel_mode")
        val slowdownPerApple: EditTextPreference? = findPreference("accel_mode_each_slowdown")

        isGridPreference?.also {
            updateWidthHeight(gridWidthPreference, gridHeightPreference, it.isChecked)
        }
        accelModePreference?.also {
            updateSpeedSettings(startSpeed, maxSpeed, countsToMax, slowdownPerApple, it.value.toAccelMode())
        }

        setInputType(startSizePreference, InputType.TYPE_CLASS_NUMBER)
        setInputType(gridHeightPreference, InputType.TYPE_CLASS_NUMBER)
        setInputType(gridWidthPreference, InputType.TYPE_CLASS_NUMBER)
        setInputType(startSpeed, InputType.TYPE_CLASS_NUMBER)
        setInputType(maxSpeed, InputType.TYPE_CLASS_NUMBER)
        setInputType(slowdownPerApple, InputType.TYPE_CLASS_NUMBER)
        setInputType(countsToMax, InputType.TYPE_CLASS_NUMBER)

        setNumberInputSummary(startSizePreference) { x -> "Size: $x"}
        setNumberInputSummary(gridWidthPreference)
        setNumberInputSummary(gridHeightPreference)
        setNumberInputSummary(startSpeed) { x -> "$x moves/s"}
        setNumberInputSummary(maxSpeed) { x -> "$x moves/s"}
        setNumberInputSummary(slowdownPerApple) { x -> "-$x moves"}
        setNumberInputSummary(countsToMax) { x -> "$x" }

        setAccelModeInputSummary(accelModePreference)

        //TODO hide accel settings on accel mode NONE, etc.
        //TODO update height on switch to true


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
            setOnPreferenceChangeListener { _, alternativePref ->
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

        accelModePreference?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                when (newValue) {
                    is String -> {
                        val mode = SnekSettings.AccelerationMode.fromString(newValue)

                        updateSpeedSettings(startSpeed, maxSpeed, countsToMax, slowdownPerApple, mode)
                    }
                }

                true
            }
        }
    }

    private fun updateSpeedSettings(
        startSpeedPreference: EditTextPreference?,
        maxSpeedPreference: EditTextPreference?,
        countsToMaxPreference: EditTextPreference?,
        slowdownPerApplePreference: EditTextPreference?,
        mode: SnekSettings.AccelerationMode?
    ) {
        if (startSpeedPreference != null && maxSpeedPreference != null
            && countsToMaxPreference != null && slowdownPerApplePreference != null && mode != null) {

            when (mode) {
                SnekSettings.AccelerationMode.NONE -> {
                    //hide everything but the start speed, which is the constant speed in this case
                    maxSpeedPreference.isVisible = false
                    countsToMaxPreference.isVisible = false
                    slowdownPerApplePreference.isVisible = false

                    startSpeedPreference.title = "Speed"

                    Log.d(TAG, "Changed acceleration mode to NONE")
                }
                SnekSettings.AccelerationMode.APPLE -> {
                    //hide the slowdown per apple preference

                    maxSpeedPreference.isVisible = true
                    countsToMaxPreference.isVisible = true
                    slowdownPerApplePreference.isVisible = false

                    startSpeedPreference.title = "Initial speed"
                    countsToMaxPreference.title = "Apples to max. speed"

                }
                SnekSettings.AccelerationMode.EACH -> {
                    //everything is visible
                    maxSpeedPreference.isVisible = true
                    countsToMaxPreference.isVisible = true
                    slowdownPerApplePreference.isVisible = true

                    startSpeedPreference.title = "Initial speed"
                    countsToMaxPreference.title = "Moves to max. speed"
                }
            }
        }
    }
    private fun updateWidthHeight(widthPreference: EditTextPreference?, heightPreference: EditTextPreference?, square: Boolean) {
        widthPreference?.also { width ->
            heightPreference?.also { height ->
                if (square) {
                    width.title = "Side lengths"
                    height.isVisible = false
                    height.text = width.text

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
    companion object {
        const val TAG = "SettingsFragment"
    }
}
