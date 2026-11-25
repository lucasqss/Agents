# Tarefa: tela "Fila de trabalho"

O protótipo já tem a lista "Processos", a tela "Abrir processo" e o detalhe do processo. Falta a **fila de trabalho**: uma tela para agir em vários processos sem abrir um por um.

## O que o negócio quer

1. Um item novo no menu, **"Fila de trabalho"**, que abre a tela (rota `#/fila`) com o título "Fila de trabalho".
2. A fila mostra os processos que este navegador abriu ou consultou (os mesmos da tela inicial), separados em duas abas: **"Em andamento"** e **"Finalizados"**. Finalizado é o processo que está numa situação final: Pago, Reprovado ou Cancelado.
3. Cada aba é uma tabela com o número do processo (link para o detalhe dele), a cota, a situação e **um botão por ação que o processo aceita agora**, com o texto da ação em linguagem de negócio (ex.: "Iniciar a análise", "Reprovar"). As ações que exigem motivo abrem o diálogo de motivo, com o botão "Confirmar".
4. Depois de executar uma ação, mostrar o aviso `<Ação>: feito. O processo agora está <Situação>.` (ex.: "Reprovar: feito. O processo agora está Reprovado."). Se a API recusar: `<Ação>: não foi possível. <mensagem da API>`. A fila se atualiza: o processo que chegou a uma situação final passa para "Finalizados".
5. Quem age é a pessoa escolhida em "Quem sou eu".
6. Aba vazia: "Nenhum processo em andamento." ou "Nenhum processo finalizado.".

## Como saber se está pronto

- `npm run verificar` sem erro.
- A tela funciona no modo simulado (`npm run dev:falso`) com processos abertos pela tela "Abrir processo".

Não rode `git` e não mexa fora desta pasta.
