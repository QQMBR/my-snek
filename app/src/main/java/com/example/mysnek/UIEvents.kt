package com.example.mysnek

sealed class UIEvent

data class UpdateSnake(val coords: ArrayList<Coords>): UIEvent()

object GameOver: UIEvent()

data class UpdateApples(val score: Int, val newApple: Coords): UIEvent()

data class UpdateAll(val coords: ArrayList<Coords>,
                     val score: Int,
                     val newApple: Coords): UIEvent()


sealed class SnekEvent

data class GameOver2(val score: Int) : SnekEvent()

sealed class SnekMoveEvent : SnekEvent()

data class UpdateBody(val coords: ArrayList<Coords>) : SnekMoveEvent()
data class UpdateApple(val coords: ArrayList<Coords>, val newApple: Coords) : SnekMoveEvent()