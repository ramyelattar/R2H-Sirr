package com.r2h.sirr

import android.os.Bundle
import android.view.WindowManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import androidx.compose.ui.platform.LocalConfiguration
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
            music.playScene(scene, musicPrefs.volume)
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
            LocalSirrUserAudioOverride provides { isPlaying -> userMediaPlaying = isPlaying },
            LocalSirrMusicStatus provides music.status.value,
            LocalSirrAudioRetry provides {
                music.playScene(scene, musicPrefs.volume)
            }
        ) {
            Box(Modifier.fillMaxSize().background(Night)) {
                SirrAmbientBackground(scene, musicPrefs.cinematic)
                if (showSplash) {
                    SirrCinematicSplash(
                        allowMotion = musicPrefs.cinematic,
                        onFinished = { showSplash = false }
                    )
                } else if (!confirmedAdult) {
                    AdultGate(onConfirm = {
                        runCatching { vault.writePrivate("adult-consent-v1", "accepted") }
                            .onSuccess { confirmedAdult = true }
                            .isSuccess
                    })
                } else Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    // Header typography is baked into the image. Its entire top band is
                    // rendered below the system inset; reserve a proportional safe band.
                    val safeHeader = (LocalConfiguration.current.screenHeightDp.dp * .128f)
                        .coerceIn(94.dp, 126.dp)
                    Spacer(Modifier.height(safeHeader))
                    Row(
                        Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 3.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (route == "home") {
                            // Brand wordmark is part of the full-bleed wallpaper.
                            Spacer(Modifier.width(24.dp))
                        } else {
                            TextButton(onClick = { navigateHome() }) {
                                Text("رجوع  ←", color = Gold, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = {
                                val current = SirrMusicPreferences.load(context)
                                val prefs = runCatching {
                                    org.json.JSONObject(vault.readPrivate("game-effects") ?: "{}")
                                }.getOrElse { org.json.JSONObject() }
                                prefs.put("music", !current.enabled)
                                prefs.put("musicVolume", current.volume)
                                runCatching { vault.writePrivate("game-effects", prefs.toString()) }
                                    .onSuccess { musicRevision++ }
                            }) {
                                Text(if (musicPrefs.enabled) "♫  إيقاف" else "♫  تشغيل",
                                    color = Gold, style = MaterialTheme.typography.labelMedium)
                            }

                        }
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
private fun AdultGate(onConfirm: () -> Boolean) {
    var ageVerified by remember { mutableStateOf(false) }
    var consent by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
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
            Text("الدخول للكبار فقط", color = Gold, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(16.dp))
            Text("R2H-Play", color = Gold, fontSize = 45.sp,
                fontWeight = FontWeight.ExtraBold, lineHeight = 57.sp)
            Text("حكاية جديدة كل مرة", color = Color(0xFFE5AFCB),
                style = MaterialTheme.typography.titleMedium)
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
            Surface(
                shape = SoftCorner, color = Color.Transparent,
                modifier = Modifier.fillMaxWidth().r2hGlass(
                    accent = Color(0xFFFFB1D8), corner = 24.dp, lift = 13.dp
                )
            ) {
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
                text = "ابدأ R2H-Play",
                onClick = { saveFailed = !onConfirm() },
                enabled = ageVerified && consent,
                explanation = if (ageVerified && consent) "يلا نكتشف الحكاية."
                    else "علّم على الموافقتين علشان تقدر تدخل"
            )
            if (saveFailed) Text(
                "تعذّر حفظ الموافقة على الموبايل بأمان. اتأكد من مساحة التخزين وحاول تاني.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
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
private fun HomeScreen(onGo: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 19.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Spacer(Modifier.height(2.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("اختاروا اللحظة.", fontSize = 31.sp, lineHeight = 39.sp,
                color = Color.White, fontWeight = FontWeight.ExtraBold)

        }
        Text("ست ألعاب مختلفة، وكل جولة على مزاجكم.",
            color = Muted, style = MaterialTheme.typography.bodyMedium)
        R2HFeaturedHomeCard(onClick = { onGo("couple") })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("كل الألعاب", color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
            Text("اختار وابدأ", color = Gold, style = MaterialTheme.typography.labelLarge)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            R2HModeCard("شخصين", "أسئلة ومواقف", R2HGameArt.COUPLE,
                Color(0xFFFF89BD), Modifier.weight(1f)) { onGo("couple") }
            R2HModeCard("اللّمة", "كل واحد ليه دور", R2HGameArt.PARTY,
                Color(0xFFA798FB), Modifier.weight(1f)) { onGo("party") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            R2HModeCard("النرد", "الصدفة تختار", R2HGameArt.DICE,
                Color(0xFFFFAF86), Modifier.weight(1f)) { onGo("dice") }
            R2HModeCard("الاختبارات", "7 أبواب من الأسئلة", R2HGameArt.QUIZ,
                Color(0xFFC4A7FD), Modifier.weight(1f)) { onGo("quiz") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            R2HModeCard("الهمسات", "رسالة مش بتتكرر", R2HGameArt.WHISPERS,
                Color(0xFFFF9CC3), Modifier.weight(1f)) { onGo("whispers") }
            R2HModeCard("اكشف السر", "اكشط الكارت واكتشف", R2HGameArt.SECRET,
                Color(0xFFE9C08C), Modifier.weight(1f)) { onGo("scratch") }
        }
        HorizontalDivider(color = Color(0xFF634560))
        FeatureRow("أوضة أونلاين", "للعب من موبايلين مختلفين", R2HGameArt.ROOM, Color(0xFFBE9CFA)) { onGo("remote") }
        FeatureRow("الصوت والخصوصية", "اختبر الموسيقى والمظهر", R2HGameArt.MUSIC, Color(0xFFFFA7C9)) { onGo("settings") }
        TextButton(onClick = { onGo("privacy") }, modifier = Modifier.fillMaxWidth()) {
            Text("الخصوصية ومسح البيانات", color = Gold)
        }
        Spacer(Modifier.height(24.dp))
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
        SirrIntro("R2H-Play · Sound Studio", "اسمع كل لحظة.",
            "جرّب موسيقى الصفحة حالًا، وعدّل الصوت أو اقفلها تمامًا.")
        Text(LocalSirrMusicStatus.current, color = Gold,
            style = MaterialTheme.typography.bodyMedium)
        val retrySound = LocalSirrAudioRetry.current
        OutlinedButton(onClick = retrySound, modifier = Modifier.fillMaxWidth()) {
            Text("♫  جرّب الموسيقى دلوقتي", color = Gold)
        }
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
