# A mesma arquitetura em cada framework

As cinco camadas, as regras e o JSON da jornada não mudam. Muda só a forma de cada peça e a raiz do código (`js/` em JavaScript puro, `src/` nos frameworks). O `scripts/verificar.sh` confere qualquer um, porque olha os caminhos das camadas (configuráveis por variável). O protótipo React deste repositório (`exemplo-testes/prototipo-telas`) é um exemplo de React com as camadas mapeadas para as pastas dele (tabela no `SKILL.md`).

| Peça | JavaScript puro (estrutura de referência) | React | Vue | Angular |
|---|---|---|---|---|
| `nucleo/http` | `chamar()` com `fetch` | o mesmo módulo (ou um hook `useApi` só para estado de carregamento, chamando `chamar`) | o mesmo módulo | `HttpService` em `nucleo/`, o único com `HttpClient` |
| `nucleo/rotas` | roteador por `#hash` | `react-router` configurado em `nucleo/rotas` | `vue-router` em `nucleo/rotas` | `Routes` em `nucleo/rotas` |
| `api/<recurso>` | módulo de funções | o mesmo módulo | o mesmo módulo | `@Injectable` `ProcessosApi` em `api/`, usando `HttpService` |
| `jornadas/*.json` | lido por `lerArquivo` | `import jornada from "./jornadas/x.json"` | idem | `import` ou `HttpService` lendo o asset |
| contexto da jornada | `telas/contexto.js` | `JornadaContext` (Context) em `telas/contexto` | `provide/inject` em `telas/contexto` | `JornadaService` em `telas/contexto` |
| `componentes/` | função → string HTML | componente de função, só props, sem efeitos | SFC só com `props` e `emits` | componente `standalone` com `@Input`/`@Output`, sem serviço injetado |
| `telas/` | função que monta a página | componente de página: busca em `useEffect` pela `api` | página com `onMounted` chamando a `api` | componente de rota chamando o serviço da `api` |
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

Angular:

```ts
@Component({ standalone: true, selector: "acoes-disponiveis", imports: [BotaoComponent], template: `
  @for (a of codigos(); track a) {
    <app-botao [perigo]="jornada.acao(a).estilo === 'perigo'" (clique)="executar.emit(a)">{{ jornada.acao(a).rotulo }}</app-botao>
  }` })
export class AcoesDisponiveisComponent {
  @Input({ required: true }) id!: string;
  @Output() executar = new EventEmitter<string>();
  jornada = inject(JornadaService);
  private api = inject(ProcessosApi);
  codigos = signal<string[]>([]);
  ngOnInit() { this.api.acoes(this.id).subscribe((r) => this.codigos.set(this.jornada.ordenarAcoes(r.corpo ?? []))); }
}
```

Em todos: a lista vem da API, o rótulo e o estilo vêm da jornada, e o botão é um componente que não sabe de API nem de jornada.
