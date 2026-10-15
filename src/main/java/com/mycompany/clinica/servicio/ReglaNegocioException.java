package com.mycompany.clinica.servicio;

/**
 * Se lanza cuando una operación incumple una regla del negocio (sin permiso, datos inválidos,
 * cita que no se puede atender...). El mensaje está redactado para mostrárselo tal cual al usuario.
 * Los fallos técnicos de la base de datos siguen siendo SQLException.
 */
public class ReglaNegocioException extends Exception {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
