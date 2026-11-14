# Entrypoints, canais e apresentação

Os fronts são gerados por low-code e não formatam nada: o backend entrega pronto para exibir.

**Todo entrypoint segue `entrypoint/<via>/<agregado>/`**, nesta ordem. A via é `rest`, um canal (`web`, `mobile`, `taa`), `mensageria` ou `batch`.

**Cada endpoint pertence a um agregado.** Tela que mostra dados de vários agregados fica no agregado principal dela, e a junção vem da consulta. Exemplo: `CotaDetalhe` junta cota, cliente e processo e é servido em `entrypoint/web/cota/`.

## API neutra (`entrypoint/rest/<agregado>/`)

Recebe HTTP, chama o caso de uso e devolve o resultado. Sem regra de negócio, sem acesso a repositório, sem formatação.

**Entrada:** um `XRequest` (record) por operação, com Bean Validation para o **formato** (`@NotBlank`, `@NotNull`, `@Size(max = <tamanho da coluna>)`, `@Positive`), recebido como `@Valid @NotNull XRequest` e convertido para o comando do caso de uso (`paraDados()`). Valor monetário leva `@Positive` (se a regra pede) e `@Digits(integer = <colunas>, fraction = 2)`. Formato inválido responde 400 antes de qualquer busca. Não anote os records de `application/` nem o domínio: a regra de negócio continua no domínio.

**Lista no request:** `@NotEmpty List<@NotNull @Valid Item> itens` no record, e o item (também record de request) leva o Bean Validation de formato dos seus campos. O nome do campo no erro traz o caminho com índice (`itens[1].campo`), montado em `ExceptionMappers`. Regra do conjunto (soma, repetição) é do domínio e sai como `DominioException` (400 com mensagem), não como erro de campo.

**Ordem das conferências:** formato (Bean Validation) e invariantes do domínio recusam com 400 antes de qualquer busca; só depois vêm 404 (agregado ou referência inexistente) e 409 (estado atual). O caso de uso constrói o agregado antes de buscar o que precisa para decidir.

**Ação com dado obrigatório:** o dado vem em um record de request próprio (`@NotBlank`, `@Size(max = <tamanho da coluna>)`), com o corpo declarado obrigatório (`@RequestBody(required = true) @Valid @NotNull`), para o OpenAPI mostrar o corpo como obrigatório. Corpo ausente, vazio ou grande demais responde 400 com o nome do campo, antes de buscar o agregado (400 vence 404 e 409). Ações sem dado seguem aceitando requisição sem corpo (`required = false`); os dois estilos convivem no mesmo resource. Quem decide se a ação exige o dado é o domínio ([domínio](dominio.md)); o entrypoint só confere o formato.

**Erros:** saem como `ErroResponse`, com a lista `campos`. Exceção vira HTTP em `ExceptionMappers`, na raiz de `entrypoint/`:

| Exceção | HTTP |
|---|---|
| `DominioException` | 400 |
| `TransicaoInvalidaException`, `ConflitoException` | 409 |
| Subclasse de `DominioException` para conflito com o estado atual (`CreditoInsuficienteException`) | 409, mapeada à parte; o mapper de `DominioException` não a captura |
| `NaoEncontradoException` | 404 |
| `PermissaoNegadaException` (subclasse de `DominioException`, mapeada à parte) | 403 |
| Sem token, token inválido ou expirado (framework) | 401, no mesmo `ErroResponse` |
| `SistemaExternoIndisponivelException` | 503 |
| `ConstraintViolationException` (Bean Validation) | 400, com a lista `campos` |
| `MismatchedInputException` (JSON com tipo ou valor incompatível) | 400 |
| `WebApplicationException` (recusa do próprio HTTP, como rota inexistente) | mantém o status |

## Segurança (quando o componente tem identidade)

A segurança mora só no `entrypoint/` (TEC-012: nenhum tipo ou anotação de segurança fora dele). A decisão de como a pessoa se identifica é de ADR ([nova decisão](receitas/nova-decisao.md)); o exemplo de um projeto resolvido é o `docs/adr/ADR_003_identidade_por_jwt.md` do projeto de exemplo.

- **Primeira barreira:** `@RolesAllowed` (papéis da ação) ou `@Authenticated` (qualquer papel) em cada resource. Os nomes dos papéis, como vêm no token, ficam numa classe de constantes do entrypoint. Vale a **união** dos papéis que a ação aceita em algum estado; o papel certo por situação é do domínio ([domínio](dominio.md)).
- **Um único lugar lê o token** (`JsonWebToken`): uma classe do entrypoint que monta o `Autor` e o entrega ao caso de uso. O `Autor` é dado puro, do domínio: usuário, papéis como enum de domínio e, quando o papel é de cliente, a identificação dele. Caso de uso e domínio nunca veem token nem `SecurityIdentity`.
- **Ordem das recusas:** 401 (token) → 403 de papel (`@RolesAllowed`, antes de ler corpo e agregado) → 400 de formato → 404 → 403 de dono e de papel por situação (domínio) → 409. 401 e 403 saem no formato de erro padrão.

## Contrato do catálogo (quando o componente provê para outro sistema)

Se o contrato da API vem do catálogo corporativo (interface ou DTOs gerados numa dependência Maven), ele fica em `entrypoint/rest/<agregado>/` e não passa dali (TEC-011):

- o `XResource` implementa ou usa o contrato gerado;
- um `XAssembler` (final, estático, sem CDI) converte o DTO do catálogo no `DadosX` do caso de uso e o resultado de volta no DTO do catálogo;
- o caso de uso recebe e devolve só tipos nossos: `DadosX`, tipos simples, records de consulta.

## Canais (`entrypoint/<canal>/<agregado>/`)

Na pasta do agregado ficam:

- `XCanalResource` (`@Path("/<canal>/v1/...")`, `@Tag(name = "Canal <canal>")`);
- `XCanalResponse` (record: **o contrato do front**);
- `XCanalAssembler` (classe final, métodos estáticos, sem CDI).

Na raiz do canal fica só `entrypoint/<canal>/Padrao<Canal>.java`.

Regras:

- O Assembler decide **o que** o canal recebe e usa `Padrao<Canal>.INSTANCIA` para decidir **como** exibir.
- Mande o valor bruto **e** o formatado (`valorCredito` + `valorCreditoFormatado`, `estado` + `situacao`).
- Somar ou formatar o que já veio da consulta é papel do Assembler. Número com significado de negócio nasce numa consulta em `persistence/<agregado>/`.
- O Resource de canal nunca devolve record de `persistence/`: ele devolve o próprio `*Response`, e o Assembler recebe o record. Na API neutra (`entrypoint/rest/<agregado>/`), o Resource pode devolver o record. Um canal nunca usa outro canal nem o `Padrao` de outro canal.
- **Contrato congelado:** só mudanças aditivas no `*Response`. Renomear ou remover campo quebra o front low-code; quebra vira `/<canal>/v2`.

## Job agendado (`entrypoint/batch/<agregado>/`)

Só chama um caso de uso, como qualquer entrypoint. Veja `RotinaDePagamentoJob` no exemplo.

- O intervalo vem de configuração: `@Scheduled(every = "{<propriedade>}", concurrentExecution = SKIP)`, com valor padrão no `application.properties`; `off` desliga o agendamento.
- `SKIP` só impede duas execuções **na mesma instância**. Entre instâncias, quem protege é o caso de uso que aplica o resultado, com bloqueio e conferência do estado ([transação e sistema externo](transacao-e-sistema-externo.md)).
- Dependência `quarkus-scheduler` ([pom](pom-obrigatorio.md)).
- Nos testes de integração o agendamento fica desligado (`%test.<propriedade>=off`) e o teste chama o caso de uso direto. Nos de aceitação o intervalo é curto e o teste espera o efeito consultando a API.

## Apresentação comum (`entrypoint/apresentacao/`)

Se a pasta ainda não existir, crie-a na primeira tela de canal ([exemplo](exemplos-codigo.md)).

- `Formatador` (funções básicas, locale pt-BR explícito, sem o espaço U+00A0) e `Rotulos` (catálogo por extenso) são comuns e não conhecem canais.
- Cada canal tem `entrypoint/<canal>/Padrao<Canal> implements PadraoApresentacao`. Formato novo entra na interface, e todos os canais implementam.
- Todo formato novo ganha linha em `src/test/java/<pacote raiz>/entrypoint/apresentacao/PadroesDeCanalTest.java`; todo endpoint de canal, teste de contrato em `src/test/java/<pacote raiz>/integracao/CanaisContratoTest.java` com os nomes exatos dos campos. Crie esses arquivos se ainda não existirem.
