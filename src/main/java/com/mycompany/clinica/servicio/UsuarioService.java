package com.mycompany.clinica.servicio;

import com.mycompany.clinica.Conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Todo el acceso a la tabla USUARIO vive aquí. Las ventanas (vista) no escriben SQL:
 * solo llaman a estos métodos y muestran el resultado.
 */
public final class UsuarioService {

    public static final int MIN_USERNAME = 3;
    public static final int MAX_USERNAME = 50;
    public static final int MIN_PASSWORD = 6;

    private UsuarioService() {
    }

    /**
     * LOGIN: busca el usuario activo y compara la contraseña con su hash.
     * Si es correcta, deja la sesión iniciada (clase Sesion).
     *
     * @return true si el usuario y la contraseña son correctos
     */
    public static boolean autenticar(String username, String password) throws SQLException {
        String sql = "SELECT id_usuario, username, password_hash, rol, id_medico "
                + "FROM usuario WHERE username = ? AND estado = 1";
        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && PasswordUtil.verificar(password, rs.getString("password_hash"))) {
                    int idMedico = rs.getInt("id_medico");
                    if (rs.wasNull()) {
                        idMedico = 0;
                    }
                    Sesion.iniciar(rs.getInt("id_usuario"), rs.getString("username"), rs.getString("rol"), idMedico);
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
        try (Connection con = Conexion.conectar();
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
        return crearUsuario(username, password, rol, 0);
    }

    /**
     * NUEVO USUARIO (paso 2, con médico): igual que el anterior, pero además guarda
     * id_medico para ligar al usuario con su registro de la tabla MEDICO.
     * Si idMedico es 0 o negativo (roles que no son Médico), id_medico queda en NULL.
     */
    public static boolean crearUsuario(String username, String password, String rol, int idMedico) throws SQLException {
        String sql = "INSERT INTO usuario (username, password_hash, rol, estado, id_medico) VALUES (?, ?, ?, 1, ?)";
        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hash(password));
            ps.setString(3, rol);
            if (idMedico > 0) {
                ps.setInt(4, idMedico);
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }
            return ps.executeUpdate() > 0;
        }
    }

    /** Médico que todavía no tiene usuario (para ligarlo al crear un usuario con rol Médico). */
    public record MedicoSinUsuario(int id, String etiqueta) {
    }

    /**
     * NUEVO USUARIO (rol Médico): lista los médicos registrados que aún no tienen usuario,
     * con la etiqueta "Apellidos, Nombres — Especialidad (CMP)".
     */
    public static List<MedicoSinUsuario> listarMedicosSinUsuario() throws SQLException {
        String sql = "SELECT m.id_medico, m.apellidos, m.nombres, m.cmp, e.nombre AS especialidad "
                + "FROM medico m INNER JOIN especialidad e ON e.id_especialidad = m.id_especialidad "
                + "LEFT JOIN usuario u ON u.id_medico = m.id_medico "
                + "WHERE u.id_usuario IS NULL ORDER BY m.apellidos, m.nombres";
        List<MedicoSinUsuario> lista = new ArrayList<>();
        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new MedicoSinUsuario(rs.getInt("id_medico"),
                        rs.getString("apellidos") + ", " + rs.getString("nombres")
                                + " — " + rs.getString("especialidad") + " (" + rs.getString("cmp") + ")"));
            }
        }
        return lista;
    }

    /**
     * NUEVO USUARIO (rol Médico, médico nuevo): en una sola transacción registra al médico en la
     * tabla MEDICO y crea su usuario ya ligado (id_medico). Si algo falla no queda ninguno de los dos.
     *
     * @throws ReglaNegocioException si el CMP ya pertenece a otro médico (el mensaje se muestra tal cual)
     * @throws SQLIntegrityConstraintViolationException si el nombre de usuario ya existe
     */
    public static boolean crearUsuarioConMedicoNuevo(String username, String password,
                                                     String nombres, String apellidos, String cmp, String telefono, String email,
                                                     int idEspecialidad) throws SQLException, ReglaNegocioException {
        String sqlMedico = "INSERT INTO medico (nombres, apellidos, cmp, telefono, email, id_especialidad) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        String sqlUsuario = "INSERT INTO usuario (username, password_hash, rol, estado, id_medico) "
                + "VALUES (?, ?, ?, 1, ?)";
        try (Connection con = Conexion.conectar()) {
            con.setAutoCommit(false);
            try {
                int idMedico;
                try (PreparedStatement ps = con.prepareStatement(sqlMedico, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, nombres);
                    ps.setString(2, apellidos);
                    ps.setString(3, cmp);
                    ps.setString(4, telefono);
                    ps.setString(5, email);
                    ps.setInt(6, idEspecialidad);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) {
                            throw new SQLException("No se pudo obtener el id del médico recién registrado.");
                        }
                        idMedico = rs.getInt(1);
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(sqlUsuario)) {
                    ps.setString(1, username);
                    ps.setString(2, PasswordUtil.hash(password));
                    ps.setString(3, Sesion.MEDICO);
                    ps.setInt(4, idMedico);
                    ps.executeUpdate();
                }
                con.commit();
                return true;
            } catch (SQLIntegrityConstraintViolationException e) {
                con.rollback();
                String detalle = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
                if (detalle.contains("uq_medico_cmp")) {
                    throw new ReglaNegocioException("Ya existe un médico con el CMP '" + cmp + "'.");
                }
                throw e;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /**
     * Indica si ya existe algún usuario ligado a ese médico (para no crear dos usuarios del mismo médico).
     */
    public static boolean medicoYaTieneUsuario(int idMedico) throws SQLException {
        String sql = "SELECT 1 FROM usuario WHERE id_medico = ? LIMIT 1";
        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idMedico);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Liga un usuario existente con un médico (para la futura gestión de usuarios).
     *
     * @return false si no se actualizó ninguna fila (el usuario no existe)
     */
    public static boolean vincularMedico(int idUsuario, int idMedico) throws SQLException {
        String sql = "UPDATE usuario SET id_medico = ? WHERE id_usuario = ?";
        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idMedico);
            ps.setInt(2, idUsuario);
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
        try (Connection con = Conexion.conectar()) {
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

    /** Fila de la tabla de gestión de usuarios (medico = "Apellidos, Nombres" o "—" si no tiene). */
    public record FilaUsuario(int id, String username, String rol, String medico, boolean activo) {
    }

    /**
     * GESTIÓN DE USUARIOS: lista todos los usuarios (activos e inactivos) ordenados por nombre,
     * con el nombre del médico ligado (si lo tienen).
     */
    public static List<FilaUsuario> listarUsuarios() throws SQLException {
        String sql = "SELECT u.id_usuario, u.username, u.rol, u.estado, m.apellidos, m.nombres "
                + "FROM usuario u LEFT JOIN medico m ON m.id_medico = u.id_medico "
                + "ORDER BY u.username";
        List<FilaUsuario> lista = new ArrayList<>();
        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String apellidos = rs.getString("apellidos");
                String medico = apellidos == null ? "—" : apellidos + ", " + rs.getString("nombres");
                lista.add(new FilaUsuario(rs.getInt("id_usuario"), rs.getString("username"),
                        rs.getString("rol"), medico, rs.getInt("estado") == 1));
            }
        }
        return lista;
    }

    /**
     * GESTIÓN DE USUARIOS: activa o desactiva un usuario. Solo el administrador puede hacerlo,
     * no puede desactivarse a sí mismo y siempre debe quedar al menos un administrador activo.
     *
     * @throws ReglaNegocioException si no se cumple alguna de esas reglas (el mensaje se muestra tal cual)
     */
    public static void cambiarEstado(int idUsuario, boolean activo) throws SQLException, ReglaNegocioException {
        if (!Permisos.puedeGestionarUsuarios()) {
            throw new ReglaNegocioException("No tiene permiso para activar o desactivar usuarios.");
        }
        if (!activo && idUsuario == Sesion.getIdUsuario()) {
            throw new ReglaNegocioException("No puede desactivar su propio usuario.");
        }
        String sqlUsuario = "SELECT rol, estado FROM usuario WHERE id_usuario = ?";
        String sqlOtrosAdmins = "SELECT COUNT(*) FROM usuario WHERE rol = 'Admin' AND estado = 1 AND id_usuario <> ?";
        String sqlUpdate = "UPDATE usuario SET estado = ? WHERE id_usuario = ?";
        try (Connection con = Conexion.conectar()) {
            if (!activo) {
                boolean esAdminActivo;
                try (PreparedStatement ps = con.prepareStatement(sqlUsuario)) {
                    ps.setInt(1, idUsuario);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new ReglaNegocioException("El usuario ya no existe.");
                        }
                        esAdminActivo = Sesion.ADMIN.equals(rs.getString("rol")) && rs.getInt("estado") == 1;
                    }
                }
                if (esAdminActivo) {
                    try (PreparedStatement ps = con.prepareStatement(sqlOtrosAdmins)) {
                        ps.setInt(1, idUsuario);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next() && rs.getInt(1) == 0) {
                                throw new ReglaNegocioException(
                                        "No se puede desactivar al último administrador activo.");
                            }
                        }
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement(sqlUpdate)) {
                ps.setInt(1, activo ? 1 : 0);
                ps.setInt(2, idUsuario);
                ps.executeUpdate();
            }
        }
    }
}