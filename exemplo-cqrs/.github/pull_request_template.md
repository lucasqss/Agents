## O que muda

<!-- Uma ou duas frases: o problema e a solução. -->

## Onde mexeu

- [ ] Domínio (`command/domain`): regra nova ou alterada tem teste JUnit puro
- [ ] Escrita (`command/application`, `command/persistence`, `command/entrypoint`)
- [ ] Leitura (`query/...`)
- [ ] Sistema externo (`shared/gateway`): tem Service Stub e teste de indisponibilidade
- [ ] Canal (`query/entrypoint/<canal>`, `shared/apresentacao`)
- [ ] Banco (migration nova)

## Checklist

- [ ] `./mvnw verify` passa localmente
- [ ] Não alterei regras em `src/test/java/**/arquitetura/` (se alterei, expliquei o motivo abaixo)
- [ ] Não editei migration existente; mudança de schema é um arquivo `V<n+1>__...sql` novo
- [ ] Contrato de canal (`*Response`): só mudanças aditivas, nenhum campo renomeado ou removido
- [ ] Regra de negócio está no domínio, não no use case, no resource nem na entidade JPA
- [ ] Usei IA? Revisei o código gerado e ele segue `.github/copilot-instructions.md`

## Observações para o revisor
