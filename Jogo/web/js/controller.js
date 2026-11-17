// CONTROLLER: teclado, toque e o loop do jogo. Mesmos comandos do GameController.java.
const STEP = 1000 / 60; // a lógica sempre roda a 60 passos por segundo (igual em tela de 60 ou 120 Hz)
const JUMP_KEYS = ['ArrowUp', 'KeyW', 'Space'];
const DOWN_KEYS = ['ArrowDown', 'KeyS', 'ShiftLeft', 'ShiftRight'];

export class GameController {
  constructor(model, view, isTouch) {
    this.m = model;
    this.v = view;
    this.isTouch = isTouch;
    this.keyJump = this.keyDown = this.touchJump = this.touchDown = false;
    this.btnJump = document.getElementById('btnPular');
    this.btnDown = document.getElementById('btnDeslizar');
    this.btnPause = document.getElementById('btnPausa');

    addEventListener('keydown', e => this.onKey(e, true));
    addEventListener('keyup', e => this.onKey(e, false));
    hold(this.btnJump, on => { this.touchJump = on; });
    hold(this.btnDown, on => { this.touchDown = on; });
    this.btnPause.addEventListener('pointerdown', e => { e.preventDefault(); this.m.pause(); });
    view.canvas.addEventListener('pointerdown', e => this.onTap(e));
    document.addEventListener('visibilitychange', () => { if (document.hidden) this.m.pause(); });
  }

  start() {
    let last = performance.now(), acc = 0;
    const frame = now => {
      acc += Math.min(250, now - last);
      last = now;
      while (acc >= STEP) {
        this.m.step(this.keyJump || this.touchJump, this.keyDown || this.touchDown);
        acc -= STEP;
      }
      this.v.render();
      const playing = this.isTouch && this.m.state === 'PLAYING';
      this.btnJump.classList.toggle('on', playing);
      this.btnDown.classList.toggle('on', playing);
      this.btnPause.classList.toggle('on', playing);
      requestAnimationFrame(frame);
    };
    requestAnimationFrame(frame);
  }

  onKey(e, pressed) {
    const k = e.code, isJump = JUMP_KEYS.includes(k), isDown = DOWN_KEYS.includes(k);
    if (isJump || isDown || k === 'Enter') e.preventDefault(); // não rolar a página
    if (!pressed) {
      if (isJump) this.keyJump = false;
      if (isDown) this.keyDown = false;
      return;
    }
    if (isJump) this.keyJump = true;
    if (isDown) this.keyDown = true;
    const m = this.m, confirm = k === 'Enter' || k === 'Space', escape = k === 'Escape';

    switch (m.state) {
      case 'MENU':
        if (k === 'ArrowUp' || k === 'KeyW') m.moveMenuSelection(-1);
        if (k === 'ArrowDown' || k === 'KeyS') m.moveMenuSelection(1);
        if (confirm) { this.keyJump = false; m.chooseMenu(); } // não pular sozinha ao começar
        break;
      case 'OPTIONS':
        if (confirm || escape) m.closeOptions();
        break;
      case 'INTRO':
        if (confirm) m.advanceIntro();
        if (escape) m.skipIntro();
        this.keyJump = false;
        break;
      case 'PLAYING':
        if (escape || k === 'KeyP') m.pause();
        break;
      case 'PAUSED':
        if (escape || k === 'KeyP') m.resume();
        if (k === 'KeyQ') m.backToMenu();
        break;
      case 'GAME_OVER':
        if (confirm) { this.keyJump = false; m.restart(); }
        if (escape) m.backToMenu();
        break;
      case 'STORY_END':
        if ((confirm || escape) && m.endTick > 60) m.backToMenu();
        break;
    }
  }

  // Toque/clique na tela: botões desenhados pela view ou "toque pra continuar".
  onTap(e) {
    e.preventDefault();
    if (this.isTouch) goFullscreen();
    const m = this.m, hit = this.v.hitAt(this.v.toWorld(e.clientX, e.clientY));
    if (hit) { hit.action(); return; }
    if (m.state === 'OPTIONS') m.closeOptions();
    else if (m.state === 'INTRO') m.advanceIntro();
    else if (m.state === 'STORY_END' && m.endTick > 60) m.backToMenu();
    else if (m.state === 'PAUSED' && !this.isTouch) m.resume();
  }
}

// Botão de segurar (pular/deslizar): funciona com dois dedos ao mesmo tempo.
function hold(btn, set) {
  btn.addEventListener('pointerdown', e => {
    e.preventDefault();
    btn.setPointerCapture?.(e.pointerId);
    btn.classList.add('down');
    set(true);
  });
  for (const type of ['pointerup', 'pointercancel', 'lostpointercapture']) {
    btn.addEventListener(type, () => { btn.classList.remove('down'); set(false); });
  }
}

// No celular: tela cheia deitada (Android). No iPhone o navegador não deixa; basta girar o celular.
function goFullscreen() {
  const el = document.documentElement;
  if (document.fullscreenElement || !el.requestFullscreen) return;
  el.requestFullscreen().then(() => screen.orientation?.lock?.('landscape')).catch(() => { });
}
