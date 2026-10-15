package com.mycompany.clinica.vista;

import com.mycompany.clinica.servicio.Permisos;
import com.mycompany.clinica.servicio.Sesion;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;

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
        if (Permisos.puedeVer(Permisos.Pantalla.MI_AGENDA)) {    // los módulos del médico (Atención, Recetas, Historial) van aquí
            opciones.add(Estilo.texto("  MÓDULOS", Font.BOLD, 11, new Color(100, 116, 139)));
            JButton btnAtencion = Estilo.botonMenu("Atención médica");
            btnAtencion.addActionListener(e -> abrirModulo("Atención"));
            opciones.add(btnAtencion);
            JButton btnRecetas = Estilo.botonMenu("Recetas médicas");
            btnRecetas.addActionListener(e -> abrirModulo("Recetas"));
            opciones.add(btnRecetas);
            JButton btnHistorial = Estilo.botonMenu("Historial clínico");
            btnHistorial.addActionListener(e -> abrirModulo("Historial"));
            opciones.add(btnHistorial);
        }
        opciones.add(Estilo.texto("  CUENTA", Font.BOLD, 11, new Color(100, 116, 139)));
        opciones.add(btnCambiarPassword);
        if (Permisos.puedeGestionarUsuarios()) {                 // solo el administrador puede crear perfiles
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

        for (Permisos.Pantalla pantalla : Permisos.pantallasDelRol()) {
            String titulo = pantalla.getTitulo();
            switch (pantalla) {
                case PACIENTES:
                    tarjetas.add(crearTarjeta(titulo, "Datos de pacientes", "P", new Color(13, 148, 136)));
                    break;
                case CITAS:
                    tarjetas.add(crearTarjeta(titulo, "Agenda de citas", "C", new Color(37, 99, 235)));
                    break;
                case ESPECIALIDADES:
                    tarjetas.add(crearTarjeta(titulo, "Registro de especialidades", "E", new Color(217, 119, 6)));
                    break;
                case MEDICOS:
                    tarjetas.add(crearTarjeta(titulo, "Registro de médicos", "M", new Color(219, 39, 119)));
                    break;
                case MEDICAMENTOS:
                    tarjetas.add(crearTarjeta(titulo, "Catálogo de medicamentos", "Rx", new Color(22, 163, 74)));
                    break;
                case HISTORIAL:
                    tarjetas.add(crearTarjeta(titulo, "Historial clínico (consulta)", "H", new Color(8, 145, 178)));
                    break;
                case REPORTES:
                    tarjetas.add(crearTarjeta(titulo, "Reportes del sistema", "R", new Color(79, 70, 229)));
                    break;
                case USUARIOS:
                    tarjetas.add(crearTarjeta(titulo, "Gestión de usuarios", "U", new Color(71, 85, 105)));
                    break;
                case MI_AGENDA:
                    tarjetas.add(crearTarjeta(titulo, "Tus citas y atenciones", "A", new Color(124, 58, 237)));
                    break;
                case ATENCION:
                    tarjetas.add(crearTarjeta(titulo, "Atención de pacientes", "At", new Color(225, 29, 72)));
                    break;
                case RECETAS:
                    tarjetas.add(crearTarjeta(titulo, "Recetas de las atenciones", "Re", new Color(234, 88, 12)));
                    break;
                default:
                    break;
            }
        }

        JPanel envoltura = new JPanel(new BorderLayout());
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
    /** CLIC EN UNA TARJETA: Conecta cada módulo con su respectiva ventana. */
    private void abrirModulo(String modulo) {
    Permisos.Pantalla pantalla = Permisos.Pantalla.deTitulo(modulo);
    if (pantalla != null && !Permisos.puedeVer(pantalla)) {
    JOptionPane.showMessageDialog(this, "No tiene permiso para acceder a este módulo.",
    modulo, JOptionPane.WARNING_MESSAGE);
    return;
    }
    // INTEGRACIÓN DE TUS MÓDULOS (Pacientes e Historial Clínico)
    if (modulo.equals("Pacientes") || "Pacientes".equalsIgnoreCase(modulo)) {
    abrirPanelComoVentana("Gestión de Pacientes", new FrmPacientes());
    return;
    }
    if (modulo.equals("Historial") || "Historial".equalsIgnoreCase(modulo)) {
    abrirPanelComoVentana("Historial Clínico de Pacientes", new FrmHistorial());
    return;
    }
    if (modulo.equals("Usuarios")) {
    abrirModuloEn(new FrmUsuarios());
    return;
    }
    if (modulo.equals("Médicos")) {
    abrirInternoEn("Gestión de Médicos", new com.mycompany.clinica.FrmMedico());
    return;
    }
    if (modulo.equals("Especialidades")) {
    abrirInternoEn("Registro de Especialidades", new com.mycompany.clinica.FrmEspecialidad());
    return;
    }
    if (modulo.equals("Medicamentos")) {
    abrirInternoEn("Gestión de Medicamentos", new FrmMedicamento());
    return;
    }
    if (modulo.equals("Atención")) {
    abrirInternoEn("Gestión de Atención Médica", new FrmAtencion());
    return;
    }
    if (modulo.equals("Recetas")) {
    abrirInternoEn("Gestión de Recetas Médicas", new FrmReceta());
    return;
    }
    JOptionPane.showMessageDialog(this, "El módulo de " + modulo + " estará disponible pronto.",
    modulo, JOptionPane.INFORMATION_MESSAGE);
    }
    private void abrirModuloEn(JFrame modulo) {
    modulo.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    modulo.addWindowListener(new WindowAdapter() {
    @Override
    public void windowClosed(WindowEvent e) {
    MenuPrincipal.this.setVisible(true);
    }
    });
    modulo.setVisible(true);
    setVisible(false);
    }
    private void abrirInternoEn(String titulo, JInternalFrame interno) {
    JFrame contenedor = new JFrame(titulo);
    JDesktopPane escritorio = new JDesktopPane();
    escritorio.setBackground(Estilo.FONDO);
    contenedor.setContentPane(escritorio);
    contenedor.setSize(1100, 650);
    contenedor.setLocationRelativeTo(null);
    escritorio.add(interno);
    interno.setVisible(true);
    interno.addInternalFrameListener(new javax.swing.event.InternalFrameAdapter() {
    @Override
    public void internalFrameClosed(javax.swing.event.InternalFrameEvent e) {
    contenedor.dispose();
    }
    });
    abrirModuloEn(contenedor);
    }
    /**
    * CORRECCIÓN DE ENVOLTURA:
    * Carga un JPanel personalizado dentro de un contenedor JFrame flotante para
    * no alterar el diseño asíncrono implementado por tus compañeros.
    */
    private void abrirPanelComoVentana(String titulo, JPanel panelModificado) {
    JFrame contenedor = new JFrame(titulo);
    contenedor.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    contenedor.setLayout(new BorderLayout());
    contenedor.setContentPane(panelModificado);
    contenedor.setSize(1150, 680);
    contenedor.setLocationRelativeTo(null);
    contenedor.addWindowListener(new WindowAdapter() {
    @Override
    public void windowClosed(WindowEvent e) {
    MenuPrincipal.this.setVisible(true);
    }
    });
    contenedor.setVisible(true);
    setVisible(false);
    }
    private void abrirCambiarPassword() {
    new CambiarPassword(this).setVisible(true);
    }
    private void abrirNuevoUsuario() {
    new NuevoUsuario().setVisible(true);
    dispose();
    }
    private void cerrarSesion() {
    Sesion.cerrar();
    new Login().setVisible(true);
    dispose();
    }
    }

