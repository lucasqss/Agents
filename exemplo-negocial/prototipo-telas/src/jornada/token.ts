/**
 * Token JWT de DESENVOLVIMENTO para a persona, com os mesmos claims do scripts/token-dev.sh (ADR 003):
 * iss, sub, upn, groups, clienteId (só da cliente), iat, exp.
 *
 * - Modo real: assinado em RS256 no navegador com docs/dev/privada-desenvolvimento.pem, o par da chave pública de %dev.
 *   Só vale contra o serviço em quarkus:dev. Produção usa o provedor de identidade.
 * - Modo simulado: assinatura falsa (o serviço simulado só lê o conteúdo). A chave privada nem entra no build.
 */
import { MODO } from "../api/api";

const EMISSOR = "https://exemplo-negocial";
const cache = new Map<string, { token: string; expira: number }>();

const base64url = (bytes: Uint8Array) =>
  btoa(String.fromCharCode(...bytes)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
const texto64 = (s: string) => base64url(new TextEncoder().encode(s));

let chave: Promise<CryptoKey> | null = null;

function chaveDeDesenvolvimento(): Promise<CryptoKey> {
  chave ??= import("../../../docs/dev/privada-desenvolvimento.pem?raw").then(({ default: pem }) => {
    const der = Uint8Array.from(atob(pem.replace(/-----[^-]+-----|\s/g, "")), (c) => c.charCodeAt(0));
    return crypto.subtle.importKey("pkcs8", der, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"]);
  });
  return chave;
}

export async function tokenDe(usuario: string, papeis: string[], clienteId?: number): Promise<string> {
  const chaveDoCache = `${usuario}|${papeis.join(",")}|${clienteId ?? ""}`;
  const agora = Math.floor(Date.now() / 1000);
  const guardado = cache.get(chaveDoCache);
  if (guardado && guardado.expira - agora > 60) return guardado.token;

  const corpo = { iss: EMISSOR, sub: usuario, upn: usuario, groups: papeis, ...(clienteId ? { clienteId } : {}), iat: agora, exp: agora + 8 * 3600 };
  const conteudo = `${texto64(JSON.stringify({ alg: "RS256", typ: "JWT" }))}.${texto64(JSON.stringify(corpo))}`;
  let assinatura: string;
  if (import.meta.env.VITE_MODO === "falso" || MODO === "falso") {
    assinatura = "assinatura-simulada";
  } else {
    const bytes = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", await chaveDeDesenvolvimento(), new TextEncoder().encode(conteudo));
    assinatura = base64url(new Uint8Array(bytes));
  }
  const token = `${conteudo}.${assinatura}`;
  cache.set(chaveDoCache, { token, expira: corpo.exp });
  return token;
}

/** Lê o conteúdo de um token, sem conferir a assinatura (para o serviço simulado). */
export function lerToken(token: string): { sub?: string; groups?: string[]; clienteId?: number } | null {
  try {
    const parte = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(new TextDecoder().decode(Uint8Array.from(atob(parte), (c) => c.charCodeAt(0))));
  } catch {
    return null;
  }
}
