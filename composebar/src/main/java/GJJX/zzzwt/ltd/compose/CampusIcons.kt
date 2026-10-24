package GJJX.zzzwt.ltd.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 底栏图标 —— 自绘矢量（24dp 网格），填充式，单色（跟随 LocalContentColor）。
 */
object CampusIcons {

    private val base = ImageVector.Builder(
        name = "CampusIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )

    /** 首页：小房子 */
    val Home: ImageVector = base.apply {
        path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(12f, 3f)
            lineTo(21f, 10f)
            lineTo(21f, 21f)
            lineTo(14f, 21f)
            lineTo(14f, 15f)
            lineTo(10f, 15f)
            lineTo(10f, 21f)
            lineTo(3f, 21f)
            lineTo(3f, 10f)
            close()
        }
    }.build()

    /** 助手：四角星（魔法闪光） */
    val Tools: ImageVector = base.apply {
        path(
            fill = SolidColor(Color.Black),
        ) {
            moveTo(12f, 2f)
            lineTo(14.2f, 9.2f)
            lineTo(21.5f, 12f)
            lineTo(14.2f, 14.8f)
            lineTo(12f, 22f)
            lineTo(9.8f, 14.8f)
            lineTo(2.5f, 12f)
            lineTo(9.8f, 9.2f)
            close()
        }
    }.build()

    /** 论坛：对话气泡 */
    val Forum: ImageVector = base.apply {
        path(
            fill = SolidColor(Color.Black),
        ) {
            // 主气泡
            moveTo(3f, 4f)
            lineTo(17f, 4f)
            lineTo(17f, 13f)
            lineTo(9f, 13f)
            lineTo(5f, 17f)
            lineTo(5f, 13f)
            lineTo(3f, 13f)
            close()
            // 次气泡
            moveTo(19f, 8f)
            lineTo(21f, 8f)
            lineTo(21f, 20f)
            lineTo(17.5f, 16.5f)
            lineTo(9f, 16.5f)
            lineTo(9f, 15f)
            lineTo(19f, 15f)
            close()
        }
    }.build()

    /** 相册：照片框（山+太阳） */
    val Gallery: ImageVector = base.apply {
        path(
            fill = SolidColor(Color.Black),
        ) {
            moveTo(3f, 5f)
            lineTo(21f, 5f)
            lineTo(21f, 19f)
            lineTo(3f, 19f)
            close()
            moveTo(5f, 7f)
            lineTo(5f, 17f)
            lineTo(19f, 17f)
            lineTo(19f, 7f)
            close()
        }
        // 山与太阳用描边层补充（fill 画框，这里画内容）
        path(
            fill = SolidColor(Color.Black),
        ) {
            // 太阳
            moveTo(15.5f, 8.5f)
            curveTo(15.5f, 9.33f, 14.83f, 10f, 14f, 10f)
            curveTo(13.17f, 10f, 12.5f, 9.33f, 12.5f, 8.5f)
            curveTo(12.5f, 7.67f, 13.17f, 7f, 14f, 7f)
            curveTo(14.83f, 7f, 15.5f, 7.67f, 15.5f, 8.5f)
            close()
            // 山
            moveTo(6.5f, 15.5f)
            lineTo(9.5f, 11f)
            lineTo(11.8f, 14f)
            lineTo(13.8f, 11.5f)
            lineTo(17.5f, 15.5f)
            lineTo(8f, 15.5f)
            close()
        }
    }.build()

    /** 数据：柱状图 */
    val Scores: ImageVector = base.apply {
        path(
            fill = SolidColor(Color.Black),
        ) {
            moveTo(5f, 20f)
            lineTo(5f, 10f)
            lineTo(8f, 10f)
            lineTo(8f, 20f)
            close()
            moveTo(10.5f, 20f)
            lineTo(10.5f, 4f)
            lineTo(13.5f, 4f)
            lineTo(13.5f, 20f)
            close()
            moveTo(16f, 20f)
            lineTo(16f, 13f)
            lineTo(19f, 13f)
            lineTo(19f, 20f)
            close()
        }
    }.build()
}