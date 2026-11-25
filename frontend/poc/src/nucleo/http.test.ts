import { describe, expect, it, vi } from "vitest";
import { comoFormulario, podeTentarDeNovo, proposta, resposta, todas, type Resposta } from "./http";

const json = (status: number, corpo?: unknown) => new Response(corpo === undefined ? null : JSON.stringify(corpo), { status });

describe("resposta", () => {
  it("2xx vira ok com os dados", async () => {
    expect(await resposta(Promise.resolve({ data: { id: 1 }, response: json(200, { id: 1 }) }))).toEqual({ ok: true, status: 200, dados: { id: 1 } });
  });

  it("erro com corpo usa a mensagem e os campos da API", async () => {
    const r = await resposta(Promise.resolve({ error: { mensagem: "Dados inválidos", campos: [{ campo: "valor", erro: "obrigatório" }] }, response: json(400) }));
    expect(r).toEqual({ ok: false, status: 400, erro: { mensagem: "Dados inválidos", campos: [{ campo: "valor", erro: "obrigatório" }] } });
  });

  it.each([[401, "Você precisa se identificar"], [403, "permissão"], [503, "Tente de novo"]])("%s sem corpo ganha mensagem clara", async (status, trecho) => {
    const r = await resposta(Promise.resolve({ error: undefined, response: json(status) }));
    expect(r.ok).toBe(false);
    if (!r.ok) expect(r.erro.mensagem).toContain(trecho);
  });

  it("falha de rede vira status 0, sem lançar", async () => {
    const r = await resposta(Promise.reject(new TypeError("Failed to fetch")));
    expect(r.ok).toBe(false);
    if (!r.ok) {
      expect(r.status).toBe(0);
      expect(r.erro.mensagem).toContain("Sem resposta");
    }
  });
});

describe("todas", () => {
  const ok = <T,>(dados: T): Promise<Resposta<T>> => Promise.resolve({ ok: true, status: 200, dados });
  const falha = (status: number, mensagem: string): Promise<Resposta<never>> => Promise.resolve({ ok: false, status, erro: { mensagem, campos: [] } });

  it("junta os dados com as mesmas chaves", async () => {
    expect(await todas({ a: ok(1), b: ok("x") })).toEqual({ ok: true, status: 200, dados: { a: 1, b: "x" } });
  });

  it("falha com o erro da primeira que falhou", async () => {
    expect(await todas({ a: ok(1), b: falha(404, "não achei"), c: falha(500, "quebrou") })).toEqual({ ok: false, status: 404, erro: { mensagem: "não achei", campos: [] } });
  });
});

describe("proposta (modo real nos testes)", () => {
  it("operação pendente não chama o serviço: 501", async () => {
    const chamar = vi.fn();
    const r = await proposta(true, chamar);
    expect(chamar).not.toHaveBeenCalled();
    expect(r).toMatchObject({ ok: false, status: 501 });
  });

  it("operação já publicada chama normalmente", async () => {
    const r = await proposta(false, () => Promise.resolve<Resposta<number>>({ ok: true, status: 200, dados: 7 }));
    expect(r).toEqual({ ok: true, status: 200, dados: 7 });
  });
});

describe("comoFormulario", () => {
  it("arquivo vai como arquivo, o resto como texto, e nulos ficam de fora", () => {
    const arquivo = new File(["oi"], "a.pdf");
    const f = comoFormulario({ tipo: "OUTRO", numero: 3, arquivo, nada: null });
    expect(f.get("tipo")).toBe("OUTRO");
    expect(f.get("numero")).toBe("3");
    expect((f.get("arquivo") as File).name).toBe("a.pdf");
    expect(f.has("nada")).toBe(false);
  });
});

it("podeTentarDeNovo: rede e 5xx sim; 4xx e 501 (proposta) não", () => {
  expect([0, 500, 503].map(podeTentarDeNovo)).toEqual([true, true, true]);
  expect([400, 404, 409, 501].map(podeTentarDeNovo)).toEqual([false, false, false, false]);
});
