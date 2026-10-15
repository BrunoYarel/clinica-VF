package view;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import service.Sesion;

/**
 * Menú principal. Diseño:
 *   - Izquierda: barra lateral oscura con el logo, las opciones de cuenta y el usuario.
 *   - Derecha: banner de bienvenida y tarjetas con los módulos que ve cada rol.
 */
public class MenuPrincipal extends JFrame {

    private JButton btnCambiarPassword;
    private JButton btnCrearUsuario;
    private JButton btnCerrarSesion;

    public MenuPrincipal() {
        super("Clínica - Menú principal");
        initComponents();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(crearMenuLateral(), BorderLayout.WEST);
        raiz.add(crearContenido(), BorderLayout.CENTER);
        setContentPane(raiz);

        setSize(1000, 620);
        setMinimumSize(new Dimension(860, 540));
    }

    // =====================================================================
    //  BARRA LATERAL (izquierda)
    // =====================================================================
    private JPanel crearMenuLateral() {
        JPanel lateral = new JPanel(new BorderLayout());
        lateral.setBackground(Estilo.AZUL_OSCURO);
        lateral.setPreferredSize(new Dimension(250, 0));

        // --- Arriba: logo y nombre ---
        JPanel marca = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        marca.setOpaque(false);
        marca.setBorder(BorderFactory.createEmptyBorder(26, 14, 26, 14));
        marca.add(Estilo.logo(40, Estilo.PRIMARIO, Color.WHITE));
        marca.add(Estilo.texto("Clínica", Font.BOLD, 22, Color.WHITE));

        // --- Centro: opciones de cuenta ---
        btnCambiarPassword = Estilo.botonMenu("Cambiar contraseña");
        btnCambiarPassword.addActionListener(e -> abrirCambiarPassword());

        btnCrearUsuario = Estilo.botonMenu("Crear nuevo perfil");
        btnCrearUsuario.addActionListener(e -> abrirNuevoUsuario());

        JPanel opciones = new JPanel(new GridLayout(0, 1, 0, 6));
        opciones.setOpaque(false);
        opciones.add(Estilo.texto("  CUENTA", Font.BOLD, 11, new Color(100, 116, 139)));
        opciones.add(btnCambiarPassword);
        if (Sesion.esAdmin()) {                 // solo el administrador puede crear perfiles
            opciones.add(btnCrearUsuario);
        }

        JPanel centro = new JPanel(new BorderLayout());
        centro.setOpaque(false);
        centro.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        centro.add(opciones, BorderLayout.NORTH);     // NORTH para que los botones no se estiren

        // --- Abajo: datos del usuario y cerrar sesión ---
        String nombre = Sesion.estaActiva() ? Sesion.getUsername() : "Invitado";
        String rol = Sesion.estaActiva() ? Estilo.nombreRol(Sesion.getRol()) : "Sin sesión";

        JPanel datos = new JPanel(new GridLayout(2, 1));
        datos.setOpaque(false);
        datos.add(Estilo.texto(nombre, Font.BOLD, 14, Color.WHITE));
        datos.add(Estilo.texto(rol, Font.PLAIN, 12, new Color(148, 163, 184)));

        JPanel usuario = new JPanel(new BorderLayout(12, 0));
        usuario.setOpaque(false);
        usuario.setBorder(BorderFactory.createEmptyBorder(0, 6, 14, 6));
        usuario.add(Estilo.icono(nombre.substring(0, 1).toUpperCase(), Estilo.PRIMARIO, 42), BorderLayout.WEST);
        usuario.add(datos, BorderLayout.CENTER);

        btnCerrarSesion = Estilo.botonMenu("Cerrar sesión");
        btnCerrarSesion.addActionListener(e -> cerrarSesion());

        JPanel abajo = new JPanel(new BorderLayout());
        abajo.setOpaque(false);
        abajo.setBorder(BorderFactory.createEmptyBorder(0, 14, 18, 14));
        abajo.add(usuario, BorderLayout.NORTH);
        abajo.add(btnCerrarSesion, BorderLayout.SOUTH);

        lateral.add(marca, BorderLayout.NORTH);
        lateral.add(centro, BorderLayout.CENTER);
        lateral.add(abajo, BorderLayout.SOUTH);
        return lateral;
    }

    // =====================================================================
    //  CONTENIDO (derecha)
    // =====================================================================
    private JPanel crearContenido() {
        JPanel contenido = new JPanel(new BorderLayout(0, 26));
        contenido.setBackground(Estilo.FONDO);
        contenido.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

        // --- Banner de bienvenida ---
        String nombre = Sesion.estaActiva() ? Sesion.getUsername() : "Invitado";
        String rol = Sesion.estaActiva() ? Estilo.nombreRol(Sesion.getRol()) : "Sin sesión";

        JPanel banner = Estilo.panelDegradado(Estilo.PRIMARIO, Estilo.AZUL, 24);
        banner.setLayout(new GridLayout(2, 1, 0, 4));
        banner.setBorder(BorderFactory.createEmptyBorder(24, 30, 24, 30));
        banner.add(Estilo.texto("¡Bienvenido, " + nombre + "!", Font.BOLD, 26, Color.WHITE));
        banner.add(Estilo.texto("Sesión iniciada como " + rol + ". Elige un módulo para comenzar.",
                Font.PLAIN, 14, Estilo.TEXTO_CLARO));

        // --- Tarjetas de módulos (según el rol) ---
        JPanel tarjetas = new JPanel(new GridLayout(0, 3, 20, 20));
        tarjetas.setOpaque(false);

        if (Sesion.esAdmin() || Sesion.esRecepcionista()) {
            tarjetas.add(crearTarjeta("Pacientes", "Datos de pacientes", "P", new Color(13, 148, 136)));
            tarjetas.add(crearTarjeta("Citas", "Agenda de citas", "C", new Color(37, 99, 235)));
        }
        if (Sesion.esMedico()) {
            tarjetas.add(crearTarjeta("Atenciones", "Consultas y recetas", "A", new Color(124, 58, 237)));
        }

        JPanel envoltura = new JPanel(new BorderLayout());   // evita que las tarjetas se estiren hacia abajo
        envoltura.setOpaque(false);
        envoltura.add(tarjetas, BorderLayout.NORTH);

        JPanel zonaModulos = new JPanel(new BorderLayout(0, 14));
        zonaModulos.setOpaque(false);
        zonaModulos.add(Estilo.texto("Módulos del sistema", Font.BOLD, 18, Estilo.TEXTO), BorderLayout.NORTH);
        zonaModulos.add(envoltura, BorderLayout.CENTER);

        contenido.add(banner, BorderLayout.NORTH);
        contenido.add(zonaModulos, BorderLayout.CENTER);
        return contenido;
    }

    /** Crea una tarjeta clicable con icono, título y descripción. */
    private JPanel crearTarjeta(String titulo, String descripcion, String letra, Color color) {
        JPanel tarjeta = new JPanel(new BorderLayout(0, 14));
        tarjeta.setBackground(Color.WHITE);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BORDE, 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        tarjeta.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel filaIcono = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filaIcono.setOpaque(false);
        filaIcono.add(Estilo.icono(letra, color, 46));

        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 2));
        textos.setOpaque(false);
        textos.add(Estilo.texto(titulo, Font.BOLD, 17, Estilo.TEXTO));
        textos.add(Estilo.texto(descripcion, Font.PLAIN, 12, Estilo.TEXTO_SUAVE));

        tarjeta.add(filaIcono, BorderLayout.NORTH);
        tarjeta.add(textos, BorderLayout.CENTER);

        // Clic = abrir el módulo; mouse encima = fondo turquesa muy claro
        tarjeta.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                abrirModulo(titulo);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                tarjeta.setBackground(new Color(240, 253, 250));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                tarjeta.setBackground(Color.WHITE);
            }
        });
        return tarjeta;
    }

    // =====================================================================
    //  ACCIONES DE LOS BOTONES
    // =====================================================================

    /** CLIC EN UNA TARJETA: aquí cada compañero abre la ventana de su módulo. */
    private void abrirModulo(String modulo) {
        // --- INTEGRACIÓN DEL MÓDULO DE CITAS (ÁREA A4) ---
        if (modulo.equals("Citas")) {
            pe.senati.clinica.view.cita.FrmCitas ventanaCitas = new pe.senati.clinica.view.cita.FrmCitas();
            ventanaCitas.setVisible(true);
            return;
        }
        
        // Ejemplo para cuando exista la ventana de Pacientes:
        // if (modulo.equals("Pacientes")) { new VentanaPacientes().setVisible(true); return; }
        
        JOptionPane.showMessageDialog(this, "El módulo de " + modulo + " estará disponible pronto.",
                modulo, JOptionPane.INFORMATION_MESSAGE);
    }


    /** BOTÓN CAMBIAR CONTRASEÑA: abre el diálogo (modal, bloquea el menú hasta cerrarlo). */
    private void abrirCambiarPassword() {
        new CambiarPassword(this).setVisible(true);
    }

    /** BOTÓN CREAR NUEVO PERFIL (solo Admin): abre el registro. */
    private void abrirNuevoUsuario() {
        new NuevoUsuario().setVisible(true);
        dispose();
    }

    /** BOTÓN CERRAR SESIÓN: borra la sesión y vuelve al login. */
    private void cerrarSesion() {
        Sesion.cerrar();
        new Login().setVisible(true);
        dispose();
    }
}