package com.mycompany.clinica.controlador;

import com.mycompany.clinica.Conexion;
import com.mycompany.clinica.modelo.Paciente;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Operaciones de base de datos para pacientes e historial clínico. */
public class PacienteControlador {
    
    // Método para insertar un paciente en XAMPP
    public boolean registrarPaciente(Paciente paciente) {
        String sql = "INSERT INTO PACIENTE (nombres, apellidos, documento, fecha_nacimiento, telefono, email) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, paciente.getNombres());
            ps.setString(2, paciente.getApellidos());
            ps.setString(3, paciente.getDocumento());
            ps.setString(4, paciente.getFechaNacimiento());
            ps.setString(5, paciente.getTelefono());
            ps.setString(6, paciente.getEmail());
            
            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0; // Retorna true si se guardó con éxito
            
        } catch (SQLException e) {
            System.out.println("Error al registrar paciente: " + e.getMessage());
            return false;
        }
    }

        // Método actualizado en PacienteControlador.java para mostrar todo por defecto
    public javax.swing.table.DefaultTableModel consultarHistorialClinico(String dni, int idMedicoLogueado, String rolUsuario) {
        String[] titulos = {"Paciente (DNI)", "Fecha/Hora", "Motivo Cita", "Síntomas", "Diagnóstico", "Observaciones"};
        javax.swing.table.DefaultTableModel modeloTabla = new javax.swing.table.DefaultTableModel(null, titulos);
        
        // Consulta base uniendo Paciente, Cita y Atencion (Agregamos el DNI y Nombre del paciente al inicio)
        String sql = "SELECT "
        + "CONCAT(p.nombres, ' ', p.apellidos, ' (', p.documento, ')') AS datos_paciente, "
        + "CONCAT(c.fecha_cita, ' ', c.hora_cita) AS fecha_hora_cita, "
        + "c.motivo, "
        + "a.sintomas, "
        + "a.diagnostico, "
        + "a.observaciones "
        + "FROM paciente p "
        + "INNER JOIN cita c ON p.id_paciente = c.id_paciente "
        + "INNER JOIN atencion a ON c.id_cita = a.id_cita "
        + "WHERE 1=1"; // Truco SQL para poder concatenar ANDs dinámicamente

        // Filtro por DNI solo si el usuario escribió algo
        if (dni != null && !dni.trim().isEmpty()) {
            sql += " AND p.documento = ?";
        }

        // Filtro de seguridad si es Médico
        if ("Medico".equalsIgnoreCase(rolUsuario) || "Médico".equalsIgnoreCase(rolUsuario)) {
            sql += " AND c.id_medico = ?";
        }
                     
        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            int paramIndex = 1;
            
            // Inyectar DNI si corresponde
            if (dni != null && !dni.trim().isEmpty()) {
                ps.setString(paramIndex++, dni.trim());
            }
            
            // Inyectar ID Médico si corresponde
            if (rolUsuario.equalsIgnoreCase("Médico") || rolUsuario.equalsIgnoreCase("Medico")) {
                ps.setInt(paramIndex++, idMedicoLogueado);
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                Object[] fila = new Object[6];
                while (rs.next()) {
                    fila[0] = rs.getString("datos_paciente");
                    fila[1] = rs.getString("fecha_hora_cita");
                    fila[2] = rs.getString("motivo");
                    fila[3] = rs.getString("sintomas");
                    fila[4] = rs.getString("diagnostico");
                    fila[5] = rs.getString("observaciones");
                    modeloTabla.addRow(fila);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al consultar el historial: " + e.getMessage());
        }
        return modeloTabla;
    }

    
    // Método para consultar y listar todos los pacientes
    public javax.swing.table.DefaultTableModel listarPacientes() {
        String[] titulos = {
            "ID", "Documento", "Nombres", "Apellidos",
            "Fecha de nacimiento", "Teléfono", "Email"
        };

        javax.swing.table.DefaultTableModel modelo =
                new javax.swing.table.DefaultTableModel(null, titulos) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };

        String sql = "SELECT id_paciente, documento, nombres, apellidos, "
                   + "fecha_nacimiento, telefono, email "
                   + "FROM paciente ORDER BY id_paciente DESC";

        try (Connection con = Conexion.conectar()) {
            if (con == null) {
                System.out.println("No se pudo conectar a la base de datos.");
                return modelo;
            }

            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    modelo.addRow(new Object[]{
                        rs.getInt("id_paciente"),
                        rs.getString("documento"),
                        rs.getString("nombres"),
                        rs.getString("apellidos"),
                        rs.getString("fecha_nacimiento"), 
                        rs.getString("telefono"),
                        rs.getString("email")
                    });
                }
            }

        } catch (SQLException e) {
            System.out.println("Error al listar pacientes: " + e.getMessage());
        }

        return modelo;
    }
    
    // Buscar pacientes por documento, nombres o apellidos
    public javax.swing.table.DefaultTableModel buscarPacientes(String texto) {
        String[] titulos = {
            "ID", "Documento", "Nombres", "Apellidos",
            "Fecha de nacimiento", "Teléfono", "Email"
        };

        javax.swing.table.DefaultTableModel modelo =
                new javax.swing.table.DefaultTableModel(null, titulos) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };

        String sql = "SELECT id_paciente, documento, nombres, apellidos, "
                   + "fecha_nacimiento, telefono, email "
                   + "FROM paciente "
                   + "WHERE documento LIKE ? OR nombres LIKE ? "
                   + "OR apellidos LIKE ? "
                   + "ORDER BY id_paciente DESC";

        try (Connection con = Conexion.conectar()) {
            if (con == null) {
                return modelo;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                String filtro = "%" + texto.trim() + "%";

                ps.setString(1, filtro);
                ps.setString(2, filtro);
                ps.setString(3, filtro);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        modelo.addRow(new Object[]{
                            rs.getInt("id_paciente"),
                            rs.getString("documento"),
                            rs.getString("nombres"),
                            rs.getString("apellidos"),
                            rs.getString("fecha_nacimiento"), 
                            rs.getString("telefono"),
                            rs.getString("email")
                        });
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al buscar pacientes: " + e.getMessage());
        }

        return modelo;
    }

    public boolean actualizarPaciente(Paciente paciente, int idPaciente) {
        String sql = "UPDATE paciente SET documento = ?, nombres = ?, "
                   + "apellidos = ?, fecha_nacimiento = ?, telefono = ?, "
                   + "email = ? WHERE id_paciente = ?";

        try (Connection con = Conexion.conectar()) {
            if (con == null) {
                return false;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, paciente.getDocumento());
                ps.setString(2, paciente.getNombres());
                ps.setString(3, paciente.getApellidos());
                ps.setString(4, paciente.getFechaNacimiento());
                ps.setString(5, paciente.getTelefono());
                ps.setString(6, paciente.getEmail());
                ps.setInt(7, idPaciente);

                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("Error al actualizar paciente: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminarPaciente(int idPaciente) {
        String sql = "DELETE FROM paciente WHERE id_paciente = ?";

        try (Connection con = Conexion.conectar()) {
            if (con == null) {
                return false;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idPaciente);

                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("Error al eliminar paciente: " + e.getMessage());
            return false;
        }
    }
}
