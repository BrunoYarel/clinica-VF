package com.mycompany.clinica.dao;

import com.mycompany.clinica.Conexion;
import com.mycompany.clinica.modelo.Especialidad;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de especialidades.
 * Los errores de la base de datos se lanzan como SQLException: la ventana decide qué mostrar.
 */
public class EspecialidadDAO {

    private static final String SQL_LISTAR = "SELECT id_especialidad, nombre FROM especialidad";
    private static final String SQL_INSERTAR = "INSERT INTO especialidad (nombre, descripcion) VALUES (?, ?)";

    /** Todas las especialidades (id + nombre), pensadas para llenar el combo de FrmMedico. */
    public List<Especialidad> listarEspecialidades() throws SQLException {
        List<Especialidad> lista = new ArrayList<>();
        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(new Especialidad(rs.getInt("id_especialidad"), rs.getString("nombre")));
            }
        }
        return lista;
    }

    public boolean registrarEspecialidad(Especialidad especialidad) throws SQLException {
        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(SQL_INSERTAR)) {

            ps.setString(1, especialidad.getNombre());
            ps.setString(2, especialidad.getDescripcion());
            return ps.executeUpdate() > 0;
        }
    }
}
