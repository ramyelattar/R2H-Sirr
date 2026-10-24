package com.r2h.sirr

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.unit.dp
import org.json.JSONObject

internal enum class GameCue { ROUND, SUCCESS, SKIP, REVEAL, COMPLETE }
internal data class EffectPreferences(val sound:Boolean=false,val haptic:Boolean=false,val cinematic:Boolean=true,val volume:Int=35) {
    fun normalized()=copy(volume=volume.coerceIn(0,80))
}

/** Local-only generated tones and user-controlled vibration; no background recording. */
internal class GameEffects(private val context:Context){
    var preferences by mutableStateOf(load())
        private set
    private val vault=LocalVault(context.applicationContext)
    private var tones:ToneGenerator?=null
    var burstNonce by mutableIntStateOf(0)
        private set
    private fun load():EffectPreferences=runCatching{
        val x=JSONObject(LocalVault(context.applicationContext).readPrivate("game-effects")?:return@runCatching EffectPreferences())
        EffectPreferences(x.optBoolean("sound"),x.optBoolean("haptic"),x.optBoolean("cinematic",true),x.optInt("volume",35)).normalized()
    }.getOrDefault(EffectPreferences())
    fun change(next:EffectPreferences){
        preferences=next.normalized()
        runCatching{vault.writePrivate("game-effects",JSONObject()
            .put("sound",preferences.sound).put("haptic",preferences.haptic)
            .put("cinematic",preferences.cinematic).put("volume",preferences.volume).toString())}
        runCatching{tones?.release()};tones=null
    }
    val animate:Boolean get()=preferences.cinematic && runCatching{
        Settings.Global.getFloat(context.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0f
    }.getOrDefault(true)
    fun cue(cue:GameCue){
        if(cue in setOf(GameCue.REVEAL,GameCue.COMPLETE,GameCue.SUCCESS) && animate)burstNonce++
        val p=preferences
        if(p.sound && p.volume>0)runCatching{
            if(tones==null)tones=ToneGenerator(AudioManager.STREAM_MUSIC,p.volume)
            val tone=when(cue){GameCue.ROUND->ToneGenerator.TONE_PROP_PROMPT
                GameCue.SUCCESS,GameCue.COMPLETE->ToneGenerator.TONE_PROP_ACK
                GameCue.SKIP->ToneGenerator.TONE_PROP_NACK
                GameCue.REVEAL->ToneGenerator.TONE_PROP_BEEP}
            tones?.startTone(tone,if(cue==GameCue.COMPLETE)220 else 90)
        }
        if(p.haptic)runCatching{
            val v:Vibrator=if(Build.VERSION.SDK_INT>=31)context.getSystemService(VibratorManager::class.java).defaultVibrator
                else (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
            if(v.hasVibrator())v.vibrate(VibrationEffect.createOneShot(if(cue==GameCue.COMPLETE)100 else 35,
                if(cue==GameCue.SKIP)55 else 95))
        }
    }
    fun release(){runCatching{tones?.release()};tones=null}
}

@Composable
internal fun rememberGameEffects():GameEffects{
    val context=androidx.compose.ui.platform.LocalContext.current
    val effects=remember(context){GameEffects(context.applicationContext)}
    DisposableEffect(effects){onDispose{effects.release()}}
    return effects
}

@Composable
internal fun EffectPreferencesPanel(effects:GameEffects){
    val p=effects.preferences
    Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
        Text("المؤثرات السينمائية",style=MaterialTheme.typography.titleMedium)
        Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
            Switch(checked=p.cinematic,onCheckedChange={effects.change(p.copy(cinematic=it))});Spacer(Modifier.width(8.dp));Text("حركات ظهور وانتقال")
        }
        Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
            Switch(checked=p.sound,onCheckedChange={effects.change(p.copy(sound=it))});Spacer(Modifier.width(8.dp));Text("أصوات قصيرة محلية")
        }
        Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
            Switch(checked=p.haptic,onCheckedChange={effects.change(p.copy(haptic=it))});Spacer(Modifier.width(8.dp));Text("اهتزازات اختيارية")
        }
        if(p.sound){Text("مستوى الصوت: "+"${p.volume}٪");Slider(value=p.volume.toFloat(),
            onValueChange={effects.change(p.copy(volume=it.toInt()))},valueRange=0f..80f)}
        OutlinedButton(onClick={effects.cue(GameCue.REVEAL)}){Text("تجربة المؤثرات")}
        if(!effects.animate)Text("الحركات معطلة بإعداد التطبيق أو إعداد النظام لتقليل الحركة",color=Color.LightGray)
    }}
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun CinematicChallenge(text:String,effects:GameEffects){
    if(!effects.animate){Card(Modifier.fillMaxWidth()){Text(text,Modifier.padding(18.dp),style=MaterialTheme.typography.titleLarge)};return}
    Box(Modifier.fillMaxWidth()) {
    AnimatedContent(targetState=text,transitionSpec={
        (fadeIn(tween(350))+scaleIn(initialScale=.92f,animationSpec=tween(350))+
            slideInVertically(tween(350)){it/6}) togetherWith
            (fadeOut(tween(150))+scaleOut(targetScale=.98f,animationSpec=tween(150))+
            slideOutVertically(tween(150)){-it/8})
    },label="سينما التحديات"){value->
        Card(Modifier.fillMaxWidth()){Text(value,Modifier.padding(18.dp),style=MaterialTheme.typography.titleLarge)}
    }
    val fraction=remember{Animatable(1f)}
    LaunchedEffect(effects.burstNonce){
        if(effects.burstNonce>0){fraction.snapTo(0f);fraction.animateTo(1f,tween(650))}
    }
    if(effects.burstNonce>0 && fraction.value<1f)Canvas(Modifier.matchParentSize()){
        for(n in 0 until 18){
            val angle=(n*2*Math.PI/18).toFloat();val dist=(30f+(n%4)*16f)*fraction.value
            drawCircle(Color(0xFFD2AE80),radius=3f*(1f-fraction.value),
                center=Offset(size.width*.5f+cos(angle)*dist,size.height*.5f+sin(angle)*dist),
                alpha=1f-fraction.value)
        }
    }
    }
}
