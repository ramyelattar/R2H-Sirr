package com.r2h.sirr

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Night = Color(0xFF0B080D)
private val Wine = Color(0xFF391522)
private val Rose = Color(0xFF79334A)
private val Gold = Color(0xFFF3D2AD)
private val Muted = Color(0xFFD3BCC4)
private val SurfaceShade = Color(0xFF1D1520)
private val Palette = darkColorScheme(
    primary = Gold, onPrimary = Color(0xFF29131A),
    secondary = Muted, background = Night, surface = SurfaceShade,
    surfaceVariant = Color(0xFF322431),
    onBackground = Color(0xFFFFF6F2), onSurface = Color(0xFFFFF6F2),
    onSurfaceVariant = Muted
)
private val SoftCorner = RoundedCornerShape(24.dp)
// A large, readable Arabic text hierarchy rather than the default compact Material typography.
private val SirrType = Typography(
    headlineMedium = TextStyle(fontFamily=FontFamily.SansSerif, fontSize=29.sp,
        lineHeight=40.sp, fontWeight=FontWeight.Bold),
    headlineSmall = TextStyle(fontFamily=FontFamily.SansSerif, fontSize=24.sp,
        lineHeight=33.sp, fontWeight=FontWeight.Bold),
    titleLarge = TextStyle(fontFamily=FontFamily.SansSerif, fontSize=22.sp,
        lineHeight=30.sp, fontWeight=FontWeight.SemiBold),
    titleMedium = TextStyle(fontFamily=FontFamily.SansSerif, fontSize=18.sp,
        lineHeight=27.sp, fontWeight=FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily=FontFamily.SansSerif, fontSize=18.sp,
        lineHeight=28.sp),
    bodyMedium = TextStyle(fontFamily=FontFamily.SansSerif, fontSize=16.sp,
        lineHeight=25.sp),
    bodySmall = TextStyle(fontFamily=FontFamily.SansSerif, fontSize=14.sp,
        lineHeight=22.sp),
    labelLarge = TextStyle(fontFamily=FontFamily.SansSerif, fontSize=15.sp,
        lineHeight=22.sp, fontWeight=FontWeight.Medium),
    labelMedium = TextStyle(fontFamily=FontFamily.SansSerif, fontSize=13.sp,
        lineHeight=19.sp),
    labelSmall = TextStyle(fontFamily=FontFamily.SansSerif, fontSize=12.sp,
        lineHeight=18.sp)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cacheDir.listFiles()?.filter { it.name.startsWith("sirr_media_") }?.forEach { it.delete() }
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContent { SirrApp() }
    }
}

@Composable
private fun SirrApp() {
    val context = LocalContext.current
    val vault = remember(context) { LocalVault(context.applicationContext) }
    // A self-declared adult opt-in is remembered only in encrypted local storage.
    // This is not identity or age verification.
    var confirmedAdult by remember { mutableStateOf(
        runCatching { vault.readPrivate("adult-consent-v1") == "accepted" }.getOrDefault(false)
    ) }
    var route by remember { mutableStateOf("home") }
    var sessionVersion by remember { mutableIntStateOf(0) }
    BackHandler(enabled = confirmedAdult && route != "home") { route = "home" }
    MaterialTheme(colorScheme = Palette, typography = SirrType) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(Modifier.fillMaxSize().background(Night)) {
                if (!confirmedAdult) {
                    AdultGate(onConfirm = {
                        runCatching { vault.writePrivate("adult-consent-v1", "accepted") }
                        confirmedAdult = true
                    })
                } else Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (route == "home") {
                            Text("سِرّ", style = MaterialTheme.typography.headlineMedium, color = Gold, fontWeight = FontWeight.Bold)
                        } else {
                            TextButton(onClick = { route = "home" }) { Text("→ رجوع", color = Gold) }
                        }
                        Text("خاص بينكم  ·  18+", style = MaterialTheme.typography.labelSmall, color = Muted)
                    }
                    key(sessionVersion, route) {
                        when (route) {
                            "home" -> HomeScreen(onGo = { route = it })
                            "dice" -> DiceScreenV2()
                            "quiz" -> QuizScreenV2()
                            "scratch" -> ScratchScreenV2()
                            "whispers" -> WhispersScreenV2()
                            "couple" -> ExperienceScreen(group = false)
                            "party" -> ExperienceScreen(group = true)
                            "remote" -> RemoteScreen()
                            "privacy" -> PrivacyScreen(onClear = {
                                LocalVault(context.applicationContext).clear()
                                sessionVersion++
                                route = "home"
                                confirmedAdult = false
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdultGate(onConfirm: () -> Unit) {
    var ageVerified by remember { mutableStateOf(false) }
    var consent by remember { mutableStateOf(false) }
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF300F22), Night, Night))
        ).statusBarsPadding().navigationBarsPadding()
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text("للكبار وبس  ·  +18", color = Gold, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(28.dp))
            Text("سِرّ", color = Gold, fontSize = 68.sp, fontWeight = FontWeight.Bold, lineHeight = 76.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "مش كل الأسرار\nبتتقال بصوت عالي.",
                color = Color.White,
                fontSize = 28.sp,
                lineHeight = 39.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))
            Text("لعبة خفيفة، لحظات مختلفة، وكل حاجة بموافقتكم.", color = Muted, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(40.dp))
            Surface(shape = SoftCorner, color = Color(0xFF241823), border = BorderStroke(1.dp, Color(0xFF563643))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = ageVerified, onCheckedChange = { ageVerified = it })
                        Text("أنا عندي 18 سنة أو أكتر", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = consent, onCheckedChange = { consent = it })
                        Text("فاهم إن أي لعبة بموافقة الكل، ومن حق أي حد يوقف", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = onConfirm,
                enabled = ageVerified && consent,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) { Text("ادخل عالم سِرّ", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(12.dp))
            Text(
                "التأكيد ده بإقرار منك، مش تحقق مستقل من السن.",
                color = Muted,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
internal fun Page(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = Gold, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        content()
        Spacer(Modifier.height(42.dp))
    }
}

@Composable
private fun SmallMode(
    title: String,
    hint: String,
    glyph: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier.clip(SoftCorner)
            .background(Brush.linearGradient(listOf(tint, Color(0xFF20151F))))
            .clickable(onClick = onClick)
            .heightIn(min = 165.dp)
    ) {
        Column(Modifier.fillMaxWidth().height(165.dp).padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(glyph, fontSize = 26.sp, color = Gold)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text(hint, style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }
    }
}

@Composable
private fun FeatureRow(title: String, detail: String, glyph: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = SurfaceShade,
        border = BorderStroke(1.dp, Color(0xFF392632)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 17.dp, vertical = 17.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF3E2434)),
                contentAlignment = Alignment.Center
            ) { Text(glyph, color = Gold, fontSize = 22.sp) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(detail, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Text("‹", fontSize = 26.sp, color = Gold)
        }
    }
}

@Composable
private fun HomeScreen(onGo: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Text("جاهز تكتشفوا حاجة جديدة؟", color = Muted, style = MaterialTheme.typography.labelLarge)
        Text(
            "الليلة ليها\nحكاية تانية.",
            color = Color.White,
            fontSize = 34.sp,
            lineHeight = 43.sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp))
                .background(Brush.linearGradient(listOf(Wine, Rose, Color(0xFF241221))))
        ) {
            Canvas(Modifier.matchParentSize()) {
                val center = Offset(size.width * .89f, size.height * .12f)
                val base = size.minDimension * .23f
                for (i in 1..4) {
                    drawCircle(
                        color = Gold.copy(alpha = .16f / i),
                        radius = base * i,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
                drawCircle(
                    color = Gold.copy(alpha = .07f),
                    radius = base * .62f,
                    center = center
                )
            }
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Text("✦  بينك وبين اللي معاك", color = Gold, style = MaterialTheme.typography.labelLarge)
                Text("كل جولة وراها سر.", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp)
                Text(
                    "اختاروا المود، وخدوا أول خطوة. مفيش ضغط، وتقدروا تعدّوا أي تحدي.",
                    color = Color(0xFFF2DDE3),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { onGo("couple") },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(17.dp)
                ) { Text("يلا نبدأ", fontWeight = FontWeight.Bold) }
            }
        }
        Text("إنتوا في مود إيه؟", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Gold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallMode("لَمّة صحاب", "ضحك ومفاجآت", "✳", Color(0xFF38304D), Modifier.weight(1f)) { onGo("party") }
            SmallMode("من بعيد", "العبوا سوا أونلاين", "↗", Color(0xFF4A2638), Modifier.weight(1f)) { onGo("remote") }
        }
        Text("حاجة مختلفة؟", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Gold)
        FeatureRow("ارمي النرد", "سيبوا الاختيار للحظ", "◇") { onGo("dice") }
        FeatureRow("قد إيه تعرفوا بعض؟", "أسئلة تكشف كلام جديد", "?") { onGo("quiz") }
        FeatureRow("اكشف السر", "خربشة واحدة وتبان المفاجأة", "✦") { onGo("scratch") }
        FeatureRow("همسة مخصوص", "رسالة خاصة بتتفتح مرة", "♡") { onGo("whispers") }
        TextButton(
            onClick = { onGo("privacy") },
            modifier = Modifier.fillMaxWidth()
        ) { Text("الخصوصية والتحكم في بياناتك", color = Muted) }
        Spacer(Modifier.height(26.dp))
    }
}

@Composable
private fun PrivacyScreen(onClear: () -> Unit) = Page(
    "خصوصيتك أولًا", "الألعاب المحلية بتشتغل على موبايلك من غير إنترنت."
) {
    Text("• الصور اللي بتختارها بتتخزن مشفرة على الموبايل، ومش بتترفع لوحدها.")
    Text("• اللعب أونلاين محتاج سيرفر HTTPS خاص.")
    Text("• الرسايل الخاصة مشفرة بين المشاركين، ومقارنة إجابات الاختبار بتستخدم رموز HMAC بدل الإجابات الأصلية.")
    Text("• تشغيل الصوت والفيديو ممكن يعمل ملفات مؤقتة، والتطبيق بيحاول يمسحها لما تقفل العرض أو تفتح التطبيق تاني.")
    Text("• محدش يقدر يمنع تصوير الشاشة بموبايل تاني.")
    Text("• مسح بيانات الموبايل مش بيمسح اللي الطرف التاني احتفظ بيه أو أوضة أونلاين لسه شغالة.")
    Button(onClick = onClear, modifier = Modifier.fillMaxWidth()) {
        Text("امسح بياناتي الخاصة")
    }
}
