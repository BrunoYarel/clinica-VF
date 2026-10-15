package com.mycompany.clinica.vista;

import com.mycompany.clinica.Conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

public class FrmReceta extends javax.swing.JInternalFrame {

    private Map<String, Integer> atencionesMap = new HashMap<>();

    public FrmReceta() {
        initComponents();
        txtIdReceta.setEnabled(false);
        tblRecetas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        cargarAtenciones();
        listarRecetas();
        estadoInicial();
    }

    private void habilitarCampos(boolean estado) {
        cboAtencion.setEnabled(estado);
        txtIndicaciones.setEnabled(estado);
    }

    private void estadoInicial() {
        habilitarCampos(false);
        btnNuevo.setEnabled(true);
        btnGuardar.setEnabled(false);
        btnActualizar.setEnabled(false);
        btnEliminar.setEnabled(false);
        btnCancelar.setEnabled(false);
    }

    private void estadoNuevo() {
        habilitarCampos(true);
        btnNuevo.setEnabled(false);
        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnEliminar.setEnabled(false);
        btnCancelar.setEnabled(true);
    }

    private void estadoEdicion() {
        habilitarCampos(true);
        btnNuevo.setEnabled(false);
        btnGuardar.setEnabled(false);
        btnActualizar.setEnabled(true);
        btnEliminar.setEnabled(true);
        btnCancelar.setEnabled(true);
    }

    private void limpiar() {
        txtIdReceta.setText("");
        txtIndicaciones.setText("");
        if (cboAtencion.getItemCount() > 0) {
            cboAtencion.setSelectedIndex(0);
        }
        tblRecetas.clearSelection();
    }

    private void cargarAtenciones() {
        cboAtencion.removeAllItems();
        atencionesMap.clear();
        String sql = "SELECT a.id_atencion, CONCAT('Atención #', a.id_atencion, ' - Diagnóstico: ', a.diagnostico) AS descripcion " +
                "FROM atencion a";

        try (Connection cn = Conexion.conectar();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int idAtencion = rs.getInt("id_atencion");
                String desc = rs.getString("descripcion");
                cboAtencion.addItem(desc);
                atencionesMap.put(desc, idAtencion);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar atenciones: " + e.getMessage());
        }
    }

    private void listarRecetas() {
        DefaultTableModel modelo = new DefaultTableModel(
                new Object[]{"ID Receta", "ID Atención", "Fecha Emisión", "Indicaciones"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        String sql = "SELECT id_receta, id_atencion, fecha_emision, indicaciones FROM receta ORDER BY id_receta DESC";

        try (Connection cn = Conexion.conectar();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Object[] fila = {
                        rs.getInt("id_receta"),
                        rs.getInt("id_atencion"),
                        rs.getString("fecha_emision"),
                        rs.getString("indicaciones")
                };
                modelo.addRow(fila);
            }
            tblRecetas.setModel(modelo);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al listar recetas: " + e.getMessage());
        }
    }

    private boolean validarCampos() {
        if (cboAtencion.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una atención médica.");
            return false;
        }
        if (txtIndicaciones.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingrese las indicaciones de la receta.");
            txtIndicaciones.requestFocus();
            return false;
        }
        return true;
    }

    private void guardarReceta() {
        if (!validarCampos()) return;

        int idAtencion = atencionesMap.get(cboAtencion.getSelectedItem().toString());
        String sql = "INSERT INTO receta (id_atencion, indicaciones) VALUES (?, ?)";

        try (Connection cn = Conexion.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, idAtencion);
            ps.setString(2, txtIndicaciones.getText().trim());

            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Receta registrada correctamente.");

            listarRecetas();
            limpiar();
            estadoInicial();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al registrar receta: " + e.getMessage());
        }
    }

    private void actualizarReceta() {
        if (txtIdReceta.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro de receta.");
            return;
        }
        if (!validarCampos()) return;

        String sql = "UPDATE receta SET indicaciones = ? WHERE id_receta = ?";

        try (Connection cn = Conexion.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, txtIndicaciones.getText().trim());
            ps.setInt(2, Integer.parseInt(txtIdReceta.getText()));

            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Receta actualizada correctamente.");

            listarRecetas();
            limpiar();
            estadoInicial();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al actualizar receta: " + e.getMessage());
        }
    }

    private void eliminarReceta() {
        if (txtIdReceta.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Seleccione una receta para eliminar.");
            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(
                this, "¿Está seguro de eliminar esta receta médica?",
                "Confirmación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE
        );

        if (respuesta != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM receta WHERE id_receta = ?";

        try (Connection cn = Conexion.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, Integer.parseInt(txtIdReceta.getText()));
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Receta eliminada correctamente.");
            listarRecetas();
            limpiar();
            estadoInicial();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al eliminar receta: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = Estilo.texto("", java.awt.Font.BOLD, 12, Estilo.TEXTO_SUAVE);
        txtIdReceta = Estilo.campoTexto();
        jLabel2 = Estilo.texto("", java.awt.Font.BOLD, 12, Estilo.TEXTO_SUAVE);
        cboAtencion = Estilo.combo(new String[0]);
        jLabel3 = Estilo.texto("", java.awt.Font.BOLD, 12, Estilo.TEXTO_SUAVE);
        jScrollPane1 = new javax.swing.JScrollPane();
        txtIndicaciones = new javax.swing.JTextArea();
        jPanel2 = new javax.swing.JPanel();
        btnNuevo = Estilo.botonSecundario("");
        btnGuardar = Estilo.botonPrimario("");
        btnActualizar = Estilo.botonPrimario("");
        btnEliminar = Estilo.botonSecundario("");
        btnCancelar = Estilo.botonSecundario("");
        jScrollPane2 = new javax.swing.JScrollPane();
        tblRecetas = new javax.swing.JTable();

        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Gestión de Recetas Médicas");

        // --- DISEÑO (viene de Estilo): tarjeta blanca, campos de texto y botones ---
        jPanel1.setBackground(java.awt.Color.WHITE);
        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(
                javax.swing.BorderFactory.createLineBorder(Estilo.BORDE, 1), "Datos de Receta",
                javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                Estilo.fuente(java.awt.Font.BOLD, 13), Estilo.TEXTO));
        txtIdReceta.setBackground(Estilo.FONDO);   // solo lectura: se rellena al elegir un registro
        txtIndicaciones.setFont(Estilo.fuente(java.awt.Font.PLAIN, 14));
        txtIndicaciones.setForeground(Estilo.TEXTO);
        txtIndicaciones.setCaretColor(Estilo.PRIMARIO);
        txtIndicaciones.setMargin(new java.awt.Insets(8, 12, 8, 12));
        txtIndicaciones.setLineWrap(true);
        txtIndicaciones.setWrapStyleWord(true);
        jScrollPane1.setBorder(javax.swing.BorderFactory.createLineBorder(Estilo.BORDE, 1));
        jPanel2.setOpaque(false);
        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));

        jLabel1.setText("ID Receta:");

        jLabel2.setText("Atención Médica:");

        jLabel3.setText("Indicaciones:");

        txtIndicaciones.setColumns(20);
        txtIndicaciones.setRows(4);
        jScrollPane1.setViewportView(txtIndicaciones);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(jLabel1)
                                                .addGap(18, 18, 18)
                                                .addComponent(txtIdReceta, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(30, 30, 30)
                                                .addComponent(jLabel2)
                                                .addGap(18, 18, 18)
                                                .addComponent(cboAtencion, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(jLabel3)
                                                .addGap(18, 18, 18)
                                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 500, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap(20, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(15, 15, 15)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel1)
                                        .addComponent(txtIdReceta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel2)
                                        .addComponent(cboAtencion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel3)
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(15, Short.MAX_VALUE))
        );

        btnNuevo.setText("Nuevo");
        btnNuevo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNuevoActionPerformed(evt);
            }
        });
        jPanel2.add(btnNuevo);

        btnGuardar.setText("Guardar");
        btnGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarActionPerformed(evt);
            }
        });
        jPanel2.add(btnGuardar);

        btnActualizar.setText("Actualizar");
        btnActualizar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnActualizarActionPerformed(evt);
            }
        });
        jPanel2.add(btnActualizar);

        btnEliminar.setText("Eliminar");
        btnEliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEliminarActionPerformed(evt);
            }
        });
        jPanel2.add(btnEliminar);

        btnCancelar.setText("Cancelar");
        btnCancelar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelarActionPerformed(evt);
            }
        });
        jPanel2.add(btnCancelar);

        tblRecetas.setModel(new javax.swing.table.DefaultTableModel(
                new Object [][] {},
                new String [] {}
        ));
        tblRecetas.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblRecetasMouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(tblRecetas);
        Estilo.tabla(tblRecetas, jScrollPane2);   // diseño de la tabla (viene de Estilo)

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(15, 15, 15)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jScrollPane2))
                                .addContainerGap(15, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(15, 15, 15)
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(15, Short.MAX_VALUE))
        );

        Estilo.ventanaModulo(this, "Gestión de recetas médicas",
                "Emite y consulta las indicaciones de cada atención");

        pack();
    }

    private void btnNuevoActionPerformed(java.awt.event.ActionEvent evt) {
        limpiar();
        estadoNuevo();
        txtIndicaciones.requestFocus();
    }

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {
        guardarReceta();
    }

    private void btnActualizarActionPerformed(java.awt.event.ActionEvent evt) {
        actualizarReceta();
    }

    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {
        eliminarReceta();
    }

    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {
        limpiar();
        estadoInicial();
    }

    private void tblRecetasMouseClicked(java.awt.event.MouseEvent evt) {
        int fila = tblRecetas.getSelectedRow();
        if (fila >= 0) {
            txtIdReceta.setText(tblRecetas.getValueAt(fila, 0).toString());
            txtIndicaciones.setText(tblRecetas.getValueAt(fila, 3).toString());
            estadoEdicion();
        }
    }

    private javax.swing.JButton btnActualizar;
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnEliminar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnNuevo;
    private javax.swing.JComboBox<String> cboAtencion;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable tblRecetas;
    private javax.swing.JTextField txtIdReceta;
    private javax.swing.JTextArea txtIndicaciones;
}