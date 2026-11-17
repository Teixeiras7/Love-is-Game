# Prompts de arte – Love is Game

## O que você precisa fazer (passo a passo)

1. Abra o **ChatGPT** (chatgpt.com) ou o **Gemini** (gemini.google.com) numa **conversa nova**.
   ⚠️ O **Claude não gera imagens** — não funciona lá.
   ⚠️ **Não cole este arquivo inteiro.** Cole **um prompt por vez**.
2. Comece pela **folha dela (item 1)**: clique no clipe 📎, anexe a foto indicada
   (ex.: `Foto Namorada.jpg`) e cole **só** o texto do prompt daquele item
   (a parte que começa com `>`, sem o `>`).
3. Veja se ficou bom (checklist abaixo). Se não ficou, peça ajuste **na mesma conversa**,
   ex.: *"deixe o fundo magenta liso"*, *"remova os textos"*, *"cabelo mais cacheado"*.
4. Quando gostar, **baixe a imagem** e renomeie com o nome indicado (ex.: `ela.png`).
5. Coloque o arquivo na pasta **`Jogo\arte\`** (já está criada).
6. Faça os itens 2 a 5 **na mesma conversa** do item 1 — assim o estilo e o tamanho
   dos personagens ficam iguais. Para o item 2, anexe as fotos dele junto.
7. Com tudo na pasta, me avise ("coloquei as imagens"). Eu recorto os quadros,
   coloco no jogo e ajusto as colisões.

Não precisa ter todas: pode mandar só `ela.png` primeiro pra testarmos.

### Checklist de cada imagem

- [ ] Fundo magenta liso (sem degradê, sem chão desenhado embaixo dos personagens)
- [ ] Nenhum texto/rótulo/número
- [ ] Personagem parecido com a foto e do mesmo tamanho em todos os quadros
- [ ] Ela virada para a **direita**; ele e o passarinho virados para a **esquerda**
- [ ] Quadros separados, sem um encostar no outro

### Arquivos esperados em `Jogo\arte\`

| Arquivo | Item | Obrigatório? |
|---|---|---|
| `ela.png` | 1 | sim |
| `ele.png` | 2 | sim |
| `abraco.png` | 2b | sim (final da história) |
| `obstaculos.png` | 3 | sim |
| `ceu-dia.png`, `ceu-por-do-sol.png`, `ceu-noite.png` | 4 | não (o jogo tem céu próprio) |
| `chao.png` | 5 | não (o jogo tem chão próprio) |

---

## Prompts

Regras para TODOS os pedidos (ajudam muito no recorte depois):

- Fundo **liso magenta #FF00FF** (sem degradê, sem chão, sem sombra no fundo).
- **Sem textos, rótulos ou números** na imagem.
- Quadros **alinhados em grade**, com espaço entre eles, todos do mesmo tamanho.
- Salve cada imagem com o nome indicado e coloque em `Jogo/arte/`.

---

## 1. Ela – `ela.png`  (anexe: Foto Namorada.jpg)

> Folha de sprites em pixel art HD estilo 16-bit moderno (como jogos indie de plataforma), sombreado suave, cores vivas, sem contorno preto grosso. Personagem: a moça da foto anexada — cabelo castanho longo e bem cacheado, pele clara, sorriso grande, camiseta azul-petróleo com estampa amarela, colar de cordão branco com cruz de madeira, calça jeans clara, tênis azul-marinho. Proporção natural (não chibi), vista de perfil virada para a DIREITA. Fundo liso magenta #FF00FF, sem textos.
> Linha 1: correndo, 8 quadros de animação em loop.
> Linha 2: pulando — 1 quadro subindo, 1 no topo, 1 descendo.
> Linha 3: deslizando no chão dando carrinho (deitada pra trás, pernas esticadas pra frente, cabelo voando), 2 quadros.
> Linha 4: parada sorrindo, 2 quadros; e feliz com as mãos no peito, 2 quadros.

## 2. Ele – `ele.png`  (anexe: Minha foto.jpeg e Nossa foto (1).jpeg)

> Mesmo estilo e mesma escala da folha anterior. Personagem: o rapaz das fotos — cabelo castanho-claro curto com franja, óculos redondos de armação preta, sorriso com aparelho, camiseta azul-clara, colar com cruz prateada, calça preta, tênis preto de 3 listras brancas. Vista de perfil virada para a ESQUERDA. Fundo liso magenta #FF00FF, sem textos.
> Linha 1: parado sorrindo, 2 quadros.
> Linha 2: acenando, 4 quadros.
> Linha 3: abrindo os braços para um abraço, 3 quadros.

## 2b. Abraço (final da história) – `abraco.png`  (anexe: Nossa foto (2).jpeg)

> Agora faça uma folha de sprites dos DOIS personagens juntos (a moça e o rapaz das folhas anteriores), EXATAMENTE no mesmo estilo de arte e na mesma escala. Use a foto anexada como referência da pose e do carinho entre eles. Fundo liso magenta #FF00FF, sem textos, sem rótulos, quadros alinhados em grade com espaço entre eles, os dois do mesmo tamanho em todos os quadros.
> Linha 1: o encontro — ela chegando correndo e pulando nos braços dele, 4 quadros.
> Linha 2: os dois abraçados de lado, felizes, balançando de leve, 3 quadros.
> Linha 3: a pose da foto — ela abraçando ele e apertando a bochecha dele, ele sorrindo, 2 quadros.

## 3. Obstáculos – `obstaculos.png`

> Pixel art HD estilo 16-bit moderno, mesmo estilo das folhas anteriores, fundo liso magenta #FF00FF, sem textos, objetos separados com espaço:
> Linha 1: três pinheiros – um pequeno, um alto, e dois pinheiros juntos.
> Linha 2: passarinho colorido (azul com barriga amarela) voando de perfil virado para a ESQUERDA, 4 quadros de batida de asa.
> Linha 3: um coração vermelho brilhante (item coletável), 4 quadros girando/brilhando.

## 4. Cenários – `ceu-dia.png`, `ceu-por-do-sol.png`, `ceu-noite.png`

Peça os três com a mesma paisagem, mudando só o horário. Formato bem largo (3:1).

> Cenário panorâmico em pixel art HD estilo 16-bit moderno, para jogo de corrida lateral, formato 3:1, que emende sem costura na horizontal (borda esquerda continua na direita). Paisagem de campo: céu, nuvens, montanhas ao fundo, coqueiros e pinheiros, colinas verdes. Sem chão no primeiro plano, sem personagens, sem textos.
> - Versão DIA: céu azul com nuvens brancas, sol.
> - Versão PÔR DO SOL: céu alaranjado e rosado, sol baixo.
> - Versão NOITE: céu escuro com estrelas e uma lua fina em formato de C.

## 5. Chão – `chao.png`

> Faixa de chão em pixel art HD estilo 16-bit moderno, vista lateral: grama verde com florzinhas coloridas em cima e terra marrom com pedrinhas embaixo, formato bem largo (6:1), que emende sem costura na horizontal. Fundo acima da grama liso magenta #FF00FF, sem textos.
