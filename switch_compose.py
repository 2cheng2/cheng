# -*- coding: utf-8 -*-
import io

p = '/root/zhy/app/src/main/java/GJJX/zzzwt/ltd/MainActivity.java'
s = io.open(p, encoding='utf-8').read()

# 1) 删除整个手写底栏系统：setupBottomNav + startGlassPipeline + startPageWatcher + dpToPx + getSafeInsetBottom
#    定位 setupBottomNav 开头
start = s.index('    private void setupBottomNav() {')
# 找到 setupWindow 的位置作为结尾锚
end = s.index('    private void setupWindow() {')
s = s[:start] + s[end:]

# 2) 删除 navBar/capture 字段
s = s.replace("""    /** 底栏实例 */
    private BottomNavBar navBar;
    /** WebView 背景捕获器（替代 PixelCopy，避免反馈循环） */
    private WebViewCapture capture;""", "")

# 3) onCreate：调用 Compose 底栏
old_on = """        // 原生底栏（应用外壳层，与网页完全隔离）
        if (!labMode) {
            setupBottomNav();
        }"""
new_on = """        // Compose 液态玻璃底栏（KSU 同款，miuix-blur 引擎）
        if (!labMode) {
            setupComposeBar();
        }"""
assert old_on in s, 'onCreate bar call not found'
s = s.replace(old_on, new_on, 1)

# 4) 加 setupComposeBar：用 ComposeView 包 root
anchor = "    private void setupWindow() {"
add = '''    private void setupComposeBar() {
        androidx.compose.ui.platform.ComposeView bar =
                new androidx.compose.ui.platform.ComposeView(this);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        root.addView(bar, lp);

        bar.setContent(new kotlin.jvm.functions.Function2<
                androidx.compose.runtime.Composer, Integer, kotlin.Unit>() {
            @Override
            public kotlin.Unit invoke(androidx.compose.runtime.Composer composer, Integer n) {
                GJJX.zzzwt.ltd.compose.CampusGlassHostKt.CampusGlassHost(
                        webView,
                        "home",
                        pageId -> {
                            if (webView != null) {
                                webView.evaluateJavascript(
                                        "try{switchPage('" + pageId + "')}catch(e){}", null);
                            }
                            return kotlin.Unit.INSTANCE;
                        },
                        true,
                        lp2 -> kotlin.Unit.INSTANCE,
                        composer, 0);
                return kotlin.Unit.INSTANCE;
            }
        });
    }

    private void setupWindow() {'''
assert anchor in s
s = s.replace(anchor, add, 1)

# 5) 删除手写 Java 底栏文件引用（文件稍后删）
io.open(p, 'w', encoding='utf-8').write(s)
print('MainActivity switched to Compose bar')