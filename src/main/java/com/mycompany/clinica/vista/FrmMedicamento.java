package com.mycompany.clinica.vista;

import com.mycompany.clinica.Conexion;
import javax.swing.table.DefaultTableModel;

public class FrmMedicamento extends javax.swing.JInternalFrame {

    // Nombre de la columna clave en la tabla "medicamento".
    // Si en tu BD se llama distinto (ej. "id"), cámbialo aquí.
    private static final String COL_ID = "id_medicamento";

    private DefaultTableModel modeloTabla;
    private int idSeleccionado = -1;

    public FrmMedicamento() {
        initComponents();
        listarMedicamentos(); // Carga el historial al abrir la ventana
        actualizarBotones();
    }

    // ------------------------------------------------------------------
    // Listar
    // ------------------------------------------------------------------
    private void listarMedicamentos() {
        modeloTabla.setRowCount(0);
        String sql = "SELECT " + COL_ID + " AS id, nombre, presentacion FROM medicamento ORDER BY " + COL_ID + " DESC";

        try (java.sql.Connection con = Conexion.conectar()) {
            if (con != null) {
                try (java.sql.PreparedStatement ps = con.prepareStatement(sql);
                     java.sql.ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {
                        modeloTabla.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getString("presentacion")
                        });
                    }
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Error al listar medicamentos: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Estilo de botones secundarios (Eliminar / Limpiar)
    // ------------------------------------------------------------------
    private void estilizarBotonSecundario(javax.swing.JButton b) {
        b.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
        b.setForeground(new java.awt.Color(15, 23, 42));
        b.setBackground(new java.awt.Color(226, 232, 240));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    }

    private void actualizarBotones() {
        boolean haySeleccion = idSeleccionado != -1;
        btnGuardar.setEnabled(!haySeleccion);
        btnEditar.setEnabled(haySeleccion);
        btnEliminar.setEnabled(haySeleccion);
    }

    // ------------------------------------------------------------------
    // Componentes
    // ------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private void initComponents() {

        jLabel1 = Estilo.texto("", java.awt.Font.BOLD, 12, Estilo.TEXTO_SUAVE);
        jLabel2 = Estilo.texto("", java.awt.Font.BOLD, 12, Estilo.TEXTO_SUAVE);
        lblHistorial = Estilo.texto("", java.awt.Font.BOLD, 13, Estilo.TEXTO_SUAVE);
        txtNombre = Estilo.campoTexto();
        txtPresentacion = Estilo.campoTexto();
        btnGuardar = Estilo.botonPrimario("");
        btnEditar = Estilo.botonPrimario("");
        btnEliminar = Estilo.botonPrimario("");
        btnLimpiar = Estilo.botonPrimario("");

        jScrollPane1 = new javax.swing.JScrollPane();
        tablaMedicamentos = new javax.swing.JTable();

        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Medicamentos");

        jLabel1.setText("Nombre:");
        jLabel2.setText("Presentación:");
        lblHistorial.setText("Historial de medicamentos");

        btnGuardar.setText("Guardar");
        btnGuardar.addActionListener(evt -> btnGuardarActionPerformed(evt));

        btnEditar.setText("Editar");
        btnEditar.addActionListener(evt -> btnEditarActionPerformed(evt));

        btnEliminar.setText("Eliminar");
        estilizarBotonSecundario(btnEliminar);
        btnEliminar.addActionListener(evt -> btnEliminarActionPerformed(evt));

        btnLimpiar.setText("Limpiar");
        estilizarBotonSecundario(btnLimpiar);
        btnLimpiar.addActionListener(evt -> limpiarFormulario());

        // Tabla: ID | Nombre | Presentación
        modeloTabla = new DefaultTableModel(
                new Object[][]{},
                new String[]{"ID", "Nombre", "Presentación"}
        ) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        };
        tablaMedicamentos.setModel(modeloTabla);
        tablaMedicamentos.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tablaMedicamentos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarSeleccion();
            }
        });
        jScrollPane1.setViewportView(tablaMedicamentos);
        Estilo.tabla(tablaMedicamentos, jScrollPane1);   // diseño de la tabla (viene de Estilo)

        // Layout
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(45, 45, 45)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel1)
                                                        .addComponent(jLabel2))
                                                .addGap(30, 30, 30)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(txtNombre, javax.swing.GroupLayout.DEFAULT_SIZE, 500, Short.MAX_VALUE)
                                                        .addComponent(txtPresentacion)))
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(15, 15, 15)
                                                .addComponent(btnEditar, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(15, 15, 15)
                                                .addComponent(btnEliminar, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(15, 15, 15)
                                                .addComponent(btnLimpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addComponent(lblHistorial)
                                        .addComponent(jScrollPane1))
                                .addGap(45, 45, 45))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(40, 40, 40)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel1)
                                        .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(22, 22, 22)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel2)
                                        .addComponent(txtPresentacion, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(30, 30, 30)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnEditar, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnEliminar, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnLimpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(35, 35, 35)
                                .addComponent(lblHistorial)
                                .addGap(12, 12, 12)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 330, Short.MAX_VALUE)
                                .addGap(40, 40, 40))
        );

        Estilo.ventanaModulo(this, "Registro de medicamentos", "Agrega, edita o elimina medicamentos del catálogo");

        pack();
    }

    // ------------------------------------------------------------------
    // Selección en tabla / limpiar
    // ------------------------------------------------------------------
    private void cargarSeleccion() {
        int fila = tablaMedicamentos.getSelectedRow();
        if (fila == -1) {
            return;
        }
        idSeleccionado = (Integer) modeloTabla.getValueAt(fila, 0);
        txtNombre.setText(String.valueOf(modeloTabla.getValueAt(fila, 1)));
        Object pres = modeloTabla.getValueAt(fila, 2);
        txtPresentacion.setText(pres == null ? "" : pres.toString());
        actualizarBotones();
    }

    private void limpiarFormulario() {
        idSeleccionado = -1;
        txtNombre.setText("");
        txtPresentacion.setText("");
        tablaMedicamentos.clearSelection();
        actualizarBotones();
        txtNombre.requestFocus();
    }

    // ------------------------------------------------------------------
    // Guardar
    // ------------------------------------------------------------------
    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {
        String nombre = txtNombre.getText().trim();
        String presentacion = txtPresentacion.getText().trim();

        if (nombre.isEmpty() || presentacion.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "Por favor, llene todos los campos del medicamento.");
            return;
        }

        String sql = "INSERT INTO medicamento (nombre, presentacion) VALUES (?, ?)";

        try (java.sql.Connection con = Conexion.conectar()) {
            if (con == null) {
                javax.swing.JOptionPane.showMessageDialog(this, "Error: No se pudo conectar a MySQL. Verifica XAMPP.");
                return;
            }

            try (java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, nombre);
                ps.setString(2, presentacion);
                ps.executeUpdate();
            }

            javax.swing.JOptionPane.showMessageDialog(this, "¡Medicamento guardado correctamente!");
            limpiarFormulario();
            listarMedicamentos();

        } catch (java.sql.SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Error de base de datos: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Editar
    // ------------------------------------------------------------------
    private void btnEditarActionPerformed(java.awt.event.ActionEvent evt) {
        if (idSeleccionado == -1) {
            javax.swing.JOptionPane.showMessageDialog(this, "Seleccione un medicamento de la tabla.");
            return;
        }

        String nombre = txtNombre.getText().trim();
        String presentacion = txtPresentacion.getText().trim();

        if (nombre.isEmpty() || presentacion.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "Por favor, llene todos los campos del medicamento.");
            return;
        }

        String sql = "UPDATE medicamento SET nombre = ?, presentacion = ? WHERE " + COL_ID + " = ?";

        try (java.sql.Connection con = Conexion.conectar()) {
            if (con == null) {
                javax.swing.JOptionPane.showMessageDialog(this, "Error: No se pudo conectar a MySQL. Verifica XAMPP.");
                return;
            }

            try (java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, nombre);
                ps.setString(2, presentacion);
                ps.setInt(3, idSeleccionado);
                ps.executeUpdate();
            }

            javax.swing.JOptionPane.showMessageDialog(this, "¡Medicamento actualizado correctamente!");
            limpiarFormulario();
            listarMedicamentos();

        } catch (java.sql.SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Error de base de datos: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Eliminar
    // ------------------------------------------------------------------
    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {
        if (idSeleccionado == -1) {
            javax.swing.JOptionPane.showMessageDialog(this, "Seleccione un medicamento de la tabla.");
            return;
        }

        int r = javax.swing.JOptionPane.showConfirmDialog(this,
                "¿Seguro que desea eliminar este medicamento?",
                "Confirmar eliminación",
                javax.swing.JOptionPane.YES_NO_OPTION);
        if (r != javax.swing.JOptionPane.YES_OPTION) {
            return;
        }

        String sql = "DELETE FROM medicamento WHERE " + COL_ID + " = ?";

        try (java.sql.Connection con = Conexion.conectar()) {
            if (con == null) {
                javax.swing.JOptionPane.showMessageDialog(this, "Error: No se pudo conectar a MySQL. Verifica XAMPP.");
                return;
            }

            try (java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idSeleccionado);
                ps.executeUpdate();
            }

            javax.swing.JOptionPane.showMessageDialog(this, "Medicamento eliminado correctamente.");
            limpiarFormulario();
            listarMedicamentos();

        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "No se puede eliminar: el medicamento está siendo usado en otros registros.");
        } catch (java.sql.SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Error de base de datos: " + e.getMessage());
        }
    }

    // Declaración de componentes
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnEditar;
    private javax.swing.JButton btnEliminar;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel lblHistorial;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tablaMedicamentos;
    private javax.swing.JTextField txtNombre;
    private javax.swing.JTextField txtPresentacion;
}