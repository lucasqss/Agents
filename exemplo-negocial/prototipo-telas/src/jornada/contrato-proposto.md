# Contrato proposto (fase 2)

O que as telas usam e o serviço ainda **não** tem. Hoje só o serviço simulado (`servicoFalso.ts`, rotas marcadas `PROPOSTA`) atende.
É a entrada da fase 2: vira regra no caso de uso (skill `especificar-caso-de-uso`, mudança de caso de uso confirmado) e, depois, código (skill `arquiteto-padrao`).
Os nomes e os formatos são proposta: o caso de uso pode mudá-los, e então o protótipo acompanha.

## Decisões do dono que as telas já seguem (a confirmar no caso de uso)

| Tema | Decisão |
|---|---|
| Tipos | Imobiliário (imóveis), auto (veículos), eletro (eletrodomésticos), serviço (serviços). Só o imobiliário tem vistoria |
| Bens | Vários por processo, todos do tipo do processo, cada um com valor; **a soma dos bens é o valor do processo** |
| Partes | Lista própria, com papel (comprador, vendedor, cônjuge, procurador...). As que recebem valor são os favorecidos (R15 continua) |
| Documentos | Cada parte envia os **próprios**, pelo link do e-mail (LGPD: só vê os dela). Os documentos dos bens são do comprador. Arquivo de verdade |
| Liberação | Nova situação **Aguardando documentos** depois da criação; com a última parte enviando, vai a **Aberto** (pronto para análise) |
| Recusa | O backoffice recusa um documento com motivo: o processo vai a pendente de documento e a parte recebe outro e-mail; com o reenvio, volta sozinho a em análise. Substitui "receber documento" manual (R10) |
| Identificação da parte | Fora do escopo: o link basta |

## Suposições das telas (o dono ainda não decidiu)

- Dados da parte: nome, CPF/CNPJ, papel, e-mail e, se recebe, o valor. Sem dados bancários.
- Exatamente um comprador por processo, e ele é o cliente da cota.
- Documentos exigidos: pessoa física (11 dígitos) identidade, comprovante de residência, certidão de estado civil; jurídica (14) contrato social e documento do representante; o comprador, por bem: imóvel matrícula e IPTU, veículo CRLV e nota ou recibo, eletro nota fiscal, serviço orçamento ou contrato.
- Campos do bem: imóvel endereço, matrícula, cartório; veículo marca e modelo, ano, placa, RENAVAM; eletro descrição, marca, modelo; serviço descrição, prestador.
- Concluir a análise exige todos os documentos aceitos.
- Arquivo: PDF, JPG ou PNG, até 10 MB.
- "Aberto" aparece como **Pronto para análise**.
- Solicitar e receber documento (manuais) saem da tela de análise.
- O cliente não vê os documentos das outras partes (403 em `GET /processos/{id}/documentos`).

## Endpoints

| Método e caminho | Quem | Corpo / resposta | Recusas |
|---|---|---|---|
| `GET /clientes/{id}/cotas` | autenticado; o cliente só as dele | `CotaDetalhe[]` (como `GET /cotas/{id}`) | 403 cliente de outra |
| `POST /processos` (aditivo) | cliente | `+ bens[{dados{...}, valor}]`, `+ partes[{nome, documento, papel, email, valor?}]`; `favorecidos` continua | 400 formato; 400 soma dos bens ≠ valor, comprador ≠ 1, parte repetida, R15; 404, 403 R18, 409 R6, 409 R13 |
| `GET /processos/{id}` | autenticado | `ProcessoResumo` + `bens[]` + `partes[]` (sem o link) | 404 |
| `GET /processos/{id}/documentos` | backoffice, aprovador, tesouraria | `[{id, parteId, codigo, texto, bem, situacao, arquivo, enviadoEm, motivoRecusa}]` | 403, 404 |
| `GET /processos/{id}/documentos/{doc}/arquivo` | idem | o arquivo | 404 |
| `POST /processos/{id}/documentos/{doc}/aceitar` | backoffice | documento | 409 fora de análise ou não enviado |
| `POST /processos/{id}/documentos/{doc}/recusar` | backoffice | `{motivo}` → documento; processo a pendente de documento; e-mail de reenvio | 400 sem motivo; 409 |
| `GET /envios/{link}` | sem login (o link) | `{processoId, tipo, situacaoDoProcesso, parte{nome, papel}, documentos[]}` só da parte | 404 link inválido |
| `POST /envios/{link}/documentos/{doc}` | sem login | multipart `arquivo` → documento | 400 tipo/tamanho; 404; 409 encerrado, ou já na análise e não recusado |
| `GET /dev/emails` | só em dev | os e-mails "enviados" | |
| `POST /dev/rotina-de-pagamento` | só no simulado | roda a rotina já | |

Situações e histórico: `AGUARDANDO_DOCUMENTOS` (nova, inicial). Entradas novas no histórico: `CRIAR`, `DOCUMENTOS_COMPLETOS` (sistema), `ACEITAR_DOCUMENTO`, `RECUSAR_DOCUMENTO`, `REENVIAR_DOCUMENTO` (autor: a parte).
