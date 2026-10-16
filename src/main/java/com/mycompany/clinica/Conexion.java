package com.mycompany.clinica;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Única conexión a la base de datos para todo el proyecto (login, pacientes, médicos, citas...).
 *
 * Prueba las configuraciones locales más comunes (casa / instituto) en este orden:
 *     3306 + clave vacía  ->  3306 + "root"  ->  3307 + clave vacía  ->  3307 + "root"
 * La primera es la configuración estándar de XAMPP, así que en ese caso se conecta al primer intento.
 *
 * Si no logra conectarse lanza una SQLException con un mensaje claro. Así ningún módulo
 * recibe una conexión nula ni necesita comprobarlo: solo captura SQLException.
 *
 * Idea de diseño: no se guarda una conexión compartida. Cada operación abre la suya y la cierra al
 * terminar (try-with-resources), así los módulos no dependen unos de otros ni comparten estado en memoria.
 * Lo único que comparten es la base de datos, que es la fuente de verdad del sistema.
 */
public final class Conexion {

    private static final Logger LOGGER = Logger.getLogger(Conexion.class.getName());
    private static final String BASE_DATOS = "clinica"; // Reemplaza por el nombre real de tu BD
    private static final String USUARIO = "root";
    private static final String[] PUERTOS = {"3306", "3307"}; // estándar y alterno de XAMPP
    private static final String[] CLAVES = {"", "root"};      // vacía y 'root'

    private Conexion() {
    }

    /**
     * Abre una conexión nueva. Quien la recibe debe cerrarla (try-with-resources).
     *
     * @throws SQLException si no hay driver o si MySQL no responde con ninguna de las configuraciones
     */
    public static Connection conectar() throws SQLException {
        try {
            // Registrar el driver de Maven
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("No se encontró el driver de MySQL. "
                    + "Verifique la dependencia mysql-connector-j en el pom.xml.", e);
        }

        SQLException primerError = null; // el de la configuración estándar, el más útil para diagnosticar
        for (String puerto : PUERTOS) {
            for (String clave : CLAVES) {
                String url = "jdbc:mysql://localhost:" + puerto + "/" + BASE_DATOS;
                try {
                    return DriverManager.getConnection(url, USUARIO, clave);
                } catch (SQLException e) {
                    if (primerError == null) {
                        primerError = e;
                    }
                    // sigue con la siguiente combinación
                }
            }
        }

        LOGGER.log(Level.WARNING, "No se pudo conectar a MySQL con ninguna configuración: {0}",
                primerError != null ? primerError.getMessage() : "sin detalle");
        String detalle = primerError != null ? "\nDetalle: " + primerError.getMessage() : "";
        throw new SQLException("No se pudo conectar a la base de datos. "
                + "Verifique que MySQL esté encendido en XAMPP." + detalle, primerError);
    }
}