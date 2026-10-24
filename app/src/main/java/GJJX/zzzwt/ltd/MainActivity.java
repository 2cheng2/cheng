package GJJX.zzzwt.ltd;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowInsets;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 校园服务主界面（适配 Android 9 ~ 15，minSdk 28 / targetSdk 34）。
 *
 * 核心设计
 * --------
 * 1. 本地资源走 https://appassets.androidplatform.net/ 安全源，而不是 file://。
 *    file:// 源在 Chromium WebView 里被限制加载 https 子框架，
 *    改用 https 安全源后，iframe / fetch / CORS 全部按标准工作，
 *    也不需要 allowUniversalAccessFromFileURLs 这类已被 Google 标记为不安全的开关。
 *
 * 2. 启动动画与站点加载并行。
 *    WebView 在 onCreate 阶段立刻开始拉取站点，启动页是覆盖其上的独立图层；
 *    站点首帧就绪后淡出。相比"先播动画再跳转"的串行方案，感知等待显著更短。
 *
 * 3. 站点自带顶部导航，应用不加底栏，保持与浏览器一致的交互。
 */
public class MainActivity extends Activity {

    /** 本地资源安全源。以 .androidplatform.net 结尾，由本应用独占解析。 */
    private static final String ASSET_HOST = "appassets.androidplatform.net";
    private static final String LOCAL_BASE = "https://" + ASSET_HOST + "/";
    private static final String SPLASH_URL = LOCAL_BASE + "splash.html";

    /** 液态玻璃调参页（仅通过 intent extra 进入，不对外暴露） */
    private static final String LAB_URL = LOCAL_BASE + "lab.html";

    private static final String SITE_URL = "https://zhy2026.ltd";
    private static final String SITE_HOST = "zhy2026.ltd";

    /** 启动页最短展示时长，避免一闪而过 */
    private static final long SPLASH_MIN_MS = 800L;
    /** 启动页最长兜底时长，站点异常也不会永久卡住 */
    private static final long SPLASH_MAX_MS = 8000L;

    private static final int REQ_FILE_CHOOSER = 1001;

    private WebView webView;
    private FrameLayout root;
    private FrameLayout splashLayer;

    private final long startAt = System.currentTimeMillis();
    private boolean splashDismissed = false;

    private ValueCallback<Uri[]> filePathCallback;

    /** 底栏注入脚本，启动时读入内存避免每次从 assets 读盘 */
    private String injectJs;
    private String injectJsPrefix;
    /** 防止 onProgressChanged 与 onPageFinished 重复注入 */
    private boolean injected = false;
    // 各进度阈值的一次性标志，保证每个阶段只注入一次
    private boolean injectStarted = false;
    private boolean injectP30 = false;
    private boolean injectP55 = false;
    private boolean injectP80 = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupWindow();

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.WHITE);
        setContentView(root);

        buildWebView();

        // 调试入口
        boolean labMode = getIntent() != null && getIntent().hasExtra("lab");

        // 并行：立刻开始拉站点
        webView.loadUrl(labMode ? LAB_URL : SITE_URL);

        // Compose 液态玻璃底栏（KSU 同款，miuix-blur 引擎）
        if (!labMode) {
            setupComposeBar();
        }

        // 启动层后添加，天然覆盖在 WebView 之上
        if (!labMode) {
            showSplash();
        }

        // 兜底：无论站点是否就绪，到点必撤启动层
        root.postDelayed(new Runnable() {
            @Override
            public void run() {
                fadeOutSplash();
            }
        }, SPLASH_MAX_MS);
    }



    /**
     * 创建原生底栏。
     *
     * 关键：底栏是 Activity 层级的 View，直接叠在 WebView 之上，
     * 而不是注入到网页 DOM 里。这样：
     *   - 网页怎么重写 DOM 都碰不到它
     *   - 坐标由 Android 原生计算，不会错位
     *   - 背景模糊可用 RenderEffect（Android 12+）或原生渐变，不受 WebView CSS 限制
     */
    private void setupComposeBar() {
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
                        androidx.compose.ui.Modifier.Companion,
                        composer, 0, 0);
                return kotlin.Unit.INSTANCE;
            }
        });
    }

    private void setupWindow() {
        Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        // 站点页头是浅色，状态栏用白底 + 深色图标，
        // 避免"透明状态栏 + 白色图标"压在站点内容上导致看不清。
        win.setStatusBarColor(Color.WHITE);
        win.setNavigationBarColor(Color.WHITE);
        win.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        // API 29+：关闭系统为透明栏自动加的对比遮罩
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            win.setStatusBarContrastEnforced(false);
            win.setNavigationBarContrastEnforced(false);
        }

        // API 28+：允许内容延伸进刘海区
        WindowManager.LayoutParams lp = win.getAttributes();
        lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        win.setAttributes(lp);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void buildWebView() {
        webView = new WebView(this);

        /* 硬件加速策略。
           实测结论：Android WebView 里 backdrop-filter 默认不生效
           （同一台设备的 Chrome 正常，说明是 WebView 的合成路径限制）。
           这里显式开启硬件加速，并配合 CSS 侧的层提升，
           尽可能让 Chromium 走"有背景纹理"的合成分支。 */
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        /* 关键：启用 WebView 的混合合成能力。
           Android WebView 默认会为省电关掉部分合成特性，
           导致 backdrop-filter（毛玻璃/液态玻璃）渲染为无效果的白板。
           NODE_BINDING 不必开，但 OVERLAY 与 AUTO 要保留默认；
           真正起作用的是 Chromium 层面的 flags，见下方 setChromiumFlags。 */
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
                // 强制 WebView 使用独立的渲染进程合成，backdrop-filter 才能拿到背景纹理
                webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false);
            } catch (Throwable ignored) {
            }
        }
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        WebSettings s = webView.getSettings();

        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);           // 注入脚本要从 assets 读取
        s.setAllowContentAccess(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setGeolocationEnabled(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        // 去掉 UA 里的 "; wv" 标记，部分站点会据此判定为"非完整浏览器"
        String ua = s.getUserAgentString();
        if (ua != null) {
            s.setUserAgentString(ua.replace("; wv", "").replace(" Version/4.0", ""));
        }

        // 站点为 https，但图片等资源可能走 http，允许混合内容避免显示失败
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return localAsset(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl());
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
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
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                CookieManager.getInstance().flush();
                if (url != null && url.contains(SITE_HOST)) {
                    // 兜底再注入一次（onPageStarted 时 body 可能尚未生成）
                    injectBottomNav();

                    // 稍等一拍，等首屏绘制完成再撤启动层，避免露出半成品
                    root.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            dismissSplash();
                        }
                    }, 120L);
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onShowFileChooser(WebView view,
                                             ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                // 站点有 upload_photo.php / upload_avatar.php，必须实现此项，
                // 否则网页里的 <input type="file"> 在 App 内完全无响应。
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = callback;
                try {
                    Intent intent = params.createIntent();
                    startActivityForResult(intent, REQ_FILE_CHOOSER);
                } catch (ActivityNotFoundException e) {
                    filePathCallback = null;
                    Toast.makeText(MainActivity.this, "未找到可用的文件选择器",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                // 站点不使用摄像头/麦克风，一律拒绝，避免静默授权
                request.deny();
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin,
                                                           GeolocationPermissions.Callback callback) {
                callback.invoke(origin, false, false);
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
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
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                // 把页面写入 title 的诊断信息落盘，便于排查
                if (title != null && title.indexOf("ZHYDIAG") >= 0) {
                    try {
                        java.io.File f = new java.io.File(getFilesDir(), "probe.txt");
                        java.io.FileOutputStream fo = new java.io.FileOutputStream(f);
                        fo.write(title.getBytes("UTF-8"));
                        fo.close();
                    } catch (Throwable ignored) {
                    }
                }
            }
        });

        // 下载交给系统（站点有照片下载场景）
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                        String mimeType, long contentLength) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "无法打开下载链接",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * 读入底栏注入脚本。
     *
     * 采用"内联注入"而非 file:// 外链，原因是：
     * 站点是 https 源，WebView 不允许从 file:// 注入脚本到 https 页面。
     * 内联执行没有跨源限制。
     */
    private void loadInjectScript() {
        // 把系统 API 级别递给脚本，用于判定液态玻璃等级
        StringBuilder sb = new StringBuilder();
        sb.append("window.__ZHY_SDK=").append(android.os.Build.VERSION.SDK_INT).append(';');
        injectJsPrefix = sb.toString();

        try {
            java.io.InputStream in = getAssets().open("inject/bottomnav.js");
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            in.close();
            injectJs = bos.toString("UTF-8");
        } catch (IOException e) {
            injectJs = null;
        }
    }

    /** 单次注入 */
    private void injectBottomNav() {
        injectBottomNav(false);
    }

    /**
     * @param notifyReady true 时注入后立即查询底栏是否已挂载，据此撤掉启动页
     */
    private void injectBottomNav(final boolean notifyReady) {
        android.util.Log.w("ZHY", "injectBottomNav: notifyReady=" + notifyReady);
        if (injectJs == null || webView == null) return;
        final String js = injectJsPrefix + "\n" + injectJs;
        webView.post(new Runnable() {
            @Override
            public void run() {
                try {
                    webView.evaluateJavascript(js, notifyReady
                            ? new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String v) {
                                    probeBar();
                                }
                            }
                            : null);
                } catch (Throwable ignored) {
                }
            }
        });
    }

    /** 查询底栏是否已挂载，是则撤启动页 */
    private void probeBar() {
        if (webView == null) return;
        webView.evaluateJavascript("!!document.querySelector('.zhy-nav')",
                new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String v) {
                        android.util.Log.w("ZHY", "probeBar: hasBar=" + v);
                        if ("true".equals(v)) {
                            dismissSplash();
                        }
                    }
                });
    }

    /**
     * 页面加载期间多次注入。
     *
     * 为什么不能只调一次：
     *   onPageStarted 触发时，新文档的 JS 上下文还没建立，
     *   此时 evaluateJavascript 会落到旧文档里，等于没注入。
     *   而 onPageFinished 又会被站点 head 里同步加载的 ECharts CDN
     *   拖到好几秒之后，底栏就跟着晚出现。
     *
     * 所以在页面加载过程中按递增间隔连打几次，
     * 哪一次落进了新上下文就哪一次生效。脚本自身幂等，重复执行无副作用。
     */
    private void scheduleInject() {
        if (webView == null) return;
        final int[] delays = {0, 60, 150, 300, 550, 900, 1400, 2100, 3000, 4200};
        for (final int d : delays) {
            webView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // notifyReady：注入后立刻探测底栏，挂上了就撤启动页，
                    // 不再等整个页面（含 ECharts CDN）加载完。
                    injectBottomNav(true);
                }
            }, d);
        }
    }

    /** 启动层：独立 WebView 承载启动动画。
     * 用独立 WebView 而非主 WebView 的初始页，是为了不占用主 WebView 的导航历史。
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void showSplash() {
        splashLayer = new FrameLayout(this);
        splashLayer.setBackgroundColor(Color.WHITE);
        splashLayer.setClickable(true);
        root.addView(splashLayer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        WebView splash = new WebView(this);
        splash.setBackgroundColor(Color.TRANSPARENT);
        splash.setOverScrollMode(View.OVER_SCROLL_NEVER);
        splash.setVerticalScrollBarEnabled(false);
        splash.setHorizontalScrollBarEnabled(false);

        WebSettings ss = splash.getSettings();
        ss.setJavaScriptEnabled(true);
        ss.setSupportZoom(false);
        ss.setAllowFileAccess(false);
        ss.setAllowContentAccess(false);

        splash.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return localAsset(request.getUrl());
            }
        });

        splashLayer.addView(splash, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        splash.loadUrl(SPLASH_URL);
    }

    /** 站点就绪后调用：满足最短展示时长才淡出，否则延后 */
    private void dismissSplash() {
        if (splashDismissed || splashLayer == null) {
            return;
        }
        long remain = SPLASH_MIN_MS - (System.currentTimeMillis() - startAt);
        if (remain > 0) {
            root.postDelayed(new Runnable() {
                @Override
                public void run() {
                    dismissSplash();
                }
            }, remain);
            return;
        }
        fadeOutSplash();
    }

    private void fadeOutSplash() {
        if (splashDismissed || splashLayer == null) {
            return;
        }
        splashDismissed = true;
        final View layer = splashLayer;
        splashLayer = null;
        layer.animate()
                .alpha(0f)
                .setDuration(220L)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        root.removeView(layer);
                    }
                })
                .start();
    }

    /**
     * 把 /assets 映射到 https://appassets.androidplatform.net/。
     * 只处理该 host，其余请求返回 null 交给网络栈正常处理。
     */
    private WebResourceResponse localAsset(Uri uri) {
        if (uri == null || !ASSET_HOST.equals(uri.getHost())) {
            return null;
        }
        String path = uri.getPath();
        if (path == null || path.length() == 0 || "/".equals(path)) {
            path = "/splash.html";
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        // 阻断路径穿越
        if (path.contains("..")) {
            return new WebResourceResponse("text/plain", "utf-8",
                    new ByteArrayInputStream(new byte[0]));
        }
        try {
            InputStream in = getAssets().open(path);
            Map<String, String> headers = new HashMap<>();
            headers.put("Cache-Control", "no-cache");
            return new WebResourceResponse(mimeOf(path), charsetOf(path), 200, "OK", headers, in);
        } catch (IOException e) {
            return null;
        }
    }

    private static String mimeOf(String path) {
        String p = path.toLowerCase();
        if (p.endsWith(".html") || p.endsWith(".htm")) return "text/html";
        if (p.endsWith(".js")) return "application/javascript";
        if (p.endsWith(".css")) return "text/css";
        if (p.endsWith(".json")) return "application/json";
        if (p.endsWith(".svg")) return "image/svg+xml";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".jpg") || p.endsWith(".jpeg")) return "image/jpeg";
        if (p.endsWith(".webp")) return "image/webp";
        if (p.endsWith(".gif")) return "image/gif";
        if (p.endsWith(".woff2")) return "font/woff2";
        if (p.endsWith(".woff")) return "font/woff";
        if (p.endsWith(".ttf")) return "font/ttf";
        return "application/octet-stream";
    }

    private static String charsetOf(String path) {
        String p = path.toLowerCase();
        if (p.endsWith(".html") || p.endsWith(".htm") || p.endsWith(".js")
                || p.endsWith(".css") || p.endsWith(".json") || p.endsWith(".svg")) {
            return "utf-8";
        }
        return null;
    }

    /**
     * 站内链接留在 WebView；站外链接与非 http(s) 协议交给系统处理。
     */
    private boolean handleUrl(Uri uri) {
        if (uri == null) return false;
        String scheme = uri.getScheme();
        String host = uri.getHost();

        if ("http".equals(scheme) || "https".equals(scheme)) {
            if (ASSET_HOST.equals(host)) {
                return false;
            }
            if (host != null && (host.equals(SITE_HOST) || host.endsWith("." + SITE_HOST))) {
                return false;
            }
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (Exception ignored) {
            }
            return true;
        }

        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception ignored) {
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_FILE_CHOOSER) {
            if (filePathCallback != null) {
                Uri[] results = null;
                if (resultCode == RESULT_OK && data != null) {
                    results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
                }
                filePathCallback.onReceiveValue(results);
                filePathCallback = null;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView != null && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
        CookieManager.getInstance().flush();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            root.removeView(webView);
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}