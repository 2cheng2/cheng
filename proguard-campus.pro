# ============================================================
# 校园服务 App —— 混淆保留规则
# 适用于 R8 / ProGuard（NP 管理器的混淆功能同源）
#
# 用法：NP 管理器「APK 混淆」里的自定义规则框粘贴本文件全部内容，
#      或者作为 -include 规则文件引入。
# ============================================================


# ---------- 1. 应用自身入口（反射/系统调用） ----------

# Activity 由系统实例化，必须保留类名与构造
-keep public class GJJX.zzzwt.ltd.MainActivity {
    public <init>(...);
    protected void onCreate(android.os.Bundle);
    protected void onResume();
    protected void onPause();
    protected void onDestroy();
}

# Application 类（如有）
-keep public class * extends android.app.Application {
    public <init>(...);
}

# 四大组件全部保留（系统按 Manifest 反射创建）
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference


# ---------- 2. 远程通知模块（JSON 字段名必须保留） ----------
#
# RemoteNotice 通过 JSONObject.optString("id") 这类方式按【字符串键】取值，
# 混淆器会把字段名混淆、但字符串常量不会变，所以真正需要保的是：
#   - RemoteNotice 的类结构（避免被优化掉/内联后逻辑失效）
#   - Notice 数据类（如果开了 data class 优化）
# 注意：JSON 的键名是字符串字面量，混淆不会改，无需额外处理。
-keep class GJJX.zzzwt.ltd.compose.RemoteNotice { *; }
-keep class GJJX.zzzwt.ltd.compose.RemoteNotice$Notice { *; }

# 通知弹窗
-keep class GJJX.zzzwt.ltd.compose.NoticeDialogKt { *; }


# ---------- 3. 启动闸门（SharedPreferences 键名） ----------

-keep class GJJX.zzzwt.ltd.compose.LaunchGate { *; }


# ---------- 4. Jetpack Compose 运行时 ----------
#
# Compose 大量依赖 lambda 生成的合成类、Composable 函数签名，
# R8 的激进优化容易把 @Composable 的 $composer 参数优化掉导致崩溃。
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.platform.** { *; }
-keep class androidx.compose.ui.viewinterop.** { *; }

# Composable lambda 合成类
-keep class **$$ExternalSyntheticLambda* { *; }
-keepclassmembers class * {
    *** lambda$*(...);
}

# Compose 的 Snapshot 状态类（反射读写）
-keep class androidx.compose.runtime.snapshots.** { *; }


# ---------- 5. miuix 模糊引擎（AGSL 着色器 + JNI） ----------
#
# miuix-blur 通过反射加载 AGSL shader，类名被混淆会导致找不到着色器
-keep class top.yukonga.miuix.kmp.blur.** { *; }
-keep class top.yukonga.miuix.kmp.shader.** { *; }
-keep class top.yukonga.miuix.kmp.theme.** { *; }

# Native 方法所在类
-keepclasseswithmembernames class * {
    native <methods>;
}


# ---------- 6. KernelSU 底栏组件（KSU 原始源码） ----------
#
# FloatingBottomBar 使用 Compose 动画 API 与手势，结构不能被裁剪
-keep class me.weishu.kernelsu.ui.component.** { *; }


# ---------- 7. WebView 交互（JS 接口反射） ----------
#
# evaluateJavascript 的回调是 ValueCallback<String>，
# 如有 @JavascriptInterface 注解的方法必须保留
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class android.webkit.** { *; }


# ---------- 8. View 的 XML 构造器（如有自定义 View） ----------

-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}


# ---------- 9. Parcelable / Serializable ----------

-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}


# ---------- 10. 枚举（values()/valueOf() 反射） ----------

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}


# ---------- 11. 注解与泛型签名 ----------

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable

# 隐藏源文件名（更彻底，但崩溃日志定位变难，按需取舍）
# -renamesourcefileattribute SourceFile


# ---------- 12. Kotlin 元数据与协程 ----------

-keep class kotlin.Metadata { *; }
-keep class kotlin.jvm.functions.** { *; }
-keep class kotlin.coroutines.** { *; }

# Kotlin 默认参数合成方法
-keepclassmembers class * {
    synthetic <methods>;
}


# ---------- 13. 优化开关建议 ----------
#
# 以下为可选项，NP 混淆一般默认已关闭激进优化。
# 如果混淆后出现「点击无反应」「玻璃不显示」等玄学问题，
# 优先关闭这些优化逐个排查。

# 保留类名不做重命名（调试期用，正式版注释掉）
# -dontobfuscate

# 不优化（避免 Compose lambda 被改写）
-dontoptimize

# 不压缩（避免反射调用的类被误删）
# -dontshrink


# ---------- 14. 自定义对话框 / 图标资源 ----------

-keep class GJJX.zzzwt.ltd.compose.CampusIcons { *; }
-keep class GJJX.zzzwt.ltd.compose.CampusIconsKt { *; }
-keep class GJJX.zzzwt.ltd.compose.*Dialog* { *; }