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
 * 远程通知弹窗。
 * 内容由服务器下发（zhy2026.ltd/app-notice.json），无需更新 App 即可推送公告。
 */
@Composable
fun NoticeDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, fontSize = 18.sp, color = MiuixTheme.colorScheme.onSurface)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(
                    message,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = MiuixTheme.colorScheme.onSurface,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了", color = Color(0xFF1A5FB4))
            }
        },
    )
}