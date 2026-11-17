// Carrega a arte (gerada pelo exportar-web.bat) e liga Model + View + Controller.
import { GameModel } from './model.js';
import { GameView } from './view.js';
import { GameController } from './controller.js';

const loading = document.getElementById('carregando');
const bar = document.querySelector('#barra div');

try {
  const manifest = await (await fetch('arte/manifest.json')).json();
  const files = [...Object.values(manifest.anims).flat().map(f => f.f), ...manifest.sky, manifest.ground.f];
  const images = {};
  let done = 0;
  await Promise.all(files.map(f => new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => {
      images[f] = img;
      bar.style.width = `${(++done / files.length) * 100}%`;
      resolve();
    };
    img.onerror = () => reject(new Error('Não consegui carregar arte/' + f));
    img.src = 'arte/' + f;
  })));

  const art = {
    anims: Object.fromEntries(Object.entries(manifest.anims).map(([name, frames]) =>
      [name, frames.map(f => ({ img: images[f.f], w: f.w, h: f.h, ax: f.ax }))])),
    sky: manifest.sky.map(f => images[f]),
    skyScale: manifest.skyScale,
    ground: { img: images[manifest.ground.f], scale: manifest.ground.scale, surface: manifest.ground.surface },
  };

  // ?touch na URL força os botões de celular (útil pra testar no PC)
  const isTouch = matchMedia('(pointer: coarse)').matches || new URLSearchParams(location.search).has('touch');
  const model = new GameModel(manifest.falas);
  const view = new GameView(document.getElementById('tela'), model, art, isTouch);
  new GameController(model, view, isTouch).start();
  loading.remove();
} catch (err) {
  loading.textContent = 'Ops, algo deu errado ao carregar o jogo: ' + err.message;
  console.error(err);
}
