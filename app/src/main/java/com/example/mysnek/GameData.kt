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



sealed class SnekData(val body: ArrayList<Coords>)
sealed class Moveable(body: ArrayList<Coords>, val apple: Coords, var direction: GameModel.Direction) : SnekData(body)

class Apple(body:      ArrayList<Coords>,
            apple:     Coords,
            direction: GameModel.Direction) : Moveable(body, apple, direction)
class Move (body:      ArrayList<Coords>,
            apple:     Coords,
            direction: GameModel.Direction) : Moveable(body, apple, direction)

class Over(body: ArrayList<Coords>) : SnekData(body)
object Finished : SnekData(arrayListOf())

