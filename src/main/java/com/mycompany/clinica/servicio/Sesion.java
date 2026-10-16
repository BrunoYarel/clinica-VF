package com.mycompany.clinica.servicio;

/**
 * Guarda quién inició sesión y con qué rol.
 * Cualquier ventana del proyecto puede consultarla:
 *
 *     if (Sesion.esAdmin()) { ... }
 *     String quien = Sesion.getUsername();
 */
public final class Sesion {

    // Valores exactos del ENUM 'rol' de la tabla USUARIO
    public static final String ADMIN = "Admin";
    public static final String MEDICO = "Medico";
    public static final String RECEPCIONISTA = "Recepcionista";

    // Estado de la sesión en campos estáticos: es la forma en que los módulos saben quién está conectado
    // sin pasarse parámetros entre ventanas. Lo escribe Login (UsuarioService.autenticar) y lo leen
    // Permisos y los módulos. El idMedico sale de aquí y no de lo que se escriba en pantalla, así un
    // médico solo trabaja con sus propios datos.
    private static int idUsuario;
    private static int idMedico;
    private static String username;
    private static String rol;
    private static boolean activa;

    private Sesion() {
    }

    public static void iniciar(int id, String usuario, String rolBD) {
        iniciar(id, usuario, rolBD, 0);
    }

    public static void iniciar(int id, String usuario, String rolBD, int idMedico) {
        idUsuario = id;
        username = usuario;
        rol = normalizar(rolBD);
        Sesion.idMedico = idMedico;
        activa = true;
    }

    public static void cerrar() {
        idUsuario = 0;
        idMedico = 0;
        username = null;
        rol = null;
        activa = false;
    }

    public static boolean estaActiva() {
        return activa;
    }

    public static int getIdUsuario() {
        return idUsuario;
    }

    /** id_medico vinculado al usuario (0 si no es médico o no está vinculado). */
    public static int getIdMedico() {
        return idMedico;
    }

    public static String getUsername() {
        return username;
    }

    public static String getRol() {
        return rol;
    }

    public static boolean esAdmin() {
        return activa && ADMIN.equals(rol);
    }

    public static boolean esMedico() {
        return activa && MEDICO.equals(rol);
    }

    public static boolean esRecepcionista() {
        return activa && RECEPCIONISTA.equals(rol);
    }

    /** Tolera bases de datos antiguas donde el rol se guardó como 'Médico' con tilde. */
    private static String normalizar(String r) {
        if (r == null) {
            return "";
        }
        String limpio = r.replace('é', 'e').replace('É', 'E').trim();
        if (limpio.equalsIgnoreCase(ADMIN)) {
            return ADMIN;
        }
        if (limpio.equalsIgnoreCase(MEDICO)) {
            return MEDICO;
        }
        if (limpio.equalsIgnoreCase(RECEPCIONISTA)) {
            return RECEPCIONISTA;
        }
        return limpio;
    }
}