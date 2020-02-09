package com.example.mysnek

sealed class UIEvent

data class UpdateSnake(val coords: ArrayList<Coords>): UIEvent()

object GameOver: UIEvent()

data class UpdateApples(val score: Int, val newApple: Coords): UIEvent()

data class UpdateAll(val coords: ArrayList<Coords>,
                     val score: Int,
                     val newApple: Coords): UIEvent()