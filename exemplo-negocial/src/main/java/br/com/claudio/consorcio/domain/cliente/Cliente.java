package br.com.claudio.consorcio.domain.cliente;

import br.com.claudio.consorcio.domain.DominioException;
import org.jspecify.annotations.Nullable;

public final class Cliente {

    private final @Nullable Long id;
    private String nome;
    private String cpf;

    private Cliente(@Nullable Long id, String nome, String cpf) {
        this.id = id;
        alterar(nome, cpf);
    }

    /** Cria um cliente novo, ainda sem id. */
    public static Cliente novo(String nome, String cpf) {
        return new Cliente(null, nome, cpf);
    }

    /** Reconstrói um cliente já persistido. */
    public static Cliente reconstituir(Long id, String nome, String cpf) {
        return new Cliente(id, nome, cpf);
    }

    public void alterar(String nome, String cpf) {
        if (nome == null || nome.isBlank()) {
            throw new DominioException("Nome do cliente é obrigatório");
        }
        if (cpf == null || cpf.isBlank()) {
            throw new DominioException("CPF do cliente é obrigatório");
        }
        this.nome = nome.strip();
        this.cpf = cpf.strip();
    }

    public @Nullable Long id() {
        return id;
    }

    public String nome() {
        return nome;
    }

    public String cpf() {
        return cpf;
    }
}
