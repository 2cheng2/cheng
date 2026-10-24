p = '/root/zhy/app/src/main/assets/inject/bottomnav.js'
s = open(p, encoding='utf-8').read()

# ============================================================
# 修复 1：点击错位 —— px2idx 的 Math.round 边界 bug
# ============================================================
old_px2 = '''    function px2idx(clientX) {
      var r = bar.getBoundingClientRect();
      var i = Math.round((clientX - r.left - PAD) / drag.tabW);
      return Math.max(0, Math.min(ITEMS.length - 1, i));
    }'''

new_px2 = '''    /**
     * 像素 → 格索引
     *
     * 【必须用 floor 而不是 round】
     *   round 会让格子中心点 i+0.5 向上取整到 i+1，
     *   于是「点第 0 格的中心」被判定成第 1 格，
     *   指示器就跑到右边一格 —— 这正是点击错位的根因。
     *
     *   floor 的判定是：格子 i 覆盖 [i*tabW, (i+1)*tabW)，符合直觉。
     */
    function px2idx(clientX) {
      var r = bar.getBoundingClientRect();
      var rel = clientX - r.left - PAD;
      var i = Math.floor(rel / drag.tabW);
      return Math.max(0, Math.min(ITEMS.length - 1, i));
    }

    /**
     * 从事件目标反查格索引。
     * 点击时元素本身是准确的，优先用它，避免像素换算的边界误差。
     */
    function targetIdx(e) {
      var t = (e.changedTouches && e.changedTouches[0]) || e;
      var node = e.target;
      while (node && node !== bar) {
        if (node.getAttribute && node.getAttribute('data-zhy-page')) {
          var id = node.getAttribute('data-zhy-page');
          for (var i = 0; i < ITEMS.length; i++) {
            if (ITEMS[i].id === id) return i;
          }
        }
        node = node.parentNode;
      }
      /* 落点在容器空白处，退回像素换算 */
      return px2idx(t.clientX);
    }'''

assert old_px2 in s, 'px2idx not found'
s = s.replace(old_px2, new_px2, 1)

# ============================================================
# 修复 2：onUp 使用正确的索引来源
# ============================================================
old_up = '''    function onUp(e) {
      if (!drag.on) return;
      drag.on = false;

      var t = (e.changedTouches && e.changedTouches[0]) || e;
      var target = px2idx(t.clientX);
      drag.idx = target;

      /* 松手：速度归零 → 弹性回弹到目标格 */
      st.vNorm = 0;
      ind.style.transition = 'transform ' + SPRING_MS + 'ms ' + SPRING + ',\\n' +
                             'box-shadow ' + SPRING_MS + 'ms ease';
      applyVisual(target * drag.tabW);
      animatePress(0);

      if (drag.moved) go(ITEMS[target].id);
    }'''

new_up = '''    function onUp(e) {
      if (!drag.on) return;
      drag.on = false;

      /* 用元素反查索引，而不是纯像素换算：
         拖动时用像素位置（跟手），点击时用元素索引（精确）。 */
      var target = drag.moved ? px2idx(((e.changedTouches && e.changedTouches[0]) || e).clientX)
                              : targetIdx(e);
      target = Math.max(0, Math.min(ITEMS.length - 1, target));
      drag.idx = target;

      /* 松手：速度归零 → 弹性回弹到目标格 */
      st.vNorm = 0;
      ind.style.transition = 'transform ' + SPRING_MS + 'ms ' + SPRING + ',\\n' +
                             'box-shadow ' + SPRING_MS + 'ms ease';
      applyVisual(target * drag.tabW);
      animatePress(0);

      if (drag.moved) {
        go(ITEMS[target].id);
      } else {
        /* 点击：阻止 item 自己的 click 再触发一次 go，避免双路径打架。
           这里直接统一走 go()，保证指示器与页面同步。 */
        go(ITEMS[target].id);
      }
    }'''

assert old_up in s, 'onUp not found'
s = s.replace(old_up, new_up, 1)

# ============================================================
# 修复 3：移除 item 上独立的 click 监听（避免双路径）
# ============================================================
old_click = '''    el.addEventListener('click', function () { go(it.id); }, false);'''
new_click = '''    /* 不在这里绑定 click：
       拖动手势统一在 attachDrag 的 onUp 里处理，
       双路径会导致「页面跳 A，指示器停 B」的错位。 */'''
assert old_click in s, 'item click not found'
s = s.replace(old_click, new_click, 1)

open(p, 'w', encoding='utf-8').write(s)
print('点击错位已修复（floor + 元素索引 + 移除双路径）')