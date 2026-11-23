import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { App } from "./App";
import "./index.css";
import { registrarSimulado } from "./nucleo/http";
import { rotasDoServicoFalso } from "./simulado/servicoFalso";

registrarSimulado(rotasDoServicoFalso);

createRoot(document.getElementById("raiz")!).render(<StrictMode><App /></StrictMode>);
