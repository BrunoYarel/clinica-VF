package com.mycompany.clinica.vista;

import com.mycompany.clinica.servicio.Permisos;
import com.mycompany.clinica.servicio.Sesion;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Supplier;
import javax.swing.*;

/**
 * Menú principal. Diseño:
 *   - Izquierda: barra lateral oscura con el logo, las opciones de cuenta y el usuario.
 *   - Derecha: banner de bienvenida y tarjetas con los módulos que ve cada rol.
 *
 * Papel en la arquitectura: es el punto central desde el que se llega a todos los módulos. No contiene
 * lógica de negocio; decide qué mostrar según Permisos (que a su vez consulta Sesion) y abre cada módulo.
 * Cada módulo es independiente: se comunican entre sí solo a través de la base de datos y de la sesión.
 */
public class MenuPrincipal extends JFrame {

    private JButton btnCambiarPassword;
    private JButton btnCrearUsuario;
    private JButton btnCerrarSesion;
    private FrmAgenda panelAgenda;   // vista por defecto del médico (null para los demás roles)
    private JPanel areaMedico;       // médico: zona derecha donde se intercambian sus módulos
    private JPanel paginaAgenda;     // médico: banner de bienvenida + agenda (vista por defecto)
    private JButton btnAgenda;       // médico: botón de "Mi agenda" en el panel izquierdo
    private JButton botonActivo;     // médico: botón del módulo que se está mostrando

    public MenuPrincipal() {
        super("Clínica Valmont - Menú principal");
        initComponents();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);   // al entrar con un perfil, el menú ocupa toda la pantalla
        // Al volver de otro módulo, la agenda del médico se vuelve a leer de la BD
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                if (panelAgenda != null && panelAgenda.isShowing()) {   // solo si la agenda está a la vista
                    panelAgenda.actualizar();
                }
            }
        });
    }

    private void initComponents() {
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(crearMenuLateral(), BorderLayout.WEST);
        raiz.add(crearContenido(), BorderLayout.CENTER);
        setContentPane(raiz);
        if (areaMedico != null) {
            marcarActivo(btnAgenda);   // el médico entra viendo su agenda
        }

        setSize(1000, 620);
        setMinimumSize(new Dimension(860, 540));
        if (Permisos.puedeVer(Permisos.Pantalla.MI_AGENDA)) {   // la agenda necesita un poco más de alto
            setSize(1100, 720);
        }
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
        JPanel nombreMarca = new JPanel(new GridLayout(2, 1));   // en dos líneas: "Clínica Valmont" no cabe junto al logo
        nombreMarca.setOpaque(false);
        nombreMarca.add(Estilo.texto("Clínica", Font.BOLD, 20, Color.WHITE));
        nombreMarca.add(Estilo.texto("Valmont", Font.BOLD, 20, Color.WHITE));
        marca.add(nombreMarca);

        // --- Centro: opciones de cuenta ---
        btnCambiarPassword = Estilo.botonMenu("Cambiar contraseña");
        btnCambiarPassword.addActionListener(e -> abrirCambiarPassword());

        btnCrearUsuario = Estilo.botonMenu("Crear nuevo perfil");
        btnCrearUsuario.addActionListener(e -> abrirNuevoUsuario());

        JPanel opciones = new JPanel(new GridLayout(0, 1, 0, 6));
        opciones.setOpaque(false);
        if (Permisos.puedeVer(Permisos.Pantalla.MI_AGENDA)) {    // los módulos del médico (Atención, Recetas, Historial) van aquí
            opciones.add(Estilo.texto("  MÓDULOS", Font.BOLD, 11, new Color(100, 116, 139)));
            btnAgenda = Estilo.botonMenu("Mi agenda");
            btnAgenda.addActionListener(e -> mostrarAgenda());
            opciones.add(btnAgenda);
            // Los módulos del médico NO abren otra ventana: se intercambian en la zona derecha de esta misma ventana
            JButton btnAtencion = Estilo.botonMenu("Atención médica");
            btnAtencion.addActionListener(e -> mostrarModulo(Permisos.Pantalla.ATENCION, btnAtencion,
                    () -> incrustarInterno(new FrmAtencion())));
            opciones.add(btnAtencion);
            JButton btnRecetas = Estilo.botonMenu("Recetas médicas");
            btnRecetas.addActionListener(e -> mostrarModulo(Permisos.Pantalla.RECETAS, btnRecetas,
                    () -> incrustarInterno(new FrmReceta())));
            opciones.add(btnRecetas);
            JButton btnHistorial = Estilo.botonMenu("Historial clínico");
            btnHistorial.addActionListener(e -> mostrarModulo(Permisos.Pantalla.HISTORIAL, btnHistorial,
                    () -> conCabecera("Historial clínico", "Consulta las atenciones registradas de los pacientes",
                            new FrmHistorial())));
            opciones.add(btnHistorial);
        }
        opciones.add(Estilo.texto("  CUENTA", Font.BOLD, 11, new Color(100, 116, 139)));
        opciones.add(btnCambiarPassword);
        if (Permisos.puedeGestionarUsuarios()) {                 // solo el administrador puede crear perfiles
            opciones.add(btnCrearUsuario);
        }
        if (Permisos.puedeVer(Permisos.Pantalla.CONFIGURACION)) { // Configuración: solo el administrador (ya no es tarjeta de módulo)
            JButton btnConfiguracion = Estilo.botonMenu("Configuración");
            btnConfiguracion.addActionListener(e -> abrirModulo(Permisos.Pantalla.CONFIGURACION.getTitulo()));
            opciones.add(btnConfiguracion);
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
        if (Permisos.puedeVer(Permisos.Pantalla.MI_AGENDA)) {
            // Médico: en lugar de las tarjetas se muestra su agenda; los atajos quedan en la barra lateral
            panelAgenda = new FrmAgenda();
            zonaModulos.add(Estilo.texto("Mi agenda", Font.BOLD, 18, Estilo.TEXTO), BorderLayout.NORTH);
            zonaModulos.add(panelAgenda, BorderLayout.CENTER);
        } else {
            zonaModulos.add(Estilo.texto("Módulos del sistema", Font.BOLD, 18, Estilo.TEXTO), BorderLayout.NORTH);
            zonaModulos.add(envoltura, BorderLayout.CENTER);
        }

        contenido.add(banner, BorderLayout.NORTH);
        contenido.add(zonaModulos, BorderLayout.CENTER);

        if (panelAgenda != null) {   // médico: la agenda es solo una de las vistas que se intercambian en areaMedico
            paginaAgenda = contenido;
            areaMedico = new JPanel(new BorderLayout());
            areaMedico.setBackground(Estilo.FONDO);
            areaMedico.add(paginaAgenda, BorderLayout.CENTER);
            return areaMedico;
        }
        return contenido;
    }

    // =====================================================================
    //  MÓDULOS DEL MÉDICO (se muestran dentro de esta ventana, sin abrir otra)
    // =====================================================================

    /** Vuelve a la agenda del médico (con los datos al día). */
    private void mostrarAgenda() {
        panelAgenda.actualizar();
        mostrarEnArea(paginaAgenda, btnAgenda);
    }

    /**
     * Muestra un módulo del médico en la zona derecha. El módulo se crea de nuevo en cada cambio para que lea
     * los datos más recientes (p. ej. una atención recién guardada debe aparecer en Recetas).
     */
    private void mostrarModulo(Permisos.Pantalla pantalla, JButton boton, Supplier<JComponent> fabrica) {
        if (!Permisos.puedeVer(pantalla)) {
            JOptionPane.showMessageDialog(this, "No tiene permiso para acceder a este módulo.",
                    pantalla.getTitulo(), JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (boton == botonActivo) {   // ya está a la vista: no se recrea para no borrar lo que se esté escribiendo
            return;
        }
        mostrarEnArea(fabrica.get(), boton);
    }

    /** Cambia lo que se ve a la derecha del panel izquierdo. */
    private void mostrarEnArea(JComponent pagina, JButton boton) {
        areaMedico.removeAll();
        areaMedico.add(pagina, BorderLayout.CENTER);
        areaMedico.revalidate();
        areaMedico.repaint();
        marcarActivo(boton);
    }

    /** Resalta en el panel izquierdo el módulo que se está viendo (barra de color + texto en negrita). */
    private void marcarActivo(JButton boton) {
        if (botonActivo != null) {
            botonActivo.setBorder(BorderFactory.createEmptyBorder(11, 16, 11, 16));
            botonActivo.setForeground(new Color(226, 232, 240));
            botonActivo.setFont(botonActivo.getFont().deriveFont(Font.PLAIN));
        }
        botonActivo = boton;
        boton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, Estilo.PRIMARIO),
                BorderFactory.createEmptyBorder(11, 12, 11, 16)));   // 4 + 12 = 16: el texto no se mueve
        boton.setForeground(Color.WHITE);
        boton.setFont(boton.getFont().deriveFont(Font.BOLD));
    }

    /**
     * Mete un formulario interno (JInternalFrame) en un escritorio que lo ocupa por completo y sin barra de título,
     * para poder mostrarlo como un panel más (misma técnica que abrirInternoEn, pero sin ventana nueva).
     */
    private JComponent incrustarInterno(JInternalFrame interno) {
        JDesktopPane escritorio = new JDesktopPane() {
            @Override
            public void doLayout() {   // el formulario siempre llena todo el espacio disponible
                super.doLayout();
                for (JInternalFrame f : getAllFrames()) {
                    f.setBounds(0, 0, getWidth(), getHeight());
                }
            }
        };
        escritorio.setBackground(Estilo.FONDO);
        escritorio.add(interno);
        if (interno.getUI() instanceof javax.swing.plaf.basic.BasicInternalFrameUI) {
            ((javax.swing.plaf.basic.BasicInternalFrameUI) interno.getUI()).setNorthPane(null);
        }
        interno.setBorder(null);
        interno.setVisible(true);
        return escritorio;
    }

    /** Pone la franja de título de los demás módulos encima de un panel (Historial no trae cabecera propia). */
    private JComponent conCabecera(String titulo, String subtitulo, JComponent contenido) {
        JPanel pagina = new JPanel(new BorderLayout());
        pagina.setBackground(Estilo.FONDO);
        pagina.add(Estilo.cabecera(titulo, subtitulo), BorderLayout.NORTH);
        pagina.add(contenido, BorderLayout.CENTER);
        return pagina;
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

    /** CLIC EN UNA TARJETA: conecta cada módulo con su respectiva ventana. */
    private void abrirModulo(String modulo) {
        if (!isVisible()) {   // ya hay un módulo abierto: no abrir otra ventana encima
            return;
        }
        // El permiso se valida aquí aunque la tarjeta ya se oculte según el rol: ocultar es una comodidad
        // de la interfaz, validar es lo que realmente impide el acceso.
        Permisos.Pantalla pantalla = Permisos.Pantalla.deTitulo(modulo);
        if (pantalla != null && !Permisos.puedeVer(pantalla)) {
            JOptionPane.showMessageDialog(this, "No tiene permiso para acceder a este módulo.",
                    modulo, JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (modulo.equalsIgnoreCase("Pacientes")) {
            abrirPanelComoVentana("Gestión de Pacientes", new FrmPacientes());
            return;
        }
        if (modulo.equalsIgnoreCase("Historial")) {
            abrirPanelComoVentana("Historial Clínico de Pacientes", new FrmHistorial());
            return;
        }
        if (modulo.equals("Usuarios")) {
            abrirModuloEn(new FrmUsuarios());
            return;
        }
        if (modulo.equals("Configuración")) {
            abrirModuloEn(new configuracion());
            return;
        }
        if (modulo.equals("Citas")) {
            FrmCitas citas = new FrmCitas(this);
            citas.setExtendedState(JFrame.MAXIMIZED_BOTH);
            this.setVisible(false);
            citas.setVisible(true);
            return;
        }
        if (modulo.equals("Médicos")) {
            abrirInternoEn("Gestión de Médicos", new FrmMedico());
            return;
        }
        if (modulo.equals("Especialidades")) {
            abrirInternoEn("Registro de Especialidades", new FrmEspecialidad());
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
        if (modulo.equals("Reportes")) {
            abrirModuloEn(new FrmReportes());
            return;
        }
        // Módulo sin ventana todavía
        JOptionPane.showMessageDialog(this, "El módulo de " + modulo + " estará disponible pronto.",
                modulo, JOptionPane.INFORMATION_MESSAGE);
    }

    // Traspaso de control entre ventanas: el menú se oculta mientras un módulo está abierto y se vuelve a
    // mostrar cuando ese módulo se cierra (windowClosed). Así solo hay una ventana de trabajo activa a la vez.
    private void abrirModuloEn(JFrame modulo) {
        modulo.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        modulo.setExtendedState(JFrame.MAXIMIZED_BOTH);
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
        // Se quita la barra de título y el borde del formulario interno para que no se vea una ventana dentro de otra
        if (interno.getUI() instanceof javax.swing.plaf.basic.BasicInternalFrameUI) {
            ((javax.swing.plaf.basic.BasicInternalFrameUI) interno.getUI()).setNorthPane(null);
        }
        interno.setBorder(null);
        try {
            interno.setMaximum(true);
        } catch (java.beans.PropertyVetoException ex) {
            // se ignora: el módulo queda con su tamaño normal
        }
    }

    /**
     * Carga un JPanel dentro de un JFrame flotante para no alterar el diseño
     * implementado por los demás compañeros.
     */
    private void abrirPanelComoVentana(String titulo, JPanel panelModificado) {
        JFrame contenedor = new JFrame(titulo);
        contenedor.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        contenedor.setLayout(new BorderLayout());
        contenedor.setContentPane(panelModificado);
        contenedor.setSize(1150, 680);
        contenedor.setLocationRelativeTo(null);
        contenedor.setExtendedState(JFrame.MAXIMIZED_BOTH);
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