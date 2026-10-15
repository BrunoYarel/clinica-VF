package service;

import com.mycompany.clinica.Conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Todo el acceso a la tabla USUARIO vive aquí. Las ventanas (view) no escriben SQL:
 * solo llaman a estos métodos y muestran el resultado.
 */
public final class UsuarioService {

    public static final int MIN_USERNAME = 3;
    public static final int MAX_USERNAME = 50;
    public static final int MIN_PASSWORD = 6;

    private UsuarioService() {
    }

    /** Abre la conexión o lanza un error claro si MySQL no responde. */
    private static Connection abrirConexion() throws SQLException {
        Connection con = Conexion.conectar();
        if (con == null) {
            throw new SQLException("No se pudo conectar a la base de datos. "
                    + "Verifique que MySQL esté encendido en XAMPP.");
        }
        return con;
    }

    /**
     * LOGIN: busca el usuario activo y compara la contraseña con su hash.
     * Si es correcta, deja la sesión iniciada (clase Sesion).
     *
     * @return true si el usuario y la contraseña son correctos
     */
    public static boolean autenticar(String username, String password) throws SQLException {
        String sql = "SELECT id_usuario, username, password_hash, rol "
                + "FROM usuario WHERE username = ? AND estado = 1";
        try (Connection con = abrirConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && PasswordUtil.verificar(password, rs.getString("password_hash"))) {
                    Sesion.iniciar(rs.getInt("id_usuario"), rs.getString("username"), rs.getString("rol"));
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * NUEVO USUARIO (paso 1): comprueba que la contraseña escrita pertenezca
     * a algún administrador activo.
     */
    public static boolean verificarPasswordAdmin(String password) throws SQLException {
        String sql = "SELECT password_hash FROM usuario WHERE rol = 'Admin' AND estado = 1";
        try (Connection con = abrirConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                if (PasswordUtil.verificar(password, rs.getString("password_hash"))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * NUEVO USUARIO (paso 2): inserta el usuario con su contraseña ya convertida en hash.
     * Si el nombre ya existe, MySQL lanza SQLIntegrityConstraintViolationException.
     */
    public static boolean crearUsuario(String username, String password, String rol) throws SQLException {
        String sql = "INSERT INTO usuario (username, password_hash, rol, estado) VALUES (?, ?, ?, 1)";
        try (Connection con = abrirConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hash(password));
            ps.setString(3, rol);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * CAMBIAR CONTRASEÑA: verifica la contraseña actual y, si es correcta,
     * guarda el hash de la nueva.
     *
     * @return false si la contraseña actual es incorrecta (no se cambió nada)
     */
    public static boolean cambiarPassword(int idUsuario, String actual, String nueva) throws SQLException {
        String sqlSelect = "SELECT password_hash FROM usuario WHERE id_usuario = ? AND estado = 1";
        String sqlUpdate = "UPDATE usuario SET password_hash = ? WHERE id_usuario = ?";
        try (Connection con = abrirConexion()) {
            String hashActual;
            try (PreparedStatement ps = con.prepareStatement(sqlSelect)) {
                ps.setInt(1, idUsuario);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                    hashActual = rs.getString("password_hash");
                }
            }
            if (!PasswordUtil.verificar(actual, hashActual)) {
                return false;
            }
            try (PreparedStatement ps = con.prepareStatement(sqlUpdate)) {
                ps.setString(1, PasswordUtil.hash(nueva));
                ps.setInt(2, idUsuario);
                return ps.executeUpdate() > 0;
            }
        }
    }
}