package GJJX.zzzwt.ltd.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 关于对话框 —— 作者署名。
 * 由底栏长按任意标签触发（见 CampusGlassHost 的手势层）。
 */
@Composable
fun AboutDialog(
    onDismiss: () -> Unit,
    versionName: String = "1.0.2",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("校园服务", fontSize = 20.sp, color = MiuixTheme.colorScheme.onSurface)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(
                    "一个把校园服务平台装进口袋的小应用",
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Text(
                    "\n作者  GJXzzzh",
                    fontSize = 15.sp,
                    color = Color(0xFF1A5FB4),
                )
                Text(
                    "开发投入  16 小时",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7A90),
                )
                Text(
                    "版本  v$versionName",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7A90),
                )
                Text(
                    "\n底栏效果来自 KernelSU / miuix 项目 (Apache 2.0)",
                    fontSize = 11.sp,
                    color = Color(0xFF6B7A90),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("好的", color = Color(0xFF1A5FB4))
            }
        },
    )
}