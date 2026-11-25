import { describe, expect, it } from "vitest";
import { cabecalhosDeIdentidade, definirIdentidade, lerToken, tokenDeDesenvolvimento } from "./identidade";

describe("identidade", () => {
  it("o projeto registra os cabeçalhos uma vez, e o núcleo os lê em cada chamada", async () => {
    expect(await cabecalhosDeIdentidade()).toEqual({});
    definirIdentidade(() => Promise.resolve({ Authorization: "Bearer x" }));
    expect(await cabecalhosDeIdentidade()).toEqual({ Authorization: "Bearer x" });
    definirIdentidade(() => ({}));
  });

  it("token de desenvolvimento leva os claims, e lerToken os devolve", async () => {
    const token = await tokenDeDesenvolvimento({ iss: "https://exemplo", sub: "ana", groups: ["analista"], clienteId: 7 });
    expect(token.split(".")).toHaveLength(3);
    expect(lerToken(token)).toMatchObject({ iss: "https://exemplo", sub: "ana", upn: "ana", groups: ["analista"], clienteId: 7 });
  });

  it("lerToken de lixo devolve nulo", () => expect(lerToken("não é token")).toBeNull());
});
