import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { Resposta } from "./http";
import { useRecurso } from "./useRecurso";

/** Uma resposta que o teste resolve quando quiser (para provar a ordem). */
function adiada<T>() {
  let resolver!: (r: Resposta<T>) => void;
  const promessa = new Promise<Resposta<T>>((r) => (resolver = r));
  return { promessa, ok: (dados: T) => resolver({ ok: true, status: 200, dados }) };
}

describe("useRecurso", () => {
  it("começa carregando e mostra os dados", async () => {
    const { result } = renderHook(() => useRecurso("a", () => Promise.resolve<Resposta<number>>({ ok: true, status: 200, dados: 1 })));
    expect(result.current.situacao).toBe("carregando");
    await waitFor(() => expect(result.current).toMatchObject({ situacao: "ok", dados: 1, atualizando: false }));
  });

  it("erro da API vira situação de erro com o status", async () => {
    const { result } = renderHook(() => useRecurso("a", () => Promise.resolve<Resposta<number>>({ ok: false, status: 404, erro: { mensagem: "não achei", campos: [] } })));
    await waitFor(() => expect(result.current).toMatchObject({ situacao: "erro", status: 404, erro: { mensagem: "não achei" } }));
  });

  it("corrida: a resposta da chave velha não sobrescreve a da nova", async () => {
    const lenta = adiada<string>();
    const rapida = adiada<string>();
    const { result, rerender } = renderHook(({ chave }) => useRecurso(chave, () => (chave === "velha" ? lenta.promessa : rapida.promessa)), { initialProps: { chave: "velha" } });
    rerender({ chave: "nova" });
    await act(() => Promise.resolve(rapida.ok("nova")));
    await act(() => Promise.resolve(lenta.ok("velha")));
    expect(result.current).toMatchObject({ situacao: "ok", dados: "nova" });
  });

  it("trocar a chave volta a carregando", async () => {
    let n = 0;
    const { result, rerender } = renderHook(({ chave }) => useRecurso(chave, () => Promise.resolve<Resposta<number>>({ ok: true, status: 200, dados: ++n })), { initialProps: { chave: "1" } });
    await waitFor(() => expect(result.current.situacao).toBe("ok"));
    rerender({ chave: "2" });
    expect(result.current.situacao).toBe("carregando");
    await waitFor(() => expect(result.current).toMatchObject({ situacao: "ok", dados: 2 }));
  });

  it("recarregar mantém os dados na tela (atualizando) até chegar a resposta nova", async () => {
    const respostas = [adiada<string>(), adiada<string>()];
    let i = 0;
    const { result } = renderHook(() => useRecurso("a", () => respostas[i++]!.promessa));
    await act(() => Promise.resolve(respostas[0]!.ok("primeira")));
    act(() => result.current.recarregar());
    expect(result.current).toMatchObject({ situacao: "ok", dados: "primeira", atualizando: true });
    await act(() => Promise.resolve(respostas[1]!.ok("segunda")));
    expect(result.current).toMatchObject({ situacao: "ok", dados: "segunda", atualizando: false });
  });
});
