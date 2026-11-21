import { Plus, Trash2 } from "lucide-react";
import { useEffect } from "react";
import { lerReais, reais } from "../../apoio/formato";
import { Aviso, Botao, Campo, Cartao, Entrada } from "../../componentes";
import { novaChave, rascunho, type BemNoRascunho } from "../../jornada/estado";
import { TIPOS } from "../../jornada/vocabulario";
import type { PropsDoPasso } from "./Criacao";
import { Navegacao } from "./Navegacao";

export const totalDosBens = (bens: BemNoRascunho[]) =>
  Math.round(bens.reduce((s, b) => s + (Number.isNaN(lerReais(b.valor)) ? 0 : lerReais(b.valor)), 0) * 100) / 100;

export function PassoBens({ cota, avancar, voltar }: PropsDoPasso) {
  const r = rascunho.usar();
  const tipo = r.tipo ? TIPOS[r.tipo] : null;
  const bemVazio = (): BemNoRascunho => ({ chave: novaChave(), dados: {}, valor: "" });

  useEffect(() => {
    if (r.tipo && !r.bens.length) rascunho.gravar({ ...r, bens: [bemVazio()] });
  });

  const mudarBem = (chave: string, mudanca: Partial<BemNoRascunho>) =>
    rascunho.gravar({ ...r, bens: r.bens.map((b) => (b.chave === chave ? { ...b, ...mudanca } : b)) });

  if (!tipo) return null; // o assistente leva de volta ao passo que falta
  const total = totalDosBens(r.bens);
  const acimaDoCredito = cota !== null && total > cota.creditoDisponivel;
  const completo = r.bens.length > 0 && r.bens.every((b) =>
    tipo.campos.every((c) => b.dados[c.chave]?.trim()) && lerReais(b.valor) > 0);

  return (
    <Cartao titulo={`Quais ${tipo.bens}?`}>
      <p className="mb-4 text-sm text-neutral-400">
        Um processo {tipo.texto.toLowerCase()} só tem {tipo.bens}. O valor do processo é a soma dos valores.
      </p>
      <div className="mb-4 space-y-4">
        {r.bens.map((b, i) => (
          <fieldset key={b.chave} aria-label={`${tipo.bem[0].toUpperCase() + tipo.bem.slice(1)} ${i + 1}`} className="rounded-lg border border-neutral-800 p-4">
            <div className="mb-3 flex items-center justify-between">
              <span className="text-sm font-medium text-neutral-200">{tipo.bem[0].toUpperCase() + tipo.bem.slice(1)} {i + 1}</span>
              {r.bens.length > 1 && (
                <Botao variante="perigo" onClick={() => rascunho.gravar({ ...r, bens: r.bens.filter((x) => x.chave !== b.chave) })}
                  icone={<Trash2 className="size-4" aria-hidden />}>Remover</Botao>
              )}
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              {tipo.campos.map((c) => (
                <Campo key={c.chave} rotulo={c.rotulo}>
                  <Entrada value={b.dados[c.chave] ?? ""} placeholder={`ex.: ${c.exemplo}`}
                    onChange={(e) => mudarBem(b.chave, { dados: { ...b.dados, [c.chave]: e.target.value } })} />
                </Campo>
              ))}
              <Campo rotulo="Valor (R$)" erro={b.valor && Number.isNaN(lerReais(b.valor)) ? "Use o formato 80.000,00" : undefined}>
                <Entrada inputMode="decimal" value={b.valor} placeholder="ex.: 80.000,00" onChange={(e) => mudarBem(b.chave, { valor: e.target.value })} />
              </Campo>
            </div>
          </fieldset>
        ))}
        <Botao onClick={() => rascunho.gravar({ ...r, bens: [...r.bens, bemVazio()] })} icone={<Plus className="size-4" aria-hidden />}>
          Acrescentar {tipo.bem}
        </Botao>
      </div>
      <div className="mb-5 flex flex-wrap items-center justify-between gap-2 rounded-lg bg-neutral-950 px-4 py-3 text-sm">
        <span className="text-neutral-400">Valor do processo (soma dos {tipo.bens})</span>
        <strong className="tabular-nums text-neutral-100">{reais(total)}</strong>
        {cota && <span className="w-full text-xs text-neutral-500">Crédito disponível da cota: {reais(cota.creditoDisponivel)}</span>}
      </div>
      {acimaDoCredito && <div className="mb-5"><Aviso tipo="erro">O valor passa do crédito disponível da cota em {reais(total - cota!.creditoDisponivel)}.</Aviso></div>}
      <Navegacao voltar={voltar} avancar={avancar} podeAvancar={completo && !acimaDoCredito} />
    </Cartao>
  );
}
