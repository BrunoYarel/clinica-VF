package com.mycompany.clinica.vista;

import com.mycompany.clinica.controlador.PacienteControlador;
import com.mycompany.clinica.modelo.Paciente;
import com.mycompany.clinica.servicio.Permisos;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
 * Vista completa de Gestión de Pacientes (CRUD con Filtro de Búsqueda Rápida).
 * Desarrollada a código limpio para integrarse dinámicamente en el Menú Principal.
 */
public class FrmPacientes extends JPanel {
    // Cajas de texto del Formulario
    private final JTextField txtDocumento = Estilo.campoTexto();
    private final JTextField txtNombres = Estilo.campoTexto();
    private final JTextField txtApellidos = Estilo.campoTexto();
    private final JTextField txtTelefono = Estilo.campoTexto();
    private final JTextField txtEmail = Estilo.campoTexto();
    private final JTextField txtFechaNacimiento = Estilo.campoTexto();

    // Campo de Búsqueda Rápida
    private final JTextField txtBuscarRealTime = Estilo.campoTexto();
    private final JButton btnBuscarFiltro = Estilo.botonPrimario("Buscar");

    // Botones del CRUD
    private final JButton btnGuardar = Estilo.botonPrimario("Guardar");
    private final JButton btnModificar = new JButton("Modificar");
    private final JButton btnEliminar = new JButton("Eliminar");
    private final JButton btnLimpiar = new JButton("Limpiar Selección");

    // Componentes de la Tabla
    private final JTable tblPacientes = new JTable();
    private final JScrollPane scrollTabla = new JScrollPane(tblPacientes);
    private final PacienteControlador controlador = new PacienteControlador();

    // Variable de estado para controlar el ID seleccionado en la BD
    private int idPacienteSeleccionado = -1;

    public FrmPacientes() {
        super(new BorderLayout(16, 0));
        setBackground(Estilo.FONDO);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        if (!Permisos.puedeGestionarPacientes()) {
            JOptionPane.showMessageDialog(this, "No tiene permiso para gestionar pacientes.",
                    "Acceso denegado", JOptionPane.WARNING_MESSAGE);
            setVisible(false);
            return;
        }

        construirInterfaz();
        recargarTablaRealTime();
    }

    private void construirInterfaz() {
        // --- 1. PANEL IZQUIERDO: FORMULARIO DE ENTRADA (MANTENIMIENTO) ---
        JPanel panelFormulario = new JPanel(new BorderLayout(0, 16));
        panelFormulario.setOpaque(false);
        panelFormulario.setPreferredSize(new Dimension(340, 0));

        JPanel tarjetaForm = new JPanel(new BorderLayout(0, 14));
        tarjetaForm.setBackground(Color.WHITE);
        tarjetaForm.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BORDE),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        JLabel subtitulo = Estilo.texto("Datos del Paciente", java.awt.Font.BOLD, 14, Estilo.TEXTO_SUAVE);
        tarjetaForm.add(subtitulo, BorderLayout.NORTH);

        JPanel campos = new JPanel(new GridLayout(0, 1, 0, 10));
        campos.setOpaque(false);
        campos.add(Estilo.campo("Documento / DNI", txtDocumento));
        campos.add(Estilo.campo("Nombres", txtNombres));
        campos.add(Estilo.campo("Apellidos", txtApellidos));
        campos.add(Estilo.campo("Fecha de nacimiento (DD/MM/AAAA)", txtFechaNacimiento));
        campos.add(Estilo.campo("Teléfono", txtTelefono));
        campos.add(Estilo.campo("Correo electrónico", txtEmail));
        tarjetaForm.add(campos, BorderLayout.CENTER);

        // Botonera de acciones CRUD
        // Dos filas: los 4 botones no caben en una sola fila dentro del panel de 340 px y la segunda quedaba recortada.
        JPanel panelBotonesForm = new JPanel(new GridLayout(2, 1, 0, 6));
        panelBotonesForm.setOpaque(false);
        JPanel filaBotones1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        filaBotones1.setOpaque(false);
        JPanel filaBotones2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        filaBotones2.setOpaque(false);

        btnModificar.setBackground(new Color(243, 156, 18));
        btnModificar.setForeground(Color.WHITE);
        btnEliminar.setBackground(new Color(211, 47, 47));
        btnEliminar.setForeground(Color.WHITE);
        btnLimpiar.setBackground(Color.LIGHT_GRAY);

        btnModificar.setEnabled(false);
        btnEliminar.setEnabled(false);
        btnEliminar.setVisible(Permisos.puedeEliminarPaciente());

        filaBotones1.add(btnGuardar);
        filaBotones1.add(btnModificar);
        filaBotones2.add(btnEliminar);
        filaBotones2.add(btnLimpiar);
        panelBotonesForm.add(filaBotones1);
        panelBotonesForm.add(filaBotones2);
        tarjetaForm.add(panelBotonesForm, BorderLayout.SOUTH);
        panelFormulario.add(tarjetaForm, BorderLayout.NORTH);

        // --- 2. PANEL DERECHO: VISTA DE LA TABLA Y FILTRO DE BÚSQUEDA ---
        JPanel panelTablaPrincipal = new JPanel(new BorderLayout(0, 12));
        panelTablaPrincipal.setBackground(Color.WHITE);
        panelTablaPrincipal.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BORDE),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));

        // Subpanel Superior: Barra de Búsqueda por Filtro
        JPanel panelFiltroBusqueda = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panelFiltroBusqueda.setOpaque(false);
        panelFiltroBusqueda.add(Estilo.texto("Filtrar Paciente:", java.awt.Font.BOLD, 12, Estilo.TEXTO_SUAVE));
        txtBuscarRealTime.setColumns(22);
        panelFiltroBusqueda.add(txtBuscarRealTime);
        panelFiltroBusqueda.add(btnBuscarFiltro);
        panelTablaPrincipal.add(panelFiltroBusqueda, BorderLayout.NORTH);

        // Configuración de la JTable
        tblPacientes.setRowHeight(26);
        tblPacientes.setFillsViewportHeight(true);
        tblPacientes.getTableHeader().setFont(Estilo.fuente(java.awt.Font.BOLD, 12));
        tblPacientes.getTableHeader().setBackground(Estilo.AZUL_OSCURO);
        tblPacientes.getTableHeader().setForeground(Color.WHITE);
        scrollTabla.setBorder(BorderFactory.createLineBorder(Estilo.BORDE));
        panelTablaPrincipal.add(scrollTabla, BorderLayout.CENTER);

        // Ensamblado en el contenedor General
        add(panelFormulario, BorderLayout.WEST);
        add(panelTablaPrincipal, BorderLayout.CENTER);

        // --- 3. GESTIÓN DE EVENTOS ---
        btnGuardar.addActionListener(e -> guardarPaciente());
        btnModificar.addActionListener(e -> modificarPaciente());
        btnEliminar.addActionListener(e -> eliminarPaciente());
        btnLimpiar.addActionListener(e -> limpiarFormulario());

        // Acciones para ejecutar la búsqueda rápida
        btnBuscarFiltro.addActionListener(e -> filtrarPacientesTabla());
        txtBuscarRealTime.addActionListener(e -> filtrarPacientesTabla());

        // Escuchador para detectar cuando el usuario escribe caracteres (búsqueda en vivo)
        txtBuscarRealTime.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filtrarPacientesTabla(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filtrarPacientesTabla(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filtrarPacientesTabla(); }
        });

        // Evento para capturar los clics de filas en la tabla
        tblPacientes.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mapearFilaAFormulario();
            }
        });
    }

    private void recargarTablaRealTime() {
        DefaultTableModel modelo = controlador.listarPacientes();
        tblPacientes.setModel(modelo);
        ocultarColumnaId();
    }

    private void filtrarPacientesTabla() {
        String query = txtBuscarRealTime.getText().trim();
        if (query.isEmpty()) {
            recargarTablaRealTime();
        } else {
            DefaultTableModel modeloFiltrado = controlador.buscarPacientes(query);
            tblPacientes.setModel(modeloFiltrado);
            ocultarColumnaId();
        }
    }

    // [FIX integración] (el nombre se deja para no tocar las llamadas) Antes esta columna se ocultaba con ancho 0,
    // pero el módulo Citas pide el "ID Paciente" escrito a mano y no había forma de verlo. Ahora se muestra angosta.
    private void ocultarColumnaId() {
        if (tblPacientes.getColumnCount() > 0) {
            tblPacientes.getColumnModel().getColumn(0).setMinWidth(30);
            tblPacientes.getColumnModel().getColumn(0).setMaxWidth(60);
            tblPacientes.getColumnModel().getColumn(0).setPreferredWidth(45);
        }
    }

    private void guardarPaciente() {
        Paciente nuevo = validarYConstruirPaciente();
        if (nuevo == null) return;

        if (controlador.registrarPaciente(nuevo)) {
            JOptionPane.showMessageDialog(this, "Paciente registrado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            limpiarFormulario();
            recargarTablaRealTime();
        } else {
            JOptionPane.showMessageDialog(this, "Error al registrar. Verifique si el DNI ya existe.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void modificarPaciente() {
        if (idPacienteSeleccionado == -1) return;

        Paciente actualizado = validarYConstruirPaciente();
        if (actualizado == null) return;

        if (controlador.actualizarPaciente(actualizado, idPacienteSeleccionado)) {
            JOptionPane.showMessageDialog(this, "Datos del paciente actualizados correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            limpiarFormulario();
            recargarTablaRealTime();
        } else {
            JOptionPane.showMessageDialog(this, "No se pudo actualizar el registro.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarPaciente() {
        if (!Permisos.puedeEliminarPaciente()) {
            JOptionPane.showMessageDialog(this, "Solo el administrador puede eliminar pacientes.", "Acceso denegado", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (idPacienteSeleccionado == -1) return;

        int confirmado = JOptionPane.showConfirmDialog(this, "¿Está seguro de eliminar a este paciente?\nEsta acción removerá sus datos permanentemente.",
                "Confirmar Baja", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmado == JOptionPane.YES_OPTION) {
            if (controlador.eliminarPaciente(idPacienteSeleccionado)) {
                JOptionPane.showMessageDialog(this, "Paciente eliminado con éxito.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                limpiarFormulario();
                recargarTablaRealTime();
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo eliminar el registro.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    private void mapearFilaAFormulario() {
        int fila = tblPacientes.getSelectedRow();
        if (fila >= 0) {
            idPacienteSeleccionado = Integer.parseInt(tblPacientes.getValueAt(fila, 0).toString());
            txtDocumento.setText(tblPacientes.getValueAt(fila, 1).toString());
            txtNombres.setText(tblPacientes.getValueAt(fila, 2).toString());
            txtApellidos.setText(tblPacientes.getValueAt(fila, 3).toString());
            Object fechaObj = tblPacientes.getValueAt(fila, 4);
            txtFechaNacimiento.setText(fechaObj != null ? fechaObj.toString() : "");
            txtTelefono.setText(tblPacientes.getValueAt(fila, 5) != null ? tblPacientes.getValueAt(fila, 5).toString() : "");
            txtEmail.setText(tblPacientes.getValueAt(fila, 6) != null ? tblPacientes.getValueAt(fila, 6).toString() : "");
            btnGuardar.setEnabled(false);
            btnModificar.setEnabled(true);
            btnEliminar.setEnabled(Permisos.puedeEliminarPaciente());
        }
    }
    private Paciente validarYConstruirPaciente() {
        String documento = txtDocumento.getText().trim();
        String nombres = txtNombres.getText().trim();
        String apellidos = txtApellidos.getText().trim();
        String fechaIngresada = txtFechaNacimiento.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String email = txtEmail.getText().trim();
        if (documento.isEmpty() || nombres.isEmpty() || apellidos.isEmpty() || fechaIngresada.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Complete los campos obligatorios.", "Campos vacíos", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        String fechaMySQL = "";
        try {
            java.time.format.DateTimeFormatter formateadorFlexible = new java.time.format.DateTimeFormatterBuilder()
                    .append(java.time.format.DateTimeFormatter.ofPattern("[yyyy-MM-dd][yyyy/MM/dd][dd-MM-yyyy][dd/MM/yyyy]"))
                    .toFormatter();
            LocalDate fechaLocalDate = LocalDate.parse(fechaIngresada, formateadorFlexible);
            fechaMySQL = fechaLocalDate.toString();
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Use un formato válido para la fecha (Ej: DD/MM/AAAA o AAAA-MM-DD).", "Fecha incorrecta", JOptionPane.WARNING_MESSAGE);
            txtFechaNacimiento.requestFocusInWindow();
            return null;
        }
        return new Paciente(nombres, apellidos, documento, fechaMySQL, telefono, email);
    }
    private void limpiarFormulario() {
        idPacienteSeleccionado = -1;
        txtDocumento.setText("");
        txtNombres.setText("");
        txtApellidos.setText("");
        txtFechaNacimiento.setText("");
        txtTelefono.setText("");
        txtEmail.setText("");
        txtBuscarRealTime.setText("");
        tblPacientes.clearSelection();
        btnGuardar.setEnabled(true);
        btnModificar.setEnabled(false);
        btnEliminar.setEnabled(false);
        this.revalidate();
        this.repaint();
        txtDocumento.requestFocusInWindow();
    }
}