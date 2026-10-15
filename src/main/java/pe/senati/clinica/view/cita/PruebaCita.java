package pe.senati.clinica.view.cita;

import javax.swing.JFrame;
import javax.swing.JDesktopPane;

public class PruebaCita {

    public static void main(String[] args) {
        try { 
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName()); 
        } catch(Exception e) {}

        JFrame ventanaPrincipal = new JFrame("Gestión de Citas - Vista de Desarrollo");
        ventanaPrincipal.setExtendedState(JFrame.MAXIMIZED_BOTH);
        ventanaPrincipal.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JDesktopPane dskPrincipal = new JDesktopPane();
        ventanaPrincipal.setContentPane(dskPrincipal);
        ventanaPrincipal.setVisible(true);

        FrmCitas frm = new FrmCitas();
        dskPrincipal.add(frm);
        
        frm.setSize(850, 600); 
        int x = (dskPrincipal.getWidth() - frm.getWidth()) / 2;
        int y = (dskPrincipal.getHeight() - frm.getHeight()) / 2;
        frm.setLocation(Math.max(0, x), Math.max(0, y));
        
        frm.setVisible(true);
    }
}
