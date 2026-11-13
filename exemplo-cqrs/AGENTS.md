# Instruções para agentes de IA

Serviço Java 25 + Quarkus 3.33 com domínio puro e padrões do *Patterns of Enterprise Application Architecture* (Fowler). A arquitetura é verificada a cada build por testes ArchUnit.

**Use as skills:** `especificar-caso-de-uso` ao transformar um pedido de negócio em requisito escrito, antes de qualquer código; `arquiteto-padrao` ao criar ou alterar código; `adequar-ao-padrao` ao trazer código legado para o padrão; `teste-aceitacao-modelos` ao criar ou alterar testes de aceitação de usuário a partir do requisito; `teste-aceitacao-legado` quando não há requisito e as regras saem do código existente.

## Como trabalhar

Vale para qualquer tarefa:

1. **Entenda antes de mexer.** Leia o código envolvido e quem o chama. Procure como o projeto já resolve algo parecido e faça igual.
2. **Planeje o que tem mais de um passo.** Liste os passos antes de editar e siga a lista.
3. **Mude o mínimo.** Só o que a tarefa pede. Não refatore, renomeie nem "melhore" o que está em volta.
4. **Verifique rodando.** Código que você não rodou não está pronto. Ao final de cada turno com mudança de código, o harness (`.github/hooks/`) roda a verificação rápida e devolve a falha para você corrigir.
5. **Relate o que é fato.** Diga o que mudou, o que você rodou e o resultado, e o que não conseguiu verificar. Teste que falhou é relatado como falha.

Conforme o tipo da tarefa:

- **Código novo.** Funcionalidade nova começa pelo caso de uso confirmado (skill `especificar-caso-de-uso`). Depois, use a skill `arquiteto-padrao` e a receita da operação. O teste nasce junto com o código.
- **Avaliação de código legado.** Não altere nenhum arquivo. Cada achado traz `arquivo:linha`, a evidência e a gravidade. Separe o que você confirmou lendo ou rodando do que é suspeita. Para comparar com o padrão, use o diagnóstico da skill `adequar-ao-padrao`.
- **Correção de bug.** Primeiro reproduza com um teste que falha pelo motivo do bug. Ache a causa, não o sintoma, e corrija na camada dona da regra. O teste novo passa e os antigos continuam passando sem terem sido alterados.

## Antes de terminar qualquer tarefa

1. Rode `./mvnw verify`.
2. **Se uma regra ArchUnit falhar, corrija o código, nunca a regra.** Não edite, apague nem afrouxe nada em `src/test/java/**/arquitetura/` nem em `.github/hooks/`. A falha traz um ID e um "Corrija:". Se achar que a regra está errada, diga isso e pare.
3. Não edite migrations existentes. Mudança de schema é sempre um arquivo novo `V<n+1>__descricao.sql`.

## Proibido

- Panache, Lombok, MapStruct, SQL nativo, herança JPA.
- Regra de negócio fora do domínio. Domínio é Java puro, sem JPA, Jakarta Data, CDI, JAX-RS, Bean Validation, Quarkus, Hibernate ou MicroProfile.
- `*Entity` com qualquer método além de getter e setter.
- Use case importar JPA, Jakarta Data ou `*Entity`.
- Bean Validation fora do entrypoint.
- Chamar sistema externo fora de um `*Gateway`, ou Gateway chamado por quem não é use case.
- Classe do catálogo corporativo (dependência Maven de outro sistema) fora de `gateway/` e `entrypoint/`. Não edite `ClassesExternas.java` em `arquitetura/` para o build passar.
- Renomear ou remover campo de um `*Response` de canal. Só mudanças aditivas; quebra vira `/v2`.
- Formatação fora do Assembler do canal.
