---
applyTo: "**/arquitetura/**"
---
# Regras de arquitetura (ArchUnit)

Esta pasta é a fitness function da arquitetura e tem dono no CODEOWNERS.

- **Não altere, apague, desligue nem afrouxe regras para fazer o build passar.** Quando uma regra falha, o código está no lugar errado: corrija o código.
- Não adicione `allowEmptyShould`, exceções, `FreezingArchRule` nem `@ArchIgnore` sem pedido explícito do dono.
- Os padrões são sempre montados com `Raiz.pacote(...)`, `Raiz.exato(...)` e `Raiz.emQualquer(...)`, nunca com o nome do projeto escrito à mão: é isso que deixa a pasta pronta para copiar entre projetos.
- Mudança de regra é decisão de arquitetura: descreva o motivo no PR e espere a revisão do CODEOWNER.
