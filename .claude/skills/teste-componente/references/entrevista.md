# Roteiro de entrevista (componente)

Poucas perguntas por vez, das que mais mudam o plano para as que menos mudam.

## O que mudou no serviço

- Endpoint novo ou alterado? Método, caminho, campos obrigatórios e limites de cada campo.
- Tabela ou coluna nova? Algum campo pode ficar vazio?
- Integração nova ou alterada? O que o sistema externo devolve quando dá certo, quando recusa e quando não responde? Qual o tempo limite?
- Papel novo, ou endpoint com quem pode chamar diferente?

## Respostas de erro

- Para cada recusa, qual status e qual mensagem o cliente deve ver? (`404` inexistente, `409` conflito, `403` sem permissão, `503` sistema externo fora.)
- Há operação que dois usuários podem fazer ao mesmo tempo sobre o mesmo registro? O que deve acontecer com o segundo?

## O que já existe

- Quais testes de componente já cobrem esse endpoint ou integração?
- A regra de negócio já está provada na unidade? (Se não, ela vai para a `teste-unidade` antes.)

## Encerramento

Repita o plano como você entendeu e pergunte: "Falta algum erro, papel ou limite?" O que ficou sem resposta vai para **Dúvidas**.
