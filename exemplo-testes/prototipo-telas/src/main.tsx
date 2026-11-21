import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { registrarServicoFalso } from "./api/api";
import { App } from "./App";
import "./index.css";
import { rotasDoServicoFalso } from "./jornada/servicoFalso";

registrarServicoFalso(rotasDoServicoFalso);

createRoot(document.getElementById("raiz")!).render(<StrictMode><App /></StrictMode>);
