// VIEW: desenha o jogo no <canvas>. Mesmo visual do GameView.java.
import { W, H, GROUND_Y as GROUND, START_LIVES, END_TICKS, MENU_ITEMS, Player } from './model.js';

const PINK = '#ff6eaa', YELLOW = '#ffe15a', WHITE = '#fff', LABEL = '#cdc3e1';
const SHADOW = 'rgba(30,15,40,.86)', PANEL = 'rgba(25,15,45,.84)';
const FONT = size => `bold ${size}px system-ui, "Segoe UI", Roboto, Arial, sans-serif`;
// Tinta do chão no pôr do sol e à noite (o céu já vem pronto na paisagem).
const GROUND_TINT = [null, 'rgba(255,110,60,.27)', 'rgba(14,18,64,.59)'];
const END_ZOOM_START = 150, END_ZOOM_TICKS = 120;
const clamp = v => Math.max(0, Math.min(1, v));

export class GameView {
  constructor(canvas, model, art, isTouch) {
    this.canvas = canvas;
    this.ctx = canvas.getContext('2d');
    this.model = model;
    this.art = art;
    this.a = art.anims;
    this.isTouch = isTouch;
    this.hits = []; // áreas tocáveis {x, y, w, h, action} em unidades do mundo
    this.grounds = GROUND_TINT.map(c => tint(art.ground.img, c));
    this.resize();
    addEventListener('resize', () => this.resize());
  }

  // Cabe o jogo (16:9) na tela, com faixas pretas se sobrar, nítido em tela retina.
  resize() {
    const dpr = devicePixelRatio || 1, vw = innerWidth, vh = innerHeight;
    this.cssScale = Math.min(vw / W, vh / H);
    this.offX = (vw - W * this.cssScale) / 2;
    this.offY = (vh - H * this.cssScale) / 2;
    Object.assign(this.canvas.style, {
      left: `${this.offX}px`, top: `${this.offY}px`,
      width: `${W * this.cssScale}px`, height: `${H * this.cssScale}px`,
    });
    this.canvas.width = Math.round(W * this.cssScale * dpr);
    this.canvas.height = Math.round(H * this.cssScale * dpr);
    this.scale = this.canvas.width / W;
  }

  toWorld(clientX, clientY) {
    return { x: (clientX - this.offX) / this.cssScale, y: (clientY - this.offY) / this.cssScale };
  }

  hitAt(p) {
    return this.hits.find(h => p.x >= h.x && p.x <= h.x + h.w && p.y >= h.y && p.y <= h.y + h.h);
  }

  render() {
    const c = this.ctx, m = this.model;
    this.tick = m.tick;
    this.hits = [];
    c.setTransform(this.scale, 0, 0, this.scale, 0, 0);
    c.imageSmoothingQuality = 'high';
    c.fillStyle = '#000';
    c.fillRect(-4, -4, W + 8, H + 8);

    c.save();
    if (m.shake > 0) c.translate(m.tick % 3 - 1, Math.floor(m.tick / 2) % 3 - 1);
    if (m.state === 'STORY_END') this.applyEndZoom(c);
    const phase = m.skyPhase();
    this.drawSky(c, phase);
    this.drawGround(c, phase);
    if (m.state === 'INTRO') {
      c.restore();
      this.drawIntro(c);
      return;
    }
    this.drawObstacles(c);
    if (m.state === 'STORY_END') this.drawHug(c);
    else { this.drawBoyfriend(c); this.drawPlayer(c); }
    c.restore(); // textos e HUD ficam fora do zoom

    switch (m.state) {
      case 'MENU': this.drawMenu(c); break;
      case 'OPTIONS': this.drawOptions(c); break;
      case 'PLAYING': this.drawHud(c); break;
      case 'PAUSED': this.drawHud(c); this.drawPause(c); break;
      case 'GAME_OVER': this.drawHud(c); this.drawGameOver(c); break;
      case 'STORY_END': this.drawStoryEnd(c); break;
    }
  }

  // ---------------- cenário ----------------

  drawSky(c, phase) { // dia / pôr do sol / noite misturando as faixas vizinhas
    const i = Math.min(1, Math.floor(phase)), f = phase - i;
    c.imageSmoothingEnabled = false;
    this.drawStrip(c, this.art.sky[i], -2, this.art.skyScale, 0.25, 1);
    if (f > 0) this.drawStrip(c, this.art.sky[i + 1], -2, this.art.skyScale, 0.25, f);
    c.imageSmoothingEnabled = true;
  }

  drawGround(c, phase) {
    const i = Math.min(1, Math.floor(phase)), f = phase - i, g = this.art.ground;
    const top = GROUND - g.surface * g.scale;
    this.drawStrip(c, this.grounds[i], top, g.scale, 1, 1);
    if (f > 0) this.drawStrip(c, this.grounds[i + 1], top, g.scale, 1, f);
  }

  // Faixa repetida em espelho (emenda sem costura), rolando com a corrida. k = unidades por pixel.
  drawStrip(c, img, top, k, parallax, alpha) {
    const w = img.width * k, x0 = -((this.model.distance * parallax) % (2 * w));
    c.save();
    c.globalAlpha = alpha;
    for (let t = -1; x0 + t * w < W; t++) { // começa em -1: cobre a esquerda no zoom do final
      const x = x0 + t * w, flip = ((t % 2) + 2) % 2 === 1;
      c.save();
      c.translate(flip ? x + w + 0.3 : x - 0.3, top); // 0.3 = sobreposição pra não aparecer fresta
      c.scale((flip ? -1 : 1) * (k + 0.6 / img.width), k);
      c.drawImage(img, 0, 0);
      c.restore();
    }
    c.restore();
  }

  // ---------------- personagens e obstáculos ----------------

  drawObstacles(c) {
    const a = this.a;
    for (const o of this.model.obstacles) {
      switch (o.type) {
        case 'PINE_SMALL': this.draw(c, a.pineSmall[0], o.centerX(), GROUND + 1); break;
        case 'PINE_TALL': this.draw(c, a.pineTall[0], o.centerX(), GROUND + 1); break;
        case 'PINE_DOUBLE': this.draw(c, a.pineDouble[0], o.centerX(), GROUND + 1); break;
        case 'BIRD': {
          const s = a.bird[Math.floor(this.tick / 6) % a.bird.length];
          this.draw(c, s, o.centerX(), o.y + o.h / 2 + s.h / 2);
          break;
        }
        case 'HEART':
          this.draw(c, a.heart[0], o.centerX(), o.bottom() + 2 + Math.sin(this.tick * 0.12 + o.x * 0.05) * 2);
          break;
        case 'LIFE':
          this.drawLifeHeart(c, o.centerX(), o.y + o.h / 2 + Math.sin(this.tick * 0.15) * 3);
          break;
      }
    }
  }

  // Coração que devolve vida: brilho dourado pulsando + estrelinhas girando.
  drawLifeHeart(c, cx, cy) {
    const r = 13 + Math.sin(this.tick * 0.2) * 2;
    const glow = c.createRadialGradient(cx, cy, 0, cx, cy, r);
    glow.addColorStop(0, 'rgba(255,236,140,.78)');
    glow.addColorStop(1, 'rgba(255,200,80,0)');
    c.fillStyle = glow;
    c.beginPath(); c.arc(cx, cy, r, 0, Math.PI * 2); c.fill();
    const s = this.a.lifeHeart[0];
    this.draw(c, s, cx, cy + s.h / 2);
    c.fillStyle = '#fffad2';
    for (let i = 0; i < 4; i++) {
      const ang = this.tick * 0.06 + i * Math.PI / 2, sx = cx + Math.cos(ang) * 13, sy = cy + Math.sin(ang) * 11;
      const z = 1.2 + 0.8 * Math.abs(Math.sin(this.tick * 0.2 + i));
      c.fillRect(sx - z, sy - 0.4, z * 2, 0.8);
      c.fillRect(sx - 0.4, sy - z, 0.8, z * 2);
    }
  }

  drawPlayer(c) {
    const p = this.model.player, a = this.a;
    if (this.model.isBlinking()) return;
    let s;
    if (p.sliding) s = a.herSlide[Math.floor(this.tick / 8) % a.herSlide.length];
    else if (!p.onGround) s = a.herJump[p.vy < -2.5 ? 0 : p.vy > 2.5 ? 2 : 1];
    else s = a.herRun[Math.floor(this.tick / 4) % a.herRun.length];

    if (p.onGround) { // poeirinha nos pés
      const n = p.sliding ? 6 : 2, fx = p.sliding ? p.x + p.w - 4 : p.x + 2;
      for (let i = 0; i < n; i++) {
        const age = (this.tick + i * 5) % 16, r = 1.5 + age * 0.15;
        c.fillStyle = `rgba(240,228,200,${Math.max(0, 200 - age * 12) / 255})`;
        c.beginPath();
        c.ellipse(fx - age * 1.2 + r, GROUND - 2 - age / 4 + r, r, r, 0, 0, Math.PI * 2);
        c.fill();
      }
    }
    this.draw(c, s, p.centerX(), p.bottom() + 1);
  }

  // Ele esperando no fim do modo história: acenando, e abre os braços quando ela chega perto.
  drawBoyfriend(c) {
    const m = this.model, a = this.a;
    if (m.mode !== 'STORY' || m.state === 'MENU') return;
    const cx = m.boyfriendX() + Player.RUN_W / 2;
    if (cx > W + 40) return;
    const rem = m.remaining();
    const s = rem < 180
      ? a.himOpen[Math.min(a.himOpen.length - 1, Math.floor((180 - rem) / 40))]
      : a.himWave[Math.floor(this.tick / 8) % a.himWave.length];
    this.draw(c, s, cx, GROUND + 1);
  }

  coupleX() {
    const m = this.model;
    return (m.player.centerX() + m.boyfriendX() + Player.RUN_W / 2) / 2;
  }

  drawHug(c) {
    const e = this.model.endTick, a = this.a;
    const s = e < 16 ? a.hugJump[Math.floor(e / 8)]
      : e < 160 ? a.hugSway[Math.floor((e - 16) / 10) % a.hugSway.length]
      : a.hugCheek[Math.floor((e - 160) / 24) % a.hugCheek.length];
    const cx = this.coupleX();
    this.draw(c, s, cx, GROUND + 1);
    for (let k = 0; k < 6; k++) { // coraçõezinhos subindo
      let t = e - 20 - k * 18;
      if (t < 0) continue;
      t %= 108;
      this.drawFaded(c, a.heart[0], cx + Math.sin((t + k * 30) * 0.08) * 10, GROUND - 66 - t / 2, 1 - t / 108);
    }
  }

  // Câmera aproxima do casal e leva ele pro meio da tela.
  applyEndZoom(c) {
    let p = clamp((this.model.endTick - END_ZOOM_START) / END_ZOOM_TICKS);
    p = p * p * (3 - 2 * p); // suave no começo e no fim
    const cx = this.coupleX(), cy = GROUND - 31, z = 1 + 1.4 * p;
    c.translate(cx + (W / 2 - cx) * p, cy + (H * 0.62 - cy) * p);
    c.scale(z, z);
    c.translate(-cx, -cy);
  }

  // ---------------- cutscene ----------------

  drawIntro(c) {
    const m = this.model, a = this.a, fala = m.fala(), typing = m.isTyping();
    let body;
    if (fala.rosto === 'APAIXONADO') body = a.frontHeart[Math.floor(this.tick / 20) % a.frontHeart.length];
    else if (fala.rosto === 'TIMIDO') body = a.frontShy[Math.floor(this.tick / 20) % a.frontShy.length];
    else body = typing ? a.frontTalk[Math.floor(this.tick / 8) % a.frontTalk.length] : a.frontIdle[this.tick % 160 < 8 ? 1 : 0];
    this.draw(c, body, 250, GROUND + 1);

    const bx = 12, by = 8, bw = W - 24, bh = 98;
    this.panel(c, bx, by, bw, bh);
    let face = { SORRINDO: 0, TIMIDO: 0, PISCANDO: 2, RINDO: 3, APAIXONADO: 5 }[fala.rosto];
    if (typing && face === 0 && Math.floor(this.tick / 5) % 2 === 0) face = 1; // boca mexendo enquanto fala
    const portrait = a.portraits[face];
    this.draw(c, portrait, bx + 6 + portrait.w / 2, by + bh - 4);

    const tx = bx + Math.floor(portrait.w) + 14;
    text(c, 'Seu amor', tx, by + 18, 10, PINK);
    c.font = FONT(11);
    let left = m.visibleChars(), y = by + 34;
    for (const line of wrap(c, fala.texto, bx + bw - tx - 10)) {
      if (left <= 0) break;
      text(c, line.slice(0, left), tx, y, 11, WHITE);
      left -= line.length + 1;
      y += 14;
    }
    if (!typing && Math.floor(this.tick / 20) % 2 === 0) {
      text(c, this.isTouch ? 'TOQUE  >' : 'ENTER  >', bx + bw - 52, by + bh - 7, 10, YELLOW);
    }
    text(c, this.isTouch ? 'PULAR  >>' : 'ESC pular', 8, H - 6, 10, WHITE);
    this.hits.push({ x: 0, y: H - 22, w: 70, h: 22, action: () => m.skipIntro() });
  }

  // ---------------- telas ----------------

  drawHud(c) {
    const m = this.model, a = this.a, rightPad = this.isTouch ? 30 : 0; // espaço pro botão de pausa
    for (let i = 0; i < START_LIVES; i++) this.drawFaded(c, a.heart[0], 12 + i * 16, 19, i < m.lives ? 1 : 0.25);
    if (m.lifeGained > 0) text(c, '+1 VIDA', 12 + START_LIVES * 16, 16 - Math.floor((60 - m.lifeGained) / 6), 10, PINK);
    if (m.mode === 'FREE') {
      const s = `RECORDE ${pad(m.highScore)}   ${pad(m.score())}`;
      c.font = FONT(10);
      text(c, s, W - 6 - rightPad - c.measureText(s).width, 14, 10, WHITE);
      return;
    }
    const bx = 130, bw = 124, p = m.storyProgress();
    c.fillStyle = SHADOW;
    roundRect(c, bx, 8, bw, 5, 2); c.fill();
    c.fillStyle = PINK;
    roundRect(c, bx, 8, bw * p, 5, 2); c.fill();
    this.draw(c, a.himIcon[0], bx + bw + 4, 18);
    this.draw(c, a.herIcon[0], bx + bw * p, 18);
    this.draw(c, a.heart[0], W - 36 - rightPad, 19);
    text(c, 'x' + m.hearts, W - 26 - rightPad, 15, 10, WHITE);
  }

  drawMenu(c) {
    const m = this.model;
    textCentered(c, 'LOVE IS GAME', 46, 28, PINK);
    textCentered(c, '~ corra até o seu amor ~', 62, 10, WHITE);
    this.panel(c, W / 2 - 80, 74, 160, 64);
    MENU_ITEMS.forEach((item, i) => {
      const sel = i === m.menuIndex, y = 92 + i * 18;
      textCentered(c, item, y, 14, sel ? YELLOW : LABEL);
      if (sel && Math.floor(this.tick / 15) % 2 === 0) this.draw(c, this.a.heart[0], W / 2 - 66, y + 1);
      this.hits.push({ x: W / 2 - 80, y: y - 14, w: 160, h: 18, action: () => m.chooseMenu(i) });
    });
    textCentered(c, 'RECORDE  ' + pad(m.highScore), 156, 10, YELLOW);
  }

  drawOptions(c) {
    this.panel(c, 22, 12, W - 44, 192);
    textCentered(c, 'OPÇÕES', 32, 14, PINK);
    const x = 36, touch = this.isTouch;
    text(c, 'CONTROLES', x, 50, 10, YELLOW);
    text(c, touch ? 'Pular:  botão PULAR (esquerda)' : 'Pular:  CIMA  /  W  /  ESPAÇO', x, 63, 10, WHITE);
    text(c, touch ? 'Deslizar (carrinho):  botão DESLIZAR (direita)' : 'Deslizar (carrinho):  BAIXO  /  S  /  SHIFT', x, 75, 10, WHITE);
    text(c, touch ? 'Pausar:  botão II  no canto' : 'Pausar:  ESC  ou  P      (na pausa, Q volta ao menu)', x, 87, 10, WHITE);
    text(c, 'Dica: segure pular = pulo alto; baixo no ar = cai rápido', x, 99, 10, LABEL);
    text(c, 'MODO HISTÓRIA', x, 117, 10, YELLOW);
    text(c, 'Uma corrida com fim, do dia até a noite, até chegar', x, 129, 10, WHITE);
    text(c, 'em quem está te esperando. Você tem 3 vidas, e o', x, 141, 10, WHITE);
    text(c, 'coração brilhante devolve 1 vida perdida.', x, 151, 10, WHITE);
    text(c, 'MODO LIVRE', x, 166, 10, YELLOW);
    text(c, 'Corrida infinita: quanto mais longe, mais pontos.', x, 177, 10, WHITE);
    text(c, 'Cada coração vale +50. Tente bater o recorde!', x, 187, 10, WHITE);
    if (Math.floor(this.tick / 20) % 2 === 0) textCentered(c, touch ? 'Toque para voltar' : 'ENTER ou ESC para voltar', 198, 10, PINK);
  }

  drawPause(c) {
    c.fillStyle = 'rgba(0,0,0,.47)';
    c.fillRect(0, 0, W, H);
    textCentered(c, 'PAUSADO', 100, 28, WHITE);
    if (this.isTouch) {
      this.button(c, 'CONTINUAR', W / 2 - 95, 114, 90, () => this.model.resume());
      this.button(c, 'MENU', W / 2 + 5, 114, 90, () => this.model.backToMenu());
    } else {
      textCentered(c, 'ESC continuar    Q menu', 120, 10, WHITE);
    }
  }

  drawGameOver(c) {
    const m = this.model;
    this.panel(c, W / 2 - 90, 60, 180, 90);
    textCentered(c, 'FIM DE JOGO', 86, 22, PINK);
    if (m.mode === 'FREE') {
      textCentered(c, 'PONTOS ' + pad(m.score()), 106, 14, WHITE);
      textCentered(c, 'RECORDE ' + pad(m.highScore), 120, 10, YELLOW);
    } else {
      textCentered(c, 'Quase lá! Tenta de novo!', 108, 14, WHITE);
    }
    if (this.isTouch) {
      this.button(c, 'RECOMEÇAR', W / 2 - 84, 126, 80, () => m.restart());
      this.button(c, 'MENU', W / 2 + 4, 126, 80, () => m.backToMenu());
    } else if (Math.floor(this.tick / 20) % 2 === 0) {
      textCentered(c, 'ENTER recomeçar    ESC menu', 140, 10, WHITE);
    }
  }

  // Textos do final (fora do zoom) e o escurecer antes de voltar pro menu.
  drawStoryEnd(c) {
    const end = this.model.endTick;
    if (end > END_ZOOM_START + END_ZOOM_TICKS / 2) {
      textCentered(c, 'VOCÊ CHEGOU ATÉ MIM!', 28, 22, PINK);
      textCentered(c, 'Corações coletados: ' + this.model.hearts, 44, 14, WHITE);
    }
    if (end > 100 && Math.floor(this.tick / 30) % 2 === 0) {
      textCentered(c, this.isTouch ? 'Toque para voltar ao menu' : 'ENTER para voltar ao menu', 208, 10, WHITE);
    }
    const fade = clamp((end - (END_TICKS - 60)) / 60);
    if (fade > 0) {
      c.fillStyle = `rgba(0,0,0,${fade})`;
      c.fillRect(-4, -4, W + 8, H + 8);
    }
  }

  // ---------------- utilitários ----------------

  draw(c, s, cx, bottom) { c.drawImage(s.img, cx - s.ax, bottom - s.h, s.w, s.h); }

  drawFaded(c, s, cx, bottom, alpha) {
    c.save();
    c.globalAlpha = clamp(alpha);
    this.draw(c, s, cx, bottom);
    c.restore();
  }

  panel(c, x, y, w, h) {
    c.fillStyle = PANEL;
    roundRect(c, x, y, w, h, 5); c.fill();
    c.strokeStyle = PINK; c.lineWidth = 1;
    roundRect(c, x, y, w, h, 5); c.stroke();
  }

  button(c, label, x, y, w, action) {
    this.panel(c, x, y, w, 20);
    textCentered(c, label, y + 14, 11, YELLOW, x + w / 2);
    this.hits.push({ x, y, w, h: 20, action });
  }
}

function text(c, s, x, y, size, color) {
  c.font = FONT(size);
  c.fillStyle = SHADOW;
  c.fillText(s, x + 1, y + 1);
  c.fillStyle = color;
  c.fillText(s, x, y);
}

function textCentered(c, s, y, size, color, cx = W / 2) {
  c.font = FONT(size);
  text(c, s, cx - c.measureText(s).width / 2, y, size, color);
}

function wrap(c, s, width) {
  const lines = [];
  let line = '';
  for (const word of s.split(' ')) {
    const test = line ? line + ' ' + word : word;
    if (c.measureText(test).width > width && line) { lines.push(line); line = word; } else line = test;
  }
  lines.push(line);
  return lines;
}

function roundRect(c, x, y, w, h, r) {
  c.beginPath();
  c.roundRect ? c.roundRect(x, y, Math.max(0, w), h, r) : c.rect(x, y, Math.max(0, w), h);
}

function pad(n) { return String(n).padStart(5, '0'); }

// Cópia do chão com uma cor pintada só onde já tem pixel.
function tint(img, color) {
  const cv = document.createElement('canvas');
  cv.width = img.width;
  cv.height = img.height;
  const g = cv.getContext('2d');
  g.drawImage(img, 0, 0);
  if (color) {
    g.globalCompositeOperation = 'source-atop';
    g.fillStyle = color;
    g.fillRect(0, 0, cv.width, cv.height);
  }
  return cv;
}
