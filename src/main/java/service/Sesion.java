package service;

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

    private static int idUsuario;
    private static String username;
    private static String rol;
    private static boolean activa;

    private Sesion() {
    }

    public static void iniciar(int id, String usuario, String rolBD) {
        idUsuario = id;
        username = usuario;
        rol = normalizar(rolBD);
        activa = true;
    }

    public static void cerrar() {
        idUsuario = 0;
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