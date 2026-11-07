---
mode: agent
description: Expõe uma consulta para um canal (web, mobile, TAA...) com Response próprio, Assembler e padrão de apresentação do canal.
---
Crie o endpoint de canal descrito abaixo seguindo `.github/copilot-instructions.md` e os exemplos em `query/entrypoint/web|mobile|taa`.

Canal: ${input:canal:web, mobile, taa ou um canal novo}
Tela: ${input:tela:O que a tela mostra, campo a campo, com o formato esperado}

1. Garanta que existe a consulta (record em `query/persistence` + use case). Se não existir, crie antes (prompt `nova-consulta`).
2. Em `query/entrypoint/<canal>/`:
   - `XCanalResponse` (record): campos com nomes estáveis; para cada valor exibido, o bruto e o formatado.
   - `XCanalAssembler` (final, estático, sem CDI): monta o Response usando `Padrao<Canal>.INSTANCIA`.
   - `XCanalResource` em `/<canal>/v1/...` com `@Tag(name = "Canal <canal>")`.
3. Formato que ainda não existe: acrescente o método em `PadraoApresentacao` e implemente em **todos** os canais; acrescente a linha em `PadroesDeCanalTest`.
4. Canal novo: crie `shared/apresentacao/<canal>/Padrao<Canal>`.
5. Teste de contrato em `CanaisContratoTest`: conjunto exato de campos e textos formatados.
6. Rode `./mvnw verify`.
