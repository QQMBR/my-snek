package com.example.mysnek

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

data class Coords(var x: Int, var y: Int)

sealed class SnekData(val body: ArrayList<Coords>) {
    override fun toString(): String {
        return "SnekData = $body"
    }
}

sealed class Movable(body: ArrayList<Coords>, val apple: Coords) : SnekData(body) {
    override fun toString(): String {
        return super.toString().plus(", $apple")
    }
}

class Grow (body:  ArrayList<Coords>,
            apple: Coords) : Movable(body, apple) {
    override fun toString(): String {
        return super.toString().plus(" (Apple)")
    }
}
class Move (body:  ArrayList<Coords>,
            apple: Coords) : Movable(body, apple) {
    override fun toString(): String {
        return super.toString().plus(" (Move)")
    }
}
class Start (body: ArrayList<Coords>, apple: Coords, val facing: GameModel.Direction): Movable(body, apple) {
    override fun toString(): String {
        return super.toString().plus( " (Start)")
    }
}

class Over(body: ArrayList<Coords>) : SnekData(body) {
    override fun toString(): String {
        return super.toString().plus("(Over)")
    }
}

class BaseViewModelFactory<T>(val creator: () -> T) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return creator() as T
    }
}

inline fun <reified T : ViewModel> Fragment.getViewModel(noinline creator: (() -> T)? = null): T {
    return if (creator == null)
        ViewModelProvider(this).get(T::class.java)
    else
        ViewModelProvider(this, BaseViewModelFactory(creator)).get(T::class.java)
}

