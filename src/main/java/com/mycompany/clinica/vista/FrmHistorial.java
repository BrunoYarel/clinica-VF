package com.mycompany.clinica.vista;

import com.mycompany.clinica.controlador.PacienteControlador;
import com.mycompany.clinica.servicio.Permisos;
import com.mycompany.clinica.servicio.Sesion;
import com.mycompany.clinica.utilidades.AjustarCelda;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/** 
 * Vista de Historial Clínico optimizada a código limpio.
 * Carga todo el historial por defecto al iniciar y cuenta con filtrado dinámico en vivo.
 */
public class FrmHistorial extends JPanel {
    // Componentes del Buscador (Panel Izquierdo)
    private final JTextField txtBuscarDni = Estilo.campoTexto();
    private final JButton btnBuscar = Estilo.botonPrimario("Buscar");
    private final JButton btnLimpiar = new JButton("Limpiar Filtro");

    // Componentes de la Tabla (Panel Derecho)
    private final JTable tblHistorial = new JTable();
    private final JScrollPane scrollTabla = new JScrollPane(tblHistorial);
    private final PacienteControlador controlador = new PacienteControlador();

    public FrmHistorial() {
        super(new BorderLayout(16, 0));
        setBackground(Estilo.FONDO);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        
        if (!Permisos.puedeVerHistorial()) {
            JOptionPane.showMessageDialog(this, "Solo Admin y Médico pueden consultar el historial clínico.",
                    "Acceso denegado", JOptionPane.WARNING_MESSAGE);
            setVisible(false);
            return;
        }
        
        construirInterfaz();
        cargarHistorialCompletoInicial();
    }

    private void construirInterfaz() {
        // --- 1. PANEL IZQUIERDO: SECCIÓN DE FILTRADO ---
        JPanel panelBusqueda = new JPanel(new BorderLayout(0, 16));
        panelBusqueda.setOpaque(false);
        panelBusqueda.setPreferredSize(new Dimension(320, 0));

        JPanel tarjetaBusqueda = new JPanel(new BorderLayout(0, 14));
        tarjetaBusqueda.setBackground(Color.WHITE);
        tarjetaBusqueda.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BORDE),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        JLabel subtitulo = Estilo.texto("Búsqueda de Antecedentes", java.awt.Font.BOLD, 14, Estilo.TEXTO_SUAVE);
        tarjetaBusqueda.add(subtitulo, BorderLayout.NORTH);

        JPanel campos = new JPanel(new GridLayout(0, 1, 0, 10));
        campos.setOpaque(false);
        campos.add(Estilo.campo("Documento / DNI del paciente", txtBuscarDni));
        tarjetaBusqueda.add(campos, BorderLayout.CENTER);

        // Botonera lateral
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        panelBotones.setOpaque(false);
        btnLimpiar.setBackground(Color.LIGHT_GRAY);
        
        panelBotones.add(btnBuscar);
        panelBotones.add(btnLimpiar);
        tarjetaBusqueda.add(panelBotones, BorderLayout.SOUTH);
        panelBusqueda.add(tarjetaBusqueda, BorderLayout.NORTH);

        // --- 2. PANEL DERECHO: INTERFAZ DE TABLA REAL-TIME ---
        JPanel panelTabla = new JPanel(new BorderLayout(0, 10));
        panelTabla.setBackground(Color.WHITE);
        panelTabla.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BORDE),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));

        JLabel tituloTabla = Estilo.texto("Historial de Atenciones Clínicas", java.awt.Font.BOLD, 14, Estilo.AZUL_OSCURO);
        panelTabla.add(tituloTabla, BorderLayout.NORTH);

        // Aplicación estética sobre la JTable
        tblHistorial.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tblHistorial.setFillsViewportHeight(true);
        tblHistorial.setRowHeight(28);
        tblHistorial.getTableHeader().setFont(Estilo.fuente(java.awt.Font.BOLD, 12));
        tblHistorial.getTableHeader().setBackground(Estilo.AZUL_OSCURO);
        tblHistorial.getTableHeader().setForeground(Color.WHITE);
        scrollTabla.setBorder(BorderFactory.createLineBorder(Estilo.BORDE));
        panelTabla.add(scrollTabla, BorderLayout.CENTER);

        // Ensamblado Estructural
        add(panelBusqueda, BorderLayout.WEST);
        add(panelTabla, BorderLayout.CENTER);

        // --- 3. CONFIGURACIÓN DE DISPARADORES Y EVENTOS ---
        btnBuscar.addActionListener(e -> buscarHistorial());
        txtBuscarDni.addActionListener(e -> buscarHistorial());
        btnLimpiar.addActionListener(e -> limpiarFiltro());

        // Escuchador dinámico de tipeo para actualización en tiempo real por caracteres
        txtBuscarDni.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { buscarHistorialRealTime(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { buscarHistorialRealTime(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { buscarHistorialRealTime(); }
        });
    }

    private void cargarHistorialCompletoInicial() {
        // Ejecuta la consulta pasándole vacío al inicio para que el controlador liste todo
        buscarHistorialRealTime();
    }

    private void buscarHistorial() {
        String documento = txtBuscarDni.getText().trim();
        if (documento.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingrese el documento del paciente para filtrar.",
                    "Dato requerido", JOptionPane.WARNING_MESSAGE);
            txtBuscarDni.requestFocusInWindow();
            return;
        }
        buscarHistorialRealTime();
    }

    private void buscarHistorialRealTime() {
        String documento = txtBuscarDni.getText().trim();

        // Obtener credenciales seguras de la sesión activa
        int idMedico = Sesion.getIdMedico();
        String rol = Sesion.getRol();

        // Consulta reactiva mediante el controlador (Muestra todo si documento está vacío)
        DefaultTableModel modelo = controlador.consultarHistorialClinico(documento, idMedico, rol);
        tblHistorial.setModel(modelo);
        
        ajustarDisenoColumnas();
    }

    private void ajustarDisenoColumnas() {
        // Dimensiones calculadas para las 6 columnas del nuevo modelo unificado
        int[] anchos = {180, 140, 160, 220, 220, 240};
        int columnasDisponibles = tblHistorial.getColumnModel().getColumnCount();
        
        // Asignación segura de tamaños para evitar desbordes de índices
        for (int i = 0; i < columnasDisponibles && i < anchos.length; i++) {
            tblHistorial.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        
        // Inyección del renderizador AjustarCelda sobre las columnas para autoajuste de texto
        AjustarCelda adaptador = new AjustarCelda();
        for (int i = 0; i < columnasDisponibles; i++) {
            tblHistorial.getColumnModel().getColumn(i).setCellRenderer(adaptador);
        }
        
        tblHistorial.revalidate();
        tblHistorial.repaint();
    }

    private void limpiarFiltro() {
        txtBuscarDni.setText("");
        cargarHistorialCompletoInicial();
        txtBuscarDni.requestFocusInWindow();
    }
}
