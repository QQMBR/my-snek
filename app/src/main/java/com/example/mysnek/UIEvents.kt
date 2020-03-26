package com.example.mysnek

sealed class SnekEvent

data class GameOver(val score: Int) : SnekEvent()
data class Update(val coords: ArrayList<Coords>, val apple: Coords) : SnekEvent()

object Pause : SnekEvent()
data class Resume(val newEvent: SnekEvent) : SnekEvent()

sealed class SingleSnekEvent

object Pause2 : SingleSnekEvent()
object Resume2 : SingleSnekEvent()
data class GameOver2(val score: Int): SingleSnekEvent()
