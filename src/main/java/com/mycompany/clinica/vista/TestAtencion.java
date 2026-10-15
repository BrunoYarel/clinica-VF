package com.mycompany.clinica.vista;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class TestAtencion {
    public static void main(String[] args) {
        // Ejecutar en el hilo de eventos de Swing
        SwingUtilities.invokeLater(() -> {
            // Creamos un JFrame contenedor temporal para probar el JInternalFrame
            JFrame ventanaPrueba = new JFrame("Test - Mantenimiento de Atención Médica");
            ventanaPrueba.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ventanaPrueba.setSize(800, 600);
            ventanaPrueba.setLocationRelativeTo(null);

            // Instanciamos el formulario de atención
            FrmAtencion frmAtencion = new FrmAtencion();
            
            // Lo añadimos a la ventana de prueba
            ventanaPrueba.add(frmAtencion);
            
            // Hacemos visible el formulario interno y la ventana
            frmAtencion.setVisible(true);
            ventanaPrueba.setVisible(true);
        });
    }
}