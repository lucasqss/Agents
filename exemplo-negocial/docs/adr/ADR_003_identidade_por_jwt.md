# ADR 003: Identidade por token JWT verificado pela aplicação; papéis no entrypoint e na tabela do fluxo

**Status:** Aceito
**Data:** 14 de novembro de 2026
**Decidido por:** dono do produto (fatia 7 do caso de uso `pagamento-processo`: autoria e papéis, R17, R18 e R19)

## 1. Contexto

Até a fatia 6 qualquer chamador podia chamar qualquer endpoint e o histórico não dizia quem fez o quê. O caso de uso passou a exigir quatro papéis (cliente, backoffice, aprovador, tesouraria), a regra da segunda pessoa na alçada (R17), o cliente só agir sobre a própria cota (R18) e o autor em cada entrada do histórico (R19). O padrão da skill `arquiteto-padrao` não diz como a pessoa se identifica; é decisão por projeto (receita "nova decisão"). O ambiente de desenvolvimento e de teste não pode depender de Docker para a identidade.

## 2. Decisão

**Identidade.** Todo pedido (menos `/q/*`, que fica aberto) leva `Authorization: Bearer <token JWT>`, verificado pela própria aplicação com `io.quarkus:quarkus-smallrye-jwt` (assinatura RS256, emissor e validade). Em produção um provedor de identidade real emite os tokens; trocá-lo é só configuração (`mp.jwt.verify.issuer`, `mp.jwt.verify.publickey.location`, variáveis `MP_JWT_VERIFY_ISSUER` e `MP_JWT_VERIFY_PUBLICKEY_LOCATION`), sem mudar código. Claims usados: `sub` (usuário), `groups` (papéis: `cliente`, `backoffice`, `aprovador`, `tesouraria`; uma pessoa pode ter mais de um), `clienteId` (o cadastro do cliente, presente quando o papel é `cliente`), `iss` e `exp`. Quem é o cliente da cota se decide só pelo `clienteId`, nunca pelo nome do usuário.

**Onde a segurança mora.** Só no `entrypoint/`: tipos e anotações de segurança (`jakarta.annotation.security.*`, `io.quarkus.security.*`, `org.eclipse.microprofile.jwt.*`, `io.smallrye.jwt.*`) não aparecem em nenhuma outra camada (regra ArchUnit `TEC-012`). O entrypoint extrai do token um `Autor` (record do domínio, em `domain/processo/`: usuário, papéis como o enum de domínio `Papel` e `clienteId` opcional, dado puro) em `AutorDoToken` e o entrega aos casos de uso. Nenhuma classe fora de `entrypoint/` vê `JsonWebToken` nem `SecurityIdentity`.

**Em camadas, nesta ordem das recusas:**

1. **401** sem token, com assinatura de outra chave, expirado, de outro emissor ou malformado (framework; o `ExceptionMappers` o devolve no formato `ErroResponse`).
2. **403 de papel, a primeira barreira:** `@RolesAllowed` em cada endpoint com os papéis do quadro "Quem pode executar cada ação"; consultas e canais são `@Authenticated` (qualquer papel); cadastros de cliente e cota, `backoffice` (decisão do coordenador, o caso de uso não trata cadastro). Conferido antes de ler o corpo e o processo.
3. **400** de formato (Bean Validation) e **404** (processo ou cota inexistente).
4. **403 de dono, de papel por situação e de R17**, no domínio e no caso de uso, com o dado: `PermissaoNegadaException` (domínio), traduzida para 403.
5. **409** de situação, conferido pela tabela do fluxo.

Em 4 e 5 a tabela confere primeiro se a linha existe (409) e depois quem pede (403): sem linha não há quem possa pedir.

**Papel que depende da situação, na tabela do fluxo.** Reprovar (backoffice em análise, aprovador em aguardando alçada) e devolver para análise (backoffice de aprovado e de aguardando vistoria, aprovador de aguardando alçada) têm em `@RolesAllowed` a **união** dos papéis; o papel certo é declarado em cada linha da tabela (`FluxoProcesso`, `.transicao(de, ação, para, Papel...)`) e `ProcessoPagamento` confere o `Autor` contra a linha, sem `if (papel == ...)`. Como a tabela guarda o papel de **todas** as linhas, o `@RolesAllowed` serve só de primeira barreira (barata, antes do corpo); a verdade é a tabela, e o teste `PapeisDoFluxoTest` a confere linha a linha. Ação da rotina de pagamento não tem papel (a tabela recusa declarar) e grava o autor `"sistema"`.

**Cliente da cota (R18).** Abrir, alterar, concordar e discordar exigem que o `clienteId` do autor seja o cliente dono da cota. Quem sabe disso é `Cota.exigirDoCliente(clienteId)`; os casos de uso, que já carregam a cota ou a carregam para isto, a chamam depois do 404 e antes do 409. Em concordar e discordar a tabela diz que a linha é do papel `cliente` (`ProcessoPagamento.exigeClienteDaCota`), então o caso de uso `ExecutarAcaoProcessoUseCase` não escolhe por ação.

**R17.** A raiz guarda o usuário da última `CONCLUIR_ANALISE` (campo `autorDaUltimaConclusaoDaAnalise`, coluna `autor_ultima_conclusao_analise`), em vez de reler o histórico; a linha `AGUARDANDO_ALCADA` + `APROVAR_NA_ALCADA` da tabela é marcada `exigindoOutraPessoaQueConcluiuAAnalise`. A comparação é por usuário. No imobiliário quem só concluiu a vistoria pode aprovar, porque só a conclusão da análise é guardada. Depois de devolver e concluir de novo, vale a nova conclusão.

**R19.** `Transicao` ganha `autor` (usuário, ou `"sistema"` nas entradas da rotina); a migration `V9` cria `processo_transicao.autor` (linhas antigas ficam `'desconhecido'`, porque foram gravadas sem identidade) e `processo_pagamento.autor_ultima_conclusao_analise`. `GET /processos/{id}/historico` devolve `autor` (mudança aditiva).

## 3. Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Basic auth com usuários em arquivo (`quarkus-security-properties-file`) | Senha por pedido, usuários e papéis no repositório ou no disco, sem emissor nem expiração, sem o `clienteId`; não se parece com a produção, e trocá-la depois exigiria refazer o contrato |
| OIDC com Keycloak e Dev Services (`quarkus-oidc`) | Exige Docker (nos ambientes de desenvolvimento e de teste daqui não há) e amarra o desenvolvimento a um servidor que não é o de produção; a verificação do token é a mesma que `quarkus-smallrye-jwt` já faz com uma chave pública, e passar a OIDC depois é configuração |
| `@RolesAllowed` com um `if (papel == ...)` no caso de uso para o papel por situação | Espalha a tabela de quem pode o quê por vários lugares; o quadro do caso de uso fica em uma só, na tabela do fluxo |
| Ler o histórico para saber quem concluiu a análise (R17) | Uma consulta a mais em toda aprovação e o histórico passaria a ser regra, não registro; o campo na raiz é lido junto com o processo |
| Identificar o cliente pelo nome de usuário | Nome não é cadastro; o dono é o `clienteId` (decisão do dono) |

## 4. Consequências

- Nova dependência `io.quarkus:quarkus-smallrye-jwt` (main). Nenhuma outra: os testes geram os tokens com o JDK.
- **Chaves.** Em `src/test/resources/aceitacao/chaves/` há um par de chaves de **teste**, no repositório só para os testes assinarem e a aplicação de teste confiar (`%test.mp.jwt.verify.publickey.location`); ele não vale em nenhum outro lugar. Para desenvolvimento local há outro par (`src/main/resources/jwt/publica-desenvolvimento.pem` e `docs/dev/privada-desenvolvimento.pem`, usado por `scripts/token-dev.sh`); também não vale fora do desenvolvimento. **Produção** configura a chave pública e o emissor do provedor (`MP_JWT_VERIFY_PUBLICKEY_LOCATION`, `MP_JWT_VERIFY_ISSUER`); sem a chave a aplicação não autentica ninguém (não há valor padrão em produção).
- Todo teste fora de `aceitacao/` que chama a API envia token (`integracao/Api` escolhe o papel certo por pedido).
- Pedido recusado por papel, dono ou R17 não muda nada e não entra no histórico.
- Processos e histórico anteriores à fatia não têm autor (`'desconhecido'`) nem a pessoa da última conclusão: R17 só vale a partir da próxima conclusão de análise.
- A comparação de R17 é por usuário (`sub`): uma pessoa com duas contas escapa dela. Tolerância de relógio na expiração e variações do emissor ficam com os padrões do `smallrye-jwt`.
- O cliente também pode ter outro papel (o mesmo usuário abre e inicia a análise); cada ação confere o papel da própria linha.

## 5. Como é verificado

- **Regra ArchUnit `TEC-012`** (`RegrasTecnologia`, instalada junto com a cópia em `.github/skills/arquiteto-padrao/assets/arquitetura/comum/`): tipos e anotações de segurança só no entrypoint.
- Testes de aceitação `PapeisEAutoriaAceitacaoTest` (tabela papel × ação, dono pelo `clienteId`, R17, ordem das recusas, autor no histórico), contra a aplicação e contra o artefato empacotado.
- JUnit puro: `PapeisDoFluxoTest` (papel por linha da tabela), `AutoriaEPapeisDoProcessoTest` (permissão por linha, R17: mesma pessoa, outra pessoa, devolveu e outra concluiu, imobiliário; autor do histórico), `ClienteCotaTest` (dono da cota); integração `AutoriaPersistenciaTest` (autor e R17 pelo banco, 401 e 403 no formato de erro).
- Revisão: a tabela do fluxo e a lista de `@RolesAllowed` são conferidas contra o quadro "Quem pode executar cada ação" do caso de uso.
