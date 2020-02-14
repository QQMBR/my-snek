package com.example.mysnek

data class Coords(var x: Int, var y: Int)

sealed class SnekData(val body: ArrayList<Coords>) {
    override fun toString(): String {
        return "SnekData = $body"
    }
}
sealed class Moveable(body: ArrayList<Coords>, val apple: Coords) : SnekData(body) {
    override fun toString(): String {
        return super.toString().plus(", $apple")
    }
}

class Apple(body:  ArrayList<Coords>,
            apple: Coords) : Moveable(body, apple) {
    override fun toString(): String {
        return super.toString().plus(" (Apple)")
    }
}
class Move (body:  ArrayList<Coords>,
            apple: Coords) : Moveable(body, apple) {
    override fun toString(): String {
        return super.toString().plus(" (Move)")
    }
}

class Over(body: ArrayList<Coords>) : SnekData(body) {
    override fun toString(): String {
        return super.toString().plus( "(Over)")
    }
}
object Finished : SnekData(arrayListOf()) {
    override fun toString(): String {
        return "Finished"
    }
}

