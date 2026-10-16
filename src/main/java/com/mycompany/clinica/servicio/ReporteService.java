package com.mycompany.clinica.servicio;

import com.mycompany.clinica.Conexion;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/**
 * Todo el acceso a datos del módulo de REPORTES (solo Admin). Las ventanas no escriben SQL:
 * piden un reporte con generar(...) y muestran las filas que devuelve.
 *
 * Nota sobre el estado de las citas: el módulo de Atención registra la atención pero no cambia
 * el estado de la cita, así que aquí una cita cuenta como "Atendida" si su estado es Atendida
 * O si ya tiene una atención registrada (ver ESTADO_REAL).
 */
public final class ReporteService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] ESTADOS = {"Pendiente", "Programada", "En Espera", "Atendida", "Cancelada"};

    /** Estado "real" de la cita: si tiene atención registrada, está Atendida. Usa los alias c y a. */
    private static final String ESTADO_REAL =
            "CASE WHEN a.id_atencion IS NOT NULL THEN 'Atendida' ELSE c.estado END";

    // Unión base de los reportes que parten de la cita (una fila por cita)
    private static final String DESDE_CITAS =
            " FROM cita c"
                    + " INNER JOIN paciente p ON p.id_paciente = c.id_paciente"
                    + " INNER JOIN medico m ON m.id_medico = c.id_medico"
                    + " INNER JOIN especialidad e ON e.id_especialidad = m.id_especialidad"
                    + " LEFT JOIN atencion a ON a.id_cita = c.id_cita";

    private ReporteService() {
    }

    // =====================================================================
    //  TIPOS Y ESTRUCTURAS
    // =====================================================================

    /** Reportes disponibles. Los dos booleanos dicen qué filtros usa cada uno. */
    public enum TipoReporte {
        CITAS("Listado de citas", true, true),
        CITAS_POR_ESTADO("Citas por estado", true, false),
        ATENCION_POR_MEDICO("Atención por médico", true, false),
        CITAS_POR_ESPECIALIDAD("Citas por especialidad", false, false),
        PACIENTES("Pacientes con citas", true, false),
        DIAGNOSTICOS("Diagnósticos más frecuentes", true, false),
        MEDICAMENTOS("Medicamentos más recetados", true, false);

        private final String titulo;
        private final boolean usaMedico;
        private final boolean usaEstado;

        TipoReporte(String titulo, boolean usaMedico, boolean usaEstado) {
            this.titulo = titulo;
            this.usaMedico = usaMedico;
            this.usaEstado = usaEstado;
        }

        public String getTitulo() {
            return titulo;
        }

        public boolean usaMedico() {
            return usaMedico;
        }

        public boolean usaEstado() {
            return usaEstado;
        }
    }

    /** Filtros. desde/hasta pueden ser null (sin límite); idMedico 0 = todos; estado null = todos. */
    public record Filtro(LocalDate desde, LocalDate hasta, int idMedico, String estado) {
    }

    /** Resultado de un reporte. Si conTotal es true, la última fila es la de totales. */
    public record Reporte(TipoReporte tipo, List<String> columnas, List<Object[]> filas, boolean conTotal) {
        /** Cantidad de registros sin contar la fila de total. */
        public int registros() {
            return conTotal ? Math.max(0, filas.size() - 1) : filas.size();
        }
    }

    public record OpcionMedico(int id, String etiqueta) {
    }

    public record Resumen(int pacientes, int medicos, int citasHoy, int citasPorConfirmar, int atenciones, int recetas) {
    }

    public static String[] estados() {
        return ESTADOS.clone();
    }

    // =====================================================================
    //  CONSULTAS PÚBLICAS
    // =====================================================================

    /** Médicos para el combo de filtro, ordenados por apellido. */
    public static List<OpcionMedico> listarMedicos() throws SQLException, ReglaNegocioException {
        exigirAdmin();
        List<OpcionMedico> lista = new ArrayList<>();
        String sql = "SELECT id_medico, CONCAT(apellidos, ', ', nombres) AS nombre FROM medico ORDER BY apellidos, nombres";
        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new OpcionMedico(rs.getInt(1), rs.getString(2)));
            }
        }
        return lista;
    }

    /** Totales generales para las tarjetas de arriba. */
    public static Resumen resumen() throws SQLException, ReglaNegocioException {
        exigirAdmin();
        String sql = "SELECT (SELECT COUNT(*) FROM paciente),"
                + " (SELECT COUNT(*) FROM medico),"
                + " (SELECT COUNT(*) FROM cita WHERE fecha_cita = ?),"
                + " (SELECT COUNT(*) FROM cita c LEFT JOIN atencion a ON a.id_cita = c.id_cita"
                + "   WHERE c.estado = 'Pendiente' AND a.id_atencion IS NULL),"
                + " (SELECT COUNT(*) FROM atencion),"
                + " (SELECT COUNT(*) FROM receta)";
        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(LocalDate.now()));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Resumen(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getInt(6));
            }
        }
    }

    /** Genera el reporte pedido aplicando solo los filtros que ese reporte usa. */
    public static Reporte generar(TipoReporte tipo, Filtro filtro) throws SQLException, ReglaNegocioException {
        exigirAdmin();
        if (filtro.desde() != null && filtro.hasta() != null && filtro.desde().isAfter(filtro.hasta())) {
            throw new ReglaNegocioException("La fecha 'Desde' no puede ser posterior a la fecha 'Hasta'.");
        }
        try (Connection con = Conexion.conectar()) {
            switch (tipo) {
                case CITAS:
                    return citas(con, filtro);
                case CITAS_POR_ESTADO:
                    return citasPorEstado(con, filtro);
                case ATENCION_POR_MEDICO:
                    return atencionPorMedico(con, filtro);
                case CITAS_POR_ESPECIALIDAD:
                    return citasPorEspecialidad(con, filtro);
                case PACIENTES:
                    return pacientes(con, filtro);
                case DIAGNOSTICOS:
                    return diagnosticos(con, filtro);
                case MEDICAMENTOS:
                    return medicamentos(con, filtro);
                default:
                    throw new ReglaNegocioException("Reporte no disponible.");
            }
        }
    }

    // =====================================================================
    //  REPORTES
    // =====================================================================

    private static Reporte citas(Connection con, Filtro f) throws SQLException {
        Condiciones w = new Condiciones();
        w.fechas("c.fecha_cita", f);
        w.medico("c.id_medico", f);
        if (f.estado() != null) {
            w.agregar(ESTADO_REAL + " = ?", f.estado());
        }
        String sql = "SELECT c.fecha_cita, c.hora_cita, CONCAT(p.apellidos, ', ', p.nombres), p.documento,"
                + " CONCAT(m.apellidos, ', ', m.nombres), e.nombre, c.motivo, " + ESTADO_REAL
                + DESDE_CITAS + w.where()
                + " ORDER BY c.fecha_cita DESC, c.hora_cita DESC, c.id_cita DESC";
        List<Object[]> filas = new ArrayList<>();
        try (PreparedStatement ps = preparar(con, sql, w.params); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                filas.add(new Object[]{fecha(rs.getDate(1)), hora(rs.getTime(2)), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), texto(rs.getString(7)), rs.getString(8)});
            }
        }
        return new Reporte(TipoReporte.CITAS,
                List.of("Fecha", "Hora", "Paciente", "Documento", "Médico", "Especialidad", "Motivo", "Estado"),
                filas, false);
    }

    private static Reporte citasPorEstado(Connection con, Filtro f) throws SQLException {
        Condiciones w = new Condiciones();
        w.fechas("c.fecha_cita", f);
        w.medico("c.id_medico", f);
        String sql = "SELECT t.estado_real, COUNT(*) FROM (SELECT " + ESTADO_REAL + " AS estado_real"
                + DESDE_CITAS + w.where() + ") t GROUP BY t.estado_real";
        Map<String, Integer> conteo = new LinkedHashMap<>();
        for (String s : ESTADOS) {
            conteo.put(s, 0);
        }
        try (PreparedStatement ps = preparar(con, sql, w.params); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                conteo.merge(rs.getString(1), rs.getInt(2), Integer::sum);
            }
        }
        int total = conteo.values().stream().mapToInt(Integer::intValue).sum();
        List<Object[]> filas = new ArrayList<>();
        for (Map.Entry<String, Integer> en : conteo.entrySet()) {
            filas.add(new Object[]{en.getKey(), en.getValue(), pct(en.getValue(), total)});
        }
        filas.add(new Object[]{"Total", total, total == 0 ? 0 : 100});
        return new Reporte(TipoReporte.CITAS_POR_ESTADO, List.of("Estado", "Citas", "% del total"), filas, true);
    }

    private static Reporte atencionPorMedico(Connection con, Filtro f) throws SQLException {
        // Las fechas van en el ON del LEFT JOIN para que los médicos sin citas también aparezcan (en 0)
        Condiciones on = new Condiciones();
        on.fechasEn("c.fecha_cita", f);
        String donde = "";
        List<Object> params = new ArrayList<>(on.params);
        if (f.idMedico() > 0) {
            donde = " WHERE m.id_medico = ?";
            params.add(f.idMedico());
        }
        String sql = "SELECT CONCAT(m.apellidos, ', ', m.nombres), e.nombre, COUNT(c.id_cita),"
                + " SUM(CASE WHEN " + ESTADO_REAL + " = 'Atendida' THEN 1 ELSE 0 END),"
                + " SUM(CASE WHEN " + ESTADO_REAL + " = 'Cancelada' THEN 1 ELSE 0 END),"
                + " SUM(COALESCE(rc.n, 0))"
                + " FROM medico m"
                + " INNER JOIN especialidad e ON e.id_especialidad = m.id_especialidad"
                + " LEFT JOIN cita c ON c.id_medico = m.id_medico" + on.sqlAnd()
                + " LEFT JOIN atencion a ON a.id_cita = c.id_cita"
                + " LEFT JOIN (SELECT id_atencion, COUNT(*) AS n FROM receta GROUP BY id_atencion) rc"
                + "   ON rc.id_atencion = a.id_atencion"
                + donde
                + " GROUP BY m.id_medico, m.apellidos, m.nombres, e.nombre"
                + " ORDER BY COUNT(c.id_cita) DESC, m.apellidos, m.nombres";
        List<Object[]> filas = new ArrayList<>();
        int tCitas = 0, tAt = 0, tCan = 0, tPor = 0, tRec = 0;
        try (PreparedStatement ps = preparar(con, sql, params); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int citas = rs.getInt(3), at = rs.getInt(4), can = rs.getInt(5), rec = rs.getInt(6);
                int por = citas - at - can;
                filas.add(new Object[]{rs.getString(1), rs.getString(2), citas, at, can, por, rec, pct(at, citas)});
                tCitas += citas;
                tAt += at;
                tCan += can;
                tPor += por;
                tRec += rec;
            }
        }
        filas.add(new Object[]{"Total", "", tCitas, tAt, tCan, tPor, tRec, pct(tAt, tCitas)});
        return new Reporte(TipoReporte.ATENCION_POR_MEDICO,
                List.of("Médico", "Especialidad", "Citas", "Atendidas", "Canceladas", "Por atender", "Recetas",
                        "% atendidas"), filas, true);
    }

    private static Reporte citasPorEspecialidad(Connection con, Filtro f) throws SQLException {
        Condiciones on = new Condiciones();
        on.fechasEn("c.fecha_cita", f);
        String sql = "SELECT e.nombre, COUNT(DISTINCT m.id_medico), COUNT(c.id_cita),"
                + " SUM(CASE WHEN " + ESTADO_REAL + " = 'Atendida' THEN 1 ELSE 0 END),"
                + " SUM(CASE WHEN " + ESTADO_REAL + " = 'Cancelada' THEN 1 ELSE 0 END)"
                + " FROM especialidad e"
                + " LEFT JOIN medico m ON m.id_especialidad = e.id_especialidad"
                + " LEFT JOIN cita c ON c.id_medico = m.id_medico" + on.sqlAnd()
                + " LEFT JOIN atencion a ON a.id_cita = c.id_cita"
                + " GROUP BY e.id_especialidad, e.nombre"
                + " ORDER BY COUNT(c.id_cita) DESC, e.nombre";
        List<Object[]> datos = new ArrayList<>();
        int tMed = 0, tCitas = 0, tAt = 0, tCan = 0;
        try (PreparedStatement ps = preparar(con, sql, on.params); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int med = rs.getInt(2), citas = rs.getInt(3), at = rs.getInt(4), can = rs.getInt(5);
                datos.add(new Object[]{rs.getString(1), med, citas, at, can});
                tMed += med;
                tCitas += citas;
                tAt += at;
                tCan += can;
            }
        }
        List<Object[]> filas = new ArrayList<>();
        for (Object[] d : datos) {
            filas.add(new Object[]{d[0], d[1], d[2], d[3], d[4], pct((Integer) d[2], tCitas)});
        }
        filas.add(new Object[]{"Total", tMed, tCitas, tAt, tCan, tCitas == 0 ? 0 : 100});
        return new Reporte(TipoReporte.CITAS_POR_ESPECIALIDAD,
                List.of("Especialidad", "Médicos", "Citas", "Atendidas", "Canceladas", "% de las citas"), filas, true);
    }

    private static Reporte pacientes(Connection con, Filtro f) throws SQLException {
        Condiciones w = new Condiciones();
        w.fechas("c.fecha_cita", f);
        w.medico("c.id_medico", f);
        String sql = "SELECT CONCAT(p.apellidos, ', ', p.nombres), p.documento, p.fecha_nacimiento,"
                + " COUNT(c.id_cita), COUNT(a.id_atencion), MAX(c.fecha_cita)"
                + " FROM paciente p"
                + " INNER JOIN cita c ON c.id_paciente = p.id_paciente"
                + " LEFT JOIN atencion a ON a.id_cita = c.id_cita"
                + w.where()
                + " GROUP BY p.id_paciente, p.apellidos, p.nombres, p.documento, p.fecha_nacimiento"
                + " ORDER BY COUNT(c.id_cita) DESC, MAX(c.fecha_cita) DESC, p.apellidos, p.nombres";
        List<Object[]> filas = new ArrayList<>();
        LocalDate hoy = LocalDate.now();
        try (PreparedStatement ps = preparar(con, sql, w.params); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Date nac = rs.getDate(3);
                Object edad = nac == null ? "" : (Object) (int) ChronoUnit.YEARS.between(nac.toLocalDate(), hoy);
                filas.add(new Object[]{rs.getString(1), rs.getString(2), edad, rs.getInt(4), rs.getInt(5),
                        fecha(rs.getDate(6))});
            }
        }
        return new Reporte(TipoReporte.PACIENTES,
                List.of("Paciente", "Documento", "Edad", "Citas", "Atenciones", "Última cita"), filas, false);
    }

    private static Reporte diagnosticos(Connection con, Filtro f) throws SQLException {
        Condiciones w = new Condiciones();
        w.fechas("c.fecha_cita", f);
        w.medico("c.id_medico", f);
        String sql = "SELECT a.diagnostico, c.id_paciente FROM atencion a"
                + " INNER JOIN cita c ON c.id_cita = a.id_cita" + w.where();
        // Se agrupa en Java ignorando mayúsculas y espacios sobrantes ("Gripe" = "gripe ")
        Map<String, String> nombre = new LinkedHashMap<>();
        Map<String, Integer> casos = new LinkedHashMap<>();
        Map<String, Set<Integer>> pacientes = new LinkedHashMap<>();
        int total = 0;
        try (PreparedStatement ps = preparar(con, sql, w.params); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String d = texto(rs.getString(1)).trim().replaceAll("\\s+", " ");
                if (d.isEmpty()) {
                    d = "(sin diagnóstico)";
                }
                String clave = d.toLowerCase(Locale.ROOT);
                nombre.putIfAbsent(clave, d);
                casos.merge(clave, 1, Integer::sum);
                pacientes.computeIfAbsent(clave, k -> new HashSet<>()).add(rs.getInt(2));
                total++;
            }
        }
        List<String> claves = new ArrayList<>(casos.keySet());
        final int totalFinal = total;
        claves.sort((x, y) -> {
            int c = Integer.compare(casos.get(y), casos.get(x));
            return c != 0 ? c : nombre.get(x).compareToIgnoreCase(nombre.get(y));
        });
        List<Object[]> filas = new ArrayList<>();
        for (String k : claves) {
            filas.add(new Object[]{nombre.get(k), casos.get(k), pacientes.get(k).size(), pct(casos.get(k), totalFinal)});
        }
        filas.add(new Object[]{"Total de atenciones", total, "", total == 0 ? 0 : 100});
        return new Reporte(TipoReporte.DIAGNOSTICOS,
                List.of("Diagnóstico", "Casos", "Pacientes", "% de las atenciones"), filas, true);
    }

    private static Reporte medicamentos(Connection con, Filtro f) throws SQLException {
        Condiciones w = new Condiciones();
        w.fechas("c.fecha_cita", f);
        w.medico("c.id_medico", f);
        String sql = "SELECT md.nombre, md.presentacion, COUNT(*), COUNT(DISTINCT r.id_receta),"
                + " COUNT(DISTINCT c.id_paciente)"
                + " FROM detalle_receta d"
                + " INNER JOIN medicamento md ON md.id_medicamento = d.id_medicamento"
                + " INNER JOIN receta r ON r.id_receta = d.id_receta"
                + " INNER JOIN atencion a ON a.id_atencion = r.id_atencion"
                + " INNER JOIN cita c ON c.id_cita = a.id_cita"
                + w.where()
                + " GROUP BY md.id_medicamento, md.nombre, md.presentacion"
                + " ORDER BY COUNT(*) DESC, md.nombre";
        List<Object[]> filas = new ArrayList<>();
        try (PreparedStatement ps = preparar(con, sql, w.params); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                filas.add(new Object[]{rs.getString(1), rs.getString(2), rs.getInt(3), rs.getInt(4), rs.getInt(5)});
            }
        }
        return new Reporte(TipoReporte.MEDICAMENTOS,
                List.of("Medicamento", "Presentación", "Veces recetado", "Recetas", "Pacientes"), filas, false);
    }

    // =====================================================================
    //  AYUDAS
    // =====================================================================

    /** Solo el administrador con sesión activa puede pedir reportes. */
    private static void exigirAdmin() throws ReglaNegocioException {
        if (!Permisos.puedeVerReportes()) {
            throw new ReglaNegocioException("Solo el administrador puede ver los reportes.");
        }
    }

    /** Arma el WHERE y la lista de parámetros en el mismo orden. */
    private static final class Condiciones {

        final List<String> partes = new ArrayList<>();
        final List<Object> params = new ArrayList<>();

        void agregar(String condicion, Object valor) {
            partes.add(condicion);
            params.add(valor);
        }

        void fechas(String columna, Filtro f) {
            fechasEn(columna, f);
        }

        void fechasEn(String columna, Filtro f) {
            if (f.desde() != null) {
                agregar(columna + " >= ?", f.desde());
            }
            if (f.hasta() != null) {
                agregar(columna + " <= ?", f.hasta());
            }
        }

        void medico(String columna, Filtro f) {
            if (f.idMedico() > 0) {
                agregar(columna + " = ?", f.idMedico());
            }
        }

        String where() {
            return partes.isEmpty() ? "" : " WHERE " + String.join(" AND ", partes);
        }

        /** Condiciones para anexar a un ON (" AND ..."), o vacío. */
        String sqlAnd() {
            return partes.isEmpty() ? "" : " AND " + String.join(" AND ", partes);
        }
    }

    private static PreparedStatement preparar(Connection con, String sql, List<Object> params) throws SQLException {
        PreparedStatement ps = con.prepareStatement(sql);
        for (int i = 0; i < params.size(); i++) {
            Object v = params.get(i);
            if (v instanceof LocalDate d) {
                ps.setDate(i + 1, Date.valueOf(d));
            } else if (v instanceof Integer n) {
                ps.setInt(i + 1, n);
            } else {
                ps.setString(i + 1, String.valueOf(v));
            }
        }
        return ps;
    }

    private static String fecha(Date d) {
        return d == null ? "" : d.toLocalDate().format(FECHA);
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

    /** Porcentaje entero redondeado (0 si el total es 0). */
    private static int pct(int parte, int total) {
        return total == 0 ? 0 : (int) Math.round(parte * 100.0 / total);
    }
}