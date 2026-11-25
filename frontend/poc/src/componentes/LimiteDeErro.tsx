import { Component, type ReactNode } from "react";
import { Aviso } from "./Aviso";

/**
 * Se a tela quebrar ao desenhar (um defeito, não um erro da API), mostra um aviso e o caminho de volta, em vez de
 * uma página em branco. A Moldura envolve cada tela com ele; trocar de rota (a `key`) começa do zero.
 */
export class LimiteDeErro extends Component<{ children: ReactNode }, { erro: Error | null }> {
  override state: { erro: Error | null } = { erro: null };

  static getDerivedStateFromError(erro: Error) {
    return { erro };
  }

  override render() {
    if (!this.state.erro) return this.props.children;
    return (
      <div className="space-y-3">
        <Aviso tipo="erro">Algo deu errado ao mostrar esta tela. ({this.state.erro.message})</Aviso>
        <a href="#/" className="text-sm font-medium text-andamento underline-offset-4 hover:underline">Voltar ao início</a>
      </div>
    );
  }
}
