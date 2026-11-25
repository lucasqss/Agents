/**
 * Identidade de quem usa o protótipo. Dois jeitos, conforme o serviço:
 *   - cabeçalho do contrato (ex.: X-Usuario): a camada api o passa como parâmetro tipado (é o caso desta POC);
 *   - token (Authorization: Bearer): o projeto registra em main.tsx, uma vez:
 *       definirIdentidade(async () => ({ Authorization: `Bearer ${await tokenDeDesenvolvimento({ ... })}` }));
 * O cliente HTTP do núcleo põe esses cabeçalhos em toda chamada (sem sobrescrever o que a chamada já trouxe).
 */
import { MODO } from "./modo";

type Cabecalhos = Record<string, string>;
let fonte: () => Cabecalhos | Promise<Cabecalhos> = () => ({});

export function definirIdentidade(cabecalhos: () => Cabecalhos | Promise<Cabecalhos>) {
  fonte = cabecalhos;
}

export async function cabecalhosDeIdentidade(): Promise<Cabecalhos> {
  return await fonte();
}

const base64url = (bytes: Uint8Array) => btoa(String.fromCharCode(...bytes)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
const texto64 = (s: string) => base64url(new TextEncoder().encode(s));

const cache = new Map<string, { token: string; expira: number }>();

/**
 * Token JWT de DESENVOLVIMENTO (nunca de produção) com os claims que o serviço espera.
 *   - modo simulado: assinatura falsa (o simulado só lê o conteúdo, com lerToken);
 *   - modo real: assinado em RS256 com a chave privada de desenvolvimento do serviço (`chavePrivadaPem`, o par da
 *     chave pública do perfil %dev). Sem a chave, o modo real não consegue token: o serviço recusa com 401.
 */
export async function tokenDeDesenvolvimento(claims: { iss: string; sub: string; groups: string[] } & Record<string, unknown>,
  chavePrivadaPem?: () => Promise<string>): Promise<string> {
  const chave = JSON.stringify(claims);
  const agora = Math.floor(Date.now() / 1000);
  const guardado = cache.get(chave);
  if (guardado && guardado.expira - agora > 60) return guardado.token;

  const corpo = { upn: claims.sub, ...claims, iat: agora, exp: agora + 8 * 3600 };
  const conteudo = `${texto64(JSON.stringify({ alg: "RS256", typ: "JWT" }))}.${texto64(JSON.stringify(corpo))}`;
  let assinatura = "assinatura-simulada";
  if (MODO === "real" && chavePrivadaPem) {
    const pem = await chavePrivadaPem();
    const der = Uint8Array.from(atob(pem.replace(/-----[^-]+-----|\s/g, "")), (c) => c.charCodeAt(0));
    const privada = await crypto.subtle.importKey("pkcs8", der, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"]);
    assinatura = base64url(new Uint8Array(await crypto.subtle.sign("RSASSA-PKCS1-v1_5", privada, new TextEncoder().encode(conteudo))));
  }
  const token = `${conteudo}.${assinatura}`;
  cache.set(chave, { token, expira: corpo.exp });
  return token;
}

/** O conteúdo de um token, sem conferir a assinatura (para o serviço simulado saber quem é). */
export function lerToken(token: string): Record<string, unknown> | null {
  try {
    const parte = (token.split(".")[1] ?? "").replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(new TextDecoder().decode(Uint8Array.from(atob(parte), (c) => c.charCodeAt(0)))) as Record<string, unknown>;
  } catch {
    return null;
  }
}
