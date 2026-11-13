# Padrões do PoEAA neste padrão

Mapa do catálogo de *Patterns of Enterprise Application Architecture* (Fowler): o que se usa e onde, o que o framework faz, o que é proibido e o que entra só quando o problema aparece. Antes de introduzir um padrão que não está em "Usados", confira aqui.

## Usados

| Padrão | Onde | Como reconhecer |
|---|---|---|
| Domain Model | `domain/<agregado>/` | entidade com as regras, Java puro |
| Service Layer | `application/<agregado>/` | `*UseCase`, um por operação, dono da transação |
| Repository + Separated Interface | interface em `domain/<agregado>/`, implementação em `persistence/<agregado>/` | `XRepository`, `XRepositoryAdapter` |
| Data Mapper | `persistence/<agregado>/` | `XMapper` escrito à mão, entre domínio e `*Entity` |
| Identity Field, Foreign Key Mapping | `*Entity` | `@Id`; FK como campo simples |
| Embedded Value | `XMapper` | Value Object gravado nas colunas do dono ([valores](valores.md)) |
| Value Object, Money | `domain/` | `record` imutável ([valores](valores.md)) |
| Optimistic, Pessimistic e Coarse-Grained Lock | `*Entity`, repositório do domínio | versão, `buscarPorIdComBloqueio` ([concorrência](concorrencia.md)) |
| Gateway | `gateway/<sistema>/` | `XGateway` ([gateway](gateway.md)) |
| Service Stub | testes | sistema externo falso ([testes](testes.md)) |
| Data Transfer Object + Assembler | `entrypoint/` | `XRequest`, `XResponse`, `XAssembler` ([canais](canais.md)) |
| Remote Facade | `entrypoint/` | resource fino, que só chama caso de uso |
| Layer Supertype (só para exceções) | raiz da camada | `DominioException` |

Leitura de tela não passa pelo Domain Model: consulta HQL projetada direto em record ([persistência](persistencia-jakarta-data.md)).

## Deixados de fora de propósito

| Padrão | Por quê |
|---|---|
| Unit of Work, Identity Map, Lazy Load | A persistência usa sessão sem estado: gravar é sempre explícito, e nada é carregado por trás. O que o caso de uso faz é o que acontece no banco |
| Active Record | Mistura regra e persistência na mesma classe (é o que o Panache faz) |
| Transaction Script | Regra de negócio em caso de uso. Caso de uso só orquestra |
| Table Module, Record Set | Regra organizada por tabela, não por objeto |
| Row/Table Data Gateway | O Data Mapper já cobre |
| Herança mapeada (Single Table, Class Table, Concrete Table) | Uma tabela por agregado; o que muda por tipo é comportamento, resolvido no domínio ([domínio](dominio.md)) |
| Serialized LOB | Esconde dado do banco e das consultas. Só com decisão do dono |
| Registry | Estado global; use injeção por construtor |
| Padrões de sessão (Client, Server, Database Session State) | Os serviços não guardam sessão |
| Padrões de apresentação web (MVC, Page/Front Controller, Template View...) | O componente expõe API; tela é do front |

## Sob demanda

Entram quando o problema aparece. Fora isso, não introduza.

| Padrão | Quando | Onde |
|---|---|---|
| Special Case | o mesmo `if (x == null)` ou "sem valor" se repete no domínio | objeto do domínio que representa o caso (ex.: "sem desconto"), no agregado |
| Query Object | consulta com muitos filtros opcionais, que a `@Query` fixa com `:filtro is null or ...` não atende mais | `persistence/<agregado>/`, dentro do `XQueryRepository`. Peça ao dono: muda como a leitura é escrita |
| Plugin | a implementação muda por ambiente ou por configuração | interface no domínio ou no gateway, escolhida pelo CDI |
| Layer Supertype (além das exceções) | três ou mais classes da mesma camada repetem o mesmo código | raiz da camada, com aprovação do dono (GER-003) |
| Dependent Mapping | filho que só existe com o pai | o adapter do pai grava e carrega os filhos |
| Association Table Mapping | relação muitos-para-muitos dentro do agregado | `*Entity` da associação, gravada pelo adapter |
