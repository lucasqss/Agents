# Resource que recebe ou devolve entidade

O destino está em `references/canais.md` da skill `arquiteto-padrao`. O **contrato HTTP não muda**: mesmos caminhos, mesmos nomes de campo, mesmos códigos.

1. **Fixe o contrato atual** com um teste de API que confere os nomes exatos dos campos, antes de mexer.
2. **Entrada:** crie um `XRequest` (record) por operação, em `entrypoint/<via>/<agregado>/`. O formato (as antigas `@NotNull`/`@Size` da entidade) vai para ele, com `@Size` igual ao tamanho da coluna. Receba como `@Valid @NotNull XRequest` e converta para o comando do use case (`paraDados()`).
3. **Saída de escrita:** devolva só o mínimo. Se o contrato atual devolve mais, mantenha: mudar seria quebra.
4. **Saída de leitura:** `XResponse`, ou `XCanalResponse` + `XCanalAssembler` em canal. Os dados vêm de record projetado em `persistence/<agregado>/`.
5. **Campos que a serialização da entidade expunha sem querer** continuam no contrato até o dono decidir. Liste-os na entrega.
6. **Atenção a anotações Jackson** (`@JsonProperty`, `@JsonIgnore`, formato de data) na entidade: elas definem o contrato atual e precisam ser reproduzidas no `*Response`.
7. Exceção nova entra em `ExceptionMappers`. Se o código HTTP de um erro existente mudaria, pare e pergunte.
8. Lógica de negócio no Resource desce para o domínio. Acesso a repositório passa por um use case.
