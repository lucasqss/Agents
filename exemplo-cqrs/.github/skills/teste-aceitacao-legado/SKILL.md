---
name: teste-aceitacao-legado
description: Cria testes de aceitação pelos endpoints HTTP de um sistema Quarkus que já existe e não tem requisito escrito. As regras de negócio são lidas do código atual (teste de caracterização) e confirmadas pelo usuário. Gera o par *Test (@QuarkusTest) e *IT (@QuarkusIntegrationTest). Use para proteger um legado antes de mudá-lo. Se houver requisito, caso de uso ou critério de aceite, use teste-aceitacao-modelos.
---

# Teste de aceitação de legado

Você vai ler o código para descobrir as regras de negócio. Depois, vai escrever testes que acionam **só os endpoints HTTP**, como um usuário faria.

O código é a fonte das regras, mas pode estar errado. Por isso, **nenhuma regra vira teste antes de o usuário confirmar o plano**.

O trabalho tem duas fases e um ponto de parada entre elas:

- **Fase A:** ler o código e montar o plano. Termina pedindo aprovação.
- **Parada:** espere o usuário responder.
- **Fase B:** escrever e rodar os testes do plano aprovado.

Siga os passos na ordem. Em cada passo com "Leia agora", leia o arquivo antes de continuar.

## Fase A: ler o código e montar o plano

**A1.** Entre em modo plan (`EnterPlanMode`). Na fase A você não cria nem altera arquivos do projeto.

**A2.** Leia agora: [references/fase-a-ler-codigo.md](references/fase-a-ler-codigo.md). Rode o comando que lista os endpoints e escolha os da capacidade pedida.

**A3.** Para **cada endpoint, um por vez**, siga o código até as regras e os suspeitos, como manda o `fase-a-ler-codigo.md`. Cada regra leva origem (`arquivo:linha`, tirada do `grep -n`) e o trecho copiado da linha.

**A4.** Leia agora: [references/criterios.md](references/criterios.md). Aplique os checklists 1 a 4 às regras e derive os casos.

**A5.** Leia agora: [references/gabarito-caso-de-uso.md](references/gabarito-caso-de-uso.md). Copie o gabarito e preencha todas as seções. Esse é o plano.

**A6.** Faça o "Antes de apresentar o plano, confira" do gabarito. Depois peça aprovação com `ExitPlanMode`. Termine com a frase:

> Responda os suspeitos S1..Sn (fixar como está ou defeito) e as dúvidas D1..Dn. Só depois eu escrevo os testes.

**Pare aqui.** Mesmo com pressa, mesmo com o plano curto, mesmo com o pedido "vá direto": espere a resposta.

**Sem modo plan:** grave o plano em `src/test/resources/aceitacao/<capacidade>/caso-de-uso.md` com `Situação: RASCUNHO`, mostre o conteúdo e pare. Esse é o único arquivo permitido na fase A.

## Fase B: escrever e rodar os testes

Comece só depois que o usuário aprovar o plano e responder os suspeitos. Se faltar resposta, pergunte de novo e espere.

**B1.** Grave o plano aprovado em `src/test/resources/aceitacao/<capacidade>/caso-de-uso.md`:
- com as respostas do usuário;
- com `Situação: CONFIRMADO`;
- sem os casos que o usuário cortou.

Daqui em diante, o resultado esperado de cada caso vem **desse arquivo**. Não volte ao código de produção para decidir um resultado esperado.

**B2.** Leia agora: [references/fase-b-escrever-testes.md](references/fase-b-escrever-testes.md). Confira o pom e os atalhos que já existem.

**B3.** Escreva o `*Test`, o `*IT` e, se precisar, o `ApiAceitacao` e o stub, seguindo os esqueletos. Um método (ou uma linha de `@ParameterizedTest`) por caso do plano, com o comentário `// Cn (Rn)`.

Quando a resposta do usuário a um suspeito for:
- **fixar como está:** o teste espera o comportamento atual e o `@DisplayName` começa com `[atual]`;
- **é defeito:** o teste espera o resultado correto que o usuário indicou.

**B4.** Rode `./mvnw verify`.

**B5.** Se falhar, classifique pela tabela do `fase-b-escrever-testes.md`:
- **Falha de mecânica** (compilação, JSON, dados): corrija o teste.
- **Divergência do plano:** registre o defeito, deixe vermelho, não faça commit e relate ao usuário.

**B6.** Faça a sessão exploratória do `fase-b-escrever-testes.md` e anote na seção 9 do `caso-de-uso.md`.

**B7.** Faça o "Antes de terminar" do `fase-b-escrever-testes.md` e relate:
- os testes criados;
- os defeitos;
- os suspeitos fixados como estão;
- as dúvidas abertas;
- o resultado da exploração.

## Nunca

1. Criar arquivo de teste antes de o usuário aprovar o plano e responder os suspeitos.
2. Transformar suspeito ou dúvida sem resposta em asserção.
3. Mudar o resultado esperado de um caso para o teste passar. Mudar o esperado exige nova aprovação do usuário.
4. Usar nos testes `@Inject`, `@InjectMock`, `QuarkusMock`, acesso ao banco ou import de classe de `src/main`.
5. Aceitar mais de um resultado num caso (`anyOf(400, 409)`, "qualquer 2xx").
6. Conferir só o status, sem verificar o efeito em outro endpoint.
7. Tirar regra ou resultado esperado de outros testes. Deles, só se copia a mecânica.

## Onde fica cada coisa

| O quê | Onde |
|---|---|
| Plano e rastreabilidade | `src/test/resources/aceitacao/<capacidade>/caso-de-uso.md` |
| Testes | `src/test/java/<pacote-base>/aceitacao/<capacidade>/<Capacidade>AceitacaoTest.java` e `...IT.java` |
| Atalhos HTTP e stubs | `src/test/java/<pacote-base>/aceitacao/` |

`<capacidade>` é o nome do negócio, nunca o de uma camada (`cadastrocliente`, e não `controller`).

---

Esta skill é irmã de `teste-aceitacao-modelos`, que parte do requisito. Os critérios e a mecânica Quarkus são os mesmos: ao mudar um deles lá, mude aqui também. Fontes: Feathers, *Working Effectively with Legacy Code*; Myers, Sandler e Badgett, *The Art of Software Testing*; Ammann e Offutt, *Introduction to Software Testing*.
