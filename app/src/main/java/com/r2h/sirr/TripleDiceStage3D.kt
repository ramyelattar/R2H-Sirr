package com.r2h.sirr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
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
        cubeProgram = runCatching { link(CUBE_VERT, CUBE_FRAG) }
            .getOrElse {
                Log.w("R2H-Dice", "Microfacet GLES2 shader unsupported; using diffuse fallback", it)
                link(CUBE_VERT, CUBE_FRAG_FALLBACK)
            }
        floorProgram = runCatching { link(FLOOR_VERT, FLOOR_FRAG) }
            .getOrElse {
                Log.w("R2H-Dice", "Contact shadow GLES2 shader unsupported; using plain floor", it)
                link(FLOOR_VERT, FLOOR_FRAG_FALLBACK)
            }
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

        // GLES2-compatible physically-inspired GGX/Fresnel lighting.
        // This is NOT an IBL/PBR-certified material pipeline.
        private const val CUBE_VERT = """
            uniform mat4 uModel;
            uniform mat4 uMvp;
            attribute vec3 aPos;
            attribute vec3 aNormal;
            attribute vec2 aUv;
            varying vec3 vNormal;
            varying vec3 vWorldPos;
            varying vec2 vUv;
            void main() {
                vec4 world = uModel * vec4(aPos, 1.0);
                vWorldPos = world.xyz;
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
            varying vec3 vWorldPos;
            varying vec2 vUv;
            const float PI = 3.14159265;

            float geometrySchlick(float angle, float k) {
                return angle / max(angle * (1.0 - k) + k, 0.001);
            }

            void main() {
                vec3 n = normalize(vNormal);
                vec3 v = normalize(vec3(0.0, 1.65, 4.25) - vWorldPos);
                vec3 l = normalize(vec3(-0.55, 0.82, 0.63));
                vec3 h = normalize(l + v);
                float ndl = max(dot(n, l), 0.0);
                float ndv = max(dot(n, v), 0.001);
                float ndh = max(dot(n, h), 0.0);
                float vdh = max(dot(v, h), 0.0);

                float rose = step(0.5, uMaterial) * (1.0 - step(1.5, uMaterial));
                float champagne = step(1.5, uMaterial);
                float roughness = mix(0.62, 0.49, rose);
                roughness = mix(roughness, 0.43, champagne);
                float metallic = mix(0.04, 0.10, rose);
                metallic = mix(metallic, 0.26, champagne);
                float a = roughness * roughness;
                float a2 = a * a;
                float d = ndh * ndh * (a2 - 1.0) + 1.0;
                float ndf = a2 / max(PI * d * d, 0.001);
                float k = (roughness + 1.0) * (roughness + 1.0) / 8.0;
                float visibility = geometrySchlick(ndv, k) *
                                   geometrySchlick(ndl, k);
                vec3 texel = texture2D(uTexture, vUv).rgb;
                vec3 f0 = mix(vec3(0.04), texel * 0.34, metallic);
                vec3 fresnel = f0 + (vec3(1.0) - f0) * pow(1.0 - vdh, 5.0);
                vec3 specular = (ndf * visibility * fresnel) /
                                max(4.0 * ndv * ndl, 0.025);

                vec3 ambient = texel * 0.46;
                vec3 diffuse = texel * (vec3(1.0) - fresnel) *
                               (1.0 - metallic) * (0.61 * ndl + 0.12);
                vec3 color = ambient + diffuse + specular * (0.62 * ndl);
                // Keep authored text/photo values distinguishable on light faces.
                gl_FragColor = vec4(clamp(color, 0.0, 0.98), 1.0);
            }
        """

        private const val CUBE_FRAG_FALLBACK = """
            precision mediump float;
            uniform sampler2D uTexture;
            varying vec3 vNormal;
            varying vec2 vWorldPos;
            varying vec2 vUv;
            void main() {
                vec3 light = normalize(vec3(-0.55, 0.82, 0.63));
                float diffuse = max(dot(normalize(vNormal), light), 0.0);
                vec3 base = texture2D(uTexture, vUv).rgb;
                gl_FragColor = vec4(base * (0.51 + diffuse * 0.48), 1.0);
            }
        """

        private const val FLOOR_FRAG_FALLBACK = """
            precision mediump float;
            varying vec2 vPlane;
            void main() {
                float depthTint = smoothstep(-3.0, 2.2, vPlane.y);
                vec3 ground = mix(vec3(0.055, 0.048, 0.064),
                                  vec3(0.094, 0.073, 0.090), depthTint);
                gl_FragColor = vec4(ground, 1.0);
            }
        """

        private const val FLOOR_VERT = """
            uniform mat4 uMvp;
            attribute vec3 aPos;
            varying vec2 vPlane;
            void main() {
                vPlane = aPos.xz;
                gl_Position = uMvp * vec4(aPos, 1.0);
            }
        """

        private const val FLOOR_FRAG = """
            precision mediump float;
            varying vec2 vPlane;
            // Each xyz is x-position, z-position, animated lift factor.
            uniform vec3 uCenters[3];

            float contact(vec3 center) {
                vec2 offset = (vPlane - center.xy) / vec2(0.62, 0.56);
                float inner = 1.0 - smoothstep(0.12, 0.94, length(offset));
                return inner * mix(0.47, 0.21, clamp(center.z, 0.0, 1.0));
            }

            void main() {
                float backFade = smoothstep(-3.0, 2.2, vPlane.y);
                vec3 base = mix(vec3(0.055, 0.048, 0.064),
                                vec3(0.094, 0.073, 0.090), backFade);
                float ambientReflection = 0.016 *
                   (1.0 - smoothstep(0.0, 2.2, abs(vPlane.x)));
                base += vec3(ambientReflection, ambientReflection * 0.75,
                             ambientReflection * 0.86);
                float ao = max(contact(uCenters[0]),
                               max(contact(uCenters[1]), contact(uCenters[2])));
                // World-space approximate soft contact occlusion, not ray casting.
                vec3 color = base * (1.0 - ao);
                gl_FragColor = vec4(color, 1.0);
            }
        """

    }
}
