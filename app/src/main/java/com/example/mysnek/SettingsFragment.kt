package com.example.mysnek

import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.preference.DropDownPreference
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

class SettingsFragment : PreferenceFragmentCompat() {
    private var wasAppleAccel: Boolean = true

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Log.d(TAG, "Creating preferences")
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
        val startingAccel : SeekBarPreference? = findPreference("starting_accel")

        wasAppleAccel = {
            val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

            when (prefs.getString("accel_mode", SnekSettings.AccelerationMode.NONE.toString())!!.toAccelMode()) {
                SnekSettings.AccelerationMode.APPLE -> {
                    true
                }
                SnekSettings.AccelerationMode.EACH -> {
                    false
                }
                else -> {
                    false
                }
            }
        } ()

        isGridPreference?.also {
            updateWidthHeight(gridWidthPreference, gridHeightPreference, it.isChecked, startSizePreference)
        }
        accelModePreference?.also {
            updateSpeedSettings(startSpeed, maxSpeed, countsToMax, slowdownPerApple, startingAccel, it.value.toAccelMode())
        }

        startingAccel?.apply {

            updateStartingAccelSummary(this, this.value)

            setOnPreferenceChangeListener { preference, newValue ->
                when (preference) {
                    is SeekBarPreference -> {
                        updateStartingAccelSummary(preference, newValue as Int)
                    }
                }

                true
            }
        }

        startingAccel?.updatesContinuously = true
        startingAccel?.max = 30

        setInputType(startSizePreference, InputType.TYPE_CLASS_NUMBER)
        setInputType(gridHeightPreference, InputType.TYPE_CLASS_NUMBER)
        setInputType(gridWidthPreference, InputType.TYPE_CLASS_NUMBER)
        setInputType(startSpeed, InputType.TYPE_CLASS_NUMBER)
        setInputType(maxSpeed, InputType.TYPE_CLASS_NUMBER)
        setInputType(slowdownPerApple, InputType.TYPE_CLASS_NUMBER)
        setInputType(countsToMax, InputType.TYPE_CLASS_NUMBER)

        val width = gridWidthPreference?.text?.toIntOrNull()
        val height = gridHeightPreference?.text?.toIntOrNull()

        if (width != null && height != null) {
            updateAllowedStartSize(startSizePreference, width, height)
        }

        gridWidthPreference?.setOnPreferenceChangeListener { _, newValue ->

            if (newValue is String && newValue.toIntOrNull() != null) {

                val isSquareGrid = isGridPreference?.isChecked

                if (isSquareGrid != null && isSquareGrid && newValue.toIntOrNull() != null) {
                    // the grid is square, so there is no need to get the latest value of gridHeightPreference
                    updateAllowedStartSize(startSizePreference, newValue.toInt(), newValue.toInt())
                    gridHeightPreference?.text = newValue
                    Log.d(TAG, "Update height to $newValue")
                }
                else {
                    val latestHeight = gridHeightPreference?.text?.toIntOrNull()

                    if (latestHeight != null) {
                        updateAllowedStartSize(startSizePreference, newValue.toInt(), latestHeight)
                    }
                }
            }
            true
        }

        gridHeightPreference?.setOnPreferenceChangeListener { _, newValue ->
            Log.d(TAG, "Called gridHeightPreference change listener")

            if (newValue is String && newValue.toIntOrNull() != null) {
                val latestWidth = gridWidthPreference?.text?.toIntOrNull()

                if (latestWidth != null) {
                    updateAllowedStartSize(startSizePreference, latestWidth, newValue.toInt())
                }
            }
            true
        }

        setNumberInputSummary(gridWidthPreference)
        setNumberInputSummary(gridHeightPreference)
        setNumberInputSummary(startSpeed) { x -> "$x moves/s"}
        setNumberInputSummary(maxSpeed) { x -> "$x moves/s"}
        setNumberInputSummary(slowdownPerApple) { x -> "-$x moves"}
        setNumberInputSummary(countsToMax) { x -> "$x" }

        setAccelModeInputSummary(accelModePreference)

        //TODO limit valid snake starting size to max(width, height)

        isGridPreference?.apply {
            setOnPreferenceChangeListener { _, isGrid ->
                when (isGrid) {
                    is Boolean -> {
                        updateWidthHeight(gridWidthPreference, gridHeightPreference, isGrid, startSizePreference)
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

                        updateSpeedSettings(startSpeed, maxSpeed, countsToMax, slowdownPerApple, startingAccel, mode)
                    }
                }

                true
            }
        }
    }

    private fun updateStartingAccelSummary(preference: SeekBarPreference, newValue: Int) {
        preference.summary = {
            val value = newValue.toString()

            if (value.length > 1) {
                "${value[0]}.${value[1]} moves/s\u00B2"
            } else {
                "0.$value moves/s\u00B2"
            }
        } ()
    }
    private fun updateSpeedSettings(
        startSpeedPreference: EditTextPreference?,
        maxSpeedPreference: EditTextPreference?,
        countsToMaxPreference: EditTextPreference?,
        slowdownPerApplePreference: EditTextPreference?,
        startingAccel: SeekBarPreference?,
        mode: SnekSettings.AccelerationMode?
    ) {
        if (startSpeedPreference != null && maxSpeedPreference != null
            && countsToMaxPreference != null && slowdownPerApplePreference != null
            && startingAccel != null && mode != null) {

            when (mode) {
                SnekSettings.AccelerationMode.NONE -> {
                    //hide everything but the start speed, which is the constant speed in this case
                    maxSpeedPreference.isVisible = false
                    countsToMaxPreference.isVisible = false
                    slowdownPerApplePreference.isVisible = false
                    startingAccel.isVisible = false

                    startSpeedPreference.title = "Speed"

                    Log.d(TAG, "Changed acceleration mode to NONE")
                }
                SnekSettings.AccelerationMode.APPLE -> {

                    updateCountToMax(countsToMaxPreference, isAppleAccel = true)

                    //hide the slowdown per apple preference
                    maxSpeedPreference.isVisible = true
                    countsToMaxPreference.isVisible = true
                    slowdownPerApplePreference.isVisible = false
                    startingAccel.isVisible = true

                    startSpeedPreference.title = "Initial speed"
                    countsToMaxPreference.title = "Apples to max. speed"

                    wasAppleAccel = true
                }
                SnekSettings.AccelerationMode.EACH -> {

                    updateCountToMax(countsToMaxPreference, isAppleAccel = false)

                    //everything is visible
                    maxSpeedPreference.isVisible = true
                    countsToMaxPreference.isVisible = true
                    slowdownPerApplePreference.isVisible = true
                    startingAccel.isVisible = true

                    startSpeedPreference.title = "Initial speed"
                    countsToMaxPreference.title = "Moves to max. speed"

                    wasAppleAccel = false
                }
            }

        }
    }

    private fun updateWidthHeight(widthPreference: EditTextPreference?, heightPreference: EditTextPreference?, square: Boolean, startSizePreference: EditTextPreference?) {
        Log.d(TAG, "Updating width and height")

        widthPreference?.also { widthPref ->
            heightPreference?.also { heightPref ->
                if (square) {
                    Log.d(TAG, "Using a square field")

                    widthPref.title = "Side lengths"
                    heightPref.isVisible = false
                    heightPref.text = widthPref.text


                }
                else {
                    widthPref.title = "Width"
                    heightPref.isVisible = true

                    widthPref.onPreferenceChangeListener = null
                }

                val currentWidth = widthPref.text.toIntOrNull()
                val currentHeight = heightPref.text.toIntOrNull()

                if (currentWidth != null && currentHeight != null) {
                    updateAllowedStartSize(startSizePreference, currentWidth, currentHeight)
                }
            }
        }
    }

    private fun updateCountToMax(countsToMaxPreference: EditTextPreference, isAppleAccel: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val width = prefs.getString("grid_width", "16")?.toIntOrNull()
        val height = prefs.getString("grid_height", "16")?.toIntOrNull()

        if (width != null && height != null) {
            //set the counts to
            countsToMaxPreference.text = countsToMaxPreference.text.let {

                val crudeAvgMovesToApple = (sqrt((width * height).toDouble()) / 2)
                val current = it.toIntOrNull()

                current?.let { cur ->
                    Log.d(TAG, "Current moves $cur, avg to apple $crudeAvgMovesToApple")

                    // switching from apple to each
                    if (wasAppleAccel && !isAppleAccel) {
                        floor((cur * crudeAvgMovesToApple)).toInt().toString()
                    }
                    else if (!wasAppleAccel && isAppleAccel){
                        ceil((cur / crudeAvgMovesToApple)).toInt().toString()
                    }
                    else {
                        it
                    }
                } ?: countsToMaxPreference.text
            }
        }
    }

    private fun updateAllowedStartSize(startSizePreference: EditTextPreference?, currentWidth: Int, currentHeight: Int) {
        Log.d(TAG, "Updating allowed start size")

        val isStartSizeAllowed = { width: Int, height: Int, x: Int ->
            x <= max(width, height)
        }

        if (startSizePreference != null) {
            Log.d(TAG, "Null checks passed; width = $currentWidth, height = $currentHeight")
            setNumberInputSummary(
                startSizePreference,
                notAllowed = "Value is too large",
                valueIsAllowed = { x -> isStartSizeAllowed(currentWidth, currentHeight, x) }
            ) { x -> "Size: $x" }
        }
    }
    companion object {
        const val TAG = "SettingsFragment"
    }
}
