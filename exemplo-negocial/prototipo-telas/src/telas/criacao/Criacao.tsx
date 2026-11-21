import { useEffect, useState } from "react";
import { ir } from "../../apoio/rotas";
import { Passos } from "../../componentes";
import { cotas, type Cota } from "../../jornada/api";
import { rascunho } from "../../jornada/estado";
import { PassoBens } from "./PassoBens";
import { PassoCota } from "./PassoCota";
import { PassoPartes } from "./PassoPartes";
import { PassoRevisao } from "./PassoRevisao";
import { PassoTipo } from "./PassoTipo";

export const PASSOS_DA_CRIACAO = [
  { chave: "cota", nome: "Cota" },
  { chave: "tipo", nome: "Tipo" },
  { chave: "bens", nome: "Bens" },
  { chave: "partes", nome: "Partes" },
  { chave: "revisao", nome: "Revisão" },
] as const;
export type PassoDaCriacao = (typeof PASSOS_DA_CRIACAO)[number]["chave"];

/** O que cada passo recebe: a cota escolhida (com o crédito disponível) e como andar. */
export interface PropsDoPasso { cota: Cota | null; avancar: () => void; voltar: () => void }

/** Assistente da criação. O rascunho fica guardado no navegador: dá para sair e voltar sem perder. */
export function Criacao({ passo }: { passo: PassoDaCriacao }) {
  const r = rascunho.usar();
  const [cota, setCota] = useState<Cota | null>(null);
  const indice = PASSOS_DA_CRIACAO.findIndex((p) => p.chave === passo);

  // Passo que ainda não pode ser feito (sem cota, sem tipo) volta para o primeiro que falta.
  const primeiroQueFalta = !r.cotaId ? 0 : !r.tipo ? 1 : !r.bens.length ? 2 : !r.partes.length ? 3 : 4;
  useEffect(() => {
    if (indice > primeiroQueFalta) ir(`/criacao/${PASSOS_DA_CRIACAO[primeiroQueFalta].chave}`);
  }, [indice, primeiroQueFalta]);

  useEffect(() => {
    if (!r.cotaId) return setCota(null);
    cotas.detalhar(r.cotaId).then((c) => setCota(c.ok ? c.dados : null));
  }, [r.cotaId]);

  const props: PropsDoPasso = {
    cota,
    avancar: () => ir(`/criacao/${PASSOS_DA_CRIACAO[Math.min(indice + 1, 4)].chave}`),
    voltar: () => ir(`/criacao/${PASSOS_DA_CRIACAO[Math.max(indice - 1, 0)].chave}`),
  };
  return (
    <>
      <Passos passos={PASSOS_DA_CRIACAO.map((p) => p.nome)} atual={indice} aoIr={(i) => ir(`/criacao/${PASSOS_DA_CRIACAO[i].chave}`)} />
      {passo === "cota" && <PassoCota {...props} />}
      {passo === "tipo" && <PassoTipo {...props} />}
      {passo === "bens" && <PassoBens {...props} />}
      {passo === "partes" && <PassoPartes {...props} />}
      {passo === "revisao" && <PassoRevisao {...props} />}
    </>
  );
}
