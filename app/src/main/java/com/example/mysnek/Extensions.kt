package com.example.mysnek

import android.content.Context
import android.opengl.Matrix
import android.text.TextUtils
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.lifecycle.Observer
import androidx.preference.EditTextPreference
import androidx.preference.Preference
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
    number: (Int) -> String = { x -> "Value: $x" } ) {
    setInputSummary(preference, notSet, notNumber, String::toIntOrNull, number)
}

fun setFloatInputSummary(preference: EditTextPreference?,
                         notSet: String = "Not set",
                         notFloat: String = "Not a valid decimal number",
                         float: (Float) -> String = {x -> "Value: $x"}) {
    setInputSummary(preference, notSet, notFloat, String::toFloatOrNull, float)
}

fun <T> setInputSummary(preference: EditTextPreference?,
                        notSet: String,
                        notNumber: String,
                        conversion: (String) -> T?,
                        number: (T) -> String) {
    preference?.apply {
        summaryProvider = Preference.SummaryProvider<EditTextPreference> { pref ->

            if (pref.text != null) {
                val value = conversion(pref.text)

                if (TextUtils.isEmpty(pref.text)) {
                    notSet
                }
                else {
                    if (value != null) {
                        number(value)
                    }
                    else {
                        notNumber
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

fun Context.themeColor(@AttrRes attrRes: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute (attrRes, typedValue, true)
    return typedValue.data
}
