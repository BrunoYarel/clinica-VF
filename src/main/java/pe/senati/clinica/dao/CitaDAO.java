package pe.senati.clinica.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import pe.senati.clinica.model.Cita;
import java.util.ArrayList;
import java.util.List;
import java.sql.ResultSet;

public class CitaDAO {

    private static final String URL = "jdbc:mysql://localhost:3306/clinica";
    private static final String USER = "root";
    private static final String PASSWORD = ""; 

        private Connection getConexion() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            System.out.println("Error de conexión en CitaDAO: " + e.getMessage());
            return null;
        }
    }

    public boolean registrarCita(int idPaciente, int idMedico, String fecha, String hora, String motivo) {
        String sql = "INSERT INTO cita (fecha_cita, hora_cita, motivo, estado, id_paciente, id_medico) VALUES (?, ?, ?, 'Pendiente', ?, ?)";

        try (Connection con = getConexion(); 
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, fecha);      
            ps.setString(2, hora);        
            ps.setString(3, motivo);      
            ps.setInt(4, idPaciente);
            ps.setInt(5, idMedico);

            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;

        } catch (SQLException e) {
            System.out.println("Error al registrar cita en BD: " + e.getMessage());
            return false;
        }
    }
    
    public List<Cita> listarCitas() {
        List<Cita> lista = new ArrayList<>();
        String sql = "SELECT id_cita, fecha_cita, hora_cita, motivo, estado, id_paciente, id_medico FROM cita";

        try (Connection con = getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Cita cita = new Cita();
                cita.setIdCita(rs.getInt("id_cita"));
                cita.setFechaCita(rs.getString("fecha_cita"));
                cita.setHoraCita(rs.getString("hora_cita"));
                cita.setMotivo(rs.getString("motivo"));
                cita.setEstado(rs.getString("estado"));
                cita.setIdPaciente(rs.getInt("id_paciente"));
                cita.setIdMedico(rs.getInt("id_medico"));
                
                lista.add(cita);
            }

        } catch (SQLException e) {
            System.out.println("Error al listar citas desde la BD: " + e.getMessage());
        }
        return lista;
    }
    
    public boolean modificarEstadoCita(int idCita, String nuevoEstado) {
        String sql = "UPDATE cita SET estado = ? WHERE id_cita = ?";

        try (Connection con = getConexion(); 
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nuevoEstado);
            ps.setInt(2, idCita);

            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;

        } catch (SQLException e) {
            System.out.println("Error al cambiar estado de la cita en BD: " + e.getMessage());
            return false;
        }
    }
}



