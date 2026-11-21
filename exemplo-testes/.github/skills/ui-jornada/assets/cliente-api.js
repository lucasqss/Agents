// Modelo: cliente de um recurso REST (copie para api/<recurso>.js e troque RECURSO).
// Regras: só aqui existe o caminho do endpoint; só importa nucleo/; cada função devolve { ok, status, corpo }.
import { chamar } from "../nucleo/http.js";

const API = window.PROTOTIPO.api;           // em outro framework: a configuração de ambiente
const caminho = (id, resto = "") => `/RECURSO/${encodeURIComponent(id)}${resto}`;

// Nomeie pela operação de negócio, não pelo verbo HTTP.
export const listar = () => chamar(API, "GET", "/RECURSO");
export const consultar = (id) => chamar(API, "GET", caminho(id));
export const criar = (dados) => chamar(API, "POST", "/RECURSO", dados);
export const executar = (id, acao, motivo) =>
  chamar(API, "POST", caminho(id, `/acoes/${encodeURIComponent(acao)}`), motivo === undefined ? undefined : { motivo });
