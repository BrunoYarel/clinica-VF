package com.mycompany.clinica.vista;

import com.mycompany.clinica.servicio.Sesion;
import com.mycompany.clinica.servicio.UsuarioService;
import java.awt.*;
import java.sql.SQLException;
import javax.swing.*;

/**
 * Ventana (modal) para que el usuario que inició sesión cambie su propia contraseña.
 * Sirve para cualquier rol: Admin, Médico o Recepcionista.
 *
 * Uso desde otra ventana:
 *     new CambiarPassword(this).setVisible(true);
 */
public class CambiarPassword extends JDialog {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(CambiarPassword.class.getName());

    private JPasswordField txtActual;
    private JPasswordField txtNueva;
    private JPasswordField txtConfirmar;
    private JLabel lblError;
    private JButton btnCambiar;
    private JButton btnCancelar;

    public CambiarPassword(Frame parent) {
        super(parent, "Cambiar contraseña", true);
        initComponents();
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        // ---------- CABECERA: título y quién está cambiando la contraseña ----------
        String quien = Sesion.estaActiva()
                ? "Usuario: " + Sesion.getUsername() + " · " + Estilo.nombreRol(Sesion.getRol())
                : "Sin sesión iniciada";

        JPanel titulos = new JPanel(new GridLayout(2, 1, 0, 3));
        titulos.setOpaque(false);
        titulos.add(Estilo.texto("Cambiar contraseña", Font.BOLD, 22, Color.WHITE));
        titulos.add(Estilo.texto(quien, Font.PLAIN, 13, Estilo.TEXTO_CLARO));

        JPanel cabecera = Estilo.panelDegradado(Estilo.PRIMARIO_OSCURO, Estilo.AZUL, 0);
        cabecera.setLayout(new BorderLayout(14, 0));
        cabecera.setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));
        cabecera.add(Estilo.logo(50, Color.WHITE, Estilo.PRIMARIO_OSCURO), BorderLayout.WEST);
        cabecera.add(titulos, BorderLayout.CENTER);

        // ---------- FORMULARIO ----------
        JPanel cuerpo = new JPanel(new GridBagLayout());
        cuerpo.setBackground(Color.WHITE);
        cuerpo.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        txtActual = Estilo.campoPassword();
        Estilo.agregar(cuerpo, Estilo.campo("Contraseña actual", txtActual), 0, 0);

        txtNueva = Estilo.campoPassword();
        Estilo.agregar(cuerpo, Estilo.campo("Nueva contraseña", txtNueva), 1, 16);
        Estilo.agregar(cuerpo, Estilo.texto("Mínimo " + UsuarioService.MIN_PASSWORD + " caracteres",
                Font.PLAIN, 12, Estilo.TEXTO_SUAVE), 2, 4);

        txtConfirmar = Estilo.campoPassword();
        Estilo.agregar(cuerpo, Estilo.campo("Confirmar nueva contraseña", txtConfirmar), 3, 12);

        Estilo.agregar(cuerpo, Estilo.verPassword(txtActual, txtNueva, txtConfirmar), 4, 10);

        lblError = Estilo.textoError();
        Estilo.agregar(cuerpo, lblError, 5, 6);

        // ---------- BOTONES ----------
        btnCancelar = Estilo.botonSecundario("Cancelar");
        btnCancelar.addActionListener(e -> dispose());

        btnCambiar = Estilo.botonPrimario("Guardar cambios");
        btnCambiar.addActionListener(e -> cambiar());

        JPanel botones = new JPanel(new GridLayout(1, 2, 12, 0));
        botones.setOpaque(false);
        botones.add(btnCancelar);
        botones.add(btnCambiar);
        Estilo.agregar(cuerpo, botones, 6, 8);

        // ---------- ARMADO ----------
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(cabecera, BorderLayout.NORTH);
        raiz.add(cuerpo, BorderLayout.CENTER);
        setContentPane(raiz);

        getRootPane().setDefaultButton(btnCambiar);
    }

    /** BOTÓN GUARDAR CAMBIOS: valida los datos y actualiza la contraseña en la base de datos. */
    private void cambiar() {
        lblError.setText(" ");

        if (!Sesion.estaActiva()) {
            JOptionPane.showMessageDialog(this, "No hay una sesión iniciada.", "Error", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        String actual = new String(txtActual.getPassword());
        String nueva = new String(txtNueva.getPassword());
        String confirmar = new String(txtConfirmar.getPassword());

        // 1. Validaciones (antes de tocar la base de datos)
        if (actual.isEmpty() || nueva.isEmpty() || confirmar.isEmpty()) {
            lblError.setText("Por favor, complete todos los campos.");
            return;
        }
        // (los mensajes son cortos a propósito: el texto de error debe caber en una línea)
        if (nueva.length() < UsuarioService.MIN_PASSWORD) {
            lblError.setText("La nueva contraseña es muy corta.");
            return;
        }
        if (!nueva.equals(confirmar)) {
            lblError.setText("Las contraseñas nuevas no coinciden.");
            return;
        }
        if (nueva.equals(actual)) {
            lblError.setText("La nueva debe ser distinta de la actual.");
            return;
        }

        // 2. Base de datos: verifica la actual y guarda la nueva
        try {
            if (UsuarioService.cambiarPassword(Sesion.getIdUsuario(), actual, nueva)) {
                JOptionPane.showMessageDialog(this, "Tu contraseña fue actualizada correctamente.",
                        "Contraseña actualizada", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                lblError.setText("La contraseña actual es incorrecta.");
                txtActual.setText("");
                txtActual.requestFocus();
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Error al cambiar la contraseña", e);
            JOptionPane.showMessageDialog(this, "Error de base de datos:\n" + e.getMessage(),
                    "Error SQL", JOptionPane.ERROR_MESSAGE);
        }
    }
}