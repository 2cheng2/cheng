package me.weishu.kernelsu.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

/**
 * KSU Theme.kt L125 的最小副本：
 * 校园服务无设置页，直接跟随系统深色模式。
 */
@Composable
fun isInDarkTheme(): Boolean = isSystemInDarkTheme()
