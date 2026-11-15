package com.example.orbescape

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbescape.ui.theme.GameBackground
import com.example.orbescape.ui.theme.NeonBlue
import com.example.orbescape.ui.theme.NeonGreen
import com.example.orbescape.ui.theme.NeonPink
import com.example.orbescape.ui.theme.WarningRed
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.random.Random

private data class Player(var x: Float, var y: Float, val radius: Float = 26f)
private data class Crystal(
    var x: Float,
    var y: Float,
    val radius: Float = 16f,
    val value: Int = 10,
    val color: Color = NeonGreen,
)

private data class ShieldPickup(
    val x: Float,
    val y: Float,
    val radius: Float = 17f,
    val value: Int = 50,
)

private data class HazardBarrier(
    var y: Float,
    val gaps: List<BarrierGap>,
    val speed: Float,
)

private data class BarrierGap(val center: Float, val width: Float)

private data class Star(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
    val speed: Float,
)

private data class Comet(
    val x: Float,
    val y: Float,
    val length: Float,
    val speedX: Float,
    val speedY: Float,
    val alpha: Float,
)

private data class NebulaLight(
    val x: Float,
    val y: Float,
    val radius: Float,
    val color: Color,
)

@Composable
fun OrbEscapeGameApp() {
    var player by remember { mutableStateOf(Player(0f, 0f)) }
    var targetPlayerX by remember { mutableFloatStateOf(0f) }
    var targetPlayerY by remember { mutableFloatStateOf(0f) }
    var score by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var energy by remember { mutableFloatStateOf(100f) }
    var shieldCharges by remember { mutableIntStateOf(0) }
    var shieldActive by remember { mutableStateOf(false) }
    var barriers by remember { mutableStateOf<List<HazardBarrier>>(emptyList()) }
    var crystals by remember { mutableStateOf<List<Crystal>>(emptyList()) }
    var shields by remember { mutableStateOf<List<ShieldPickup>>(emptyList()) }
    var stars by remember { mutableStateOf<List<Star>>(emptyList()) }
    var comets by remember { mutableStateOf<List<Comet>>(emptyList()) }
    var nebulaLights by remember { mutableStateOf<List<NebulaLight>>(emptyList()) }
    var started by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf(false) }
    var arenaWidth by remember { mutableFloatStateOf(0f) }
    var arenaHeight by remember { mutableFloatStateOf(0f) }

    fun resetGame() {
        player = Player(arenaWidth / 2f, arenaHeight / 2f)
        targetPlayerX = arenaWidth / 2f
        targetPlayerY = arenaHeight / 2f
        barriers = emptyList()
        crystals = emptyList()
        shields = emptyList()
        score = 0
        lives = 3
        energy = 100f
        shieldCharges = 0
        shieldActive = false
        started = false
        gameOver = false
    }


    LaunchedEffect(arenaWidth, arenaHeight) {
        if (arenaWidth <= 0f || arenaHeight <= 0f) return@LaunchedEffect
        stars = List(28) {
            Star(
                x = Random.nextFloat() * arenaWidth,
                y = Random.nextFloat() * arenaHeight,
                radius = Random.nextFloat() * 2.5f + 1f,
                alpha = Random.nextFloat() * 0.7f + 0.3f,
                speed = Random.nextFloat() * 1.1f + 0.5f,
            )
        }
        comets = List(4) { spawnComet(arenaWidth, arenaHeight) }
        nebulaLights = List(3) {
            NebulaLight(
                x = Random.nextFloat() * arenaWidth,
                y = Random.nextFloat() * arenaHeight,
                radius = Random.nextFloat() * 150f + 100f,
                color = if (it % 2 == 0) NeonBlue else NeonPink,
            )
        }
        if (!started) {
            player = Player(arenaWidth / 2f, arenaHeight / 2f)
            targetPlayerX = player.x
            targetPlayerY = player.y
        }
    }

    LaunchedEffect(started, gameOver, arenaWidth, arenaHeight) {
        if (arenaWidth <= 0f || arenaHeight <= 0f) return@LaunchedEffect

        var ticksUntilNextBarrier = 0
        var elapsedGameTicks = 0
        var invulnerabilityTicks = 0
        if (started && !gameOver) {
            if (barriers.isEmpty()) {
                val firstBarrier = spawnBarrier(arenaWidth, -BARRIER_HEIGHT, INITIAL_BARRIER_SPEED)
                val firstCrystal = spawnCrystalBetween(firstBarrier)
                barriers = listOf(firstBarrier)
                crystals = listOf(firstCrystal)
                shields = listOfNotNull(spawnShieldBetween(firstBarrier, firstCrystal))
            }
        }

        while (true) {
            delay(16L)
            stars = stars.map { star ->
                val nextY = star.y + star.speed
                star.copy(y = if (nextY > arenaHeight) 0f else nextY)
            }
            comets = comets.map { comet ->
                val nextX = comet.x + comet.speedX
                val nextY = comet.y + comet.speedY
                if (
                    nextY > arenaHeight + comet.length ||
                    nextX < -comet.length ||
                    nextX > arenaWidth + comet.length
                ) {
                    spawnComet(arenaWidth, arenaHeight)
                } else {
                    comet.copy(x = nextX, y = nextY)
                }
            }

            if (!started || gameOver) continue

            elapsedGameTicks += 1
            val barrierSpeed = (
                INITIAL_BARRIER_SPEED + elapsedGameTicks * BARRIER_ACCELERATION_PER_TICK
                ).coerceAtMost(MAX_BARRIER_SPEED)
            energy = (energy - ENERGY_DRAIN_PER_TICK).coerceAtLeast(0f)
            if (invulnerabilityTicks > 0) {
                invulnerabilityTicks -= 1
                shieldActive = invulnerabilityTicks > 0
            }

            barriers = barriers
                .map { barrier ->
                    barrier.copy(
                        y = barrier.y + barrierSpeed,
                        speed = barrierSpeed,
                    )
                }
                .filter { barrier -> barrier.y < arenaHeight + BARRIER_HEIGHT }

            val spawnInterval = (BARRIER_SPACING_PIXELS / barrierSpeed)
                .roundToInt()
                .coerceAtLeast(MIN_BARRIER_SPAWN_INTERVAL)
            ticksUntilNextBarrier += 1
            if (ticksUntilNextBarrier >= spawnInterval) {
                val nextBarrier = spawnBarrier(arenaWidth, -BARRIER_HEIGHT, barrierSpeed)
                barriers += nextBarrier
                val nextCrystal = spawnCrystalBetween(nextBarrier)
                crystals += nextCrystal
                spawnShieldBetween(nextBarrier, nextCrystal)?.let { shield ->
                    shields += shield
                }
                ticksUntilNextBarrier = 0
            }

            player = player.copy(
                x = player.x + (targetPlayerX - player.x) * PLAYER_FOLLOW_FACTOR,
                y = player.y + (targetPlayerY - player.y) * PLAYER_FOLLOW_FACTOR,
            )

            shields = shields
                .map { shield -> shield.copy(y = shield.y + barrierSpeed) }
                .filter { shield -> shield.y < arenaHeight + shield.radius }
            shields = shields.filterNot { shield ->
                val distanceFromPlayer = hypot(player.x - shield.x, player.y - shield.y)
                if (distanceFromPlayer < player.radius + shield.radius) {
                    shieldCharges += 1
                    score += shield.value
                    true
                } else {
                    false
                }
            }

            val collidingBarrier = barriers.firstOrNull { barrier ->
                val overlapsVertically =
                    kotlin.math.abs(player.y - barrier.y) < player.radius + BARRIER_HEIGHT / 2f
                val fitsThroughGap = barrier.gaps.any { gap ->
                    player.x - player.radius >= gap.center - gap.width / 2f &&
                        player.x + player.radius <= gap.center + gap.width / 2f
                }
                overlapsVertically && !fitsThroughGap
            }

            if (collidingBarrier != null && !shieldActive) {
                if (shieldCharges > 0) {
                    shieldCharges -= 1
                    invulnerabilityTicks = SHIELD_INVULNERABILITY_TICKS
                    shieldActive = true
                } else {
                    lives -= 1
                    barriers = barriers.filterNot { it == collidingBarrier }
                    if (lives <= 0) {
                        gameOver = true
                    } else {
                        player = Player(arenaWidth / 2f, arenaHeight / 2f)
                        targetPlayerX = player.x
                        targetPlayerY = player.y
                    }
                }
            }

            crystals = crystals
                .map { crystal -> crystal.copy(y = crystal.y + barrierSpeed) }
                .filter { crystal -> crystal.y < arenaHeight + crystal.radius }

            crystals = crystals.filterNot { crystal ->
                val distanceFromPlayer = hypot(player.x - crystal.x, player.y - crystal.y)
                if (distanceFromPlayer < player.radius + crystal.radius) {
                    score += crystal.value
                    energy = (energy + CRYSTAL_ENERGY).coerceAtMost(100f)
                    true
                } else {
                    false
                }
            }

            if (energy <= 0f) {
                gameOver = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground)
            .onSizeChanged { size ->
                arenaWidth = size.width.toFloat()
                arenaHeight = size.height.toFloat()
            }
            .pointerInput(gameOver) {
                detectDragGestures(
                    onDragStart = { position ->
                        if (gameOver) return@detectDragGestures
                        started = true
                        targetPlayerX = position.x.coerceIn(player.radius, arenaWidth - player.radius)
                        targetPlayerY = position.y.coerceIn(player.radius, arenaHeight - player.radius)
                    },
                    onDrag = { change, _ ->
                        if (gameOver) return@detectDragGestures
                        targetPlayerX = change.position.x.coerceIn(player.radius, arenaWidth - player.radius)
                        targetPlayerY = change.position.y.coerceIn(player.radius, arenaHeight - player.radius)
                    },
                )
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = GameBackground)

            nebulaLights.forEach { light ->
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(light.color.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(light.x, light.y),
                        radius = light.radius,
                    ),
                    radius = light.radius,
                    center = Offset(light.x, light.y),
                )
            }

            shields.forEach { shield ->
                drawCircle(
                    color = SHIELD_GOLD.copy(alpha = 0.22f),
                    radius = shield.radius + 6f,
                    center = Offset(shield.x, shield.y),
                )
                drawCircle(
                    color = SHIELD_GOLD,
                    radius = shield.radius,
                    center = Offset(shield.x, shield.y),
                )
                drawLine(
                    color = Color.White,
                    start = Offset(shield.x - 5f, shield.y + 5f),
                    end = Offset(shield.x + 1f, shield.y - 5f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = Color.White,
                    start = Offset(shield.x + 1f, shield.y - 5f),
                    end = Offset(shield.x + 5f, shield.y + 1f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round,
                )
            }

            stars.forEach { star ->
                drawCircle(
                    color = Color.White.copy(alpha = star.alpha),
                    radius = star.radius,
                    center = Offset(star.x, star.y),
                )
            }

            comets.forEach { comet ->
                drawLine(
                    color = Color.White.copy(alpha = comet.alpha * 0.35f),
                    start = Offset(comet.x - comet.speedX * comet.length * 0.3f, comet.y - comet.length),
                    end = Offset(comet.x, comet.y),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = Color.White.copy(alpha = comet.alpha),
                    radius = 2.5f,
                    center = Offset(comet.x, comet.y),
                )
            }

            barriers.forEach { barrier ->
                var segmentStart = 0f
                barrier.gaps.sortedBy { it.center }.forEach { gap ->
                    val gapLeft = gap.center - gap.width / 2f
                    val gapRight = gap.center + gap.width / 2f
                    drawBarrierSegment(segmentStart, gapLeft, barrier.y)
                    segmentStart = gapRight
                }
                drawBarrierSegment(segmentStart, size.width, barrier.y)
            }

            crystals.forEach { crystal ->
                drawCircle(
                    color = crystal.color,
                    radius = crystal.radius,
                    center = Offset(crystal.x, crystal.y),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.45f),
                    radius = crystal.radius * 0.35f,
                    center = Offset(crystal.x, crystal.y),
                )
            }

            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = player.radius * 1.7f,
                center = Offset(player.x, player.y + player.radius * 0.15f),
                style = Stroke(width = 2f),
            )
            if (shieldActive) {
                drawCircle(
                    color = SHIELD_GOLD.copy(alpha = 0.8f),
                    radius = player.radius * 1.55f,
                    center = Offset(player.x, player.y),
                    style = Stroke(width = 4f),
                )
            }
            drawLine(
                color = NeonBlue.copy(alpha = 0.35f),
                start = Offset(player.x, player.y + player.radius * 0.65f),
                end = Offset(player.x, player.y + player.radius * 1.5f),
                strokeWidth = 5f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = NeonBlue,
                start = Offset(player.x, player.y - player.radius * 0.25f),
                end = Offset(player.x, player.y + player.radius * 0.55f),
                strokeWidth = 8f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = NeonBlue,
                start = Offset(player.x - player.radius * 0.8f, player.y - player.radius * 0.1f),
                end = Offset(player.x + player.radius * 0.8f, player.y - player.radius * 0.1f),
                strokeWidth = 6f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = NeonBlue,
                start = Offset(player.x, player.y + player.radius * 0.5f),
                end = Offset(player.x - player.radius * 0.42f, player.y + player.radius),
                strokeWidth = 6f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = NeonBlue,
                start = Offset(player.x, player.y + player.radius * 0.5f),
                end = Offset(player.x + player.radius * 0.42f, player.y + player.radius),
                strokeWidth = 6f,
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = Color(0xFFFFD6B0),
                radius = player.radius * 0.28f,
                center = Offset(player.x, player.y - player.radius * 0.53f),
            )
            drawCircle(
                color = NeonBlue,
                radius = player.radius * 0.12f,
                center = Offset(player.x, player.y - player.radius * 0.53f),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Score: $score",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Text(
                    text = "Lives: $lives",
                    color = WarningRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Text(
                    text = "Energy: ${energy.toInt()}%",
                    color = if (energy <= 25f) WarningRed else NeonGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Text(
                    text = "Shield: $shieldCharges",
                    color = SHIELD_GOLD,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            }
        }

        if (!started && !gameOver) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Orb Escape",
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Collect green crystals for energy and gold shields to pass through barriers.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        if (gameOver) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Game Over",
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Final score: $score",
                        color = NeonPink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (energy <= 0f) {
                        Text(
                            text = "Energy depleted. Collect crystals to survive.",
                            color = NeonGreen,
                            fontSize = 14.sp,
                        )
                    }
                    Button(onClick = { resetGame() }) {
                        Text("Restart")
                    }
                }
            }
        }
    }
}

private const val BARRIER_HEIGHT = 28f
private const val INITIAL_BARRIER_SPEED = 12.4f
private const val BARRIER_ACCELERATION_PER_TICK = 0.02f
private const val MAX_BARRIER_SPEED = 20f
private const val BARRIER_SPACING_PIXELS = 340f
private const val MIN_BARRIER_SPAWN_INTERVAL = 15
private const val PLAYER_FOLLOW_FACTOR = 0.9f
private const val ENERGY_DRAIN_PER_TICK = 0.05f
private const val CRYSTAL_ENERGY = 30f
private const val SHIELD_INVULNERABILITY_TICKS = 30
private const val SHIELD_SPAWN_CHANCE = 0.3f
private val SHIELD_GOLD = Color(0xFFFFD166)

private fun spawnBarrier(width: Float, y: Float, speed: Float): HazardBarrier {
    val gapWidth = width * 0.18f
    val leftCenter = width * (Random.nextFloat() * 0.28f + 0.16f)
    val rightCenter = width * (Random.nextFloat() * 0.28f + 0.56f)
    return HazardBarrier(
        y = y,
        gaps = listOf(
            BarrierGap(leftCenter, gapWidth),
            BarrierGap(rightCenter, gapWidth),
        ),
        speed = speed,
    )
}

private fun spawnCrystalBetween(barrier: HazardBarrier): Crystal {
    return Crystal(
        x = barrier.gaps[Random.nextInt(barrier.gaps.size)].center,
        y = barrier.y + BARRIER_SPACING_PIXELS / 2f,
        radius = 16f,
        value = 10,
        color = NeonGreen,
    )
}

private fun spawnShieldBetween(barrier: HazardBarrier, crystal: Crystal): ShieldPickup? {
    if (Random.nextFloat() >= SHIELD_SPAWN_CHANCE) return null

    val availableGaps = barrier.gaps.filter { gap ->
        kotlin.math.abs(gap.center - crystal.x) > (gap.width + crystal.radius * 2f)
    }.ifEmpty { barrier.gaps }
    val chosenGap = availableGaps.random()
    return ShieldPickup(
        x = chosenGap.center,
        y = barrier.y + BARRIER_SPACING_PIXELS * (Random.nextFloat() * 0.6f + 0.2f),
    )
}

private fun spawnComet(width: Float, height: Float): Comet {
    return Comet(
        x = Random.nextFloat() * width,
        y = Random.nextFloat() * height - height,
        length = Random.nextFloat() * 45f + 35f,
        speedX = Random.nextFloat() * 1.6f - 0.8f,
        speedY = Random.nextFloat() * 2.5f + 2f,
        alpha = Random.nextFloat() * 0.45f + 0.25f,
    )
}

private fun DrawScope.drawBarrierSegment(left: Float, right: Float, y: Float) {
    val segmentWidth = right - left
    if (segmentWidth <= 0f) return

    drawRoundRect(
        color = WarningRed.copy(alpha = 0.22f),
        topLeft = Offset(left, y - BARRIER_HEIGHT / 2f - 6f),
        size = Size(segmentWidth, BARRIER_HEIGHT + 12f),
        cornerRadius = CornerRadius(10f),
    )
    drawRoundRect(
        color = WarningRed,
        topLeft = Offset(left, y - BARRIER_HEIGHT / 2f),
        size = Size(segmentWidth, BARRIER_HEIGHT),
        cornerRadius = CornerRadius(7f),
    )
}
