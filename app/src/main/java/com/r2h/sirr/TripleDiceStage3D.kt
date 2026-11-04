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
    // World-space ground quad with upward facing triangle winding.
    private val floorQuad = floatBuffer(floatArrayOf(
        -5f, TripleDiceStageGeometry.FLOOR_Y, -4f,
        -5f, TripleDiceStageGeometry.FLOOR_Y,  3f,
         5f, TripleDiceStageGeometry.FLOOR_Y, -4f,
         5f, TripleDiceStageGeometry.FLOOR_Y, -4f,
        -5f, TripleDiceStageGeometry.FLOOR_Y,  3f,
         5f, TripleDiceStageGeometry.FLOOR_Y,  3f
    ))
    private val textures = IntArray(18)
    private var nextTiles: List<Bitmap>? = null
    private var uploaded: List<Bitmap>? = null
    private var uploadNeeded = true
    private var faces: List<Int> = listOf(0, 0, 0)
    private var spins: List<Float> = listOf(1f, 1f, 1f)
    private var cubeProgram = 0
    private var floorProgram = 0
    private var posAttr = -1
    private var normalAttr = -1
    private var uvAttr = -1
    private var modelUniform = -1
    private var mvpUniform = -1
    private var materialUniform = -1
    private var samplerUniform = -1
    private var floorPosAttr = -1
    private var floorMvpUniform = -1
    private var floorCentersUniform = -1
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
        floorProgram = link(FLOOR_VERT, FLOOR_FRAG)
        posAttr = GLES20.glGetAttribLocation(cubeProgram, "aPos")
        normalAttr = GLES20.glGetAttribLocation(cubeProgram, "aNormal")
        uvAttr = GLES20.glGetAttribLocation(cubeProgram, "aUv")
        modelUniform = GLES20.glGetUniformLocation(cubeProgram, "uModel")
        mvpUniform = GLES20.glGetUniformLocation(cubeProgram, "uMvp")
        materialUniform = GLES20.glGetUniformLocation(cubeProgram, "uMaterial")
        samplerUniform = GLES20.glGetUniformLocation(cubeProgram, "uTexture")
        floorPosAttr = GLES20.glGetAttribLocation(floorProgram, "aPos")
        floorMvpUniform = GLES20.glGetUniformLocation(floorProgram, "uMvp")
        floorCentersUniform = GLES20.glGetUniformLocation(floorProgram, "uCenters[0]")
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
            Matrix.perspectiveM(projection, 0, 42f, width.toFloat() / height, 0.1f, 20f)
        }
        Matrix.setLookAtM(view, 0, 0f, 1.65f, 4.25f, 0f, -0.44f, 0f, 0f, 1f, 0f)
    }

    override fun onDrawFrame(unused: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        drawFloor()
        if (uploadNeeded) uploadTextures()
        if (uploaded == null) return
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
        val rotation = FloatArray(16)
        Matrix.setIdentityM(rotation, 0)
        Matrix.rotateM(rotation, 0, -28f, 0f, 1f, 0f)
        Matrix.rotateM(rotation, 0, 23f, 1f, 0f, 0f)
        val spinY = when (cube) { 0 -> 720f; 1 -> 1080f; else -> -1080f }
        val spinX = when (cube) { 0 -> 1080f; 1 -> -1440f; else -> 720f }
        Matrix.rotateM(rotation, 0, rest * spinY, 0f, 1f, 0f)
        Matrix.rotateM(rotation, 0, rest * spinX, 1f, 0f, 0f)
        Matrix.rotateM(rotation, 0, baseY, 0f, 1f, 0f)
        Matrix.rotateM(rotation, 0, baseX, 1f, 0f, 0f)

        val pose = TripleDiceStageGeometry.pose(cube)
        val landingY = TripleDiceStageGeometry.restingCenterY(rotation, pose.scale)
        val lift = 0.15f * kotlin.math.sin(Math.PI * spins[cube]).toFloat().coerceAtLeast(0f)
        val translationScale = FloatArray(16)
        Matrix.setIdentityM(translationScale, 0)
        Matrix.translateM(translationScale, 0, pose.x, landingY + lift, pose.z)
        Matrix.scaleM(translationScale, 0, pose.scale, pose.scale, pose.scale)
        val model = FloatArray(16)
        Matrix.multiplyMM(model, 0, translationScale, 0, rotation, 0)
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

    private fun drawFloor() {
        // Draw first so the real depth-tested cubes sit on a shared surface.
        // Contact darkening is computed in world coordinates, not screen UV.
        val viewProjection = FloatArray(16)
        Matrix.multiplyMM(viewProjection, 0, projection, 0, view, 0)
        val contacts = FloatArray(9)
        for (cube in 0..2) {
            val p = TripleDiceStageGeometry.pose(cube)
            contacts[cube * 3] = p.x
            contacts[cube * 3 + 1] = p.z
            // As the cube tumbles, the soft contact shadow relaxes slightly.
            contacts[cube * 3 + 2] =
                (kotlin.math.sin(Math.PI * spins[cube]).toFloat().coerceAtLeast(0f))
        }
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glUseProgram(floorProgram)
        GLES20.glUniformMatrix4fv(floorMvpUniform, 1, false, viewProjection, 0)
        GLES20.glUniform3fv(floorCentersUniform, 3, contacts, 0)
        floorQuad.position(0)
        GLES20.glEnableVertexAttribArray(floorPosAttr)
        GLES20.glVertexAttribPointer(floorPosAttr, 3, GLES20.GL_FLOAT, false, 12, floorQuad)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
        GLES20.glDisableVertexAttribArray(floorPosAttr)
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
                float hardness = mix(20.0, 29.0, rose);
                hardness = mix(hardness, 38.0, champagne);
                vec3 halfVector = normalize(key + vec3(0.0, 0.0, 1.0));
                float spec = pow(max(dot(normal, halfVector), 0.0), hardness);
                vec3 glint = mix(vec3(0.79, 0.62, 0.86), vec3(1.0, 0.86, 0.91), rose);
                glint = mix(glint, vec3(0.99, 0.96, 0.87), champagne);
                vec3 textureColor = texture2D(uTexture, vUv).rgb;
                // Softer satin response: avoid blowing pale owner texture
                // details out to pure white while the dice rotate.
                vec3 finalColor = textureColor * (0.38 + 0.44 * diffuse + 0.12 * bounce);
                float reflection = mix(0.065, 0.09, rose);
                reflection = mix(reflection, 0.115, champagne);
                finalColor += glint * spec * reflection;
                gl_FragColor = vec4(clamp(finalColor, 0.0, 1.0), 1.0);
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
