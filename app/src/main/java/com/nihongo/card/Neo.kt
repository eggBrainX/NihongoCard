package com.nihongo.card

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*

data class NeoColors(
    val bg: Color, val surface: Color, val text: Color, val border: Color, val shadow: Color,
    val main: Color, val pink: Color, val blue: Color, val green: Color, val red: Color,
    val onAccent: Color = Color(0xFF111111)
)

val LightNeo = NeoColors(
    bg = Color(0xFFFFF8E7), surface = Color.White, text = Color(0xFF111111),
    border = Color.Black, shadow = Color.Black,
    main = Color(0xFFFFD93D), pink = Color(0xFFFF9EBB), blue = Color(0xFF7CC6FF),
    green = Color(0xFF8BE8A5), red = Color(0xFFFF7A7A)
)
// Night mode: shadow berwarna (ungu), bukan hitam
val DarkNeo = NeoColors(
    bg = Color(0xFF16161D), surface = Color(0xFF262633), text = Color(0xFFF4F4F8),
    border = Color(0xFFEDEDED), shadow = Color(0xFF8B6CFF),
    main = Color(0xFFFFD93D), pink = Color(0xFFFF9EBB), blue = Color(0xFF7CC6FF),
    green = Color(0xFF8BE8A5), red = Color(0xFFFF7A7A)
)

val LocalNeo = staticCompositionLocalOf { LightNeo }

@Composable
fun NeoTheme(dark: Boolean, content: @Composable () -> Unit) {
    val c = if (dark) DarkNeo else LightNeo
    val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
        background = c.bg, surface = c.surface, onBackground = c.text, onSurface = c.text,
        primary = c.main, onPrimary = c.onAccent
    )
    val d = LocalDensity.current
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalNeo provides c, LocalDensity provides Density(d.density, d.fontScale * 1.2f)) {
            Surface(Modifier.fillMaxSize(), color = c.bg, content = content)
        }
    }
}

@Composable
fun Modifier.neo(
    bg: Color, radius: Dp = 10.dp, depth: Dp = 3.dp, pressed: Boolean = false
): Modifier {
    val c = LocalNeo.current
    val shape = RoundedCornerShape(radius)
    val shift = if (pressed) depth else 0.dp
    return this
        .padding(end = depth, bottom = depth)
        .offset(shift, shift)
        .drawBehind {
            if (!pressed) drawRoundRect(
                c.shadow, topLeft = Offset(depth.toPx(), depth.toPx()),
                size = size, cornerRadius = CornerRadius(radius.toPx())
            )
        }
        .clip(shape).background(bg).border(2.dp, c.border, shape)
}

@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    bg: Color = LocalNeo.current.surface,
    onClick: (() -> Unit)? = null,
    center: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    var m = modifier.neo(bg, pressed = pressed && onClick != null)
    if (onClick != null) m = m.clickable(src, null, onClick = onClick)
    Column(
        m.padding(12.dp),
        horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = if (center) Arrangement.Center else Arrangement.Top,
        content = content
    )
}

fun String.hasJp() = any { it.code >= 0x3000 }

/** Huruf Jepang pakai bobot normal (bold palsu membuat goresan menggumpal). */
fun jpWeight(t: String, jp: FontWeight = FontWeight.Normal, other: FontWeight = FontWeight.Black) =
    if (t.hasJp()) jp else other

@Composable
fun NeoButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    bg: Color = LocalNeo.current.main, enabled: Boolean = true, fontSize: TextUnit = 14.sp,
    fit: Boolean = false // fit = true hanya untuk tombol dengan tinggi pasti (weight)
) {
    val c = LocalNeo.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val fill = if (enabled) bg else c.surface
    val tc = if (!enabled) c.text.copy(alpha = .4f) else if (bg == c.surface) c.text else c.onAccent
    Box(
        modifier.neo(fill, pressed = pressed)
            .clickable(src, null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (fit) BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val dp = maxOf(minOf(maxHeight * 0.55f, maxWidth * 0.92f / text.length.coerceAtLeast(1)), 14.dp)
            val sz = with(LocalDensity.current) { dp.toSp() }
            Text(text, color = tc, fontWeight = jpWeight(text, FontWeight.Normal, FontWeight.Bold), fontSize = sz, textAlign = TextAlign.Center)
        } else Text(text, color = tc, fontWeight = jpWeight(text, FontWeight.Medium, FontWeight.Bold), fontSize = fontSize, textAlign = TextAlign.Center)
    }
}

@Composable
fun NeoChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = LocalNeo.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    Box(
        Modifier.neo(if (selected) c.main else c.surface, radius = 8.dp, depth = 2.dp, pressed = pressed)
            .clickable(src, null, onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp)
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (selected) c.onAccent else c.text)
    }
}

@Composable
fun NeoField(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, password: Boolean = false) {
    val c = LocalNeo.current
    Box(modifier.neo(c.surface, depth = 2.dp).padding(horizontal = 10.dp, vertical = 9.dp)) {
        BasicTextField(
            value, onChange, singleLine = true,
            textStyle = TextStyle(color = c.text, fontSize = 14.sp),
            cursorBrush = SolidColor(c.text),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(hint, color = c.text.copy(alpha = .4f), fontSize = 14.sp)
                inner()
            }
        )
    }
}

@Composable
fun Label(t: String) = Text(t, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LocalNeo.current.text)

@Composable
fun Chips(content: @Composable RowScope.() -> Unit) =
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)

@Composable
fun TopBar(title: String, back: () -> Unit) {
    val c = LocalNeo.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NeoButton("←", back, bg = c.blue)
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Black, color = c.text)
    }
}

@Composable
fun EmptyCard() {
    val c = LocalNeo.current
    NeoCard(Modifier.fillMaxWidth(), bg = c.pink) {
        Text("Belum ada kartu untuk deck/level ini. Ubah filter di Home, atau buat kartu lewat ✨ Generate AI.", color = c.onAccent, fontSize = 14.sp)
    }
}
