// @vitest-environment node
// No ambiente node (não jsdom): Request, FormData e File nativos e compatíveis entre si, como no navegador.
import { afterEach, describe, expect, it, vi } from "vitest";
import { erroSimulado, fetchSimulado, type RotaSimulada } from "./simulado";

const rotas: RotaSimulada[] = [
  { metodo: "GET", caminho: "/itens/:id", tratar: ({ parametros, busca }) => ({ status: 200, resposta: { id: parametros["id"], filtro: busca.get("f") } }) },
  { metodo: "POST", caminho: "/itens", tratar: ({ corpo, cabecalhos }) => ({ status: 201, resposta: { recebi: corpo, quem: cabecalhos["x-usuario"] } }) },
  { metodo: "POST", caminho: "/arquivos", tratar: ({ corpo }) => ({ status: 201, resposta: { nome: ((corpo as { arquivo: File }).arquivo).name } }) },
  { metodo: "DELETE", caminho: "/itens/:id", tratar: () => erroSimulado(409, "Não pode") },
];
const pedido = (metodo: string, caminho: string, init: RequestInit = {}) => new Request(`http://simulado${caminho}`, { method: metodo, ...init });

describe("fetchSimulado", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("casa caminho com parâmetros e lê a busca", async () => {
    const r = await fetchSimulado(pedido("GET", "/itens/12?f=x"), rotas);
    expect(r.status).toBe(200);
    expect(await r.json()).toEqual({ id: "12", filtro: "x" });
  });

  it("lê corpo JSON e cabeçalhos", async () => {
    const r = await fetchSimulado(pedido("POST", "/itens", { body: JSON.stringify({ a: 1 }), headers: { "Content-Type": "application/json", "X-Usuario": "ana" } }), rotas);
    expect(await r.json()).toEqual({ recebi: { a: 1 }, quem: "ana" });
  });

  it("lê multipart com arquivo", async () => {
    const formulario = new FormData();
    formulario.append("arquivo", new File(["%PDF"], "doc.pdf"));
    const r = await fetchSimulado(pedido("POST", "/arquivos", { body: formulario }), rotas);
    expect(await r.json()).toEqual({ nome: "doc.pdf" });
  });

  it("erro da rota sai no formato da API", async () => {
    const r = await fetchSimulado(pedido("DELETE", "/itens/1"), rotas);
    expect(r.status).toBe(409);
    expect(await r.json()).toEqual({ mensagem: "Não pode", campos: [] });
  });

  it("rota que não existe: 404 no formato da API", async () => {
    const r = await fetchSimulado(pedido("GET", "/nada"), rotas);
    expect(r.status).toBe(404);
    expect(((await r.json()) as { mensagem: string }).mensagem).toContain("Rota simulada inexistente");
  });

  it("sem rede ligado: lança como o fetch de verdade", async () => {
    vi.stubGlobal("localStorage", { getItem: (k: string) => (k === "simulado:sem-rede" ? "1" : null) });
    await expect(fetchSimulado(pedido("GET", "/itens/1"), rotas)).rejects.toThrow(TypeError);
  });
});
