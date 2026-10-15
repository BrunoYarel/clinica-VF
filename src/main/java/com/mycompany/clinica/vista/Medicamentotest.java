package com.mycompany.clinica.vista;

import javax.swing.JFrame;
import javax.swing.JDesktopPane;

public class Medicamentotest {
    public static void main(String[] args) {
        // 1. Creamos la ventana principal de pruebas
        JFrame ventanaPrincipal = new JFrame("Panel de Pruebas de Óscar");
        ventanaPrincipal.setSize(800, 600);
        ventanaPrincipal.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventanaPrincipal.setLocationRelativeTo(null);

        // 2. Creamos el contenedor para poder meter tu JInternalFrame
        JDesktopPane contenedor = new JDesktopPane();
        ventanaPrincipal.setContentPane(contenedor);

        // 3. Instanciamos tu formulario de medicamentos con la tabla
        FrmMedicamento formulario = new FrmMedicamento();
        contenedor.add(formulario);
        formulario.setVisible(true);

        // 4. Mostramos todo en la pantalla
        ventanaPrincipal.setVisible(true);
    }
}
