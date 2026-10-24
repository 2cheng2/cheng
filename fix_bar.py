# -*- coding: utf-8 -*-
import io

p = '/root/zhy/composebar/src/main/java/GJJX/zzzwt/ltd/compose/CampusBottomBar.kt'
s = io.open(p, encoding='utf-8').read()

# 1) TextStyles() → defaultTextStyles()
s = s.replace('MiuixTheme(colors, TextStyles(), false, content)',
              'MiuixTheme(colors, defaultTextStyles(), false, content)')
s = s.replace('import top.yukonga.miuix.kmp.theme.TextStyles',
              'import top.yukonga.miuix.kmp.theme.TextStyles\nimport top.yukonga.miuix.kmp.theme.defaultTextStyles')

# 2) 删掉 CampusTab 与 clickableNoRipple（KSU content 是标准 RowScope 内容，
#    KSU 自己的调用点用语义 onClick 而非 Modifier.clickable——
#    看 FloatingBottomBar L349-353: semantics(mergeDescendants) { onClick { activateTab } }，
#    意味着 content 里不需要自己绑点击！）
start = s.index('@Composable\nprivate fun RowScope.CampusTab')
s = s[:start].rstrip() + '\n'

io.open(p, 'w', encoding='utf-8').write(s)
print('fixed TextStyles + removed CampusTab')