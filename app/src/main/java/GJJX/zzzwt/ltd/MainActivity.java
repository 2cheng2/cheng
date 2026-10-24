package GJJX.zzzwt.ltd;

import android.os.Bundle;
import androidx.activity.ComponentActivity;
import androidx.activity.compose.setContent;

/**
 * 校园服务 —— 液态玻璃演示模式（Android 9 ~ 15，minSdk 28）。
 *
 * 当前阶段：纯 Compose 演示页（KSU 液态玻璃底栏 + 曲线背景）。
 * WebView 站点壳暂缓接入：miuix-blur 的 layerBackdrop 录制范围
 * 不能包含 AndroidView（ViewLayer 会被重复挂进渲染树，
 * RenderThread prepareTreeImpl 无限递归 → SIGSEGV，2026-10-24 实测）。
 * 玻璃效果的恢复方案待定（如离屏截图源 / 纯 Compose 渲染站点）。
 */
public class MainActivity extends ComponentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        androidx.activity.EdgeToEdge.enable(this);

        setContent(new kotlin.jvm.functions.Function2<
                androidx.compose.runtime.Composer, Integer, kotlin.Unit>() {
            @Override
            public kotlin.Unit invoke(androidx.compose.runtime.Composer composer, Integer n) {
                GJJX.zzzwt.ltd.compose.CampusGlassHostKt.CampusGlassHost(
                        "home",
                        pageId -> {
                            /* 演示模式：切页仅更新选中态 */
                            return kotlin.Unit.INSTANCE;
                        },
                        true,
                        composer, 0, 0);
                return kotlin.Unit.INSTANCE;
            }
        });
    }
}