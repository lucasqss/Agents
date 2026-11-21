# Protótipo de telas: pagamento de consórcio (rascunho)

Telas de **rascunho** sobre a API do `exemplo-negocial`, para conversar com o negócio sobre as jornadas e para testar a API sem montar JSON à mão. **Não é o produto final.**

A tela inicial escolhe uma de três jornadas, e cada jornada tem as suas telas e as suas personas:

| Jornada | Quem atua | Telas |
|---|---|---|
| 1. Criar o processo | Maria, compradora e cliente da cota | Assistente: cota, tipo, bens, partes, revisão; depois, os e-mails enviados às partes |
| 2. Enviar os documentos | Cada parte, pelo link do e-mail (sem login) | Caixa de e-mail simulada; página da parte, só com os documentos dela |
| 3. Analisar | Ana e Carlos (backoffice), Bruno (aprovador), Tesouraria | Fila por situação; processo com documentos (ver, aceitar, recusar), ações, bens, partes e histórico |

Feito a partir do modelo `exemplo-testes/prototipo-telas` (skill `prototipo-de-jornada`).

## Como subir

```bash
./subir.sh falso    # só as telas, com o serviço simulado no navegador (sem Java, sem Docker)
./subir.sh          # o serviço em modo dev + as telas contra ele (JDK 25 e Docker)
```

Abra http://localhost:5173 e clique em **Preparar demonstração**: ele cadastra a cliente Maria Souza e duas cotas dela (R$ 150.000,00 e R$ 40.000,00).

| Comando | O que faz |
|---|---|
| `npm run dev` / `npm run dev:falso` | Telas contra o serviço real (`VITE_API`, padrão `http://localhost:8080`) / simulado |
| `npm run build` / `npm run build:falso` | Typecheck + build |
| `npm run test:e2e` | As 3 jornadas no Playwright, contra o serviço simulado, no computador e no celular |

## O que já existe no serviço e o que é proposta

Bens, partes, tipos auto e eletro, documentos por parte, e-mails e a situação "Aguardando documentos" **ainda não existem no serviço**. As telas os usam pelo serviço simulado. O que falta está em [`src/jornada/contrato-proposto.md`](src/jornada/contrato-proposto.md), que é a entrada da fase 2 (caso de uso, depois os endpoints).

- **Modo simulado:** as 3 jornadas funcionam inteiras.
- **Modo real:** funciona o que o serviço já tem: preparar demonstração (cadastro), fila, ações do fluxo com papéis e R17, histórico. Criar processo com bens e partes, e documentos, só depois da fase 2.

## Como é feito

- **Identidade (ADR 003):** cada persona chama a API com um token JWT de desenvolvimento, com os mesmos claims do `scripts/token-dev.sh`. No modo real, ele é assinado no navegador com `docs/dev/privada-desenvolvimento.pem`, que só vale em `quarkus:dev`. No modo simulado, a assinatura é falsa e a chave nem entra no build.
- **Serviço simulado** (`src/jornada/servicoFalso.ts`): é uma **cópia** da tabela do fluxo (papéis por linha, R6, R13, R15, R17, a ordem das recusas da ADR 003) e das rotas propostas. A regra que vale é a do domínio; se divergir, o serviço está certo. Os arquivos enviados ficam só na memória e somem ao recarregar a página.
- **Genérico × deste projeto:**
  - `src/api`, `src/apoio` e `src/componentes` são iguais aos do modelo;
  - `src/jornada`, `src/telas/<jornada>/`, `App.tsx` e `e2e/` são deste projeto.
- **No serviço:** só o CORS, em `%dev` (`application.properties`).
