# Dossiê de arquitetura: um processo que se desdobra em vários tipos

O que precisa mudar no `exemplo-cqrs` antes de implementar as próximas features, o padrão arquitetural alvo e a organização de pastas.

**Premissas confirmadas:** os tipos (imobiliário, móveis, serviços) variam em quatro eixos (etapas, dados, regras por etapa, integrações), sempre com parte comum e parte específica; são poucos tipos, estáveis e definidos em código; é preciso histórico de transições e retorno de etapa.

Os caminhos citados são relativos a `src/main/java/br/com/claudio/consorcio/`.

## 1. Diagnóstico (o que encontrei no código)

| # | Achado | Onde | Por que atrapalha |
|---|---|---|---|
| D1 | Só existe 1 ponto de variação por tipo: `FluxoProcesso.aposAnalise()` | `domain/processo/FluxoProcesso.java` | Dados, regras e integrações por tipo não têm lugar. Um segundo desvio vira outro método na interface e em todos os fluxos. |
| D2 | A interface de estado conhece as ações de todos os tipos (`concluirVistoria` é do imobiliário) | `domain/processo/estado/EstadoProcesso.java` | Cada etapa própria de um tipo incha a interface comum. Ação nova = `EstadoProcesso` + `ProcessoPagamento` + use case + endpoint + construtor do resource. |
| D3 | Os 6 estados são records sem dados e sem comportamento além de "qual o próximo" | `domain/processo/estado/*.java` | É um enum escrito em 7 arquivos, com três listas para manter em sincronia (`permits`, `todos()`, `NOME`). O padrão State do GoF se paga quando o estado carrega comportamento rico, o que não ocorre aqui. |
| D4 | Ciclo de pacotes `domain.processo` ⇄ `domain.processo.estado` | `FluxoProcesso` devolve `EstadoProcesso`; `EstadoProcesso` recebe `FluxoProcesso` e lança `TransicaoInvalidaException` | A regra GER-002 não pega: ela fatia em `(*).(*)..`, então `processo` e `processo.estado` caem na mesma fatia. |
| D5 | 4 use cases de transição idênticos, mudando só o método chamado | `application/processo/{IniciarAnalise,ConcluirAnalise,ConcluirVistoria,Cancelar}ProcessoUseCase.java` | Com histórico e autor, a mesma mudança terá de ser repetida em todos. |
| D6 | Pagar acontece em dois passos soltos e usa `instanceof Pago` | `ProcessoPagamento.pagar()` + `registrarProtocolo()`; `PagarProcessoUseCase` | O domínio permite "pago sem protocolo". |
| D7 | Sem controle de concorrência no processo | `ProcessoPagamentoEntity` (sem `@Version`), `PagarProcessoUseCase` (leitura sem bloqueio) | Dois `pagar` simultâneos passam pela checagem e geram duas chamadas à tesouraria. |
| D8 | Tabela única só com `tipo` e `estado`; sem datas, autor ou histórico | `V1__schema.sql`, `ProcessoPagamentoEntity` | Dados próprios por tipo e auditoria não cabem. |
| D9 | Integração única e igual para todos os tipos | `PagarProcessoUseCase` → `TesourariaGateway` | Sem ponto de extensão para integrações por tipo. |
| D10 | Tipo e estado trafegam como `String` na leitura | `ListarProcessosUseCase`, `ProcessoQueryRepository`, `Rotulos` | Filtro inválido devolve lista vazia em vez de 400. |

O que está bom e **deve ser mantido**: camadas no topo com agregado dentro; domínio puro com Separated Interface; escrita e leitura separadas sem pastas de CQRS; Gateway para sistema externo; BFF por canal com `PadraoApresentacao` + Assembler; ArchUnit com catálogo de regras; testes de aceitação pelos endpoints.

## 2. Padrão arquitetural alvo

**Camadas (mantém):** `entrypoint → application → domain ← persistence`, mais `gateway`. Leitura continua `entrypoint → application → persistence`.

**Dentro do agregado `processo` (muda):** um motor único por **composição**, com um ponto de extensão para cada eixo de variação. Regra geral: *o comum fica no motor, o específico fica num objeto do tipo; o motor nunca faz `if (tipo == ...)`.*

| Eixo | Parte comum | Parte específica | Padrão |
|---|---|---|---|
| Etapas | `FluxoProcesso.comum()`: tabela `(estado, ação) → estado` | `FluxoImobiliario`, `FluxoMovel`, `FluxoServico` partem da tabela comum e acrescentam/trocam linhas | Máquina de estados dirigida por tabela (a alternativa "table-driven" que o próprio GoF descreve no State) |
| Dados | campos de `ProcessoPagamento` | `DadosDoTipo` (sealed) com `DadosImobiliario`, `DadosMovel`, `DadosServico` | Composição + tipo soma (sealed interface + records) |
| Regras por etapa | checagens no próprio `ProcessoPagamento` | `DadosDoTipo.exigirPara(acao)`: cada record valida o que a ação exige dos seus dados | Strategy, embutida no objeto de dados do tipo |
| Integrações | passo comum (tesouraria) | `PagamentoDoTipo` com uma implementação por tipo, escolhida por `TipoProcesso` | Strategy na camada application, sobre os Gateways |

**Decisões de herança x composição**

- **Não usar herança no agregado** (`ProcessoImobiliario extends ProcessoPagamento`). Os quatro eixos variam de forma independente; herança amarraria todos numa hierarquia só e puxaria herança JPA.
- **Usar hierarquia selada só em `DadosDoTipo`.** É o único ponto onde "é um" descreve bem o problema, e o `switch` exaustivo faz o compilador apontar tudo que falta quando entra um tipo novo.
- **Não usar classe base abstrata nos use cases.** A repetição (D5) se resolve com um use case parametrizado pela ação.
- **Trocar State (classes) por enum + tabela.** Retorno de etapa vira uma linha na tabela (`EM_VISTORIA + DEVOLVER → EM_ANALISE`) e "quais ações estão disponíveis agora" sai de graça da mesma tabela.

**Motor (esboço)**

```java
// domain.processo.fluxo
public final class FluxoProcesso {
    public static Builder comum() { ... }                 // ABERTO→EM_ANALISE→APROVADO→PAGO, cancelar, devolver
    public EstadoProcesso proximo(EstadoProcesso atual, AcaoProcesso acao) { ... } // lança TransicaoInvalidaException
    public Set<AcaoProcesso> acoesDisponiveis(EstadoProcesso atual) { ... }
}

// domain.processo
public Transicao executar(AcaoProcesso acao, String autor, Instant quando, String observacao) {
    dados.exigirPara(acao);                               // regra específica do tipo
    EstadoProcesso destino = tipo.fluxo().proximo(estado, acao);
    Transicao t = new Transicao(estado, destino, acao, autor, quando, observacao);
    estado = destino;
    novasTransicoes.add(t);                               // o adapter grava em processo_transicao
    return t;
}
```

## 3. Organização de pastas alvo

```
domain/processo/
  ProcessoPagamento                 agregado: motor único
  ProcessoPagamentoRepository
  fluxo/                            vocabulário do motor; não conhece tipo nenhum
    EstadoProcesso (enum)  AcaoProcesso (enum)  FluxoProcesso  Transicao  TransicaoInvalidaException
  tipo/                             tudo que é específico de tipo, no domínio
    TipoProcesso (enum)  DadosDoTipo (sealed)
    DadosImobiliario  DadosMovel  DadosServico
    FluxoImobiliario  FluxoMovel  FluxoServico
application/processo/
  CriarProcessoUseCase  ExecutarAcaoProcessoUseCase  PagarProcessoUseCase
  ListarProcessosUseCase  ListarAcoesDisponiveisUseCase  ListarHistoricoProcessoUseCase
  PagamentoDoTipo (interface)  PagamentoImobiliario  PagamentoMovel  PagamentoServico
persistence/processo/
  ProcessoPagamentoEntity (+ @Version)  ProcessoTransicaoEntity
  ProcessoImobiliarioEntity  ProcessoMovelEntity  ProcessoServicoEntity     tabelas 1:1, sem herança JPA
  ProcessoPagamentoMapper  ProcessoPagamentoRepositoryAdapter  *DataRepository  ProcessoQueryRepository
entrypoint/rest/processo/
  ProcessoResource  ProcessoQueryResource
  NovoProcessoImobiliarioRequest  NovoProcessoMovelRequest  NovoProcessoServicoRequest
gateway/<sistema>/                  inalterado; um pacote por sistema externo
```

Dependências dentro do domínio, sem ciclo: `fluxo` ← `tipo` ← `processo`. Os tipos ficam juntos em `tipo/` (e não em um pacote por tipo) porque o Java só aceita `sealed` com os permitidos no mesmo pacote, e porque são poucos.

**Custo de um tipo novo depois da mudança:** constante em `TipoProcesso`, `DadosX`, `FluxoX`, `PagamentoX`, `ProcessoXEntity` + migration, `NovoProcessoXRequest` + um método no resource, rótulo em `Rotulos`. Nenhuma classe do motor é editada, e o compilador acusa cada `switch` que ficou incompleto.

## 4. O que mudar antes das próximas features

**P0: bloqueia as features**

1. **Motor por tabela.** Criar `fluxo/` com `EstadoProcesso` e `AcaoProcesso` como enums e `FluxoProcesso` como tabela. Remover os 6 records de estado, a interface selada e `estado/package-info.java`. `ProcessoPagamento` troca os 5 métodos de transição por `executar(acao, ...)`. Resolve D1, D2, D3, D4 e habilita retorno de etapa.
2. **Dados por tipo.** `DadosDoTipo` selado com `exigirPara(acao)`; `ProcessoPagamento.novo(cotaId, dados)` (o tipo sai dos dados). Migration com uma tabela 1:1 por tipo; o mapper compõe. Resolve D8 (dados) e o eixo de regras.
3. **Histórico.** `Transicao` no domínio, tabela `processo_transicao` (de, para, ação, autor, quando, observação), gravada pelo adapter no mesmo `salvar`. O `Instant` vem de um `Clock` injetado no use case; o autor vem do entrypoint.
4. **Use cases de transição.** Os 4 idênticos viram `ExecutarAcaoProcessoUseCase.executar(id, acao, autor, observacao)`. Ações com efeito externo (`Pagar`) continuam com use case próprio. As URLs atuais são preservadas (`POST /processos/{id}/{acao}` resolvido pelo enum), então os testes de aceitação continuam valendo. Resolve D5.
5. **Pagar atômico e concorrência.** `processo.pagar(protocolo, ...)` numa única operação, com `processo.exigirQuePode(PAGAR)` antes de chamar a tesouraria; remover `registrarProtocolo` e o `instanceof`. `@Version` em `ProcessoPagamentoEntity`, com conflito virando 409. Resolve D6 e D7.
6. **Guardrails.** Ajustar GER-002 para enxergar ciclos entre subpacotes do mesmo agregado; novas regras: `domain.processo.fluxo` não depende de `tipo`; só `domain.processo.tipo` e os pontos por tipo de cada camada referenciam `Dados*` concretos. Atualizar as regras que citam o pacote `estado`.

**P1: junto com a primeira feature que precisar**

7. **Integrações por tipo.** `PagamentoDoTipo` + uma implementação por tipo; `PagarProcessoUseCase` escolhe pelo `TipoProcesso`. Resolve D9.
8. **Criação por tipo no REST.** `POST /processos/imobiliario`, `/movel`, `/servico`, cada um com seu Request e Bean Validation, todos chamando o mesmo `CriarProcessoUseCase`. Melhor para OpenAPI e low-code do que JSON polimórfico.
9. **`GET /processos/{id}/acoes`** e **`GET /processos/{id}/historico`**: o front low-code deixa de replicar a máquina de estados para decidir quais botões mostrar.
10. **Tipo `MOVEL` no exemplo**, com uma etapa, um dado, uma regra e uma integração próprios (fictícios). É a prova de que o terceiro tipo custa só os arquivos listados na seção 3.

**P2: melhoria, não bloqueia**

11. Filtros de leitura tipados com os enums (400 em valor inválido). Resolve D10.
12. `DetalharCotaUseCase` é só repasse e a API neutra expõe records de `persistence` como contrato. Avaliar mover os read models para `application.<agregado>`; é troca consciente do desenho atual, decidir com calma.

## 5. Classes removidas e criadas (resumo)

- **Saem (12):** `estado/` inteiro (8 arquivos) e `IniciarAnalise/ConcluirAnalise/ConcluirVistoria/CancelarProcessoUseCase` (4).
- **Entram:** `fluxo/` (5), `tipo/` (3 por tipo + 2), `ExecutarAcaoProcessoUseCase`, `PagamentoDoTipo` + 1 por tipo, entidades e migrations de detalhe e de histórico, requests por tipo.
- **Mudam:** `ProcessoPagamento`, `TipoProcesso`, `Fluxo*`, `PagarProcessoUseCase`, `CriarProcessoUseCase`, mapper/adapter/entity de processo, `ProcessoResource`, `ExceptionMappers` (conflito de versão), `Rotulos`, regras ArchUnit, README.

## 6. Fora de escopo, mas anotado

- Fluxo configurável em banco e engines de BPM: descartados, pois os tipos são poucos e estáveis.
- Outbox para a chamada à tesouraria: o trade-off já está documentado em `PagarProcessoUseCase`; segue valendo.
- As cópias das regras ArchUnit e das receitas dentro de `.github/skills/` precisarão acompanhar a mudança quando ela for implementada.

