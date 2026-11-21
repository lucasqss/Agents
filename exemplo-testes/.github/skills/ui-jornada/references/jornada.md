# A jornada como dado: `jornadas/<jornada>.json`

O desenho da jornada que o negócio lê e valida. É **dado**, não código: qualquer framework o carrega igual. As chaves são os **códigos da API**; os valores são o que a tela mostra.

## Campos

| Campo | Forma | Para quê |
|---|---|---|
| `_sobre` | texto | explica o arquivo a quem abre (o JSON não tem comentário) |
| `nome` | texto | nome do sistema (título da janela) |
| `situacoes` | `{ CODIGO: { rotulo, cor, quemAge?, final? } }` | rótulo e cor da etiqueta de cada situação. `cor`: `neutro`, `andamento`, `espera`, `sucesso`, `erro`. `quemAge`: o `usuario` da persona sugerida para a etapa (cartão "Próximo passo", só orientação); `final: true`: não há próximo passo |
| `acoes` | `{ CODIGO: { rotulo, pedeMotivo?, estilo? } }` | rótulo do botão; `pedeMotivo: true` abre o diálogo de motivo **antes** de chamar; `estilo: "perigo"` para ações destrutivas. **A ordem das chaves é a ordem dos botões** |
| `tipos` | `{ CODIGO: { rotulo, explicacao, etapas: [...] } }` | o caminho de cada tipo. Etapa é o código da situação, ou `{ "situacao": CODIGO, "quando": "acima da alçada" }` para etapa que só acontece em certos casos (o texto é só explicação) |
| `personas` | `[{ usuario, nome, papel }]` | quem pode ser escolhido em "Quem sou eu"; `usuario` vai no cabeçalho de identificação |
| `textos` | `{ chave: texto }` | textos de ajuda que as telas usam por chave (`texto("alcada")`) |
| `contrato` | `{ situacoes: "Schema", acoes: "Schema", tipos: "Schema" }` | o nome do enum no OpenAPI de cada grupo, para a conferência |

## Exemplo (recorte)

```json
{
  "nome": "Processos de pagamento",
  "situacoes": {
    "EM_ANALISE":        { "rotulo": "Em análise",        "cor": "andamento" },
    "AGUARDANDO_ALCADA": { "rotulo": "Aguardando alçada", "cor": "espera" }
  },
  "acoes": {
    "CONCLUIR_ANALISE": { "rotulo": "Concluir a análise" },
    "REPROVAR":         { "rotulo": "Reprovar", "pedeMotivo": true, "estilo": "perigo" }
  },
  "tipos": {
    "SERVICO": {
      "rotulo": "Serviço",
      "explicacao": "Análise e, acima da alçada, aprovação.",
      "etapas": ["ABERTO", "EM_ANALISE", { "situacao": "AGUARDANDO_ALCADA", "quando": "acima da alçada" }, "APROVADO", "PAGO"]
    }
  },
  "textos": { "alcada": "Até a alçada, o processo é aprovado na análise; acima dela, precisa de quem tem alçada." },
  "contrato": { "situacoes": "EstadoProcesso", "acoes": "AcaoProcesso", "tipos": "TipoProcesso" }
}
```

Modelo vazio para começar: [`assets/jornada.json`](../assets/jornada.json).

## O que NÃO entra na jornada

| Não entra | Por quê | Onde fica |
|---|---|---|
| Valor de regra (`50000`, prazo, percentual) | duplicaria a regra; o backend muda e o front mente | backend; o texto fala "acima da alçada", sem número |
| Condição (`"se": "valor > alcada"`) | é decisão, não desenho | backend: a situação e as ações disponíveis vêm da API |
| Transição (de → para) | o backend é quem sabe para onde a ação leva | a resposta da ação traz a situação nova |
| Quem **pode** executar (permissão) | segurança é do backend | a API filtra as ações por perfil; a persona é só a identidade enviada |
| URL de endpoint | é da camada `api/` | `api/<recurso>` |

`pedeMotivo` **entra**: é desenho da tela (abrir o diálogo antes). Se o front esquecer, o backend recusa com 400 e a mensagem dele aparece; se o front pedir à toa, só sobra um diálogo a mais. O backend continua sendo quem decide.

## Conferência contra o contrato

Recomendado: um módulo de conferência que, ao subir em dev, lê o OpenAPI (por um cliente `contrato` na camada api) e, para cada grupo em `contrato`, confere se toda chave da jornada existe no enum. Divergência mostra a faixa "Jornada desatualizada: a API não conhece acoes: PAGAMENTO". O arquivo pode ter menos códigos que a API (a jornada não precisa mostrar tudo); não pode ter códigos que a API não conhece.
