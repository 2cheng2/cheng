p = '/root/zhy/app/src/main/assets/inject/bottomnav.js'
s = open(p, encoding='utf-8').read()

start = s.index("  /* ---------- l1：玻璃 ----------")
end = s.index("  /* ---------- l2：降级 ----------")

new = r'''  /* ---------- l1：玻璃 ----------
   *
   * 【核心问题】用户反复反馈"背后有个矩形灰色方框"。
   *
   * 根因：只要给底栏设了哪怕很淡的 background 色，在 WebView 里
   *       没有 backdrop-filter 做色彩混合，它就会显示成一块"实心色板"，
   *       边缘和卡片背景的边界非常生硬 —— 这就是"矩形灰框"。
   *
   * 实测支撑：同设备同 Chromium 155，
   *   Chrome 应用   → backdrop-filter 生效，range 42
   *   本 App WebView → 完全不生效，range 254
   * 已排除：CSS 语法、透明/不透明背景、硬件加速、层提升、省电、动画缩放、GPU。
   *
   * 【解决思路】完全不设 background，改用"光"来构造玻璃：
   *   底栏内部保持透明，看到的就是真实的网站内容；
   *   只在其上叠加：
   *     1. 外投影          —— 让它在视觉上"浮起"，而非贴着一张板
   *     2. 顶部 1.5px 亮线 —— 玻璃上缘的反光（最关键，直接决定"像不像玻璃"）
   *     3. 内侧顶部柔光     —— 光从上方射入的扩散
   *     4. 底部副光         —— 下缘的反射
   *     5. 底部内阴影       —— 厚度感
   *     6. 1px 边缘聚光环   —— 圆角边缘的光线汇聚
   *     7. 左右侧壁高光     —— 玻璃侧面的折射
   *
   * 关键点：这些全部是 box-shadow / 伪元素渐变，不产生任何不透明填充区，
   *         所以背后内容 100% 可见，不会出现"灰色方块"。
   */
  css.push(
    '.zhy-nav.zhy-l1{',
      /* 完全透明 —— 这是消除"矩形灰框"的关键 */
      'background:transparent;',
      'box-shadow:',
        /* 外投影：悬浮 */
        '0 6px 20px rgba(0,0,0,.10),',
        '0 1px 4px rgba(0,0,0,.05),',
        /* 主光：顶部 1.5px 亮线（NPatch BloomStroke width 1dp, white 0.12~0.75） */
        'inset 0 1.5px 0 rgba(255,255,255,.95),',
        /* 主光扩散：内侧柔光，模拟光线射入 */
        'inset 0 3px 9px -3px rgba(255,255,255,.55),',
        /* 副光：底部反射（NPatch secondaryLight strength 0.4） */
        'inset 0 -1px 0 rgba(255,255,255,.42),',
        /* 底部内阴影：玻璃厚度（NPatch InnerShadow radius24 offsetY=radius black .15） */
        'inset 0 -7px 14px -8px rgba(0,0,0,.13),',
        /* 边缘聚光环：1px，让圆角有"折射边缘" */
        'inset 0 0 0 1px rgba(255,255,255,.68);',
    '}',
    /* 光学层：垂直光带 + 左右侧壁，全部是渐变，没有实色 */
    '.zhy-nav.zhy-l1::before{',
      'content:"";position:absolute;inset:0;border-radius:inherit;',
      'pointer-events:none;z-index:0;',
      'background:',
        /* 垂直方向：上亮 → 中透 → 下微亮 */
        'linear-gradient(180deg,',
          'rgba(255,255,255,.34) 0%,',
          'rgba(255,255,255,.16) 9%,',
          'rgba(255,255,255,.03) 26%,',
          'rgba(255,255,255,0) 52%,',
          'rgba(255,255,255,.06) 78%,',
          'rgba(255,255,255,.18) 100%),',
        /* 左右侧壁：玻璃边缘的折射高光 */
        'linear-gradient(90deg,',
          'rgba(255,255,255,.26) 0%,',
          'rgba(255,255,255,.05) 3%,',
          'rgba(255,255,255,0) 8%,',
          'rgba(255,255,255,0) 92%,',
          'rgba(255,255,255,.05) 97%,',
          'rgba(255,255,255,.26) 100%);',
    '}',
    /* 底部折射折线：玻璃下缘的暗折 */
    '.zhy-nav.zhy-l1::after{',
      'content:"";position:absolute;left:5%;right:5%;bottom:0;height:2px;',
      'pointer-events:none;z-index:1;',
      'background:linear-gradient(90deg,',
        'transparent 0%,',
        'rgba(0,0,0,.10) 50%,',
        'transparent 100%);',
    '}'
  );

'''

s = s[:start] + new + s[end:]
open(p, 'w', encoding='utf-8').write(s)
print('l1 已改为纯光效（无 background 填充）')