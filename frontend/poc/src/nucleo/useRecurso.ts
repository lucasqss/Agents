import { useCallback, useEffect, useRef, useState } from "react";
import type { ErroApi, Resposta } from "./http";

export type Recurso<T> =
  | { situacao: "carregando"; recarregar: () => void }
  | { situacao: "ok"; dados: T; atualizando: boolean; recarregar: () => void }
  | { situacao: "erro"; erro: ErroApi; status: number; recarregar: () => void };

/**
 * O jeito único de uma tela buscar dados. Trata carregando, erro, corrida (resposta velha não sobrescreve a nova)
 * e recarga, sem a tela escrever useEffect.
 *
 *   const recurso = useRecurso(`processo:${id}`, () => todas({ processo: processos.consultar(id), ... }));
 *   if (recurso.situacao === "carregando") ...; if (recurso.situacao === "erro") ...; recurso.dados...
 *
 * `chave`: quando muda, busca de novo (ponha nela tudo de que a busca depende: id, filtro). `recarregar()`: busca de
 * novo com a mesma chave (ex.: depois de uma ação); os dados antigos continuam na tela até chegar a resposta.
 */
export function useRecurso<T>(chave: string, buscar: () => Promise<Resposta<T>>): Recurso<T> {
  const [versao, setVersao] = useState(0);
  const [ultimo, setUltimo] = useState<{ chave: string; versao: number; resposta: Resposta<T> } | null>(null);
  const buscarAtual = useRef(buscar);

  useEffect(() => {
    buscarAtual.current = buscar;
  });

  useEffect(() => {
    let vivo = true;
    void buscarAtual.current().then((resposta) => {
      if (vivo) setUltimo({ chave, versao, resposta });
    });
    return () => {
      vivo = false;
    };
  }, [chave, versao]);

  const recarregar = useCallback(() => setVersao((v) => v + 1), []);

  if (!ultimo || ultimo.chave !== chave) return { situacao: "carregando", recarregar };
  const r = ultimo.resposta;
  if (!r.ok) return { situacao: "erro", erro: r.erro, status: r.status, recarregar };
  return { situacao: "ok", dados: r.dados, atualizando: ultimo.versao !== versao, recarregar };
}
