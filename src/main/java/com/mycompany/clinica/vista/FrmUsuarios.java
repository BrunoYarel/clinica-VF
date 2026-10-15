package com.mycompany.clinica.vista;

import com.mycompany.clinica.servicio.ReglaNegocioException;
import com.mycompany.clinica.servicio.UsuarioService;
import com.mycompany.clinica.utilidades.ModeloTabla;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.JTableHeader;

/**
 * Gestión de usuarios (solo Admin): lista los usuarios del sistema y permite activarlos o desactivarlos.
 * Las reglas (permiso, no desactivarse a sí mismo, último administrador) viven en UsuarioService.
 */
public class FrmUsuarios extends JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(FrmUsuarios.class.getName());

    private final ModeloTabla modelo = new ModeloTabla("Usuario", "Rol", "Médico", "Estado");
    // Usuarios mostrados, en el mismo orden que las filas de la tabla (la tabla no muestra el id)
    private List<UsuarioService.FilaUsuario> filas = new ArrayList<>();
    private JTable tblUsuarios;

    public FrmUsuarios() {
        super("Gestión de usuarios");
        initComponents();
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(760, 480);
        setLocationRelativeTo(null);
        cargar();
    }

    private void initComponents() {
        // ---------- CABECERA ----------
        JPanel titulos = new JPanel(new GridLayout(2, 1, 0, 3));
        titulos.setOpaque(false);
        titulos.add(Estilo.texto("Gestión de usuarios", Font.BOLD, 24, Color.WHITE));
        titulos.add(Estilo.texto("Activa o desactiva el acceso de los usuarios del sistema",
                Font.PLAIN, 13, Estilo.TEXTO_CLARO));

        JButton btnVolver = Estilo.botonClaro("Volver al menú");
        btnVolver.addActionListener(e -> dispose());
        JPanel envoltura = new JPanel(new GridBagLayout());   // centra el botón sin estirarlo
        envoltura.setOpaque(false);
        envoltura.add(btnVolver);

        JPanel cabecera = Estilo.panelDegradado(Estilo.PRIMARIO_OSCURO, Estilo.AZUL, 0);
        cabecera.setLayout(new BorderLayout(14, 0));
        cabecera.setBorder(BorderFactory.createEmptyBorder(22, 30, 22, 30));
        cabecera.add(Estilo.logo(52, Color.WHITE, Estilo.PRIMARIO_OSCURO), BorderLayout.WEST);
        cabecera.add(titulos, BorderLayout.CENTER);
        cabecera.add(envoltura, BorderLayout.EAST);

        // ---------- TABLA ----------
        tblUsuarios = new JTable(modelo);
        tblUsuarios.setFont(Estilo.fuente(Font.PLAIN, 13));
        tblUsuarios.setForeground(Estilo.TEXTO);
        tblUsuarios.setRowHeight(32);
        tblUsuarios.setShowVerticalLines(false);
        tblUsuarios.setGridColor(Estilo.BORDE);
        tblUsuarios.setSelectionBackground(Estilo.TEXTO_CLARO);
        tblUsuarios.setSelectionForeground(Estilo.TEXTO);
        tblUsuarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblUsuarios.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JTableHeader encabezado = tblUsuarios.getTableHeader();
        encabezado.setFont(Estilo.fuente(Font.BOLD, 13));
        encabezado.setBackground(Estilo.AZUL_OSCURO);
        encabezado.setForeground(Color.WHITE);
        encabezado.setPreferredSize(new Dimension(0, 36));
        encabezado.setReorderingAllowed(false);

        JScrollPane scroll = new JScrollPane(tblUsuarios);
        scroll.setBorder(BorderFactory.createLineBorder(Estilo.BORDE, 1));
        scroll.getViewport().setBackground(Color.WHITE);

        // ---------- BOTÓN ----------
        JButton btnEstado = Estilo.botonPrimario("Activar / Desactivar");
        btnEstado.addActionListener(e -> alternarEstado());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        botones.setOpaque(false);
        botones.add(btnEstado);

        // ---------- ARMADO ----------
        JPanel cuerpo = new JPanel(new BorderLayout(0, 14));
        cuerpo.setBackground(Estilo.FONDO);
        cuerpo.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        cuerpo.add(scroll, BorderLayout.CENTER);
        cuerpo.add(botones, BorderLayout.SOUTH);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(cabecera, BorderLayout.NORTH);
        raiz.add(cuerpo, BorderLayout.CENTER);
        setContentPane(raiz);
    }

    /** Vuelve a leer los usuarios de la base de datos y rellena la tabla. */
    private void cargar() {
        try {
            filas = UsuarioService.listarUsuarios();
            modelo.setRowCount(0);
            for (UsuarioService.FilaUsuario u : filas) {
                modelo.addRow(new Object[]{u.username(), Estilo.nombreRol(u.rol()), u.medico(),
                        u.activo() ? "Activo" : "Inactivo"});
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Error al cargar los usuarios", e);
            JOptionPane.showMessageDialog(this, "No se pudieron cargar los usuarios:\n" + e.getMessage(),
                    "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** BOTÓN ACTIVAR / DESACTIVAR: cambia el estado del usuario seleccionado, previa confirmación. */
    private void alternarEstado() {
        int fila = tblUsuarios.getSelectedRow();
        if (fila < 0 || fila >= filas.size()) {
            JOptionPane.showMessageDialog(this, "Seleccione un usuario de la tabla.",
                    "Gestión de usuarios", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        UsuarioService.FilaUsuario usuario = filas.get(fila);
        boolean activar = !usuario.activo();

        Object[] opciones = {"Sí", "No"};
        int respuesta = JOptionPane.showOptionDialog(this,
                "¿" + (activar ? "Activar" : "Desactivar") + " al usuario '" + usuario.username() + "'?",
                "Confirmar cambio de estado", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, opciones, opciones[1]);
        if (respuesta != 0) {
            return;
        }

        try {
            UsuarioService.cambiarEstado(usuario.id(), activar);
            cargar();
        } catch (ReglaNegocioException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Gestión de usuarios", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Error al cambiar el estado del usuario", e);
            JOptionPane.showMessageDialog(this, "No se pudo cambiar el estado:\n" + e.getMessage(),
                    "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }
}