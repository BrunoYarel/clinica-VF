package com.mycompany.clinica.servicio;

import java.util.List;

/**
 * Define en un solo lugar qué ve y qué puede hacer cada rol.
 * Se apoya en Sesion (no consulta la base de datos). Cualquier ventana puede preguntar:
 *
 *     if (Permisos.puedeGestionarCitas()) { ... }
 *     for (Permisos.Pantalla p : Permisos.pantallasDelRol()) { ... }
 */
public final class Permisos {

    /** Pantallas (módulos) del sistema. El título es el texto que se muestra en el menú. */
    public enum Pantalla {
        INICIO("Inicio"),
        PACIENTES("Pacientes"),
        CITAS("Citas"),
        MEDICOS("Médicos"),
        ESPECIALIDADES("Especialidades"),
        MEDICAMENTOS("Medicamentos"),
        HISTORIAL("Historial"),
        REPORTES("Reportes"),
        USUARIOS("Usuarios"),
        MI_AGENDA("Mi agenda"),
        ATENCION("Atención"),
        RECETAS("Recetas");

        private final String titulo;

        Pantalla(String titulo) {
            this.titulo = titulo;
        }

        public String getTitulo() {
            return titulo;
        }

        /** Devuelve la pantalla que tiene ese título, o null si no existe. */
        public static Pantalla deTitulo(String titulo) {
            for (Pantalla p : values()) {
                if (p.titulo.equals(titulo)) {
                    return p;
                }
            }
            return null;
        }
    }

    private Permisos() {
    }

    // =====================================================================
    //  QUÉ PANTALLAS VE CADA ROL
    // =====================================================================

    /** Indica si el rol de la sesión actual puede entrar a la pantalla (INICIO: cualquier sesión activa). */
    public static boolean puedeVer(Pantalla p) {
        if (p == Pantalla.INICIO) {
            return Sesion.estaActiva();
        }
        return pantallasDelRol().contains(p);
    }

    /** Pantallas del rol actual, en el orden del menú y sin INICIO (que es el propio menú); vacía sin sesión. */
    public static List<Pantalla> pantallasDelRol() {
        if (Sesion.esAdmin()) {
            return List.of(Pantalla.PACIENTES, Pantalla.CITAS, Pantalla.MEDICOS, Pantalla.ESPECIALIDADES,
                    Pantalla.MEDICAMENTOS, Pantalla.HISTORIAL, Pantalla.REPORTES, Pantalla.USUARIOS);
        }
        if (Sesion.esRecepcionista()) {
            return List.of(Pantalla.PACIENTES, Pantalla.CITAS, Pantalla.MEDICOS);
        }
        if (Sesion.esMedico()) {
            return List.of(Pantalla.MI_AGENDA, Pantalla.ATENCION, Pantalla.RECETAS, Pantalla.HISTORIAL);
        }
        return List.of();
    }

    // =====================================================================
    //  QUÉ ACCIONES PUEDE HACER CADA ROL
    // =====================================================================

    /** Buscar, registrar y editar pacientes: Admin y Recepcionista. */
    public static boolean puedeGestionarPacientes() {
        return Sesion.esAdmin() || Sesion.esRecepcionista();
    }

    /** Eliminar pacientes: solo Admin. */
    public static boolean puedeEliminarPaciente() {
        return Sesion.esAdmin();
    }

    /** Alta y edición de médicos: solo Admin (Recepcionista solo los consulta). */
    public static boolean puedeGestionarMedicos() {
        return Sesion.esAdmin();
    }

    /** Alta y edición de especialidades: solo Admin. */
    public static boolean puedeGestionarEspecialidades() {
        return Sesion.esAdmin();
    }

    /** Alta y edición del catálogo de medicamentos: solo Admin. */
    public static boolean puedeGestionarMedicamentos() {
        return Sesion.esAdmin();
    }

    /** Agendar, confirmar y cancelar citas: Admin y Recepcionista. */
    public static boolean puedeGestionarCitas() {
        return Sesion.esAdmin() || Sesion.esRecepcionista();
    }

    /** Consultar el historial clínico: Admin (solo lectura) y Médico. */
    public static boolean puedeVerHistorial() {
        return Sesion.esAdmin() || Sesion.esMedico();
    }

    /** Registrar atenciones (consultas): solo Médico. */
    public static boolean puedeRegistrarAtencion() {
        return Sesion.esMedico();
    }

    /** Emitir recetas: solo Médico. */
    public static boolean puedeEmitirReceta() {
        return Sesion.esMedico();
    }

    /** Ver los reportes del sistema: solo Admin. */
    public static boolean puedeVerReportes() {
        return Sesion.esAdmin();
    }

    /** Crear y administrar usuarios (perfiles): solo Admin. */
    public static boolean puedeGestionarUsuarios() {
        return Sesion.esAdmin();
    }
}