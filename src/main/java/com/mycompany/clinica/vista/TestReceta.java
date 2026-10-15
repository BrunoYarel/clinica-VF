package com.mycompany.clinica.vista;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class TestReceta {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame ventanaPrueba = new JFrame("Test - Mantenimiento de Recetas Médicas");
            ventanaPrueba.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ventanaPrueba.setSize(800, 600);
            ventanaPrueba.setLocationRelativeTo(null);

            FrmReceta frmReceta = new FrmReceta();
            ventanaPrueba.add(frmReceta);

            frmReceta.setVisible(true);
            ventanaPrueba.setVisible(true);
        });
    }
}