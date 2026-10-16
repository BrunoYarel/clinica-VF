package com.mycompany.clinica.servicio;

import com.mycompany.clinica.Conexion;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Todo el acceso a datos de "MI AGENDA" (solo Médico). Es de SOLO LECTURA: no inserta ni modifica nada,
 * así que no puede interferir con Citas ni con Atención; solo muestra lo que esos módulos ya guardaron.
 *
 * Reglas:
 *   - El médico solo ve SUS citas: el id_medico sale de la sesión (Sesion.getIdMedico()), nunca de la pantalla.
 *   - Igual que ReporteService, una cita cuenta como "Atendida" si su estado es Atendida O si ya tiene
 *     una atención registrada (el módulo de Atención no cambia el estado de la cita).
 */
public final class AgendaService {

    private static final String[] ESTADOS = {"Pendiente", "Programada", "En Espera", "Atendida", "Cancelada"};

    /** Mismo criterio que ReporteService.ESTADO_REAL (alias c = cita, a = atencion). */
    private static final String ESTADO_REAL =
            "CASE WHEN a.id_atencion IS NOT NULL THEN 'Atendida' ELSE c.estado END";

    private AgendaService() {
    }

    /** Una fila de la agenda (una cita del médico con los datos del paciente y, si existe, su atención). */
    public record FilaAgenda(int idCita, LocalDate fecha, String hora, String paciente, String documento,
                             String telefono, String motivo, String estado, String diagnostico) {
    }

    /** Estados que se pueden usar como filtro (el primero, "Todos", se agrega en la ventana). */
    public static String[] estados() {
        return ESTADOS.clone();
    }

    /**
     * Citas del médico en sesión, ordenadas por fecha y hora.
     *
     * @param desde  primer día (incluido) o null = sin límite
     * @param hasta  último día (incluido) o null = sin límite
     * @param estado estado real a mostrar o null = todos
     * @throws ReglaNegocioException si la sesión no es de un médico vinculado a un registro de médico
     */
    public static List<FilaAgenda> listar(LocalDate desde, LocalDate hasta, String estado)
            throws SQLException, ReglaNegocioException {
        if (!Permisos.puedeVer(Permisos.Pantalla.MI_AGENDA)) {
            throw new ReglaNegocioException("Solo el médico puede ver su agenda.");
        }
        int idMedico = Sesion.getIdMedico();
        if (idMedico <= 0) {
            throw new ReglaNegocioException("Su usuario no está vinculado a ningún médico. "
                    + "Pídale al administrador que lo vincule.");
        }

        StringBuilder sql = new StringBuilder(
                "SELECT c.id_cita, c.fecha_cita, c.hora_cita, p.nombres, p.apellidos, p.documento, p.telefono,"
                        + " c.motivo, " + ESTADO_REAL + " AS estado_real, a.diagnostico"
                        + " FROM cita c"
                        + " INNER JOIN paciente p ON p.id_paciente = c.id_paciente"
                        + " LEFT JOIN atencion a ON a.id_cita = c.id_cita"
                        + " WHERE c.id_medico = ?");
        List<Object> params = new ArrayList<>();
        params.add(idMedico);
        if (desde != null) {
            sql.append(" AND c.fecha_cita >= ?");
            params.add(Date.valueOf(desde));
        }
        if (hasta != null) {
            sql.append(" AND c.fecha_cita <= ?");
            params.add(Date.valueOf(hasta));
        }
        if (estado != null) {
            sql.append(" AND ").append(ESTADO_REAL).append(" = ?");
            params.add(estado);
        }
        sql.append(" ORDER BY c.fecha_cita, c.hora_cita, c.id_cita");

        List<FilaAgenda> filas = new ArrayList<>();
        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object v = params.get(i);
                if (v instanceof Integer n) {
                    ps.setInt(i + 1, n);
                } else if (v instanceof Date d) {
                    ps.setDate(i + 1, d);
                } else {
                    ps.setString(i + 1, String.valueOf(v));
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date f = rs.getDate("fecha_cita");
                    filas.add(new FilaAgenda(
                            rs.getInt("id_cita"),
                            f == null ? null : f.toLocalDate(),
                            hora(rs.getTime("hora_cita")),
                            texto(rs.getString("nombres")) + " " + texto(rs.getString("apellidos")),
                            texto(rs.getString("documento")),
                            texto(rs.getString("telefono")),
                            texto(rs.getString("motivo")),
                            texto(rs.getString("estado_real")),
                            texto(rs.getString("diagnostico"))));
                }
            }
        }
        return filas;
    }

    private static String hora(Time t) {
        if (t == null) {
            return "";
        }
        String s = t.toString();                       // HH:mm:ss
        return s.length() >= 5 ? s.substring(0, 5) : s;
    }

    private static String texto(String s) {
        return s == null ? "" : s;
    }
}