import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { App } from "./App";
import "./index.css";
import { registrarSimulado } from "./nucleo/http";
import { rotasDoServicoFalso } from "./simulado/servicoFalso";

registrarSimulado(rotasDoServicoFalso);
// Serviço com token (Authorization: Bearer)? Registre aqui a identidade da persona: ver src/nucleo/identidade.ts.
// Este serviço identifica pelo cabeçalho X-Usuario do contrato, que a camada api passa em cada chamada.

createRoot(document.getElementById("raiz")!).render(<StrictMode><App /></StrictMode>);
