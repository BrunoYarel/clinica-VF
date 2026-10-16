package com.mycompany.clinica.modelo;

public enum EstadoCita {
    PENDIENTE("Pendiente"),
    PROGRAMADA("Programada"),
    ATENDIDA("Atendida"),
    CANCELADA("Cancelada");

    private final String valor;

    EstadoCita(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }
}


