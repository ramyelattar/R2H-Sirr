package com.r2h.sirr

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val Ink = Color(0xFF090B12)
private val Panel = Color(0xFF191C29)
private val Accent = Color(0xFFD2AE80)
private val Palette = darkColorScheme(primary = Accent, onPrimary = Color(0xFF191006),
    secondary = Color(0xFFA6A8BD), background = Ink, surface = Panel,
    onBackground = Color(0xFFF8F5F0), onSurface = Color(0xFFF8F5F0))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Crash recovery: remove private plaintext playback files from prior sessions.
        cacheDir.listFiles()?.filter { it.name.startsWith("sirr_media_") }?.forEach { it.delete() }
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContent { SirrApp() }
    }
}

@Composable
private fun SirrApp() {
    val context = LocalContext.current
    var confirmedAdult by remember { mutableStateOf(false) }
    var route by remember { mutableStateOf("home") }
    var sessionVersion by remember { mutableIntStateOf(0) }
    BackHandler(enabled = confirmedAdult && route != "home") { route = "home" }
    MaterialTheme(colorScheme = Palette) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
                if (!confirmedAdult) AdultGate(onConfirm = { confirmedAdult = true })
                else Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        if (route == "home") Text("سِرّ", style = MaterialTheme.typography.headlineMedium, color = Accent)
                        else TextButton(onClick = { route = "home" }) { Text("→ الرئيسية") }
                        Text("للبالغين +18", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
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
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("سِرّ", style = MaterialTheme.typography.displayLarge, color = Accent)
        Spacer(Modifier.height(12.dp))
        Text("مساحة تفاعلية خاصة للبالغين", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(28.dp))
        Text("الدخول مقتصر على البالغين ١٨ سنة فأكثر. المشاركة اختيارية، ويحق لكل طرف الامتناع أو التوقف فورًا.", textAlign = TextAlign.Center)
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(ageVerified, { ageVerified = it }); Text("أؤكد أن عمري ١٨ سنة أو أكثر")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(consent, { consent = it }); Text("أفهم أن المشاركة تتطلب موافقة جميع الأطراف")
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onConfirm, enabled = ageVerified && consent, modifier = Modifier.fillMaxWidth()) { Text("دخول التطبيق") }
        Text("هذه موافقة ذاتية وليست نظامًا مستقلًا للتحقق من العمر.", color = Color.LightGray, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun Page(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Accent, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        content()
        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun Tile(title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Panel)) {
        Column(Modifier.padding(17.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = Accent, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(5.dp))
            Text(subtitle, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun HomeScreen(onGo: (String) -> Unit) = Page("مساحتك الخاصة", "ألعاب عربية للبالغين تعمل محليًا، مع خيار جلسة عن بُعد بخادم خاص.") {
    Tile("تحديات الشريكين", "١٠ جولات ومراحل جرأة وردود اختيارية محلية", { onGo("couple") })
    Tile("تحديات المجموعة", "١٢ جولة ومقاعد من ٢ إلى ٤ ونتائج عادلة", { onGo("party") })
    Tile("جلسة عبر الإنترنت", "دعوة بالرابط — ألعاب متزامنة عبر خدمة خاصة", { onGo("remote") })
    Tile("النرد التفاعلي", "٦ أوضاع و١٨ وجهًا وصور واحتمالات مخصصة", { onGo("dice") })
    Tile("اختبارات البالغين", "٧ مجموعات، ١٤٠ سؤالًا أصليًا ومقارنة محلية", { onGo("quiz") })
    Tile("الكشط والكشف", "من طبقة إلى ثلاث طبقات مع صورة خاصة", { onGo("scratch") })
    Tile("الهمسات الخاصة", "٣ رسائل وخاتمة مصوّرة وانتهاء تلقائي", { onGo("whispers") })
    Tile("الخصوصية ومسح بيانات التطبيق", "مسح صور وإعدادات النرد المشفرة", { onGo("privacy") })
}

@Composable
private fun PrivacyScreen(onClear: () -> Unit) = Page("الخصوصية", "الإعدادات الحساسة والصور محفوظة في مساحة التطبيق الخاصة.") {
    Text("• النرد والاختبارات والكشط والهمسات تعمل دون اتصال.")
    Text("• الصور المستوردة تحفظ محليًا مشفرة بـAndroid Keystore ولا تُرفع تلقائيًا.")
    Text("• رابط الدعوة والجلسة عبر الإنترنت يستخدمان خادم HTTPS تختار استضافته.")
    Text("• الملفات المشتركة مشفرة بطرفَي الجلسة بمفتاح رابط الدعوة، والرسائل مشفرة طرفيًا؛ أما الاختبارات فيتلقى الخادم وسوم HMAC ويحسب عدد الإجابات المتطابقة فقط.")
    Text("• معاينة الصوت والفيديو تنشئ ملفًا مؤقتًا داخل ذاكرة التطبيق، ويُحذف عند الخروج أو التشغيل التالي.")
    Text("• لا يدّعي التطبيق منع تصوير المحتوى بكاميرا خارجية.")
    Text("• مسح بيانات الهاتف لا يحذف ما أرسله الآخرون ولا يمسح جلسة خادم نشطة ما لم تُلغَ منفصلًا.")
    Button(onClick = onClear, modifier = Modifier.fillMaxWidth()) { Text("مسح الصور والإعدادات والعودة لبوابة العمر") }
}
