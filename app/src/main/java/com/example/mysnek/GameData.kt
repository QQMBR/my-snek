package com.example.mysnek

data class Coords(var x: Int, var y: Int)

data class GameData(val body: ArrayList<Coords>,
                    var apple: Coords,
                    var points: Int,
                    var grow: Boolean = false)

sealed class GameDataUpdates(val body: ArrayList<Coords>)

class BodyUpdate(body: ArrayList<Coords>) : GameDataUpdates(body)

class ScoreUpdate(body: ArrayList<Coords>,
                  var apple: Coords,
                  val points: Int) : GameDataUpdates(body)

data class SnekData(val body: ArrayList<Coords>)
