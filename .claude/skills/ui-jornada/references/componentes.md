# Catálogo de componentes e tema

Antes de criar, procure aqui. Cada componente é uma função pura (dados prontos entram, visão sai) com um bloco de mesmo nome em `estilo/componentes.css`. Em JavaScript puro a visão é uma string HTML; em React/Vue/Angular é o componente equivalente, com as mesmas props.

## Componentes

| Componente | Entrada (props) | Uso |
|---|---|---|
| `etiqueta` | `{ rotulo, cor }` (cor: `neutro`, `andamento`, `espera`, `sucesso`, `erro`) | situação de um registro; a tela pega rótulo e cor da jornada |
| `aviso` | `{ tipo: "sucesso" \| "erro" \| "atencao", texto }` | resultado de uma ação, erro da API, divergência da jornada |
| `caminho` | `{ passos: [{ nome, estado: "feito" \| "atual" \| "futuro" \| "talvez" }], foraDoCaminho? }` | etapas do tipo (stepper); vira lista vertical no celular |
| `linhaDoTempo` | `{ itens: [{ titulo, autor, quando, de, para, motivo? }], vazio }` | histórico; `de`/`para` no formato de `etiqueta` |
| `tabela` + `aoClicarLinha` | `{ colunas, linhas: [{ chave, celulas }] }` | listas; células chegam como visão já montada (texto passado por `h()`) |
| `indicadores` | `{ itens: [{ rotulo, valor }] }` | faixa de dados principais no topo do detalhe |
| `pagina` | `{ titulo, trilha: [[texto, link?]], menu, sistema }` | moldura: título, trilha de navegação, item ativo do menu |
| `pedirMotivo` | `(titulo) -> Promise<texto \| null>` | diálogo modal de motivo antes de ações com `pedeMotivo` |

Classes de apoio no CSS (sem componente JS): `cartao`, `cartao-topo`, `linha`, `botao` (`principal`, `perigo`), `campo` + `rotulo` + `erro-campo` (o `data-campo` casa com o `campo` do erro da API), `opcoes`/`opcao` (escolha em cartões), `vazio`, `secundario`, `apagado`, `numero` (números tabulares).

## Tema (tokens)

Todo valor visual vem de `estilo/tokens.css`. **Nunca escreva cor, raio ou sombra fora dele.**

| Grupo | Tokens |
|---|---|
| Superfícies | `--fundo`, `--painel`, `--painel-alto`, `--borda`, `--borda-forte` |
| Texto | `--texto`, `--texto-secundario`, `--texto-apagado` |
| Destaque e estados | `--destaque`, `--destaque-forte`, `--sucesso`, `--atencao`, `--erro`, `--neutro` |
| Forma | `--raio`, `--raio-pequeno`, `--espaco`, `--sombra`, `--fonte`, `--fonte-mono` |

Cor de estado em fundo e borda translúcidos: `color-mix(in srgb, var(--erro) 14%, transparent)`. Tema claro: redefina os mesmos tokens num seletor `[data-tema="claro"]` (comentado em `tokens.css`).

## Componente novo

1. Confirme que não dá para compor com os existentes.
2. Função pura em `componentes/<nome>.js` (modelo: [`assets/componente.js`](../assets/componente.js)); documente as props no comentário.
3. Bloco `/* <nome> */` em `componentes.css`, só com tokens; regra de celular dentro de `@media (max-width: 760px)` no mesmo bloco.
4. Acrescente a linha na tabela acima.
5. Acessibilidade mínima: foco visível (já vem da base), texto alternativo e `aria-*` onde o elemento não fala por si, contraste dos tokens.
