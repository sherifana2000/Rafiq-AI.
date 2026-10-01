package com.example.ui.character

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * المكون المستقل لشخصية رفيق التفاعلية (Rafiq Character Companion)
 * تصميم رقمي عصري ومحايد جنسياً، يعتمد على Canvas ورسوم متحركة خفيفة ومرنة.
 */
@Composable
fun RafiqCharacter(
    state: CharacterState,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "character_anim")

    // حركة التنفس والطفو اللطيفة (Breathing Animation)
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_offset"
    )

    // نبض الاستماع (Listening Pulse)
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    // دوران جسيمات التفكير (Thinking Orbits)
    val thinkingAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "thinking_angle"
    )

    // حركة التحدث اللطيفة (Speaking Scale)
    val speakingMouthRatio by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(280, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speaking_mouth"
    )

    Box(
        modifier = modifier
            .size(size)
            .testTag("rafiq_character_view"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val canvasW = this.size.width
            val canvasH = this.size.height
            val centerX = canvasW / 2f
            val centerY = canvasH / 2f + floatOffset

            // 1. هالات وخلفيات الحالات المختلفة (Auras)
            when (state) {
                CharacterState.LISTENING -> {
                    // حلقات أمواج الاستماع النبضية
                    drawCircle(
                        color = Color(0xFFC084FC).copy(alpha = pulseAlpha * 0.7f),
                        radius = (canvasW * 0.44f) * pulseRadius,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFE9D5FF).copy(alpha = (pulseAlpha * 0.5f)),
                        radius = (canvasW * 0.38f) * (pulseRadius * 0.85f),
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                CharacterState.THINKING -> {
                    // مدار جسيمات التفكير المضيئة
                    val orbitRadius = canvasW * 0.44f
                    val angleRad1 = Math.toRadians(thinkingAngle.toDouble())
                    val angleRad2 = Math.toRadians((thinkingAngle + 180).toDouble())

                    val node1X = centerX + (orbitRadius * cos(angleRad1)).toFloat()
                    val node1Y = centerY + (orbitRadius * 0.35f * sin(angleRad1)).toFloat()
                    val node2X = centerX + (orbitRadius * cos(angleRad2)).toFloat()
                    val node2Y = centerY + (orbitRadius * 0.35f * sin(angleRad2)).toFloat()

                    drawCircle(
                        color = Color(0xFFFBBF24).copy(alpha = 0.85f),
                        radius = 5.dp.toPx(),
                        center = Offset(node1X, node1Y)
                    )
                    drawCircle(
                        color = Color(0xFFC084FC).copy(alpha = 0.85f),
                        radius = 4.dp.toPx(),
                        center = Offset(node2X, node2Y)
                    )
                }
                CharacterState.SUCCESS, CharacterState.HAPPY -> {
                    // وهج الاحتفال الناعم
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFF3E8FF).copy(alpha = 0.7f),
                                Color(0xFFD8B4FE).copy(alpha = 0.2f),
                                Color.Transparent
                            ),
                            center = Offset(centerX, centerY),
                            radius = canvasW * 0.5f
                        ),
                        center = Offset(centerX, centerY),
                        radius = canvasW * 0.5f
                    )
                }
                else -> {
                    // وهج الهدوء الطبيعي (IDLE)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFEDE9FE).copy(alpha = 0.45f),
                                Color.Transparent
                            ),
                            center = Offset(centerX, centerY),
                            radius = canvasW * 0.48f
                        ),
                        center = Offset(centerX, centerY),
                        radius = canvasW * 0.48f
                    )
                }
            }

            // 2. جسم الشخصية الرقمية (Luminous Capsule Companion Body)
            val bodyWidth = canvasW * 0.58f
            val bodyHeight = canvasH * 0.64f
            val bodyLeft = centerX - (bodyWidth / 2f)
            val bodyTop = centerY - (bodyHeight / 2f)

            // تدرج لوني بنفسجي هادئ وفخم
            val bodyBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF8B5CF6), // Soft vibrant violet
                    Color(0xFF6D28D9), // Deep royal purple
                    Color(0xFF4C1D95)  // Deep midnight purple
                ),
                startY = bodyTop,
                endY = bodyTop + bodyHeight
            )

            // رسم الجسم الرئيسي
            drawRoundRect(
                brush = bodyBrush,
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(bodyWidth * 0.48f, bodyWidth * 0.48f)
            )

            // إضاءة زجاجية علوية ناعمة (Glass Specular Highlight)
            drawOval(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.42f),
                        Color.White.copy(alpha = 0.05f)
                    ),
                    startY = bodyTop + 4.dp.toPx(),
                    endY = bodyTop + (bodyHeight * 0.35f)
                ),
                topLeft = Offset(centerX - (bodyWidth * 0.36f), bodyTop + 6.dp.toPx()),
                size = Size(bodyWidth * 0.72f, bodyHeight * 0.3f)
            )

            // 3. جوهرة الرأس العلوية (Antenna / Spark Gem)
            val gemY = bodyTop - 6.dp.toPx()
            drawCircle(
                color = when (state) {
                    CharacterState.LISTENING -> Color(0xFF38BDF8)
                    CharacterState.THINKING -> Color(0xFFFBBF24)
                    CharacterState.SUCCESS -> Color(0xFF34D399)
                    else -> Color(0xFFE9D5FF)
                },
                radius = 6.dp.toPx(),
                center = Offset(centerX, gemY)
            )

            // 4. العينان التفاعليتان (Expressive Animated Eyes)
            val eyeSpacing = bodyWidth * 0.22f
            val eyeY = centerY - (bodyHeight * 0.08f)
            val eyeWidth = 8.dp.toPx()
            val eyeHeight = 12.dp.toPx()

            when (state) {
                CharacterState.SUCCESS, CharacterState.HAPPY -> {
                    // أعين سعيدة مقوسة (^ ^)
                    val arcPathLeft = Path().apply {
                        moveTo(centerX - eyeSpacing - eyeWidth, eyeY + 2.dp.toPx())
                        quadraticBezierTo(
                            centerX - eyeSpacing, eyeY - 7.dp.toPx(),
                            centerX - eyeSpacing + eyeWidth, eyeY + 2.dp.toPx()
                        )
                    }
                    val arcPathRight = Path().apply {
                        moveTo(centerX + eyeSpacing - eyeWidth, eyeY + 2.dp.toPx())
                        quadraticBezierTo(
                            centerX + eyeSpacing, eyeY - 7.dp.toPx(),
                            centerX + eyeSpacing + eyeWidth, eyeY + 2.dp.toPx()
                        )
                    }
                    drawPath(arcPathLeft, color = Color.White, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                    drawPath(arcPathRight, color = Color.White, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                }
                CharacterState.THINKING -> {
                    // أعين تنظر للأعلى تفكيراً
                    drawOval(
                        color = Color.White,
                        topLeft = Offset(centerX - eyeSpacing - (eyeWidth / 2f), eyeY - 3.dp.toPx()),
                        size = Size(eyeWidth * 1.1f, eyeHeight * 0.85f)
                    )
                    drawOval(
                        color = Color.White,
                        topLeft = Offset(centerX + eyeSpacing - (eyeWidth / 2f), eyeY - 3.dp.toPx()),
                        size = Size(eyeWidth * 1.1f, eyeHeight * 0.85f)
                    )
                    // بؤبؤ للأعلى
                    drawCircle(
                        color = Color(0xFF2E1065),
                        radius = 2.5.dp.toPx(),
                        center = Offset(centerX - eyeSpacing + 1.dp.toPx(), eyeY - 1.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFF2E1065),
                        radius = 2.5.dp.toPx(),
                        center = Offset(centerX + eyeSpacing + 1.dp.toPx(), eyeY - 1.dp.toPx())
                    )
                }
                CharacterState.ERROR -> {
                    // تعبير هادئ وودود محتار
                    val linePathLeft = Path().apply {
                        moveTo(centerX - eyeSpacing - eyeWidth, eyeY)
                        lineTo(centerX - eyeSpacing + eyeWidth, eyeY)
                    }
                    val linePathRight = Path().apply {
                        moveTo(centerX + eyeSpacing - eyeWidth, eyeY)
                        lineTo(centerX + eyeSpacing + eyeWidth, eyeY)
                    }
                    drawPath(linePathLeft, color = Color.White, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
                    drawPath(linePathRight, color = Color.White, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
                }
                else -> {
                    // أعين طبيعية لماعة وودية
                    val currentEyeH = if (state == CharacterState.LISTENING) eyeHeight * 1.25f else eyeHeight

                    // العين اليسرى
                    drawOval(
                        color = Color.White,
                        topLeft = Offset(centerX - eyeSpacing - (eyeWidth / 2f), eyeY - (currentEyeH / 2f)),
                        size = Size(eyeWidth, currentEyeH)
                    )
                    // بريق العين
                    drawCircle(
                        color = Color(0xFFC084FC),
                        radius = 2.5.dp.toPx(),
                        center = Offset(centerX - eyeSpacing - 1.dp.toPx(), eyeY - 2.dp.toPx())
                    )

                    // العين اليمنى
                    drawOval(
                        color = Color.White,
                        topLeft = Offset(centerX + eyeSpacing - (eyeWidth / 2f), eyeY - (currentEyeH / 2f)),
                        size = Size(eyeWidth, currentEyeH)
                    )
                    // بريق العين
                    drawCircle(
                        color = Color(0xFFC084FC),
                        radius = 2.5.dp.toPx(),
                        center = Offset(centerX + eyeSpacing - 1.dp.toPx(), eyeY - 2.dp.toPx())
                    )
                }
            }

            // 5. الوجنتان المضيئتان باللون الوردي الناعم (Blush Cheeks)
            val cheekSpacing = bodyWidth * 0.28f
            val cheekY = eyeY + 12.dp.toPx()
            drawCircle(
                color = Color(0xFFF472B6).copy(alpha = 0.45f),
                radius = 5.dp.toPx(),
                center = Offset(centerX - cheekSpacing, cheekY)
            )
            drawCircle(
                color = Color(0xFFF472B6).copy(alpha = 0.45f),
                radius = 5.dp.toPx(),
                center = Offset(centerX + cheekSpacing, cheekY)
            )

            // 6. الفم التفاعلي اللطيف (Interactive Cute Mouth)
            val mouthY = eyeY + 13.dp.toPx()
            when (state) {
                CharacterState.LISTENING -> {
                    // فم دائري صغير مندهش بلطف ("o")
                    drawCircle(
                        color = Color.White.copy(alpha = 0.9f),
                        radius = 3.dp.toPx(),
                        center = Offset(centerX, mouthY)
                    )
                }
                CharacterState.SPEAKING -> {
                    // فم يتحدث بنعومة
                    drawOval(
                        color = Color.White,
                        topLeft = Offset(centerX - 4.dp.toPx(), mouthY - (2.5.dp.toPx() * speakingMouthRatio)),
                        size = Size(8.dp.toPx(), 5.dp.toPx() * speakingMouthRatio)
                    )
                }
                CharacterState.SUCCESS, CharacterState.HAPPY -> {
                    // ابتسامة عريضة فرحة
                    val mouthPath = Path().apply {
                        moveTo(centerX - 7.dp.toPx(), mouthY - 1.dp.toPx())
                        quadraticBezierTo(
                            centerX, mouthY + 7.dp.toPx(),
                            centerX + 7.dp.toPx(), mouthY - 1.dp.toPx()
                        )
                    }
                    drawPath(mouthPath, color = Color.White, style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round))
                }
                else -> {
                    // ابتسامة هادئة ورقيقة (IDLE)
                    val mouthPath = Path().apply {
                        moveTo(centerX - 5.dp.toPx(), mouthY)
                        quadraticBezierTo(
                            centerX, mouthY + 4.dp.toPx(),
                            centerX + 5.dp.toPx(), mouthY
                        )
                    }
                    drawPath(mouthPath, color = Color.White, style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round))
                }
            }
        }
    }
}
