/* 校园服务 · 启动页进度条
 *
 * 注意：这里只负责"视觉进度"，不负责跳转。
 * 真正的退出时机由原生层控制——站点首帧就绪后原生淡出启动层。
 * 因此进度条采用渐近式推进：前面跑得快，接近尾部越走越慢，
 * 始终不会走满，避免出现"进度条已 100% 但页面还没出来"的割裂感。
 */
(function () {
  'use strict';

  var n = document.getElementById('n');
  if (!n) return;

  var prog = 0;
  var timer = null;

  function step() {
    // 越接近上限越慢，形成自然的阻尼感
    var s = prog < 45 ? 6.5 : prog < 70 ? 3.4 : prog < 86 ? 1.6 : prog < 94 ? 0.6 : 0;
    if (!s) {
      clearInterval(timer);
      timer = null;
      return;
    }
    prog = Math.min(94, prog + s);
    n.style.width = prog + '%';
  }

  timer = setInterval(step, 140);

  /* 原生层淡出启动页前会调用此方法，让进度条走满再消失 */
  window.__splashFinish = function () {
    if (timer) {
      clearInterval(timer);
      timer = null;
    }
    prog = 100;
    n.style.width = '100%';
  };
})();