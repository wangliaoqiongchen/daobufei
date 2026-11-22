package com.mo.daobufei

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule

private data class TabItem(val iconRes: Int, val labelRes: Int)

private val bottomTabs = listOf(
    TabItem(R.drawable.ic_vpn, R.string.tab_vpn),
    TabItem(R.drawable.ic_medication, R.string.tab_medication),
    TabItem(R.drawable.ic_alarm, R.string.tab_reminder),
    TabItem(R.drawable.ic_more, R.string.tab_more)
)

// 液态玻璃底栏:整条胶囊对页面内容做 vibrancy + blur + lens 折射,
// 选中的 tab 再叠一层小胶囊透镜高亮。参考 Kyant0/AndroidLiquidGlass catalog 实现
@Composable
fun GlassBottomBar(
    selected: Int,
    onSelected: (Int) -> Unit,
    backdrop: Backdrop,
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

    Row(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    blur(8f.dp.toPx())
                    lens(24f.dp.toPx(), 24f.dp.toPx())
                },
                onDrawSurface = { drawRect(containerColor) }
            )
            .height(64f.dp)
            .padding(horizontal = 4f.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        bottomTabs.forEachIndexed { index, tab ->
            val isSelected = index == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(56f.dp)
                    .padding(horizontal = 2f.dp)
                    .clickable { onSelected(index) }
            ) {
                if (isSelected) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .drawBackdrop(
                                backdrop = backdrop,
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
                    )
                }
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
