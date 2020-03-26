package com.example.mysnek

import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import kotlin.math.max
import kotlin.math.min

open class Square(private var color: FloatArray) {
    companion object {
        const val COORDS_PER_VERTEX = 3
        const val TAG = "Square"

        val coords = floatArrayOf(
            -0.5f, 0.5f, 0.0f, //top left
            -0.5f, -0.5f, 0.0f, //bottom left
            0.5f, -0.5f, 0.0f, //bottom right
            0.5f, 0.5f, 0.0f  //top right
        )

        //TODO check for incorrect sizes
        fun calculateHitboxFromCorners(corners: Array<FloatArray>): Hitbox {
            return corners.toList().fold(
                Hitbox(
                    Pair(corners[0][0], corners[0][1]),
                    Pair(corners[0][0], corners[0][1])
                )
            ) { acc, new: FloatArray ->
                acc.apply {
                    var minX = min.first
                    var maxX = max.first
                    var minY = min.second
                    var maxY = max.second

                    if (new[0] < minX) {
                        minX = new[0]
                    } else if (new[0] > maxX) {
                        maxX = new[0]
                    }
                    if (new[1] < minY) {
                        minY = new[1]
                    } else if (new[1] > maxY) {
                        maxY = new[1]
                    }

                    Hitbox(Pair(minX, maxY), Pair(maxX, minY))
                }
            }
        }
    }

    private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3)

    //TODO abstract code a bit
    private val vertexBuffer = ByteBuffer.allocateDirect(coords.size * 4).run {
        order(ByteOrder.nativeOrder())

        asFloatBuffer().apply {
            put(coords)
            position(0)
        }
    }

    private val drawListBuffer = ByteBuffer.allocateDirect(drawOrder.size * 2).run {
        order(ByteOrder.nativeOrder())

        asShortBuffer().apply {
            put(drawOrder)
            position(0)
        }
    }

    private val vertexShaderCode =
        "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "void main() {" +
                "   gl_Position = uMVPMatrix * vPosition;" +
                "}"

    private val fragmentShaderCode =
        "precision mediump float;" +
                "uniform vec4 vColor;" +
                "void main() {" +
                "   gl_FragColor = vColor;" +
                "}"

    private var program: Int

    init {
        Log.d(TAG, "Initializing Square")

        val vertexShader = GameRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = GameRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)

            GLES20.glLinkProgram(it)

            val buf: IntBuffer = IntBuffer.allocate(1)
            GLES20.glGetProgramiv(it, GLES20.GL_LINK_STATUS, buf)

            if (buf[0] == GLES20.GL_FALSE) {
                Log.e(TAG, "Error Linking program")
            }

            GLES20.glDetachShader(it, vertexShader)
            GLES20.glDetachShader(it, fragmentShader)

            Log.d(TAG, "Program is $it")
        }

        GameRenderer.checkForErrors("Init")
    }

    private val vertexCount = coords.size / COORDS_PER_VERTEX
    private val vertexStride = COORDS_PER_VERTEX * 4

    //private val color = floatArrayOf(0.8f, 0.0f, 0.3f, 1.0f)

    open fun draw(mvpMatrix: FloatArray) {
        GLES20.glUseProgram(program)

        GameRenderer.checkForErrors("Using Program")

        val vMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")

        if (vMVPMatrixHandle == -1) {
            Log.e(TAG, "Matrix Handle Not Found $program")
        }
        GameRenderer.checkForErrors("Getting matrix location")

        //pass the mvpMatrix to the shader
        GLES20.glUniformMatrix4fv(vMVPMatrixHandle, 1, false, mvpMatrix, 0)

        GameRenderer.checkForErrors("Setting mvpMatrix")

        //position handle
        GLES20.glGetAttribLocation(program, "vPosition").also {
            GLES20.glEnableVertexAttribArray(it)

            GameRenderer.checkForErrors("Enabling VertexAttribArray")

            GLES20.glVertexAttribPointer(
                it,
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                vertexStride,
                vertexBuffer
            )

            GameRenderer.checkForErrors("VertexAttribPointer")

            //set color
            GLES20.glGetUniformLocation(program, "vColor").also { colorHandle ->
                GLES20.glUniform4fv(colorHandle, 1, color, 0)
            }

            GameRenderer.checkForErrors("Setting Color")

            GLES20.glDrawElements(
                GLES20.GL_TRIANGLES,
                drawOrder.size,
                GLES20.GL_UNSIGNED_SHORT,
                drawListBuffer
            )

            GameRenderer.checkForErrors("Drawing Elements")

            GLES20.glDisableVertexAttribArray(it)
        }

        GameRenderer.checkForErrors("Draw")
    }

    fun changeColor(newColor: FloatArray) {
        newColor.copyInto(color)
    }

    fun getAllActiveAttributes(program: Int) {
        val size : IntBuffer = IntBuffer.allocate(1)
        GLES20.glGetProgramiv(program, GLES20.GL_ACTIVE_ATTRIBUTES, size)

        for (i in 0 until size[0]) {
            val attribSize = IntBuffer.allocate(1)
            val attribType = IntBuffer.allocate(1)
            GLES20.glGetActiveAttrib(program, i, attribSize, attribType)

        }
    }
    fun calculateHitbox(mvpMatrix: FloatArray): Hitbox {
        //split the coords array into an array of individual vertices
        val corners: Array<FloatArray> = coords.toList().windowed(3, 3)
        {
            Log.d(TAG, "FloatArray in calculateHitbox: $it")
            val result = FloatArray(4)
            Matrix.multiplyMV(result, 0, mvpMatrix, 0, floatArrayOf(*it.toFloatArray(), 1f), 0)

            result
        }.toTypedArray().apply {
            Log.d(TAG, "Corners: ${ this.map { "${it.asList()}, " } }")
        }

        //TODO make this understandable
        //pass through all corners, updating the hitbox in the process
        //start with the first corner and then check get the minimum
        //and maximum x and y values needed for the hitbox by calculating
        //the respective values against each next x and y pair in the list
        return corners.map { Pair(it[0], it[1]) }.fold(
            Pair(corners.first()[0], corners.first()[1]).let {
                Hitbox(it, it)
            })
        {acc, pair ->
            Hitbox(
                Pair(min(acc.min.first, pair.first), min(acc.min.second, pair.second)),
                Pair(max(acc.max.first, pair.first), max(acc.max.second, pair.second))
            )
        }
    }
}

