p = '/root/zhy/app/src/main/assets/inject/bottomnav.js'
s = open(p, encoding='utf-8').read()

start = s.index("  /* ---------- l1：玻璃 ----------")
end = s.index("  /* ---------- l2：降级 ----------")

new = r'''  /* ---------- l1：玻璃 ----------
   *
   * 设计依据（来自对本站截图的实际采样）：
   *   底栏所在的屏幕底部区域，网站自身内容是**深灰色**（约 RGB 118~131）。
   *
   * 之前的两种做法都失败：
   *   A. background:rgba(252,252,253,.62) → 一块死白色板，太实（用户："矩形灰色方框"）
   *   B. background:transparent            → 直接透出底下深灰，仍是灰框（当前问题）
   *
   * 真正的原因：底层是深灰，中间层无论"实白"还是"全透"，
   *   视觉边缘都会和周围形成硬边界，读起来就是一个方框。
   *
   * 正确做法：**用白色玻璃覆盖深灰底**，靠 alpha 把它提亮成"磨砂白"，
   *   再用顶部强光 + 边缘聚光 + 内阴影制造圆柱体积，
   *   这样在深色背景上会呈现「浮起的一块毛玻璃」，
   *   而不是"一个灰色的矩形洞"。
   *
   * 关键数值：
   *   底色 alpha .72  —— 足够把深灰(120)提亮到 ~215，形成明显的玻璃面
   *   顶部主光 .95    —— 上缘最亮，模拟光从上射入
   *   底部副光 .45    —— 下缘反射
   *   内阴影黑 .16    —— 底部厚度，让胶囊有立体感
   *   1px 边缘环 .80  —— 圆角边缘光线汇聚
   */
  css.push(
    '.zhy-nav.zhy-l1{',
      /* 白色磨砂玻璃面：把深灰底提亮，形成清晰的"玻璃"层 */
      'background:linear-gradient(180deg,',
        'rgba(255,255,255,.88) 0%,',
        'rgba(252,252,254,.80) 45%,',
        'rgba(248,249,252,.78) 100%);',
      /* 圆角必须显式声明，并且 overflow 保证子层不溢出 */
      'border-radius:' + RAD + 'px;',
      'overflow:hidden;',
      'box-shadow:',
        /* 外投影：让胶囊从深灰底上"浮起" */
        '0 10px 26px rgba(0,0,0,.20),',
        '0 3px 8px rgba(0,0,0,.10),',
        /* 顶部主光：上缘高亮线 */
        'inset 0 1.5px 0 rgba(255,255,255,1),',
        /* 顶部柔光扩散 */
        'inset 0 4px 10px -4px rgba(255,255,255,.90),',
        /* 底部副光：下缘反射 */
        'inset 0 -1px 0 rgba(255,255,255,.55),',
        /* 底部内阴影：玻璃厚度 */
        'inset 0 -8px 16px -9px rgba(0,0,0,.16),',
        /* 边缘聚光环：1px */
        'inset 0 0 0 1px rgba(255,255,255,.80);',
    '}',
    /* 光学层：垂直光带 + 左右侧壁高光 */
    '.zhy-nav.zhy-l1::before{',
      'content:"";position:absolute;inset:0;border-radius:inherit;',
      'pointer-events:none;z-index:0;',
      'background:',
        'linear-gradient(180deg,',
          'rgba(255,255,255,.50) 0%,',
          'rgba(255,255,255,.22) 10%,',
          'rgba(255,255,255,.04) 30%,',
          'rgba(255,255,255,0) 55%,',
          'rgba(255,255,255,.08) 80%,',
          'rgba(255,255,255,.24) 100%),',
        'linear-gradient(90deg,',
          'rgba(255,255,255,.40) 0%,',
          'rgba(255,255,255,.06) 3%,',
          'rgba(255,255,255,0) 9%,',
          'rgba(255,255,255,0) 91%,',
          'rgba(255,255,255,.06) 97%,',
          'rgba(255,255,255,.40) 100%);',
    '}',
    /* 底部折线：玻璃下缘的暗折 */
    '.zhy-nav.zhy-l1::after{',
      'content:"";position:absolute;left:5%;right:5%;bottom:0;height:2px;',
      'pointer-events:none;z-index:1;',
      'background:linear-gradient(90deg,transparent 0%,rgba(0,0,0,.09) 50%,transparent 100%);',
    '}'
  );

'''

s = s[:start] + new + s[end:]
open(p, 'w', encoding='utf-8').write(s)
print('l1 改为白色磨砂玻璃（适配深灰底）')