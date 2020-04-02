package com.example.mysnek

sealed class SnekEvent

data class Update(val coords: ArrayList<Coords>, val apple: Coords) : SnekEvent()

sealed class SingleSnekEvent

object Pause : SingleSnekEvent()
object Resume : SingleSnekEvent()

data class GameOver(val score: Int): SingleSnekEvent()
