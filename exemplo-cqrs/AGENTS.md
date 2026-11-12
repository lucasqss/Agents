# Instruções para agentes de IA

Serviço Java 25 + Quarkus 3.33 com domínio puro e padrões do *Patterns of Enterprise Application Architecture* (Fowler). A arquitetura é verificada a cada build por testes ArchUnit.

**Use as skills:** `arquiteto-padrao` ao criar ou alterar código; `adequar-ao-padrao` ao trazer código legado para o padrão; `teste-aceitacao-modelos` ao criar ou alterar testes de aceitação de usuário a partir do requisito; `teste-aceitacao-legado` quando não há requisito e as regras saem do código existente.

## Antes de terminar qualquer tarefa

1. Rode `./mvnw verify`.
2. **Se uma regra ArchUnit falhar, corrija o código, nunca a regra.** Não edite, apague nem afrouxe nada em `src/test/java/**/arquitetura/`. A falha traz um ID e um "Corrija:". Se achar que a regra está errada, diga isso e pare.
3. Não edite migrations existentes. Mudança de schema é sempre um arquivo novo `V<n+1>__descricao.sql`.

## Proibido

- Panache, Lombok, MapStruct, SQL nativo, herança JPA.
- Regra de negócio fora do domínio. Domínio é Java puro, sem JPA, Jakarta Data, CDI, JAX-RS, Bean Validation, Quarkus, Hibernate ou MicroProfile.
- `*Entity` com qualquer método além de getter e setter.
- Use case importar JPA, Jakarta Data ou `*Entity`.
- Bean Validation fora do entrypoint.
- Chamar sistema externo fora de um `*Gateway`, ou Gateway chamado por quem não é use case.
- Classe do catálogo corporativo (dependência Maven de outro sistema) fora de `gateway/` e `entrypoint/`. Não edite `Catalogo.java` em `arquitetura/` para o build passar.
- Renomear ou remover campo de um `*Response` de canal. Só mudanças aditivas; quebra vira `/v2`.
- Formatação fora do Assembler do canal.
