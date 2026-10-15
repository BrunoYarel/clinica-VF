
package com.mycompany.clinica;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLException;


public class Prueba {
    public static void main(String[] args){
        try (Connection cn = Conexion.conectar()){
            System.out.println("Conexion correcta con mySQL");
            
        }catch (SQLException e){
            System.out.println("Error: " + e.getMessage());
        }
    }
}
