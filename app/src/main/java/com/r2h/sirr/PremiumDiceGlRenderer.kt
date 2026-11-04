package com.r2h.sirr

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.r2h.sirr.core.DiceCubeProjection
import com.r2h.sirr.core.RoundedDiceMesh
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Genuine GLES2 rounded cube: beveled geometry, smooth normals, dynamic lighting
 * and six independently textured owner-authored faces.
 *
 * All GL calls are confined to GLSurfaceView's rendering thread. The view
 * requests frames only when the selected face, animation or textures change.
 * No cloud services, model assets, or generated game content.
 */
internal class PremiumDiceGlView(context: Context) : GLSurfaceView(context) {
    private val scene = PremiumDiceScene()
    private var lifecycleOwner: LifecycleOwner? = null
    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> onResume()
            Lifecycle.Event.ON_STOP -> onPause()
            else -> Unit
        }
    }

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 24, 0)
        setRenderer(scene)
        renderMode = RENDERMODE_WHEN_DIRTY
        setPreserveEGLContextOnPause(false)
    }

    fun submit(
        faceIndex: Int,
        spinProgress: Float,
        material: DiceMaterial,
        rollStyle: Int,
        tiles: List<Bitmap>
    ) {
        require(faceIndex in 0..5 && tiles.size == 6)
        queueEvent { scene.submit(faceIndex, spinProgress, material, rollStyle, tiles) }
        requestRender()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lifecycleOwner = findViewTreeLifecycleOwner()
        lifecycleOwner?.lifecycle?.addObserver(lifecycleObserver)
        if (lifecycleOwner?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) == true) {
            onResume()
        }
    }

    override fun onDetachedFromWindow() {
        lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
        lifecycleOwner = null
        onPause()
        super.onDetachedFromWindow()
    }
}

private class PremiumDiceScene : GLSurfaceView.Renderer {
    private val vertexCount = RoundedDiceMesh.verticesPerFace()
    private val cubeData = buffer(RoundedDiceMesh.generate())
    private val shadowData = buffer(floatArrayOf(
        // Position NDC xy, local UV xy — a soft cast/contact glow below the cube.
        -0.73f, -0.82f, -1f, -1f,
        -0.73f, -0.32f, -1f, 1f,
         0.73f, -0.82f, 1f, -1f,
         0.73f, -0.82f, 1f, -1f,
        -0.73f, -0.32f, -1f, 1f,
         0.73f, -0.32f, 1f, 1f
    ))
    private val textures = IntArray(6)
    private var tiles: List<Bitmap>? = null
    private var uploaded: List<Bitmap>? = null
    private var needUpload = true
    private var face = 0
    private var spin = 1f
    private var material = DiceMaterial.PLUM
    private var rollStyle = 0
    private var cubeProgram = 0
    private var shadowProgram = 0
    private var positionLoc = 0
    private var normalLoc = 0
    private var uvLoc = 0
    private var modelLoc = 0
    private var mvpLoc = 0
    private var materialLoc = 0
    private var samplerLoc = 0
    private var shadowPositionLoc = 0
    private var shadowUvLoc = 0
    private var shadowMaterialLoc = 0
    private val projection = FloatArray(16)
    private val view = FloatArray(16)

    fun submit(newFace: Int, newSpin: Float, newMaterial: DiceMaterial, newStyle: Int, newTiles: List<Bitmap>) {
        // Invoked ONLY through GLSurfaceView.queueEvent, never on the Compose thread.
        face = newFace
        spin = newSpin.coerceIn(0f, 1f)
        material = newMaterial
        rollStyle = newStyle % 3
        if (uploaded == null || uploaded!!.indices.any { uploaded!![it] !== newTiles[it] }) {
            needUpload = true
        }
        tiles = newTiles
    }

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        cubeProgram = link(CUBE_VERTEX_SHADER, CUBE_FRAGMENT_SHADER)
        shadowProgram = link(SHADOW_VERTEX_SHADER, SHADOW_FRAGMENT_SHADER)
        positionLoc = GLES20.glGetAttribLocation(cubeProgram, "aPosition")
        normalLoc = GLES20.glGetAttribLocation(cubeProgram, "aNormal")
        uvLoc = GLES20.glGetAttribLocation(cubeProgram, "aUv")
        modelLoc = GLES20.glGetUniformLocation(cubeProgram, "uModel")
        mvpLoc = GLES20.glGetUniformLocation(cubeProgram, "uMvp")
        materialLoc = GLES20.glGetUniformLocation(cubeProgram, "uMaterial")
        samplerLoc = GLES20.glGetUniformLocation(cubeProgram, "uTexture")
        shadowPositionLoc = GLES20.glGetAttribLocation(shadowProgram, "aPosition")
        shadowUvLoc = GLES20.glGetAttribLocation(shadowProgram, "aUv")
        shadowMaterialLoc = GLES20.glGetUniformLocation(shadowProgram, "uMaterial")
        GLES20.glGenTextures(textures.size, textures, 0)
        uploaded = null
        needUpload = true
        GLES20.glClearColor(0.042f, 0.033f, 0.061f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        if (height > 0) {
            Matrix.perspectiveM(projection, 0, 37f, width.toFloat() / height, 0.1f, 20f)
        }
        Matrix.setLookAtM(view, 0, 0f, 0f, 3.15f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    override fun onDrawFrame(unused: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (cubeProgram == 0 || shadowProgram == 0) return
        if (needUpload) uploadTextures()

        drawShadow()
        drawCube()
    }

    private fun uploadTextures() {
        val next = tiles ?: return
        if (next.size != 6) return
        for (i in 0 until 6) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, next[i], 0)
        }
        uploaded = next.toList()
        needUpload = false
    }

    private fun drawCube() {
        val (baseX, baseY) = DiceCubeProjection.baseRotation(face)
        val remaining = 1f - spin
        val model = FloatArray(16)
        Matrix.setIdentityM(model, 0)
        // Order matches DiceCubeProjection: outermost view tilt, then spin,
        // then the deterministic orientation for the selected face.
        Matrix.rotateM(model, 0, -28f, 0f, 1f, 0f)
        Matrix.rotateM(model, 0, 23f, 1f, 0f, 0f)
        val spinY = when (rollStyle) { 0 -> 720f; 1 -> 1080f; else -> -1080f }
        val spinX = when (rollStyle) { 0 -> 1080f; 1 -> -1440f; else -> 720f }
        Matrix.rotateM(model, 0, remaining * spinY, 0f, 1f, 0f)
        Matrix.rotateM(model, 0, remaining * spinX, 1f, 0f, 0f)
        Matrix.rotateM(model, 0, baseY, 0f, 1f, 0f)
        Matrix.rotateM(model, 0, baseX, 1f, 0f, 0f)

        val viewModel = FloatArray(16)
        val mvp = FloatArray(16)
        Matrix.multiplyMM(viewModel, 0, view, 0, model, 0)
        Matrix.multiplyMM(mvp, 0, projection, 0, viewModel, 0)

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glUseProgram(cubeProgram)
        GLES20.glUniformMatrix4fv(modelLoc, 1, false, model, 0)
        GLES20.glUniformMatrix4fv(mvpLoc, 1, false, mvp, 0)
        GLES20.glUniform1f(materialLoc, material.ordinal.toFloat())
        GLES20.glUniform1i(samplerLoc, 0)
        cubeData.position(0)
        GLES20.glEnableVertexAttribArray(positionLoc)
        GLES20.glVertexAttribPointer(positionLoc, 3, GLES20.GL_FLOAT, false, 8 * 4, cubeData)
        cubeData.position(3)
        GLES20.glEnableVertexAttribArray(normalLoc)
        GLES20.glVertexAttribPointer(normalLoc, 3, GLES20.GL_FLOAT, false, 8 * 4, cubeData)
        cubeData.position(6)
        GLES20.glEnableVertexAttribArray(uvLoc)
        GLES20.glVertexAttribPointer(uvLoc, 2, GLES20.GL_FLOAT, false, 8 * 4, cubeData)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        for (index in 0 until 6) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[index])
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, index * vertexCount, vertexCount)
        }
        GLES20.glDisableVertexAttribArray(positionLoc)
        GLES20.glDisableVertexAttribArray(normalLoc)
        GLES20.glDisableVertexAttribArray(uvLoc)
    }

    private fun drawShadow() {
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glUseProgram(shadowProgram)
        GLES20.glUniform1f(shadowMaterialLoc, material.ordinal.toFloat())
        shadowData.position(0)
        GLES20.glEnableVertexAttribArray(shadowPositionLoc)
        GLES20.glVertexAttribPointer(shadowPositionLoc, 2, GLES20.GL_FLOAT, false, 4 * 4, shadowData)
        shadowData.position(2)
        GLES20.glEnableVertexAttribArray(shadowUvLoc)
        GLES20.glVertexAttribPointer(shadowUvLoc, 2, GLES20.GL_FLOAT, false, 4 * 4, shadowData)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
        GLES20.glDisableVertexAttribArray(shadowPositionLoc)
        GLES20.glDisableVertexAttribArray(shadowUvLoc)
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    private fun compile(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        check(shader != 0) { "Unable to create GLES2 shader" }
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val details = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            error("Dice GLES2 shader failed: $details")
        }
        return shader
    }

    private fun link(vertexSource: String, fragmentSource: String): Int {
        val vertex = compile(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragment = compile(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertex)
        GLES20.glAttachShader(program, fragment)
        GLES20.glLinkProgram(program)
        GLES20.glDeleteShader(vertex)
        GLES20.glDeleteShader(fragment)
        val status = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            val details = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            error("Dice GLES2 program link failed: $details")
        }
        return program
    }

    companion object {
        private fun buffer(data: FloatArray): FloatBuffer =
            ByteBuffer.allocateDirect(data.size * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                    put(data)
                    position(0)
                }

        private const val CUBE_VERTEX_SHADER = """
            uniform mat4 uModel;
            uniform mat4 uMvp;
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            attribute vec2 aUv;
            varying vec3 vNormal;
            varying vec2 vUv;
            void main() {
                vNormal = normalize(mat3(uModel) * aNormal);
                vUv = aUv;
                gl_Position = uMvp * vec4(aPosition, 1.0);
            }
        """

        private const val CUBE_FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D uTexture;
            uniform float uMaterial;
            varying vec3 vNormal;
            varying vec2 vUv;
            void main() {
                vec4 texel = texture2D(uTexture, vUv);
                vec3 normal = normalize(vNormal);
                vec3 keyLight = normalize(vec3(-0.58, 0.73, 0.56));
                vec3 fillLight = normalize(vec3(0.75, -0.25, 0.50));
                float key = max(dot(normal, keyLight), 0.0);
                float fill = max(dot(normal, fillLight), 0.0);
                float rose = step(0.5, uMaterial) * (1.0 - step(1.5, uMaterial));
                float champagne = step(1.5, uMaterial);
                float gloss = mix(35.0, 49.0, rose);
                gloss = mix(gloss, 76.0, champagne);
                vec3 halfVector = normalize(keyLight + vec3(0.0, 0.0, 1.0));
                float specular = pow(max(dot(normal, halfVector), 0.0), gloss);
                float rim = pow(1.0 - abs(normal.z), 3.0);
                vec3 highlight = mix(vec3(0.81, 0.58, 0.85),
                                     vec3(1.0, 0.87, 0.93), rose);
                highlight = mix(highlight, vec3(1.0, 0.95, 0.82), champagne);
                vec3 color = texel.rgb * (0.42 + 0.49 * key + 0.14 * fill);
                color += highlight * specular * mix(0.30, 0.40, champagne);
                color += highlight * rim * 0.11;
                gl_FragColor = vec4(color, 1.0);
            }
        """

        private const val SHADOW_VERTEX_SHADER = """
            attribute vec2 aPosition;
            attribute vec2 aUv;
            varying vec2 vUv;
            void main() {
                vUv = aUv;
                gl_Position = vec4(aPosition, 0.8, 1.0);
            }
        """

        private const val SHADOW_FRAGMENT_SHADER = """
            precision mediump float;
            uniform float uMaterial;
            varying vec2 vUv;
            void main() {
                float falloff = 1.0 - smoothstep(0.0, 1.0, dot(vUv, vUv));
                float rose = step(0.5, uMaterial) * (1.0 - step(1.5, uMaterial));
                float champagne = step(1.5, uMaterial);
                vec3 tint = mix(vec3(0.40, 0.21, 0.48),
                                vec3(0.62, 0.31, 0.46), rose);
                tint = mix(tint, vec3(0.61, 0.56, 0.47), champagne);
                gl_FragColor = vec4(tint, falloff * 0.28);
            }
        """
    }
}
