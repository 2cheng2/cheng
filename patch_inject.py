import re

p = '/root/zhy/app/src/main/java/GJJX/zzzwt/ltd/MainActivity.java'
s = open(p, encoding='utf-8').read()

# ---- 1. 补充进度标志字段 ----
old_fields = '''    /** 防止 onProgressChanged 与 onPageFinished 重复注入 */
    private boolean injected = false;'''
new_fields = '''    /** 防止 onProgressChanged 与 onPageFinished 重复注入 */
    private boolean injected = false;
    // 各进度阈值的一次性标志，保证每个阶段只注入一次
    private boolean injectStarted = false;
    private boolean injectP30 = false;
    private boolean injectP55 = false;
    private boolean injectP80 = false;'''
assert old_fields in s, 'fields not found'
s = s.replace(old_fields, new_fields, 1)

# ---- 2. onPageStarted 重置状态 ----
old_started = '''            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                // 页面一开始加载就连打多次注入，抢在 CDN 阻塞之前把底栏挂上
                if (url != null && url.contains(SITE_HOST)) {
                    scheduleInject();
                }
            }'''
new_started = '''            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (url != null && url.contains(SITE_HOST)) {
                    // 每次导航都是新的 JS 上下文，重置全部注入标志
                    injected = false;
                    injectStarted = false;
                    injectP30 = false;
                    injectP55 = false;
                    injectP80 = false;
                    scheduleInject();
                }
            }'''
assert old_started in s, 'onPageStarted not found'
s = s.replace(old_started, new_started, 1)

# ---- 3. onProgressChanged 改为进度驱动 ----
old_prog = '''            public void onProgressChanged(WebView view, int newProgress) {
                // 兜底：部分页面 onPageFinished 不触发（如长轮询/流式渲染），
                // 用 90% 进度作为注入时机。
                if (newProgress >= 90 && !injected) {
                    injected = true;
                    injectBottomNav();
                }
            }'''
new_prog = '''            public void onProgressChanged(WebView view, int newProgress) {
                // 进度驱动的持续注入：
                //   只靠 onPageStarted 不行 —— 加载慢时新文档 JS 上下文还没建立，
                //     evaluateJavascript 会落到旧文档上；
                //   只靠 onPageFinished 也不行 —— 站点 head 里同步加载 ECharts CDN，
                //     onPageFinished 可能拖到十几秒后。
                //   所以从 10% 起每跨一档补注一次，哪次落进新上下文就哪次生效。
                if (newProgress >= 10 && !injectStarted) {
                    injectStarted = true;
                    injectBottomNav();
                }
                if (newProgress >= 30 && !injectP30) {
                    injectP30 = true;
                    injectBottomNav();
                }
                if (newProgress >= 55 && !injectP55) {
                    injectP55 = true;
                    injectBottomNav();
                }
                if (newProgress >= 80 && !injectP80) {
                    injectP80 = true;
                    injectBottomNav();
                }
                if (newProgress >= 95 && !injected) {
                    injected = true;
                    injectBottomNav();
                }
            }'''
assert old_prog in s, 'onProgressChanged not found'
s = s.replace(old_prog, new_prog, 1)

open(p, 'w', encoding='utf-8').write(s)
print('MainActivity 注入时机已改为进度驱动')
print('injectStarted:', s.count('injectStarted'))
print('injectP30:', s.count('injectP30'))
print('injectP55:', s.count('injectP55'))
print('injectP80:', s.count('injectP80'))