# A mesma arquitetura em cada framework

As cinco camadas, as regras e o JSON da jornada não mudam. Muda só a forma de cada peça e a raiz do código (`js/` em JavaScript puro, `src/` em React e Vue, `src/app/` em Angular). O `scripts/verificar.sh` confere qualquer um, porque olha os caminhos das camadas (configuráveis por variável). A referência em React é a POC `frontend/poc`, com os nomes padrão das camadas (receitas na skill `react-jornada`).

| Peça | JavaScript puro (estrutura de referência) | React | Vue | Angular |
|---|---|---|---|---|
| `nucleo/http` | `chamar()` com `fetch` | o mesmo módulo (ou um hook `useApi` só para estado de carregamento, chamando `chamar`) | o mesmo módulo | `chamar()` em `nucleo/` (`fetch`, ou `HttpClient` num serviço do núcleo); nada fora dele injeta `HttpClient` |
| `nucleo/rotas` | roteador por `#hash` | `react-router` configurado em `nucleo/rotas` | `vue-router` em `nucleo/rotas` | `Routes` + `provideRouter(rotas, withHashLocation(), withComponentInputBinding())`: o `:id` e o `?aberto` chegam à tela como `input()` |
| `api/<recurso>` | módulo de funções | o mesmo módulo | o mesmo módulo | o mesmo módulo de funções (Promise), ou um `@Injectable` em `api/` |
| `jornadas/*.json` | lido por `lerArquivo` | `import jornada from "./jornadas/x.json"` | idem | `import` ou `HttpService` lendo o asset |
| contexto da jornada | `telas/contexto.js` | `JornadaContext` (Context) em `telas/contexto` | `provide/inject` em `telas/contexto` | as constantes da jornada importadas, ou um `JornadaService` |
| `componentes/` | função → string HTML | componente de função, só props, sem efeitos | SFC só com `props` e `emits` | componente standalone com `input()`/`model()`/`output()`, OnPush, sem serviço da api nem da jornada; se melhora um elemento nativo, seletor de atributo (`button[app-botao]`) |
| `telas/` | função que monta a página | componente de página: busca em `useEffect` pela `api` | página com `onMounted` chamando a `api` | componente de rota: estado em `signal`, derivados em `computed`, carga num `effect` que lê o `input()` do id |
| estilo | `tokens.css` + `componentes.css` | os mesmos arquivos (ou CSS Modules usando os tokens) | `<style scoped>` usando os tokens | `styles` usando os tokens |

Regras que mudam de forma, não de sentido:
- **Estado global:** só o contexto (jornada e persona). Nada de store para dados da API: a tela busca de novo, porque a API é a fonte da verdade.
- **Biblioteca de componentes de terceiros:** só se ela aceitar os tokens (tema por variáveis CSS). Senão, componente próprio pelo catálogo.
- **Tipos (TypeScript):** defina os tipos da resposta em `api/` (ex.: `Processo`, `Transicao`), e os da jornada em `jornadas/tipos.ts`. Componente recebe props tipadas, nunca o tipo cru da API.

## A mesma tela ("ações disponíveis") em cada um

JavaScript puro (o protótipo):

```js
const r = await processos.acoes(id);
raiz.innerHTML = ordenarAcoes(r.corpo ?? []).map((a) =>
  `<button data-acao="${h(a)}" class="botao ${acao(a).estilo === "perigo" ? "perigo" : ""}">${h(acao(a).rotulo)}</button>`).join("");
```

React:

```jsx
function AcoesDisponiveis({ id, aoExecutar }) {
  const { acao, ordenarAcoes } = useJornada();          // telas/contexto
  const [codigos, setCodigos] = useState([]);
  useEffect(() => { processos.acoes(id).then((r) => setCodigos(r.corpo ?? [])); }, [id]);
  return ordenarAcoes(codigos).map((a) => (
    <Botao key={a} perigo={acao(a).estilo === "perigo"} onClick={() => aoExecutar(a)}>{acao(a).rotulo}</Botao>
  ));
}
```

Vue:

```vue
<script setup>
const props = defineProps({ id: String });
const emit = defineEmits(["executar"]);
const { acao, ordenarAcoes } = inject("jornada");
const codigos = ref([]);
onMounted(async () => { codigos.value = (await processos.acoes(props.id)).corpo ?? []; });
</script>
<template>
  <Botao v-for="a in ordenarAcoes(codigos)" :key="a" :perigo="acao(a).estilo === 'perigo'" @click="emit('executar', a)">
    {{ acao(a).rotulo }}
  </Botao>
</template>
```

Angular (standalone, signals, sem zone.js):

```ts
@Component({ selector: "app-acoes", imports: [Botao], changeDetection: ChangeDetectionStrategy.OnPush, template: `
  @for (a of disponiveis(); track a) {
    <button app-botao [variante]="ACOES[a].perigo ? 'perigo' : 'normal'" (click)="executar.emit(a)">{{ ACOES[a].texto }}</button>
  }` })
export class AcoesDisponiveis {
  readonly id = input.required<string>();
  readonly executar = output<Acao>();
  protected readonly ACOES = ACOES;                      // jornada: rótulo e estilo
  private readonly codigos = signal<Acao[]>([]);
  protected readonly disponiveis = computed(() => (Object.keys(ACOES) as Acao[]).filter((a) => this.codigos().includes(a)));
  constructor() { effect(() => { void processos.acoes(this.id()).then((r) => this.codigos.set(r.ok ? r.dados : [])); }); }
}
```

No Angular, sem zone.js, **todo estado que a tela mostra fica em `signal`**: um campo comum alterado depois de um `await` não redesenha a tela. Diálogo (o motivo) é o `Dialog` do Angular CDK, aberto por um serviço do catálogo, nunca um `<dialog>` montado na tela. Ícone é `<svg lucideNome aria-hidden="true">` com o componente do `@lucide/angular` nos `imports`. Rode o `lint` do projeto (angular-eslint, com as regras de acessibilidade de template) além do typecheck.

Em todos: a lista vem da API, o rótulo e o estilo vêm da jornada, e o botão é um componente que não sabe de API nem de jornada.
