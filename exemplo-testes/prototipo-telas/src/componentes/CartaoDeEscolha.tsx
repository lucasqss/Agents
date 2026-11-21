import type { ReactNode } from "react";

/** Cartão clicável para escolher uma opção (jornada, tipo, cota...). Com `href` é link; com `aoEscolher`, botão. */
export function CartaoDeEscolha({ titulo, descricao, icone, rodape, selecionado, desabilitado, href, aoEscolher }: {
  titulo: ReactNode; descricao?: ReactNode; icone?: ReactNode; rodape?: ReactNode;
  selecionado?: boolean; desabilitado?: boolean; href?: string; aoEscolher?: () => void;
}) {
  const classe = `flex h-full w-full flex-col gap-3 rounded-xl border p-5 text-left transition-colors ${
    desabilitado ? "cursor-not-allowed border-neutral-800 bg-neutral-900/30 opacity-60"
    : selecionado ? "border-blue-500 bg-blue-500/10" : "border-neutral-800 bg-neutral-900/60 hover:border-neutral-600 hover:bg-neutral-900"}`;
  const corpo = (
    <>
      {icone && <span className="flex size-10 items-center justify-center rounded-lg bg-neutral-800 text-blue-300">{icone}</span>}
      <span className="block text-base font-semibold text-neutral-100">{titulo}</span>
      {descricao && <span className="block text-sm text-neutral-400">{descricao}</span>}
      {rodape && <span className="mt-auto block pt-1 text-xs text-neutral-500">{rodape}</span>}
    </>
  );
  if (href && !desabilitado) return <a href={href} className={classe}>{corpo}</a>;
  return <button type="button" disabled={desabilitado} aria-pressed={selecionado} onClick={aoEscolher} className={classe}>{corpo}</button>;
}
