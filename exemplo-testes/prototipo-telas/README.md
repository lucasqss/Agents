# Protótipo de telas (rascunho)

Telas de **rascunho** sobre a API do `exemplo-testes`. Servem para duas coisas:

- conversar com o negócio sobre as jornadas, com telas de verdade, sem Swagger;
- apoiar o teste manual da API: preencher e acionar os endpoints sem montar JSON à mão.

**Não é o produto final:** um selo "Protótipo" na barra lateral lembra isso, sem dominar a tela.

## Como subir

Requisitos: JDK 25, Docker (o Dev Services sobe o PostgreSQL) e python3.

```bash
./subir.sh                    # sobe a API em modo dev e as telas; Ctrl+C derruba os dois
```

Ou em dois terminais:

```bash
cd exemplo-testes && ./mvnw quarkus:dev
cd exemplo-testes/prototipo-telas && python3 -m http.server 5173
```

Depois, abra http://localhost:5173.

## A jornada que dá para mostrar

1. **Persona:** escolha quem você é na barra lateral, em "Quem sou eu" (Ana, analista; Vitor, vistoriador; Carla, aprovadora; Tesouraria). O nome vai no cabeçalho `X-Usuario` e aparece no histórico.
2. **Abrir:** abra um processo imobiliário de R$ 80.000,00.
3. **Ana:** inicia e conclui a análise. O caminho mostra que o imobiliário vai para a vistoria.
4. **Vitor:** conclui a vistoria. Como o valor está acima da alçada, o processo vai para "Aguardando alçada".
5. **Carla:** aprova na alçada.
6. **Tesouraria:** paga.
7. **Histórico:** cada passo, com quem fez e quando.

Para mostrar as regras na prática:
- "Reprovar" e "Cancelar" pedem motivo, que entra no histórico.
- Um serviço de R$ 50.000,00 é aprovado direto na análise.
- O rodapé "Por trás da tela" mostra a requisição e a resposta da última chamada.

## Como é feito, e por que fica fora do serviço

- **Sem build:** HTML, CSS e JavaScript puro (`index.html`, `estilo.css`, `app.js`, `config.js`). Sem npm, sem framework e sem dependência externa (nem CDN). Altere e recarregue a página.
- **Visual em um lugar só:** o tema escuro é definido pelas variáveis no topo de `estilo.css` (superfícies, texto, destaque, estados, raio e espaçamento). Para trocar as cores, ou criar um tema claro, mude só esse bloco; o comentário ali mostra como.
- **Poucos componentes, reutilizados:** `cartao`, `botao` (`principal`, `perigo`), `campo`, `etiqueta`, `aviso`, `tabela`, `caminho`, `linha-do-tempo`, `dialogo`. Tela nova se monta com eles em `app.js`.
- **Celular:** a barra lateral vira barra de topo, e o caminho do processo vira uma lista vertical.
- **Fora do jar:** a pasta fica fora de `src/`, então o Maven nem a vê.
- **Mudança no serviço:** só o CORS, que vale apenas no perfil `%dev` (`application.properties`). Em teste e em produção, nada muda.
- **Outro ambiente:** troque a URL em `config.js`. O ambiente precisa liberar CORS para esta página.
- **Lista de processos:** a API não tem listagem, então os "Recentes" são os processos que este navegador abriu ou consultou (`localStorage`).
- **Perfis:** todos veem todos os botões da situação. O perfil de cada pessoa ainda vai ser definido; quando existir na API, os botões passam a vir filtrados.
