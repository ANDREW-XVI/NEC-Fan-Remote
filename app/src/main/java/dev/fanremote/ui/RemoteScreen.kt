package dev.fanremote.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.fanremote.ir.FanCommand

private object C {
    val Bg = Color(0xFF0B0D10)
    val Surface = Color(0xFF14171C)
    val Surface2 = Color(0xFF1C2027)
    val Border = Color(0xFF262B33)
    val Track = Color(0xFF232830)
    val Text = Color(0xFFE8EBEF)
    val TextSoft = Color(0xFFC9CFD6)
    val Muted = Color(0xFF8A93A0)
    val Dim = Color(0xFF4A515C)
    val Accent = Color(0xFF6CC4FF)
    val Warn = Color(0xFFFFB547)
}

private val Label = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.16.em, color = C.Muted)

@Composable
fun RemoteScreen(
    on: Boolean,
    speed: Int,
    last: FanCommand?,
    irReady: Boolean,
    onCommand: (FanCommand) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(C.Bg)
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Header(irReady)

        Dial(on = on, speed = speed, modifier = Modifier.align(Alignment.CenterHorizontally))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundButton(64.dp, "Decrease speed", C.Surface, C.Border, onClick = { onCommand(FanCommand.SPEED_DOWN) }) {
                MinusIcon(C.Text)
            }
            PowerButton(on) { onCommand(if (on) FanCommand.OFF else FanCommand.ON) }
            RoundButton(64.dp, "Increase speed", C.Surface, C.Border, onClick = { onCommand(FanCommand.SPEED_UP) }) {
                PlusIcon(C.Text)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("SPEED", style = Label)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (n in 1..5) {
                    SpeedPill(n, selected = on && speed == n, modifier = Modifier.weight(1f)) {
                        onCommand(FanCommand.speed(n))
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        LastSent(last)
    }
}

@Composable
private fun Header(irReady: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Bedroom Fan", color = C.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em)
            Text("NEC · 38 kHz · addr 0x02", color = C.Muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(C.Surface)
                .border(1.dp, C.Border, CircleShape)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (irReady) C.Accent else C.Warn))
            Text(if (irReady) "IR ready" else "No IR blaster", color = C.TextSoft, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// Fan blades, same paths as the design (56×56 viewBox).
private val bladePaths = listOf(
    "M28 25 C 24 14, 27 5, 33 6 C 38 7, 36 18, 30 25 Z",
    "M30.6 29.5 C 38.6 37.8, 37.4 47.2, 31.6 48.8 C 26.6 50.2, 24.4 39.6, 27.6 31.4 Z",
    "M25.4 29.5 C 14.2 30.6, 7.4 24, 9.6 18.4 C 11.4 13.6, 21.6 17.4, 25.8 25.6 Z",
).map { PathParser().parsePathString(it).toPath() }

@Composable
private fun Dial(on: Boolean, speed: Int, modifier: Modifier = Modifier) {
    val segColors = (1..5).map { i ->
        animateColorAsState(if (on && speed >= i) C.Accent else C.Track, label = "seg$i").value
    }
    val bladeColor by animateColorAsState(if (on) C.Accent else C.Dim, label = "blade")

    // Spin the blades; rate follows speed, and they coast to a stop when off.
    var angle by remember { mutableFloatStateOf(0f) }
    var rate by remember { mutableFloatStateOf(0f) }
    val target by rememberUpdatedState(if (on) 360f / (1.6f - speed * 0.27f) else 0f)
    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val dt = (now - lastNanos) / 1e9f
                    rate += (target - rate) * (dt * 4f).coerceAtMost(1f)
                    angle = (angle + rate * dt) % 360f
                }
                lastNanos = now
            }
        }
    }

    Box(
        modifier
            .size(260.dp)
            .semantics { contentDescription = if (on) "Fan on, speed $speed of 5" else "Fan off" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val u = size.width / 260f            // design units → px
            val stroke = 14f * u
            val r = 108f * u
            val gap = 12f
            val seg = (270f - gap * 4) / 5f
            for (i in 0 until 5) {
                drawArc(
                    color = segColors[i],
                    startAngle = 135f + i * (seg + gap),
                    sweepAngle = seg,
                    useCenter = false,
                    topLeft = Offset(center.x - r, center.y - r),
                    size = Size(r * 2, r * 2),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Canvas(Modifier.size(56.dp)) {
                val k = size.width / 56f
                rotate(angle) {
                    scale(k, k, pivot = Offset.Zero) {
                        val s = Stroke(width = 2.5f, join = StrokeJoin.Round)
                        bladePaths.forEach { drawPath(it, bladeColor, style = s) }
                        drawCircle(bladeColor, radius = 3.5f, center = Offset(28f, 28f), style = s)
                    }
                }
            }
            Text(
                if (on) speed.toString() else "Off",
                color = if (on) C.Text else C.Dim,
                fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.04).em, lineHeight = 64.sp,
            )
            Text(if (on) "OF 5" else "TAP POWER", style = Label)
        }
    }
}

@Composable
private fun Pressable(
    modifier: Modifier,
    shape: Shape,
    description: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.94f else 1f, label = "press")
    Box(
        modifier
            .scale(s)
            .clip(shape)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun RoundButton(
    size: Dp,
    description: String,
    bg: Color,
    border: Color,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Pressable(
        Modifier.size(size).background(bg, CircleShape).border(1.dp, border, CircleShape),
        CircleShape, description, onClick, icon,
    )
}

@Composable
private fun PowerButton(on: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) C.Accent else C.Surface2, label = "pwrBg")
    val fg by animateColorAsState(if (on) C.Bg else C.TextSoft, label = "pwrFg")
    val glow by animateFloatAsState(if (on) 1f else 0f, label = "glow")
    Box(
        Modifier.size(96.dp).drawBehind {
            drawCircle(C.Accent.copy(alpha = 0.12f * glow), radius = size.width / 2 + 8.dp.toPx())
        },
        contentAlignment = Alignment.Center,
    ) {
        Pressable(
            Modifier.size(96.dp).background(bg, CircleShape).border(1.dp, if (on) C.Accent else C.Border, CircleShape),
            CircleShape, if (on) "Turn fan off" else "Turn fan on", onClick,
        ) { PowerIcon(fg) }
    }
}

@Composable
private fun SpeedPill(n: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val bg by animateColorAsState(if (selected) C.Accent else C.Surface, label = "pillBg")
    val bd by animateColorAsState(if (selected) C.Accent else C.Border, label = "pillBd")
    val fg by animateColorAsState(if (selected) C.Bg else C.TextSoft, label = "pillFg")
    Pressable(modifier.height(56.dp).background(bg, shape).border(1.dp, bd, shape), shape, "Speed $n", onClick) {
        Text(n.toString(), color = fg, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LastSent(last: FanCommand?) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().background(C.Surface, shape).border(1.dp, C.Border, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("LAST SENT", style = Label.copy(fontSize = 11.sp, letterSpacing = 0.14.em))
            Text(last?.label ?: "Nothing yet", color = C.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(last?.hex ?: "—", color = C.Accent, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
    }
}

// --- Icons (stroke, 24×24 grid, same as the design) ---

@Composable
private fun MinusIcon(color: Color) = Canvas(Modifier.size(24.dp)) {
    val k = size.width / 24f
    drawLine(color, Offset(5 * k, 12 * k), Offset(19 * k, 12 * k), 2.5f * k, StrokeCap.Round)
}

@Composable
private fun PlusIcon(color: Color) = Canvas(Modifier.size(24.dp)) {
    val k = size.width / 24f
    drawLine(color, Offset(5 * k, 12 * k), Offset(19 * k, 12 * k), 2.5f * k, StrokeCap.Round)
    drawLine(color, Offset(12 * k, 5 * k), Offset(12 * k, 19 * k), 2.5f * k, StrokeCap.Round)
}

@Composable
private fun PowerIcon(color: Color) = Canvas(Modifier.size(36.dp)) {
    val k = size.width / 24f
    val w = 2.2f * k
    drawLine(color, Offset(12 * k, 3 * k), Offset(12 * k, 11 * k), w, StrokeCap.Round)
    // Arc of radius 8 centred at (12,12.5), open at the top.
    drawArc(
        color, startAngle = -50f, sweepAngle = 280f, useCenter = false,
        topLeft = Offset(4 * k, 4.5f * k), size = Size(16 * k, 16 * k),
        style = Stroke(w, cap = StrokeCap.Round),
    )
}
