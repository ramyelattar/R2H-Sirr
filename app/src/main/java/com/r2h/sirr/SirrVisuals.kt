package com.r2h.sirr

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

private val DeepWine = Color(0xFF3A1728)
private val Amber = Color(0xFFF1CBA4)
private val Pale = Color(0xFFF7E8E0)

/** Lightweight native decorative scene; never relies on stock art or remote assets. */
@Composable
internal fun MysteryHeader(eyebrow:String, headline:String, caption:String, symbol:String="✦"){
    Box(
        Modifier.fillMaxWidth().heightIn(min=190.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(DeepWine,Color(0xFF6A304B),Color(0xFF1C111B))))
    ){
        Canvas(Modifier.matchParentSize()){
            val center=Offset(size.width*.86f,size.height*.22f)
            val r=min(size.width,size.height)*.22f
            for(i in 1..4){
                drawCircle(Amber.copy(alpha=.22f/i),r*i,center,style=Stroke(width=1.7.dp.toPx()))
            }
            drawCircle(Amber.copy(alpha=.065f),r*.8f,center)
            drawCircle(Color.White.copy(alpha=.2f),2.dp.toPx(),Offset(size.width*.19f,size.height*.18f))
            drawCircle(Color.White.copy(alpha=.15f),1.5.dp.toPx(),Offset(size.width*.73f,size.height*.75f))
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal=22.dp,vertical=20.dp),
            verticalArrangement=Arrangement.spacedBy(10.dp)
        ){
            Text(eyebrow,color=Amber,style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.SemiBold)
            Text(symbol,color=Amber,fontSize=28.sp)
            Text(headline,color=Color.White,fontSize=25.sp,lineHeight=32.sp,fontWeight=FontWeight.Bold)
            Text(caption,color=Pale,style=MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Faux-3D dice illustration; selection and weights remain in DiceConfiguration. */
@Composable
internal fun MysteryDie(number:Int,spinning:Boolean,modifier:Modifier=Modifier) {
    val transition=rememberInfiniteTransition(label="mystery-die")
    val spin by transition.animateFloat(
        initialValue=0f,targetValue=360f,
        animationSpec=infiniteRepeatable(tween(850,easing=LinearEasing)),label="die-spin"
    )
    Box(modifier.aspectRatio(1f).graphicsLayer {
        rotationZ=if(spinning) spin else 0f
        scaleX=if(spinning) .93f else 1f
        scaleY=if(spinning) .93f else 1f
    },contentAlignment=Alignment.Center){
        Canvas(Modifier.fillMaxSize()){
            val w=size.width;val h=size.height
            fun polygon(points:List<Offset>,brush:Brush){
                val path=Path()
                points.forEachIndexed { i,p->if(i==0)path.moveTo(p.x,p.y) else path.lineTo(p.x,p.y)}
                path.close();drawPath(path,brush)
            }
            polygon(listOf(Offset(w*.13f,h*.26f),Offset(w*.69f,h*.17f),Offset(w*.85f,h*.30f),Offset(w*.30f,h*.39f)),
                Brush.linearGradient(listOf(Color(0xFFFFE6C7),Color(0xFFB57483))))
            polygon(listOf(Offset(w*.69f,h*.17f),Offset(w*.85f,h*.30f),Offset(w*.85f,h*.76f),Offset(w*.69f,h*.87f)),
                Brush.verticalGradient(listOf(Color(0xFF8B526B),Color(0xFF34192B))))
            polygon(listOf(Offset(w*.13f,h*.26f),Offset(w*.30f,h*.39f),Offset(w*.69f,h*.35f),Offset(w*.69f,h*.87f),Offset(w*.13f,h*.77f)),
                Brush.linearGradient(listOf(Color(0xFFAD6480),Color(0xFF51283E),Color(0xFF1D1020))))
            val dots=when(number.coerceIn(1,6)){
                1->listOf(.42f to .56f)
                2->listOf(.26f to .45f,.55f to .7f)
                3->listOf(.24f to .43f,.42f to .56f,.59f to .73f)
                4->listOf(.26f to .46f,.55f to .46f,.26f to .69f,.55f to .69f)
                5->listOf(.26f to .44f,.55f to .44f,.42f to .57f,.26f to .71f,.55f to .71f)
                else->listOf(.25f to .43f,.42f to .43f,.58f to .43f,.25f to .70f,.42f to .70f,.58f to .70f)
            }
            dots.forEach {(x,y)->drawCircle(Amber.copy(alpha=.92f),radius=w*.041f,center=Offset(w*x,h*y))}
        }
    }
}

@Composable
internal fun SecretLetter(text:String,order:Int,total:Int) {
    Box(
        Modifier.fillMaxWidth().heightIn(min=235.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF462437),Color(0xFF1A1221)))),
        contentAlignment=Alignment.Center
    ){
        Canvas(Modifier.matchParentSize()){
            drawCircle(Amber.copy(alpha=.13f),size.minDimension*.36f,
                Offset(size.width*.89f,size.height*.08f),style=Stroke(1.dp.toPx()))
            drawLine(Amber.copy(alpha=.20f),Offset(23.dp.toPx(),size.height-22.dp.toPx()),
                Offset(size.width-23.dp.toPx(),size.height-22.dp.toPx()),strokeWidth=1.dp.toPx())
        }
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(18.dp)
        ){
            Text("✦  سِرّ ${order} / ${total}",color=Amber,
                style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Bold)
            Text(text,color=Color.White,fontSize=23.sp,lineHeight=36.sp,
                textAlign=TextAlign.Center,fontWeight=FontWeight.Medium)
            Text("خاص بيكم",color=Pale,style=MaterialTheme.typography.labelSmall)
        }
    }
}


/** One focused prompt; no generic grey Material card during the actual game. */
@Composable
internal fun ChallengeRevealCard(text:String,modifier:Modifier=Modifier) {
    Box(
        modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF4C2237),Color(0xFF231521),Color(0xFF180F19))))
    ){
        Canvas(Modifier.matchParentSize()){
            drawCircle(Amber.copy(alpha=.15f),size.minDimension*.39f,
                Offset(size.width*.92f,size.height*.07f),style=Stroke(width=1.4.dp.toPx()))
            drawCircle(Amber.copy(alpha=.07f),size.minDimension*.28f,
                Offset(size.width*.88f,size.height*.18f))
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=30.dp),
            verticalArrangement=Arrangement.spacedBy(19.dp)
        ){
            Text("✦  جولة سرّية",color=Amber,style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Bold)
            Text(text,color=Color.White,fontSize=24.sp,lineHeight=37.sp,fontWeight=FontWeight.SemiBold)
            Text("تقدر تقول «عدّي» من غير أي إحراج",color=Pale,style=MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun RoundChapter(current:Int,total:Int){
    val chapter=when {
        current*3<total -> "البداية لسه بتسخن"
        current*3<total*2 -> "الأسرار بتقرب"
        else -> "وصلنا للمرحلة الأخيرة"
    }
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.SpaceBetween){
        Text(chapter,color=Amber,style=MaterialTheme.typography.labelLarge)
        Text("${current+1} / ${total}",color=Pale,style=MaterialTheme.typography.labelMedium)
    }
}
