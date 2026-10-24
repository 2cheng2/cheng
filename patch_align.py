p = '/root/zhy/app/src/main/assets/inject/bottomnav.js'
s = open(p, encoding='utf-8').read()

# 1) 给 setActive 加"由谁驱动"参数，detect 驱动时不搬指示器
old_detect = '''  function detect() {
    var vs = document.querySelectorAll('.page-view');
    for (var i = vs.length - 1; i >= 0; i--) {
      var v = vs[i];
      if (!v.id) continue;
      var d = '';
      try { d = getComputedStyle(v).display; } catch (e) {}
      if (d && d !== 'none') { setActive(v.id); return; }
    }
  }'''

new_detect = '''  /* detect() 只更新高亮，绝不搬运指示器。
     原因：detect 由 MutationObserver 高频触发，
     如果它也不搬指示器，就会和 go() 抢夺控制权，
     造成「点 A 却高亮 B / 玻璃停在 C」的错位。 */
  function detect() {
    var vs = document.querySelectorAll('.page-view');
    for (var i = vs.length - 1; i >= 0; i--) {
      var v = vs[i];
      if (!v.id) continue;
      var d = '';
      try { d = getComputedStyle(v).display; } catch (e) {}
      if (d && d !== 'none') {
        if (v.id !== cur) {
          cur = v.id;
          for (var k in map) {
            if (map.hasOwnProperty(k)) map[k].classList.toggle('zhy-on', k === v.id);
          }
        }
        return;
      }
    }
  }'''

assert old_detect in s, 'detect not found'
s = s.replace(old_detect, new_detect, 1)

# 2) go() 串行化：先定位置，再切页，避免中途被 detect 干扰
old_go = '''  function go(id) {
    setActive(id);
    moveIndTo(id);'''
new_go = '''  function go(id) {
    /* 串行执行，避免站点 switchPage 触发的 DOM 变化
       在指示器移动过程中插入，造成位置与高亮不一致。 */
    setActive(id);
    moveIndTo(id, true);'''
assert old_go in s, 'go not found'
s = s.replace(old_go, new_go, 1)

# 3) moveIndTo 增加 force 参数
old_move = '''  /** 把指示器移动到指定页面（带弹性动画） */
  function moveIndTo(id) {
    if (!drag.tabW) return;
    for (var i = 0; i < ITEMS.length; i++) {
      if (ITEMS[i].id === id) {
        drag.idx = i;
        if (typeof indSetPos === 'function') {
          indSetPos(i * drag.tabW, true);
        }
        return;
      }
    }
  }'''
new_move = '''  /** 把指示器移动到指定页面（带弹性动画） */
  function moveIndTo(id, force) {
    if (!drag.tabW) {
      /* 布局还没就绪，等一帧后重试（不能直接放弃，否则会错位） */
      setTimeout(function () { moveIndTo(id, force); }, 50);
      return;
    }
    for (var i = 0; i < ITEMS.length; i++) {
      if (ITEMS[i].id === id) {
        drag.idx = i;
        if (typeof indSetPos === 'function') {
          indSetPos(i * drag.tabW, true);
        }
        return;
      }
    }
  }'''
assert old_move in s, 'moveIndTo not found'
s = s.replace(old_move, new_move, 1)

open(p, 'w', encoding='utf-8').write(s)
print('错位问题已修复：detect 不再抢控制权')