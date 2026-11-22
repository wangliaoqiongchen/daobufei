package com.mo.daobufei

import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class TabItem(val iconRes: Int, val labelRes: Int)

private val bottomTabs = listOf(
    TabItem(R.drawable.ic_vpn, R.string.tab_vpn),
    TabItem(R.drawable.ic_medication, R.string.tab_medication),
    TabItem(R.drawable.ic_alarm, R.string.tab_reminder),
    TabItem(R.drawable.ic_more, R.string.tab_more)
)

// 坐标依赖的 Backdrop:玻璃面板绘制时,把容器(View)内容按
// "面板在屏幕上的位置"平移后画进来,磨砂采样的是真实页面内容
class ViewContentBackdrop(private val contentView: () -> View) : Backdrop {

    override val isCoordinatesDependent: Boolean = true

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        val surfaceOffset = coordinates?.positionOnScreen() ?: return
        val view = contentView()
        val contentOffset = IntArray(2).also(view::getLocationOnScreen)
        val canvas = drawContext.canvas.nativeCanvas
        canvas.save()
        canvas.translate(
            (contentOffset[0] - surfaceOffset.x).toFloat(),
            (contentOffset[1] - surfaceOffset.y).toFloat()
        )
        view.draw(canvas)
        canvas.restore()
    }
}

// 液态玻璃底栏:玻璃面板只做磨砂(vibrancy+blur);选中指示器自带
// 透镜折射(实时采样页面内容,含拖动偏移),弹簧滑动 + 可拖动
@Composable
fun GlassBottomBar(
    selected: Int,
    onSelected: (Int) -> Unit,
    contentView: () -> View,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val isLight = !isSystemInDarkTheme()
    // 深色玻璃用"比背景亮一档"的白调,浅色用"比背景亮一档"的白,轮廓才读得出来
    val containerColor =
        if (isLight) Color(0xFFFFFFFF).copy(alpha = 0.72f)
        else Color(0xFFFFFFFF).copy(alpha = 0.10f)
    val rimColor =
        if (isLight) Color(0xFFFFFFFF).copy(alpha = 0.9f)
        else Color(0xFFFFFFFF).copy(alpha = 0.16f)
    val contentColor =
        if (isLight) Color(0xFF5D6673)
        else Color(0xFF98A2B0)
    val accent = colorResource(R.color.accent)
    val accentContainer = colorResource(R.color.accent_container)
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier, contentAlignment = Alignment.CenterStart) {
        val density = LocalDensity.current
        // 主行有 4dp 水平内边距,每个 tab 的实际宽度按去掉内边距后的宽度均分
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - 8f.dp.toPx()) / bottomTabs.size
        }
        val pillOffset = remember { Animatable(0f) }
        val pillScale = remember { Animatable(1f) }
        var pillCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

        // 点 tab 选中 → 指示器弹簧滑过去
        LaunchedEffect(selected) {
            pillOffset.animateTo(selected * tabWidth, spring(dampingRatio = 0.9f, stiffness = 500f))
        }

        // ---------- 1) 玻璃面板:只做磨砂,无子内容 ----------
        Row(
            Modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        // 8dp 是这台设备上实测可渲染的上限,16dp 会让整个效果层消失
                        blur(8f.dp.toPx())
                    },
                    shadow = { Shadow(alpha = if (isLight) 0.25f else 0f) },
                    onDrawSurface = {
                        drawRect(containerColor)
                        // 亮色描边:胶囊轮廓在深浅背景上都清晰
                        drawRoundRect(
                            color = rimColor,
                            cornerRadius = CornerRadius(32f.dp.toPx()),
                            style = Stroke(width = 1.5f.dp.toPx())
                        )
                    }
                )
                .height(64f.dp)
                .fillMaxWidth()
                .padding(horizontal = 4f.dp)
        ) {}

        // ---------- 2) 选中指示器:自带透镜折射(实时按视觉位置采样容器内容) ----------
        Box(
            Modifier
                .graphicsLayer {
                    translationX = pillOffset.value
                    scaleX = pillScale.value
                    scaleY = pillScale.value
                }
                .height(48f.dp)
                .fillMaxWidth(1f / bottomTabs.size)
                .padding(horizontal = 6f.dp, vertical = 4f.dp)
                .onGloballyPositioned { pillCoords = it }
                .drawBehind {
                    val coords = pillCoords ?: return@drawBehind
                    // 视觉位置 = 布局位置 + 拖动偏移(graphicsLayer 平移不进坐标)
                    val surface = coords.positionOnScreen()
                    val view = contentView()
                    val contentOffset = IntArray(2).also(view::getLocationOnScreen)
                    val canvas = drawContext.canvas.nativeCanvas
                    canvas.save()
                    canvas.translate(
                        (surface.x + pillOffset.value - contentOffset[0]).toFloat(),
                        (surface.y - contentOffset[1]).toFloat()
                    )
                    view.draw(canvas)
                    canvas.restore()
                    // 品牌绿玻璃 + 亮边
                    drawRoundRect(
                        brush = SolidColor(accentContainer.copy(alpha = 0.85f)),
                        cornerRadius = CornerRadius(20f.dp.toPx())
                    )
                    drawRoundRect(
                        color = rimColor,
                        cornerRadius = CornerRadius(20f.dp.toPx()),
                        style = Stroke(width = 1f.dp.toPx())
                    )
                }
        )

        // ---------- 3) 内容层:图标 + 文字 + 点击 + 拖动手势 ----------
        Row(
            Modifier
                .height(64f.dp)
                .fillMaxWidth()
                .padding(horizontal = 4f.dp)
                .pointerInput(tabWidth) {
                    detectDragGestures(
                        onDragStart = {
                            scope.launch { pillScale.animateTo(1.12f, spring(stiffness = 500f)) }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch { pillOffset.snapTo(pillOffset.value + dragAmount.x) }
                        },
                        onDragEnd = {
                            val nearest = (pillOffset.value / tabWidth)
                                .roundToInt()
                                .coerceIn(0, bottomTabs.size - 1)
                            onSelected(nearest)
                            scope.launch {
                                pillScale.animateTo(1f, spring(stiffness = 500f))
                                pillOffset.animateTo(
                                    nearest * tabWidth,
                                    spring(dampingRatio = 0.85f, stiffness = 500f)
                                )
                            }
                        },
                        onDragCancel = {
                            scope.launch { pillScale.animateTo(1f, spring(stiffness = 500f)) }
                        }
                    )
                }
        ) {
            bottomTabs.forEachIndexed { index, tab ->
                val isSelected = index == selected
                Box(
                    Modifier
                        .weight(1f)
                        .height(56f.dp)
                        .clickable(
                            interactionSource = null,
                            indication = null
                        ) { onSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Image(
                            painter = painterResource(tab.iconRes),
                            contentDescription = stringResource(tab.labelRes),
                            modifier = Modifier.size(22f.dp),
                            colorFilter = ColorFilter.tint(if (isSelected) accent else contentColor)
                        )
                        Spacer(Modifier.height(2f.dp))
                        androidx.compose.foundation.text.BasicText(
                            text = stringResource(tab.labelRes),
                            style = androidx.compose.ui.text.TextStyle(
                                color = if (isSelected) accent else contentColor,
                                fontSize = 11.sp
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// 顶部状态栏磨砂条:内容上滑穿过状态栏区域时被磨砂化,
// 底色用垂直渐隐过渡,避免和页面内容之间出现硬边界
@Composable
fun GlassStatusBar(backdrop: Backdrop, modifier: Modifier = Modifier) {
    val isLight = !isSystemInDarkTheme()
    val tintTop =
        if (isLight) Color(0xFFFFFFFF).copy(alpha = 0.7f)
        else Color(0xFF000000).copy(alpha = 0.5f)
    Row(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(bottomStart = 20f.dp, bottomEnd = 20f.dp) },
                effects = {
                    vibrancy()
                    blur(8f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            0f to tintTop,
                            1f to tintTop.copy(alpha = 0f)
                        )
                    )
                }
            )
            .fillMaxSize()
    ) {}
}
