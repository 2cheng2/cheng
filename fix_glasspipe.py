# -*- coding: utf-8 -*-
import io

p = '/root/zhy/app/src/main/java/GJJX/zzzwt/ltd/MainActivity.java'
m = io.open(p, encoding='utf-8').read()

# 1) 字段：移除 glass，新增 capture
m = m.replace(
    "    /** 底栏实例 */\n    private BottomNavBar navBar;\n    /** 液态玻璃背景采样器 */\n    private GlassBackdrop glass;",
    "    /** 底栏实例 */\n    private BottomNavBar navBar;\n    /** WebView 背景捕获器（替代 PixelCopy，避免反馈循环） */\n    private WebViewCapture capture;")

# 2) 启动块：替换为玻璃管线
old_block = """        /* 启动液态玻璃背景采样：抓底栏背后的真实像素并模糊 */
        glass = new GlassBackdrop(getWindow(), navBar);
        glass.setOnFrame(new GlassBackdrop.OnFrame() {
            @Override
            public void onFrame(android.graphics.Bitmap b) {
                if (navBar != null) navBar.setGlassBitmap(b);
            }
        });
        // 等布局完成后启动
        navBar.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (glass != null) glass.start();
            }
        }, 800);
    }"""
new_block = """        startGlassPipeline();
    }

    /**
     * 液态玻璃管线：
     *   每 90ms 从 WebView 直接绘制底栏区域（不含底栏自身，无反馈循环）
     *   → GlassProcessor 做 blur + lens + vibrancy → 交给底栏绘制。
     *   这就是 KSU drawBackdrop 的等价数据流。
     */
    private void startGlassPipeline() {
        if (capture == null) capture = new WebViewCapture(webView);
        final android.os.Handler h = new android.os.Handler(getMainLooper());
        final Runnable[] task = new Runnable[1];
        task[0] = new Runnable() {
            @Override
            public void run() {
                if (navBar != null && capture != null && webView != null
                        && navBar.getWidth() > 0 && navBar.getHeight() > 0) {
                    int w = navBar.getWidth(), h = navBar.getHeight();
                    android.graphics.Bitmap full = capture.capture(
                            webView.getWidth(), webView.getHeight());
                    if (full != null) {
                        int[] loc = new int[2];
                        navBar.getLocationInWindow(loc);
                        int[] wloc = new int[2];
                        webView.getLocationInWindow(wloc);
                        android.graphics.Rect region = new android.graphics.Rect(
                                loc[0] - wloc[0], loc[1] - wloc[1],
                                loc[0] - wloc[0] + w, loc[1] - wloc[1] + h);
                        android.graphics.Bitmap glass =
                                GlassProcessor.process(full, region,
                                        getResources().getDisplayMetrics().density);
                        if (glass != null) navBar.setGlassBitmap(glass);
                    }
                }
                h.postDelayed(task[0], 90L);
            }
        };
        h.postDelayed(task[0], 500);
    }"""
assert old_block in m, 'glass block not found'
m = m.replace(old_block, new_block, 1)

# 3) 生命周期里的 glass 引用
m = m.replace('        if (glass != null) glass.stop();\n', '')
m = m.replace('        if (glass != null) glass.start();\n', '')

io.open(p, 'w', encoding='utf-8').write(m)
print('MainActivity glass pipeline installed')