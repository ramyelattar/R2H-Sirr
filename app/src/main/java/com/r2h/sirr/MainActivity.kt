package com.r2h.sirr

import android.os.Bundle
import android.view.WindowManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
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

private val Night = Color(0xFF09080D)
private val Wine = Color(0xFF391522)
private val Rose = Color(0xFF79334A)
private val Gold = Color(0xFFF3D2AD)
private val Muted = Color(0xFFE4C9D2)
private val SurfaceShade = Color(0xFF1D1520)
private val Palette = darkColorScheme(
    primary = Gold, onPrimary = Color(0xFF29131A),
    secondary = Muted, background = Night, surface = SurfaceShade,
    surfaceVariant = Color(0xFF322431),
    onBackground = Color(0xFFFFF6F2), onSurface = Color(0xFFFFF6F2),
    onSurfaceVariant = Muted,
    outline = Color(0xFFAD879A)
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
    var route by rememberSaveable { mutableStateOf("home") }
    var sessionVersion by remember { mutableIntStateOf(0) }
    var askToLeave by remember { mutableStateOf(false) }
    var showSplash by rememberSaveable { mutableStateOf(true) }
    var musicRevision by remember { mutableIntStateOf(0) }
    var userMediaPlaying by remember { mutableStateOf(false) }
    val music = remember(context) { SirrPageMusic(context.applicationContext) }
    val musicPrefs = remember(route, sessionVersion, musicRevision) {
        SirrMusicPreferences.load(context)
    }
    val scene = when {
        showSplash -> "splash"
        !confirmedAdult -> "gate"
        else -> route
    }
    DisposableEffect(music) {
        val activity = context as? ComponentActivity
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> music.setForeground(true)
                Lifecycle.Event.ON_STOP -> music.setForeground(false)
                else -> Unit
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose {
            activity?.lifecycle?.removeObserver(observer)
            music.release()
        }
    }
    LaunchedEffect(route) { userMediaPlaying = false }
    LaunchedEffect(scene, musicPrefs.enabled, musicPrefs.volume > 0, userMediaPlaying) {
        if (!musicPrefs.enabled || musicPrefs.volume == 0 || userMediaPlaying) {
            music.stop()
        } else {
            val samples = withContext(Dispatchers.Default) {
                SirrSoundtrack.render(scene)
            }
            music.playScene(samples, musicPrefs.volume)
        }
    }
    // Adjust volume on the live track instead of regenerating an eight-second
    // musical loop on every slider movement.
    LaunchedEffect(musicPrefs.volume) {
        music.setMusicVolume(musicPrefs.volume)
    }
    val immersiveRoutes = remember { setOf("couple","party","dice","quiz","scratch","whispers","remote") }
    fun navigateHome() {
        if (route in immersiveRoutes) askToLeave = true else route = "home"
    }
    BackHandler(enabled = confirmedAdult && route != "home") { navigateHome() }
    MaterialTheme(colorScheme = Palette, typography = SirrType) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl,
            LocalSirrAudioRefresh provides { musicRevision++ },
            LocalSirrUserAudioOverride provides { isPlaying -> userMediaPlaying = isPlaying }
        ) {
            Box(Modifier.fillMaxSize().background(Night)) {
                SirrAmbientBackground(scene, musicPrefs.cinematic)
                if (showSplash) {
                    SirrCinematicSplash(onFinished = { showSplash = false })
                } else if (!confirmedAdult) {
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
                            TextButton(onClick = { navigateHome() }) { Text("→ رجوع", color = Gold) }
                        }
                        Text("خاص بينكم  ·  18+", style = MaterialTheme.typography.labelSmall, color = Muted)
                    }
                    if (askToLeave && route in immersiveRoutes) {
                        AlertDialog(
                            onDismissRequest = { askToLeave = false },
                            title = { Text("عايز تخرج من اللعبة؟") },
                            text = { Text("جولات التحديات المحلية محفوظة مشفّرة، لكن وسائط المعاينة المؤقتة ممكن تتمسح.") },
                            confirmButton = {
                                TextButton(onClick = { askToLeave = false; route = "home" }) {
                                    Text("أيوه، اخرج")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { askToLeave = false }) {
                                    Text("كمّل اللعبة")
                                }
                            }
                        )
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
                            "settings" -> GameSettingsScreen(onPrivacy = { route = "privacy" })
                            "privacy" -> PrivacyScreen(onClear = {
                                val appContext = context.applicationContext
                                LocalVault(appContext).clear()
                                // Encrypted vault isn't the only private location: media previews
                                // can be temporarily decrypted into app-owned cache files.
                                appContext.cacheDir.listFiles()
                                    ?.filter { it.name.startsWith("sirr_media_") }
                                    ?.forEach { runCatching { it.delete() } }
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
            Brush.verticalGradient(listOf(
                Color(0xDD300F22), Color(0x960B0711), Color(0x8809070D)))
        ).statusBarsPadding().navigationBarsPadding()
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text("للكبار وبس  ·  +18", color = Gold, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(16.dp))
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
            Spacer(Modifier.height(28.dp))
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
            SirrAction(
                text = "ادخل عالم سِرّ",
                onClick = onConfirm,
                enabled = ageVerified && consent,
                explanation = if (ageVerified && consent) "يلا نكتشف الحكاية."
                    else "علّم على الموافقتين علشان تقدر تدخل"
            )
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
            .heightIn(min = 144.dp)
    ) {
        Column(Modifier.fillMaxWidth().heightIn(min = 144.dp).padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
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
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Spacer(Modifier.height(6.dp))
        Text("6 طرق للعب  ·  على مزاجكم", color = Gold,
            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text("الليلة مش عادية.", color = Color.White,
            fontSize = 35.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold)
        Text("اختاروا حاجة تضحككم… أو سؤال يفتح حكاية.",
            color = Muted, style = MaterialTheme.typography.bodyMedium)

        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(
                    Color(0xFF77425C), Color(0xFF401D35), Color(0xFF1C1225))))
                .border(1.dp, Gold.copy(alpha = .56f), RoundedCornerShape(28.dp))
        ) {
            Canvas(Modifier.matchParentSize()) {
                val c = Offset(size.width * .83f, size.height * .28f)
                val r = size.minDimension * .17f
                for (i in 1..4) {
                    drawCircle(Gold.copy(alpha = .28f / i), r * (i * .52f + .60f),
                        center = c, style = Stroke(width = 1.5.dp.toPx()))
                }
                val keyhole = Path().apply {
                    moveTo(c.x, c.y-r*.67f)
                    cubicTo(c.x-r*.75f,c.y-r*.65f,c.x-r*.70f,c.y+r*.15f,c.x-r*.21f,c.y+r*.28f)
                    lineTo(c.x-r*.36f,c.y+r*.99f)
                    lineTo(c.x+r*.36f,c.y+r*.99f)
                    lineTo(c.x+r*.21f,c.y+r*.28f)
                    cubicTo(c.x+r*.70f,c.y+r*.15f,c.x+r*.75f,c.y-r*.65f,c.x,c.y-r*.67f)
                    close()
                }
                drawPath(keyhole, Brush.verticalGradient(listOf(Gold.copy(alpha=.75f),Gold.copy(alpha=.14f))))
            }
            Column(
                Modifier.fillMaxWidth().padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("01  /  حكاية لشخصين", color = Gold,
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))
                Text("اختاروا مين\nهيكشف السر الأول.",
                    color = Color.White, fontSize = 28.sp,
                    lineHeight = 37.sp, fontWeight = FontWeight.Bold)
                Text("من ضحكة صغيرة لكلام مكنش بيتقال… كل جولة ليها حكاية.",
                    color = Color(0xFFF3DEEB), style = MaterialTheme.typography.bodyMedium)
                SirrAction(
                    text = "ادخلوا اللعبة  ✦",
                    onClick = { onGo("couple") }
                )
            }
        }

        SirrSection("02", "اللّمة ليها جو مختلف", "تحديات ولحظات كل واحد ليه دور فيها")
        SmallMode(
            title = "اللّمة ليها حكاية",
            hint = "من 2 لـ 4 لاعبين · كل واحد يختار",
            glyph = "✳",
            tint = Color(0xFF43365F),
            modifier = Modifier.fillMaxWidth()
        ) { onGo("party") }

        SirrSection("03", "مش لازم تلعبوا بنفس الطريقة", "اختاروا واحدة وابدأوا فورًا")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DiscoveryTile("سيبها على النرد", "3 نردات وحكاية",
                DiscoveryMotif.DICE, Modifier.weight(1f)) { onGo("dice") }
            DiscoveryTile("إنتوا شبه بعض؟", "أسئلة تفتح كلام",
                DiscoveryMotif.QUIZ, Modifier.weight(1f)) { onGo("quiz") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DiscoveryTile("اكشف السر", "الخدش يكشف المفاجأة",
                DiscoveryMotif.SCRATCH, Modifier.weight(1f)) { onGo("scratch") }
            DiscoveryTile("همسة مخصوص", "3 رسايل ورا بعض",
                DiscoveryMotif.WHISPER, Modifier.weight(1f)) { onGo("whispers") }
        }
        SirrSection("04", "من بعيد برضه تنفع", "اتصلوا بأوضة خاصة لما يكون عندكم سيرفر آمن")
        FeatureRow("أوضة خاصة أونلاين", "للعب من موبايلين مختلفين", "↗") { onGo("remote") }
        FeatureRow("ظبّط تجربة اللعب", "الصوت · الاهتزاز · الحركة", "⚙") { onGo("settings") }
        TextButton(onClick = { onGo("privacy") }, modifier = Modifier.fillMaxWidth()) {
            Text("الخصوصية والتحكم في بياناتك", color = Gold)
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun PrivacyScreen(onClear: () -> Unit) {
    var confirmClear by remember { mutableStateOf(false) }
    Page("خصوصيتك أولًا", "الألعاب المحلية بتشتغل على موبايلك من غير إنترنت.") {
    Text("• الصور اللي بتختارها بتتخزن مشفرة على الموبايل، ومش بتترفع لوحدها.")
    Text("• اللعب أونلاين محتاج سيرفر HTTPS خاص.")
    Text("• الرسايل الخاصة مشفرة بين المشاركين، ومقارنة إجابات الاختبار بتستخدم رموز HMAC بدل الإجابات الأصلية.")
    Text("• تشغيل الصوت والفيديو ممكن يعمل ملفات مؤقتة، والتطبيق بيحاول يمسحها لما تقفل العرض أو تفتح التطبيق تاني.")
    Text("• محدش يقدر يمنع تصوير الشاشة بموبايل تاني.")
    Text("• مسح بيانات الموبايل مش بيمسح اللي الطرف التاني احتفظ بيه أو أوضة أونلاين لسه شغالة.")
    OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth()) {
        Text("امسح بياناتي الخاصة")
    }
    }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("متأكد إنك عايز تمسح بياناتك؟") },
        text = { Text("ده هيمسح الصور والتحديات والإعدادات الخاصة الموجودة على الموبايل. مش هتقدر ترجعها من التطبيق، ومش هيمسح أي نسخ عند الطرف التاني.") },
        confirmButton = {
            TextButton(onClick = { confirmClear = false; onClear() }) {
                Text("امسح البيانات")
            }
        },
        dismissButton = {
            TextButton(onClick = { confirmClear = false }) {
                Text("خليها زي ما هي")
            }
        }
    )
}

@Composable
private fun GameSettingsScreen(onPrivacy: () -> Unit) {
    val effects = rememberGameEffects()
    Page("ظبّطها على مزاجك", "الاختيارات دي بتتطبق على الألعاب اللي بتدعم المؤثرات.") {
        MysteryHeader("تجربتك ليك", "كل تفصيلة بإيدك.", "صوت، حركة، اهتزاز… خليك على راحتك.", "✦")
        EffectPreferencesPanel(effects, initiallyExpanded = true)
        Text(
            "لو قفلت الصوت من هنا، مؤثرات اللعبة مش هتشتغل. الأغاني اللي بتختارها بنفسك في الهمسات ليها زر تشغيل وإيقاف مستقل.",
            style = MaterialTheme.typography.bodySmall,
            color = Muted
        )
        OutlinedButton(onClick = onPrivacy, modifier = Modifier.fillMaxWidth()) {
            Text("خصوصيتك ومسح بياناتك")
        }
    }
}
