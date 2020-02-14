package com.example.mysnek

sealed class SnekEvent

data class GameOver(val score: Int) : SnekEvent()

sealed class SnekMoveEvent : SnekEvent()

data class UpdateBody(val coords: ArrayList<Coords>) : SnekMoveEvent()
data class UpdateApple(val coords: ArrayList<Coords>, val newApple: Coords) : SnekMoveEvent()