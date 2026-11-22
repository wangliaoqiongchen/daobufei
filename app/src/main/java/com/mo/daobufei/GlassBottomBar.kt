package com.mo.daobufei

import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
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
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
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

// 坐标依赖的 Backdrop:每个玻璃表面绘制时,把容器(View)内容按
// "表面在屏幕上的位置"平移后画进来——整条底栏和选中胶囊各自带坐标,
// 不会出现"第一个正常其他偏移"
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

// 液态玻璃底栏(参考 Kyant0/AndroidLiquidGlass catalog 的 LiquidBottomTabs):
// 整条胶囊 vibrancy+blur+lens 折射页面内容;选中胶囊用
// 组合 backdrop(页面内容 + 隐藏 tab 内容副本)折射出 tab 内容,
// 支持点击与拖动,松手弹簧归位
@Composable
fun GlassBottomBar(
    selected: Int,
    onSelected: (Int) -> Unit,
    contentView: () -> View,
    modifier: Modifier = Modifier
) {
    val isLight = !isSystemInDarkTheme()
    val containerColor =
        if (isLight) Color(0xFFFAFAFA).copy(alpha = 0.4f)
        else Color(0xFF121212).copy(alpha = 0.4f)
    val contentColor =
        if (isLight) Color(0xFF171C22)
        else Color(0xFFE9ECF0)
    val accent = colorResource(R.color.accent)
    val backdrop = remember { ViewContentBackdrop(contentView) }
    val tabsBackdrop = rememberLayerBackdrop()
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier, contentAlignment = Alignment.CenterStart) {
        val density = LocalDensity.current
        // 主行有 4dp 水平内边距,每个 tab 的实际宽度按去掉内边距后的宽度均分
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - 8f.dp.toPx()) / bottomTabs.size
        }
        val pillOffset = remember { Animatable(0f) }
        val pillScale = remember { Animatable(1f) }

        // 点 tab 选中 → 胶囊弹簧滑过去
        LaunchedEffect(selected) {
            pillOffset.animateTo(selected * tabWidth, spring(dampingRatio = 0.9f, stiffness = 500f))
        }

        // ---------- 主行:玻璃胶囊 + 四个 tab ----------
        // 只用 vibrancy+blur,不加 lens:lens 的折射位移会在滚动时产生重影;
        // 折射细节留给选中胶囊的小面积透镜
        Row(
            Modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                    },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .height(64f.dp)
                .fillMaxWidth()
                .padding(horizontal = 4f.dp),
            verticalAlignment = Alignment.CenterVertically
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

        // ---------- 隐藏的 tab 内容副本:给选中胶囊的透镜提供采样源(避免灰块) ----------
        Row(
            Modifier
                .alpha(0f)
                .layerBackdrop(tabsBackdrop)
                .height(56f.dp)
                .fillMaxWidth()
                .graphicsLayer(colorFilter = ColorFilter.tint(accent))
                .padding(horizontal = 4f.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomTabs.forEach { tab ->
                Box(
                    Modifier.weight(1f).height(56f.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(painterResource(tab.iconRes), null, Modifier.size(22f.dp))
                }
            }
        }

        // ---------- 选中胶囊:可拖动,按住微放大(下压感),松手弹簧归位到最近 tab ----------
        Box(
            Modifier
                .padding(horizontal = 4f.dp)
                .graphicsLayer {
                    translationX = pillOffset.value
                    scaleX = pillScale.value
                    scaleY = pillScale.value
                }
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
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { Capsule() },
                    effects = {
                        lens(10f.dp.toPx(), 14f.dp.toPx(), chromaticAberration = true)
                    },
                    highlight = { Highlight.Default },
                    shadow = { Shadow(alpha = 0.3f) },
                    onDrawSurface = {
                        drawRect(
                            if (isLight) Color.Black.copy(alpha = 0.08f)
                            else Color.White.copy(alpha = 0.08f)
                        )
                    }
                )
                .height(56f.dp)
                .fillMaxWidth(1f / bottomTabs.size)
        )
    }
}
