# Layout antigo → módulos

Camada `estrutura`. Só mover e renomear pacotes: **nenhuma lógica muda aqui.** A estrutura de destino está na skill `arquiteto-padrao`.

## De onde para onde

| Hoje | Destino |
|---|---|
| `controller/`, `resource/`, `rest/` | `<modulo>/entrypoint/rest` |
| Resource específico de canal | `<modulo>/entrypoint/<canal>` |
| `service/` | `<modulo>/application` nesta camada; a quebra vem na camada `application` |
| `model/`, `entity/` com regra | `<modulo>/domain` nesta camada; a separação vem na camada `dominio` |
| `repository/` | `<modulo>/persistence` |
| `client/`, `integration/` | `<modulo>/gateway/<sistema>` |
| `command/`, `query/` no topo | `<modulo>/command`, `<modulo>/query` |
| `shared/persistence` | `<modulo>/persistence` |
| `shared/gateway/<sistema>` | `<modulo>/gateway/<sistema>` do módulo consumidor |
| `shared/apresentacao` | módulo `apresentacao` |
| `util/`, `common/`, `helper/` | caso a caso: o módulo que usa. Se vários usam, pare e pergunte ao dono |

## Passos

1. Mova um módulo de cada vez, usando a refatoração da IDE ou atualizando todos os imports e strings com nome de pacote (HQL com `select new <pacote>.X`, `application.properties`, `META-INF`).
2. Atualize os `package-info.java`.
3. Mova os testes para o pacote equivalente.
4. Rode `./mvnw verify` e faça o commit da camada.

**Atenção:** se dois módulos usarem a mesma classe, não crie `shared`. Pare e pergunte ao dono.
