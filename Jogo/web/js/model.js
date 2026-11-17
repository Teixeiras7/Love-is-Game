// MODEL: regras do jogo. Mesma lógica do GameModel.java (versão Java).
export const W = 384, H = 216, GROUND_Y = 184, PLAYER_X = 48;
export const STORY_LENGTH = 20000, START_LIVES = 3, END_TICKS = 600;
export const MENU_ITEMS = ['MODO HISTÓRIA', 'MODO LIVRE', 'OPÇÕES'];
export const MENU_STORY = 0, MENU_FREE = 1, MENU_OPTIONS = 2;

const INVINCIBLE_TICKS = 90, HEART_POINTS = 50, BIRDS_FROM = 1500, TICKS_PER_CHAR = 2;
const BIRD_LOW = 44, BIRD_HIGH = 68; // baixo: só passa deslizando; alto: só acerta se pular
const START_SPEED = 3.2, MAX_SPEED = 7.5, STORY_MAX_SPEED = 5.6;
const RECORD_KEY = 'loveIsGame.recorde';

// Tamanhos = hitbox (um pouco menor que o desenho, pra colisão ser justa).
export const TYPES = {
  PINE_SMALL: { w: 11, h: 28 }, PINE_TALL: { w: 17, h: 44 }, PINE_DOUBLE: { w: 26, h: 39 },
  BIRD: { w: 18, h: 10 }, HEART: { w: 11, h: 9 }, LIFE: { w: 12, h: 10 },
};
const PINES = ['PINE_SMALL', 'PINE_TALL', 'PINE_DOUBLE'];

class Box {
  constructor(x, y, w, h) { this.x = x; this.y = y; this.w = w; this.h = h; }
  centerX() { return this.x + this.w / 2; }
  bottom() { return this.y + this.h; }
  collides(o) { // hitbox 2 unidades menor de cada lado
    return this.x + 2 < o.x + o.w - 2 && o.x + 2 < this.x + this.w - 2
      && this.y + 2 < o.y + o.h - 2 && o.y + 2 < this.y + this.h - 2;
  }
}

export class Player extends Box {
  static RUN_W = 22; static RUN_H = 54; static SLIDE_W = 56; static SLIDE_H = 34;

  constructor() {
    super(PLAYER_X, GROUND_Y - Player.RUN_H, Player.RUN_W, Player.RUN_H);
    this.vy = 0; this.onGround = true; this.sliding = false;
  }

  // jump/down = botões segurados neste frame
  update(jump, down) {
    if (this.onGround && jump) { this.vy = -9.5; this.onGround = false; }
    this.sliding = this.onGround && down;
    this.w = this.sliding ? Player.SLIDE_W : Player.RUN_W;
    this.h = this.sliding ? Player.SLIDE_H : Player.RUN_H;
    if (this.onGround) { this.y = GROUND_Y - this.h; return; }
    if (!jump && this.vy < -4) this.vy = -4;  // soltou cedo = pulo curto
    this.vy += down ? 0.55 * 3 : 0.55;        // baixo no ar = desce rápido
    this.y += this.vy;
    if (this.y + this.h >= GROUND_Y) { this.y = GROUND_Y - this.h; this.vy = 0; this.onGround = true; }
  }
}

class Obstacle extends Box {
  constructor(type, x, bottomY) {
    const t = TYPES[type];
    super(x, bottomY - t.h, t.w, t.h);
    this.type = type;
  }
}

const ramp = (v, a, b) => Math.max(0, Math.min(1, (v - a) / (b - a)));

export class GameModel {
  constructor(falas) {
    this.falas = falas;
    this.state = 'MENU';
    this.mode = 'FREE';
    this.menuIndex = 0;
    this.tick = 0;
    this.distance = 0;
    this.highScore = loadRecord();
    this.player = new Player();
    this.obstacles = [];
    this.lives = START_LIVES; this.hearts = 0;
    this.invincible = 0; this.shake = 0; this.endTick = 0; this.lifeGained = 0;
    this.introLine = 0; this.introTick = 0;
  }

  moveMenuSelection(d) { this.menuIndex = (this.menuIndex + d + MENU_ITEMS.length) % MENU_ITEMS.length; }

  chooseMenu(i = this.menuIndex) {
    this.menuIndex = i;
    if (i === MENU_STORY) this.startIntro();
    else if (i === MENU_FREE) this.start('FREE');
    else this.state = 'OPTIONS';
  }

  start(mode) {
    this.mode = mode;
    this.distance = 0; this.speed = START_SPEED; this.lives = START_LIVES;
    this.hearts = this.invincible = this.shake = this.endTick = this.lifeGained = 0;
    this.nextSpawn = 350;
    this.player = new Player();
    this.obstacles = [];
    this.state = 'PLAYING';
  }

  // ---------- cutscene antes do modo história ----------
  startIntro() { this.mode = 'STORY'; this.distance = 0; this.introLine = this.introTick = 0; this.state = 'INTRO'; }
  fala() { return this.falas[this.introLine]; }
  visibleChars() { return Math.min(this.fala().texto.length, Math.floor(this.introTick / TICKS_PER_CHAR)); }
  isTyping() { return this.visibleChars() < this.fala().texto.length; }
  advanceIntro() { // toque/ENTER: termina a fala; se já terminou, vai pra próxima
    if (this.state !== 'INTRO') return;
    if (this.isTyping()) this.introTick = this.fala().texto.length * TICKS_PER_CHAR;
    else if (++this.introLine < this.falas.length) this.introTick = 0;
    else this.start('STORY');
  }
  skipIntro() { this.start('STORY'); }

  closeOptions() { this.state = 'MENU'; }
  restart() { this.start(this.mode); }
  pause() { if (this.state === 'PLAYING') this.state = 'PAUSED'; }
  resume() { if (this.state === 'PAUSED') this.state = 'PLAYING'; }
  backToMenu() { this.mode = 'FREE'; this.obstacles = []; this.player = new Player(); this.state = 'MENU'; }

  // Chamado 60x por segundo pelo controller.
  step(jump, down) {
    if (this.state === 'PAUSED') return;
    this.tick++;
    if (this.shake > 0) this.shake--;
    switch (this.state) {
      case 'MENU': case 'OPTIONS': this.distance += 3; this.player.update(false, false); break;
      case 'INTRO': this.introTick++; break;
      case 'PLAYING': this.update(jump, down); break;
      case 'STORY_END':
        this.endTick++;
        this.player.update(false, false);
        if (this.endTick >= END_TICKS) this.backToMenu(); // fim da animação do abraço
        break;
    }
  }

  update(jump, down) {
    this.speed = this.mode === 'STORY' ? this.storySpeed() : Math.min(MAX_SPEED, START_SPEED + this.distance * 0.00008);
    this.distance += this.speed;
    this.player.update(jump, down);
    if (this.invincible > 0) this.invincible--;
    if (this.lifeGained > 0) this.lifeGained--;
    this.spawnObstacles();

    for (const o of [...this.obstacles]) {
      o.x -= o.type === 'BIRD' ? this.speed + 0.8 : this.speed;
      if (o.x + o.w < -30) { this.remove(o); continue; }
      if (!o.collides(this.player)) continue;
      if (o.type === 'HEART') { this.hearts++; this.remove(o); }
      else if (o.type === 'LIFE') { this.lives = Math.min(START_LIVES, this.lives + 1); this.lifeGained = 60; this.remove(o); }
      else if (this.invincible === 0) this.hit();
    }

    if (this.state === 'PLAYING' && this.mode === 'STORY' && this.distance >= STORY_LENGTH) {
      this.distance = STORY_LENGTH;
      this.state = 'STORY_END';
    }
  }

  remove(o) { this.obstacles.splice(this.obstacles.indexOf(o), 1); }

  storySpeed() {
    const s = START_SPEED + (STORY_MAX_SPEED - START_SPEED) * Math.min(1, this.distance / (STORY_LENGTH * 0.9));
    const remaining = STORY_LENGTH - this.distance;
    return Math.min(s, Math.max(1.2, remaining / 50)); // desacelera chegando nele
  }

  hit() {
    this.lives--;
    this.invincible = INVINCIBLE_TICKS;
    this.shake = 12;
    if (this.lives > 0) return;
    this.state = 'GAME_OVER';
    if (this.mode === 'FREE' && this.score() > this.highScore) {
      this.highScore = this.score();
      saveRecord(this.highScore);
    }
  }

  spawnObstacles() {
    if (this.distance < this.nextSpawn) return;
    if (this.mode === 'STORY' && this.distance + W > STORY_LENGTH - 100) return; // reta final livre
    this.obstacles.push(this.randomObstacle());
    this.nextSpawn = this.distance + this.speed * (48 + rand(50));
  }

  randomObstacle() {
    const x = W + 4, roll = rand(100);
    const lifeOnScreen = this.obstacles.some(o => o.type === 'LIFE');
    if (this.lives < START_LIVES && !lifeOnScreen && roll < 18) return new Obstacle('LIFE', x, GROUND_Y - 62 - rand(20));
    if (roll < 12) return new Obstacle('HEART', x, GROUND_Y - 62 - rand(20));
    if (this.distance > BIRDS_FROM && roll < 45) {
      return new Obstacle('BIRD', x, GROUND_Y - (rand(3) > 0 ? BIRD_LOW : BIRD_HIGH));
    }
    return new Obstacle(PINES[rand(PINES.length)], x, GROUND_Y);
  }

  // 0 = dia, 1 = pôr do sol, 2 = noite (valores entre = transição)
  skyPhase() {
    if (this.mode === 'STORY') {
      const p = this.distance / STORY_LENGTH;
      return ramp(p, 0.30, 0.40) + ramp(p, 0.62, 0.72);
    }
    const s = (this.distance / 10) % 1500;
    return ramp(s, 400, 500) + ramp(s, 900, 1000) - 2 * ramp(s, 1400, 1500);
  }

  boyfriendX() { return STORY_LENGTH + PLAYER_X + Player.RUN_W + 2 - this.distance; }
  remaining() { return STORY_LENGTH - this.distance; }
  storyProgress() { return Math.min(1, this.distance / STORY_LENGTH); }
  score() { return Math.floor(this.distance / 10) + this.hearts * HEART_POINTS; }
  isBlinking() { return this.invincible > 0 && Math.floor(this.invincible / 5) % 2 === 0; }
}

function rand(n) { return Math.floor(Math.random() * n); }

// Recorde fica salvo no navegador (pode não existir em aba anônima).
function loadRecord() {
  try { return parseInt(localStorage.getItem(RECORD_KEY), 10) || 0; } catch { return 0; }
}
function saveRecord(v) {
  try { localStorage.setItem(RECORD_KEY, String(v)); } catch { /* sem armazenamento: só não salva */ }
}
