package com.r2h.sirr

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// M10: one shared visual language for all game entry/decision surfaces.
private val M10Cream = Color(0xFFFFE0C6)
private val M10Ink = Color(0xFF23121B)
private val M10Faint = Color(0xFFE7CCD7)
private val M10Panel = Color(0xFF201621)

@Composable
internal fun SirrAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    explanation: String? = null
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = M10Cream,
                contentColor = M10Ink,
                disabledContainerColor = Color(0xFF473640),
                disabledContentColor = Color(0xFFFFE1DC)
            ),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Text(text, fontSize = 17.sp, lineHeight = 24.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        if (!explanation.isNullOrBlank()) {
            Text(explanation, color = M10Faint,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
internal fun SirrSection(
    number: String,
    title: String,
    hint: String? = null
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(13.dp),
            color = Color(0xFF523046),
            border = BorderStroke(1.dp, Color(0xFF9F6882))
        ) {
            Text(number, modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                color = M10Cream, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = Color.White,
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (!hint.isNullOrBlank()) {
                Text(hint, color = M10Faint, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun SirrOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        color = if (selected) Color(0xFF65415C) else M10Panel,
        border = BorderStroke(if (selected) 1.7.dp else 1.dp,
            if (selected) M10Cream else Color(0xFF5F4556))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 13.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(title, color = Color.White,
                fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = M10Faint, fontSize = 12.sp,
                    lineHeight = 18.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
internal fun SirrConsent(
    name: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (checked) Color(0xFF30283A) else M10Panel,
        border = BorderStroke(1.dp,
            if (checked) Color(0xFFB68AA5) else Color(0xFF513B4B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = checked, onCheckedChange = onChange)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, color = Color.White,
                    style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(if (checked) "وافق على المشاركة" else "لسه مستنيين موافقته",
                    color = M10Faint, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun SirrIntro(
    overline: String,
    title: String,
    description: String,
    accent: Color = Color(0xFF9F4F79)
) {
    Box(
        Modifier.fillMaxWidth()
            .background(Brush.linearGradient(listOf(Color(0xFF2C1929),accent.copy(alpha=.52f),Color(0xFF1A1424))),
                RoundedCornerShape(24.dp))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 21.dp, vertical = 21.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(overline, color = M10Cream, style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold)
            Text(title, color = Color.White,
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(description, color = Color(0xFFF4DEE8),
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}
