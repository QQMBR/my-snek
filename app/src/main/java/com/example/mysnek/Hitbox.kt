package com.example.mysnek

data class Hitbox(val min: Pair<Float, Float>, val max: Pair<Float, Float>) {

    fun checkCollision(x: Float, y: Float) : Boolean {
            return x > min.first && x < max.first &&
                    y > min.second && y < max.second
    }
}