package com.example.mysnek

import android.content.Context
import android.opengl.Matrix
import android.text.TextUtils
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.lifecycle.Observer
import androidx.preference.DropDownPreference
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import io.reactivex.Observable
import io.reactivex.functions.Predicate

//get the opposite direction
fun GameModel.Direction.flip()
    = when (this) {
        GameModel.Direction.UP    -> GameModel.Direction.DOWN
        GameModel.Direction.LEFT  -> GameModel.Direction.RIGHT
        GameModel.Direction.DOWN  -> GameModel.Direction.UP
        GameModel.Direction.RIGHT -> GameModel.Direction.LEFT
    }

//turn the direction into a unit vector representing
//the direction of the movement in screen coordinate space
fun GameModel.Direction.vectorize(): Coords
    = when (this) {
        GameModel.Direction.UP    -> Coords( 0, -1)
        GameModel.Direction.LEFT  -> Coords(-1,  0)
        GameModel.Direction.DOWN  -> Coords( 0,  1)
        GameModel.Direction.RIGHT -> Coords( 1,  0)
    }

fun <T> Predicate<T>.not(): Predicate<T> {
    return Predicate {
        this.test(it).not()
    }
}

fun Int.colorToRGBAFloatArray(): FloatArray {
    val maskToRange = { x: Int, n : Int -> ((this and x) shr n) / 255f }

    return floatArrayOf(
        maskToRange(0x00FF0000, 16), //R
        maskToRange(0x0000FF00, 8), //G
        maskToRange(0x000000FF, 0), //B
        maskToRange(0xFF000000.toInt(), 24)  //A
    )
}

fun getIdentity() : FloatArray = FloatArray(16).also {
    Matrix.setIdentityM(it, 0)
}

fun scaleFloatArray(sx: Float = 1.0f, sy: Float = 1.0f, sz: Float = 1.0f)
        = arrayFromIdentity { Matrix.scaleM(it, 0, sx, sy, sz) }

fun translateFloatArray(dx: Float, dy: Float, dz: Float) : FloatArray {
    return getIdentity().also {
        Matrix.translateM(it, 0, dx, dy, dz)
    }
}

fun arrayFromIdentity(f: (FloatArray) -> Unit): FloatArray {
    return getIdentity().also {
        f(it)
    }
}

fun mulMM(lhs: FloatArray, rhs: FloatArray) = FloatArray(16).also {
    Matrix.multiplyMM(it, 0, lhs, 0, rhs, 0)
}

infix fun FloatArray.mul(rhs: FloatArray) = FloatArray(16).also {
    Matrix.multiplyMM(it, 0, this, 0, rhs, 0)
}

fun setInputType(preference: EditTextPreference?, type: Int) {
    preference?.apply {
        setOnBindEditTextListener {
            it.inputType = type
        }
    }
}

fun setNumberInputSummary(preference: EditTextPreference?,
    notSet: String = "Not set",
    notNumber: String = "Not a valid number",
    notAllowed: String ="Value not allowed",
    valueIsAllowed: (Int) -> Boolean = {true},
    number: (Int) -> String = { x -> "Value: $x" } ) {
    setInputSummary(preference, notSet, notNumber, notAllowed, valueIsAllowed, String::toIntOrNull, number)
}

fun setFloatInputSummary(preference: EditTextPreference?,
    notSet: String = "Not set",
    notFloat: String = "Not a valid decimal number",
    notAllowed: String = "Value not allowed",
    valueIsAllowed: (Float) -> Boolean = {true},
    float: (Float) -> String = {x -> "Value: $x"}) {
    setInputSummary(preference, notSet, notFloat, notAllowed, valueIsAllowed, String::toFloatOrNull, float)
}

fun setAccelModeInputSummary(preference: DropDownPreference?,
    notSet: String = "Not set",
    notAccelMode: String = "Not a valid option",
    accelMode: (SnekSettings.AccelerationMode) -> String = {x -> "$x"}) {
    preference?.apply {
        summaryProvider = Preference.SummaryProvider<DropDownPreference> { pref ->

            if (pref.value != null) {
                val value = SnekSettings.AccelerationMode.fromString(pref.value)

                if (TextUtils.isEmpty(pref.value)) {
                    notSet
                }
                else {
                    if (value != null) {
                        accelMode(value)
                    }
                    else {
                        notAccelMode
                    }
                }
            }
            else {
                notSet
            }
        }
    }
}

fun <T> setInputSummary(preference: EditTextPreference?,
    notSet: String,
    notOfType: String,
    notAllowed: String,
    isValueAllowed: (T) -> Boolean,
    conversion: (String) -> T?,
    number: (value: T) -> String) {

    preference?.apply {
        summaryProvider = Preference.SummaryProvider<EditTextPreference> { pref ->

            if (pref.text != null) {
                val value = conversion(pref.text)

                if (TextUtils.isEmpty(pref.text)) {
                    notSet
                }
                else {
                    if (value != null) {
                        if (isValueAllowed(value)) {
                            number(value)
                        }
                        else {
                            notAllowed
                        }
                    }
                    else {
                        notOfType
                    }
                }
            }
            else {
                notSet
            }
        }
    }
}


/**
 * An [Observer] for [Event]s, simplifying the pattern of checking if the [Event]'s content has
 * already been handled.
 *
 * [onEventUnhandledContent] is *only* called if the [Event]'s contents has not been handled.
 */
class EventObserver<T>(private val onEventUnhandledContent: (T) -> Unit) : Observer<Event<T>> {
    override fun onChanged(event: Event<T>?) {
        event?.getContentIfNotHandled()?.let { value ->
            onEventUnhandledContent(value)
        }
    }
}

fun Int.toDirection(): GameModel.Direction
    = when (this) {
        0 -> GameModel.Direction.UP
        1 -> GameModel.Direction.DOWN
        2 -> GameModel.Direction.LEFT
        3 -> GameModel.Direction.RIGHT
        else -> GameModel.Direction.UP
    }

fun Context.themeColor(@AttrRes attrRes: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute (attrRes, typedValue, true)
    return typedValue.data
}

fun String.toAccelMode() : SnekSettings.AccelerationMode? = SnekSettings.AccelerationMode.fromString(this)

fun Observable<SnekData>.slownessFromSnake(settings: SnekSettings): Observable<Long>
    = settings.slownessFromSnake(this)