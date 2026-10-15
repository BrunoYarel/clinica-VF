package com.mycompany.clinica;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexion {
    private static final String URL = "jdbc:mysql://localhost:3306/clinica"; // Reemplaza por el nombre real de tu BD
    private static final String USUARIO = "root";
    private static final String CLAVE = ""; 

    public static Connection conectar() {
        Connection conexion = null;
        try {
            // Registrar el driver de Maven
            Class.forName("com.mysql.cj.jdbc.Driver");
            conexion = DriverManager.getConnection(URL, USUARIO, CLAVE);
            System.out.println("¡Conexión establecida con éxito en XAMPP!");
        } catch (ClassNotFoundException e) {
            System.out.println("Error: No se encontró el driver de MySQL: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Error de conexión: " + e.getMessage());
        }
        return conexion;
    }
}
