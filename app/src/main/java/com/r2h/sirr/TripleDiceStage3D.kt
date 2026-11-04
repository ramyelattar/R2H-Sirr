package com.r2h.sirr

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.r2h.sirr.core.DiceCubeProjection
import com.r2h.sirr.core.PersonalDiceFace
import com.r2h.sirr.core.RoundedDiceMesh
import com.r2h.sirr.core.TripleDiceStageGeometry
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * One real 3D stage: three physically beveled cubes share camera, shaders,
 * lighting, depth buffer and the same GPU context. No built-in face content.
 */
@Composable
internal fun TripleDiceStage3D(
    cubes: List<List<PersonalDiceFace>>,
    selected: List<Int>,
    progress: List<Float>,
    vault: LocalVault,
    modifier: Modifier = Modifier
) {
    require(cubes.size == 3 && cubes.all { it.size == 6 })
    require(selected.size == 3 && selected.all { it in 0..5 })
    require(progress.size == 3)
    val tiles = cubes.flatMapIndexed { cube, faces ->
        faces.map { face ->
            val photo = remember(vault, face.imageId) {
                vault.readPhotoThumbnail(face.imageId)?.asAndroidBitmap()
            }
            remember(face.text, face.imageId, photo, cube) {
                createFaceTile(face.text, photo, DiceMaterial.forCube(cube))
            }
        }
    }
    AndroidView(
        factory = { TripleDiceStageGlView(it) },
        modifier = modifier.semantics {
            contentDescription = "ثلاثة مكعبات ثلاثية الأبعاد"
        },
        update = { it.submit(selected, progress, tiles) }
    )
}

private class TripleDiceStageGlView(context: Context) : GLSurfaceView(context) {
    private val renderer = TripleDiceStageRenderer()
    private var lifecycleOwner: LifecycleOwner? = null
    private val observer = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> onResume()
            Lifecycle.Event.ON_STOP -> onPause()
            else -> Unit
        }
    }

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 24, 0)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
        setPreserveEGLContextOnPause(false)
    }

    fun submit(selected: List<Int>, progress: List<Float>, tiles: List<Bitmap>) {
        require(selected.size == 3 && tiles.size == 18 && progress.size == 3)
        val faces = selected.toList()
        val fractions = progress.map { it.coerceIn(0f, 1f) }
        val bitmaps = tiles.toList()
        queueEvent { renderer.submit(faces, fractions, bitmaps) }
        requestRender()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lifecycleOwner = findViewTreeLifecycleOwner()
        lifecycleOwner?.lifecycle?.addObserver(observer)
        if (lifecycleOwner?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) == true) {
            onResume()
        }
    }

    override fun onDetachedFromWindow() {
        lifecycleOwner?.lifecycle?.removeObserver(observer)
        lifecycleOwner = null
        onPause()
        super.onDetachedFromWindow()
    }
}

private class TripleDiceStageRenderer : GLSurfaceView.Renderer {
    private val mesh = floatBuffer(RoundedDiceMesh.generate())
    private val verticesPerFace = RoundedDiceMesh.verticesPerFace()
    private val shadowQuad = floatBuffer(floatArrayOf(
        -1f, -1f, -1f, -1f,
        -1f,  1f, -1f,  1f,
         1f, -1f,  1f, -1f,
         1f, -1f,  1f, -1f,
        -1f,  1f, -1f,  1f,
         1f,  1f,  1f,  1f
    ))
    private val textures = IntArray(18)
    private var nextTiles: List<Bitmap>? = null
    private var uploaded: List<Bitmap>? = null
    private var uploadNeeded = true
    private var faces: List<Int> = listOf(0, 0, 0)
    private var spins: List<Float> = listOf(1f, 1f, 1f)
    private var cubeProgram = 0
    private var shadowProgram = 0
    private var posAttr = -1
    private var normalAttr = -1
    private var uvAttr = -1
    private var modelUniform = -1
    private var mvpUniform = -1
    private var materialUniform = -1
    private var samplerUniform = -1
    private var shadowPosAttr = -1
    private var shadowUvAttr = -1
    private var shadowMaterialUniform = -1
    private var shadowCenterUniform = -1
    private var shadowSizeUniform = -1
    private var aspect = 1f
    private val projection = FloatArray(16)
    private val view = FloatArray(16)

    fun submit(selected: List<Int>, progress: List<Float>, tiles: List<Bitmap>) {
        faces = selected
        spins = progress
        val previous = uploaded
        if (previous == null || previous.indices.any { previous[it] !== tiles[it] }) {
            uploadNeeded = true
        }
        nextTiles = tiles
    }

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        cubeProgram = link(CUBE_VERT, CUBE_FRAG)
        shadowProgram = link(SHADOW_VERT, SHADOW_FRAG)
        posAttr = GLES20.glGetAttribLocation(cubeProgram, "aPos")
        normalAttr = GLES20.glGetAttribLocation(cubeProgram, "aNormal")
        uvAttr = GLES20.glGetAttribLocation(cubeProgram, "aUv")
        modelUniform = GLES20.glGetUniformLocation(cubeProgram, "uModel")
        mvpUniform = GLES20.glGetUniformLocation(cubeProgram, "uMvp")
        materialUniform = GLES20.glGetUniformLocation(cubeProgram, "uMaterial")
        samplerUniform = GLES20.glGetUniformLocation(cubeProgram, "uTexture")
        shadowPosAttr = GLES20.glGetAttribLocation(shadowProgram, "aPos")
        shadowUvAttr = GLES20.glGetAttribLocation(shadowProgram, "aUv")
        shadowMaterialUniform = GLES20.glGetUniformLocation(shadowProgram, "uMaterial")
        shadowCenterUniform = GLES20.glGetUniformLocation(shadowProgram, "uCenter")
        shadowSizeUniform = GLES20.glGetUniformLocation(shadowProgram, "uRadii")
        GLES20.glGenTextures(18, textures, 0)
        uploaded = null
        uploadNeeded = true
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
        GLES20.glClearColor(0.043f, 0.035f, 0.051f, 1f)
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        if (width > 0 && height > 0) {
            aspect = width.toFloat() / height
            Matrix.perspectiveM(projection, 0, 42f, aspect, 0.1f, 20f)
        }
        Matrix.setLookAtM(view, 0, 0f, 0f, 3.9f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    override fun onDrawFrame(unused: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (uploadNeeded) uploadTextures()
        if (uploaded == null) return
        for (cube in 0..2) drawShadow(cube)
        for (cube in 0..2) drawCube(cube)
    }

    private fun uploadTextures() {
        val pending = nextTiles ?: return
        if (pending.size != 18) return
        for (i in 0 until 18) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, pending[i], 0)
        }
        uploaded = pending.toList()
        uploadNeeded = false
    }

    private fun drawCube(cube: Int) {
        val (baseX, baseY) = DiceCubeProjection.baseRotation(faces[cube])
        val rest = 1f - spins[cube]
        val model = FloatArray(16)
        Matrix.setIdentityM(model, 0)
        val pose = TripleDiceStageGeometry.pose(cube)
        Matrix.translateM(model, 0, pose.x, pose.y, pose.z)
        Matrix.scaleM(model, 0, pose.scale, pose.scale, pose.scale)
        Matrix.rotateM(model, 0, -28f, 0f, 1f, 0f)
        Matrix.rotateM(model, 0, 23f, 1f, 0f, 0f)
        val spinY = when (cube) { 0 -> 720f; 1 -> 1080f; else -> -1080f }
        val spinX = when (cube) { 0 -> 1080f; 1 -> -1440f; else -> 720f }
        Matrix.rotateM(model, 0, rest * spinY, 0f, 1f, 0f)
        Matrix.rotateM(model, 0, rest * spinX, 1f, 0f, 0f)
        Matrix.rotateM(model, 0, baseY, 0f, 1f, 0f)
        Matrix.rotateM(model, 0, baseX, 1f, 0f, 0f)
        val vm = FloatArray(16)
        val mvp = FloatArray(16)
        Matrix.multiplyMM(vm, 0, view, 0, model, 0)
        Matrix.multiplyMM(mvp, 0, projection, 0, vm, 0)

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glUseProgram(cubeProgram)
        GLES20.glUniformMatrix4fv(modelUniform, 1, false, model, 0)
        GLES20.glUniformMatrix4fv(mvpUniform, 1, false, mvp, 0)
        GLES20.glUniform1f(materialUniform, cube.toFloat())
        GLES20.glUniform1i(samplerUniform, 0)
        mesh.position(0)
        GLES20.glEnableVertexAttribArray(posAttr)
        GLES20.glVertexAttribPointer(posAttr, 3, GLES20.GL_FLOAT, false, 32, mesh)
        mesh.position(3)
        GLES20.glEnableVertexAttribArray(normalAttr)
        GLES20.glVertexAttribPointer(normalAttr, 3, GLES20.GL_FLOAT, false, 32, mesh)
        mesh.position(6)
        GLES20.glEnableVertexAttribArray(uvAttr)
        GLES20.glVertexAttribPointer(uvAttr, 2, GLES20.GL_FLOAT, false, 32, mesh)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        for (face in 0 until 6) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[TripleDiceStageGeometry.textureIndex(cube, face)])
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, face * verticesPerFace, verticesPerFace)
        }
        GLES20.glDisableVertexAttribArray(posAttr)
        GLES20.glDisableVertexAttribArray(normalAttr)
        GLES20.glDisableVertexAttribArray(uvAttr)
    }

    private fun drawShadow(cube: Int) {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glUseProgram(shadowProgram)
        GLES20.glUniform1f(shadowMaterialUniform, cube.toFloat())
        val pose = TripleDiceStageGeometry.pose(cube)
        GLES20.glUniform2f(shadowCenterUniform, pose.x * 0.728f / aspect, pose.shadowY)
        GLES20.glUniform2f(shadowSizeUniform, 0.47f / aspect, 0.13f)
        shadowQuad.position(0)
        GLES20.glEnableVertexAttribArray(shadowPosAttr)
        GLES20.glVertexAttribPointer(shadowPosAttr, 2, GLES20.GL_FLOAT, false, 16, shadowQuad)
        shadowQuad.position(2)
        GLES20.glEnableVertexAttribArray(shadowUvAttr)
        GLES20.glVertexAttribPointer(shadowUvAttr, 2, GLES20.GL_FLOAT, false, 16, shadowQuad)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
        GLES20.glDisableVertexAttribArray(shadowPosAttr)
        GLES20.glDisableVertexAttribArray(shadowUvAttr)
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    private fun compile(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        check(shader != 0)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val ok = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, ok, 0)
        if (ok[0] == 0) {
            val error = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw IllegalStateException("Triple Dice shader compile: " + error)
        }
        return shader
    }

    private fun link(vertex: String, fragment: String): Int {
        val vs = compile(GLES20.GL_VERTEX_SHADER, vertex)
        val fs = compile(GLES20.GL_FRAGMENT_SHADER, fragment)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vs)
        GLES20.glAttachShader(program, fs)
        GLES20.glLinkProgram(program)
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
        val ok = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, ok, 0)
        if (ok[0] == 0) {
            val error = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            throw IllegalStateException("Triple Dice shader link: " + error)
        }
        return program
    }

    companion object {
        private fun floatBuffer(data: FloatArray): FloatBuffer =
            ByteBuffer.allocateDirect(data.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply { put(data); position(0) }

        private const val CUBE_VERT = """
            uniform mat4 uModel;
            uniform mat4 uMvp;
            attribute vec3 aPos;
            attribute vec3 aNormal;
            attribute vec2 aUv;
            varying vec3 vNormal;
            varying vec2 vUv;
            void main() {
                vNormal = normalize(mat3(uModel) * aNormal);
                vUv = aUv;
                gl_Position = uMvp * vec4(aPos, 1.0);
            }
        """
        private const val CUBE_FRAG = """
            precision mediump float;
            uniform sampler2D uTexture;
            uniform float uMaterial;
            varying vec3 vNormal;
            varying vec2 vUv;
            void main() {
                vec3 normal = normalize(vNormal);
                vec3 key = normalize(vec3(-0.63, 0.77, 0.62));
                vec3 fill = normalize(vec3(0.68, -0.10, 0.58));
                float diffuse = max(dot(normal, key), 0.0);
                float bounce = max(dot(normal, fill), 0.0);
                float rose = step(0.5, uMaterial) * (1.0 - step(1.5, uMaterial));
                float champagne = step(1.5, uMaterial);
                float hardness = mix(28.0, 45.0, rose);
                hardness = mix(hardness, 66.0, champagne);
                vec3 halfVector = normalize(key + vec3(0.0, 0.0, 1.0));
                float spec = pow(max(dot(normal, halfVector), 0.0), hardness);
                vec3 glint = mix(vec3(0.79, 0.62, 0.86), vec3(1.0, 0.86, 0.91), rose);
                glint = mix(glint, vec3(0.99, 0.96, 0.87), champagne);
                vec3 textureColor = texture2D(uTexture, vUv).rgb;
                vec3 finalColor = textureColor * (0.49 + 0.43 * diffuse + 0.11 * bounce);
                finalColor += glint * spec * mix(0.13, 0.23, champagne);
                gl_FragColor = vec4(finalColor, 1.0);
            }
        """
        private const val SHADOW_VERT = """
            attribute vec2 aPos;
            attribute vec2 aUv;
            uniform vec2 uCenter;
            uniform vec2 uRadii;
            varying vec2 vUv;
            void main() {
                vUv = aUv;
                gl_Position = vec4(uCenter + aPos * uRadii, 0.8, 1.0);
            }
        """
        private const val SHADOW_FRAG = """
            precision mediump float;
            uniform float uMaterial;
            varying vec2 vUv;
            void main() {
                float fade = 1.0 - smoothstep(0.0, 1.0, dot(vUv, vUv));
                float rose = step(0.5, uMaterial) * (1.0 - step(1.5, uMaterial));
                float champagne = step(1.5, uMaterial);
                vec3 shadow = mix(vec3(0.40, 0.22, 0.43), vec3(0.53, 0.31, 0.42), rose);
                shadow = mix(shadow, vec3(0.53, 0.46, 0.36), champagne);
                gl_FragColor = vec4(shadow, fade * 0.29);
            }
        """
    }
}
