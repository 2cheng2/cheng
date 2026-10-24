# 校园服务（zhy2026.ltd 原生壳）

将 https://zhy2026.ltd（校园服务平台）包装成 Android 应用。

## 特性

- WebView 加载站点，Compose 液态玻璃底栏（KernelSU 同款）
- 底栏引擎来自 [KernelSU](https://github.com/tiann/KernelSU) 的
  `FloatingBottomBar.kt` + `DampedDragAnimation.kt` + `liquid/*`（Apache 2.0），
  依赖 [miuix-blur](https://github.com/compose-miuix-ui/miuix)（Backdrop/lens/vibrancy）
- Android 13+ 获得完整液态玻璃效果（实时模糊+折射），
  9~12 自动降级为纯色胶囊（与 KSU 行为一致）

## 构建

```bash
./gradlew :app:assembleDebug
```

或直接 push 到 main 分支，GitHub Actions 自动构建并上传 APK artifact。

## 结构

```
app/           主模块（WebView + Activity）
composebar/    液态玻璃底栏（KSU 源码 + 调用层）
```
---

第三方组件：
- 底栏 UI 来自 [KernelSU](https://github.com/tiann/KernelSU) (Apache 2.0)
- 效果引擎 [miuix](https://github.com/compose-miuix-ui/miuix) (Apache 2.0)
