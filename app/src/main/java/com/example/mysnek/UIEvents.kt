package com.example.mysnek

sealed class UIEvent

data class UpdateSnake(val coords: ArrayList<Coords>): UIEvent()
object GameOver: UIEvent()