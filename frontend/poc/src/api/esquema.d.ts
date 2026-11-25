// GERADO por `npm run gerar:api` a partir de contrato.config.json. Não edite à mão.
export interface paths {
    "/processos": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Lista os processos, com filtro por situação, em páginas */
        get: operations["listar"];
        put?: never;
        /** Abrir */
        post: operations["abrir"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/processos/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Consultar */
        get: operations["consultar"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/processos/{id}/acoes": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Acoes */
        get: operations["acoes"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/processos/{id}/acoes/{acao}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** Executar */
        post: operations["executar"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/processos/{id}/historico": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Historico */
        get: operations["historico"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/processos/{id}/documentos": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Os documentos anexados ao processo, do mais recente para o mais antigo */
        get: operations["documentos"];
        put?: never;
        /** Anexa um documento (arquivo) ao processo */
        post: operations["anexarDocumento"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
}
export type webhooks = Record<string, never>;
export interface components {
    schemas: {
        /** @enum {string} */
        AcaoProcesso: "INICIAR_ANALISE" | "SOLICITAR_DOCUMENTO" | "RECEBER_DOCUMENTO" | "CONCLUIR_ANALISE" | "CONCLUIR_VISTORIA" | "APROVAR_NA_ALCADA" | "DEVOLVER_PARA_ANALISE" | "REPROVAR" | "PAGAR" | "CANCELAR";
        CampoInvalido: {
            campo: string | null;
            erro: string;
        };
        ErroResponse: {
            mensagem: string | null;
            campos: components["schemas"]["CampoInvalido"][];
        };
        EstadoAtual: {
            estado: components["schemas"]["EstadoProcesso"];
        };
        /** @enum {string} */
        EstadoProcesso: "ABERTO" | "EM_ANALISE" | "PENDENTE_DOCUMENTO" | "AGUARDANDO_VISTORIA" | "AGUARDANDO_ALCADA" | "APROVADO" | "PAGO" | "REPROVADO" | "CANCELADO";
        IdCriado: {
            /** Format: int64 */
            id: number;
        };
        /**
         * Format: date-time
         * @example 2022-03-10T16:15:50Z
         */
        Instant: string;
        Motivo: {
            motivo: string;
        };
        NovoProcesso: {
            tipo: components["schemas"]["TipoProcesso"];
            numeroCota: string;
            valor: number;
        };
        ProcessoResumo: {
            /** Format: int64 */
            id: number;
            tipo: string;
            numeroCota: string;
            valor: number;
            estado: string;
        };
        ProcessoTransicaoResumo: {
            de: string;
            para: string;
            acao: string;
            quando: components["schemas"]["Instant"];
            autor: string;
            observacao: string;
        };
        /** @enum {string} */
        TipoProcesso: "IMOBILIARIO" | "SERVICO";
        PaginaDeProcessos: {
            itens: components["schemas"]["ProcessoResumo"][];
            pagina: number;
            tamanho: number;
            /** @description Total de processos que atendem ao filtro */
            total: number;
        };
        /** @enum {string} */
        TipoDocumento: "IDENTIDADE" | "COMPROVANTE_RESIDENCIA" | "MATRICULA_DO_IMOVEL" | "OUTRO";
        DocumentoDoProcesso: {
            /** Format: int64 */
            id: number;
            tipo: components["schemas"]["TipoDocumento"];
            /** @description Nome do arquivo enviado */
            nome: string;
            /**
             * Format: int64
             * @description Tamanho em bytes
             */
            tamanho: number;
            /** Format: date-time */
            quando: string;
            autor: string;
        };
    };
    responses: never;
    parameters: never;
    requestBodies: never;
    headers: never;
    pathItems: never;
}
export type $defs = Record<string, never>;
export interface operations {
    listar: {
        parameters: {
            query?: {
                situacao?: components["schemas"]["EstadoProcesso"];
                pagina?: number;
                tamanho?: number;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Uma página de processos, do mais recente para o mais antigo */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["PaginaDeProcessos"];
                };
            };
            /** @description Filtro ou página inválidos */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErroResponse"];
                };
            };
        };
    };
    abrir: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["NovoProcesso"];
            };
        };
        responses: {
            /** @description Processo aberto */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["IdCriado"];
                };
            };
            /** @description Dados inválidos (campos com o erro de cada um) */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErroResponse"];
                };
            };
        };
    };
    consultar: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ProcessoResumo"];
                };
            };
            /** @description Processo não encontrado */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErroResponse"];
                };
            };
        };
    };
    acoes: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AcaoProcesso"][];
                };
            };
            /** @description Processo não encontrado */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErroResponse"];
                };
            };
        };
    };
    executar: {
        parameters: {
            query?: never;
            header: {
                "X-Usuario": string;
            };
            path: {
                acao: components["schemas"]["AcaoProcesso"];
                id: number;
            };
            cookie?: never;
        };
        requestBody?: {
            content: {
                "application/json": components["schemas"]["Motivo"];
                "*/*": components["schemas"]["Motivo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["EstadoAtual"];
                };
            };
            /** @description Dados inválidos, ou motivo ausente numa ação que o exige */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErroResponse"];
                };
            };
            /** @description Processo ou ação não encontrado */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErroResponse"];
                };
            };
            /** @description A ação não vale na situação atual do processo, ou alteração concorrente */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErroResponse"];
                };
            };
        };
    };
    historico: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ProcessoTransicaoResumo"][];
                };
            };
            /** @description Processo não encontrado */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErroResponse"];
                };
            };
        };
    };
    documentos: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["DocumentoDoProcesso"][];
                };
            };
            /** @description Processo não encontrado */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErroResponse"];
                };
            };
        };
    };
    anexarDocumento: {
        parameters: {
            query?: never;
            header: {
                "X-Usuario": string;
            };
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "multipart/form-data": {
                    tipo: components["schemas"]["TipoDocumento"];
                    /** Format: binary */
                    arquivo: Blob;
                };
            };
        };
        responses: {
            /** @description Documento anexado */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["DocumentoDoProcesso"];
                };
            };
            /** @description Tipo ou arquivo inválido (formato não aceito, arquivo vazio) */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErroResponse"];
                };
            };
            /** @description Processo não encontrado */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErroResponse"];
                };
            };
            /** @description Arquivo maior que o limite */
            413: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErroResponse"];
                };
            };
        };
    };
}
