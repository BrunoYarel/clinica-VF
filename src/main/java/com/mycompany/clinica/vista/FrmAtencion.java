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

public class FrmAtencion extends javax.swing.JInternalFrame {

    private Map<String, Integer> citasMap = new HashMap<>();

    public FrmAtencion() {
        initComponents();
        txtIdAtencion.setEnabled(false);
        tblAtenciones.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        cargarCitasPendientes();
        listarAtenciones();
        estadoInicial();
    }

    private void habilitarCampos(boolean estado) {
        cboCita.setEnabled(estado);
        txtSintomas.setEnabled(estado);
        txtDiagnostico.setEnabled(estado);
        txtObservaciones.setEnabled(estado);
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
        txtIdAtencion.setText("");
        txtSintomas.setText("");
        txtDiagnostico.setText("");
        txtObservaciones.setText("");
        if (cboCita.getItemCount() > 0) {
            cboCita.setSelectedIndex(0);
        }
        tblAtenciones.clearSelection();
    }

    private void cargarCitasPendientes() {
        cboCita.removeAllItems();
        citasMap.clear();
        String sql = "SELECT c.id_cita, CONCAT('Cita #', c.id_cita, ' - Paciente: ', p.nombres, ' ', p.apellidos) AS descripcion " +
                     "FROM cita c INNER JOIN paciente p ON c.id_paciente = p.id_paciente " +
                     "WHERE c.estado IN ('Pendiente', 'Programada', 'En Espera')";
        
        try (Connection cn = Conexion.conectar();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                int idCita = rs.getInt("id_cita");
                String desc = rs.getString("descripcion");
                cboCita.addItem(desc);
                citasMap.put(desc, idCita);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar citas: " + e.getMessage());
        }
    }

    private void listarAtenciones() {
        DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"ID Atención", "ID Cita", "Fecha/Hora", "Síntomas", "Diagnóstico", "Observaciones"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        String sql = "SELECT id_atencion, id_cita, fecha_hora_atencion, sintomas, diagnostico, observaciones " +
                     "FROM atencion ORDER BY id_atencion DESC";

        try (Connection cn = Conexion.conectar();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Object[] fila = {
                    rs.getInt("id_atencion"),
                    rs.getInt("id_cita"),
                    rs.getString("fecha_hora_atencion"),
                    rs.getString("sintomas"),
                    rs.getString("diagnostico"),
                    rs.getString("observaciones")
                };
                modelo.addRow(fila);
            }
            tblAtenciones.setModel(modelo);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al listar atenciones: " + e.getMessage());
        }
    }

    private boolean validarCampos() {
        if (cboCita.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una cita médica.");
            return false;
        }
        if (txtSintomas.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingrese los síntomas.");
            txtSintomas.requestFocus();
            return false;
        }
        if (txtDiagnostico.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingrese el diagnóstico.");
            txtDiagnostico.requestFocus();
            return false;
        }
        return true;
    }

    private void guardarAtencion() {
        if (!validarCampos()) return;

        int idCita = citasMap.get(cboCita.getSelectedItem().toString());
        String sql = "INSERT INTO atencion (id_cita, sintomas, diagnostico, observaciones) VALUES (?, ?, ?, ?)";

        try (Connection cn = Conexion.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, idCita);
            ps.setString(2, txtSintomas.getText().trim());
            ps.setString(3, txtDiagnostico.getText().trim());
            ps.setString(4, txtObservaciones.getText().trim());

            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Atención registrada correctamente.");
            
            listarAtenciones();
            limpiar();
            estadoInicial();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al registrar: " + e.getMessage());
        }
    }

    private void actualizarAtencion() {
        if (txtIdAtencion.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro.");
            return;
        }
        if (!validarCampos()) return;

        String sql = "UPDATE atencion SET sintomas = ?, diagnostico = ?, observaciones = ? WHERE id_atencion = ?";

        try (Connection cn = Conexion.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, txtSintomas.getText().trim());
            ps.setString(2, txtDiagnostico.getText().trim());
            ps.setString(3, txtObservaciones.getText().trim());
            ps.setInt(4, Integer.parseInt(txtIdAtencion.getText()));

            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Atención actualizada correctamente.");

            listarAtenciones();
            limpiar();
            estadoInicial();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al actualizar: " + e.getMessage());
        }
    }

    private void eliminarAtencion() {
        if (txtIdAtencion.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro para eliminar.");
            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(
            this, "¿Está seguro de eliminar esta atención?",
            "Confirmación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE
        );

        if (respuesta != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM atencion WHERE id_atencion = ?";

        try (Connection cn = Conexion.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, Integer.parseInt(txtIdAtencion.getText()));
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Atención eliminada correctamente.");
            listarAtenciones();
            limpiar();
            estadoInicial();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al eliminar: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        txtIdAtencion = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        cboCita = new javax.swing.JComboBox<>();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtSintomas = new javax.swing.JTextArea();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtDiagnostico = new javax.swing.JTextArea();
        jLabel5 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        txtObservaciones = new javax.swing.JTextArea();
        jPanel2 = new javax.swing.JPanel();
        btnNuevo = new javax.swing.JButton();
        btnGuardar = new javax.swing.JButton();
        btnActualizar = new javax.swing.JButton();
        btnEliminar = new javax.swing.JButton();
        btnCancelar = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        tblAtenciones = new javax.swing.JTable();

        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Gestión de Atención Médica");

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Datos de Atención"));

        jLabel1.setText("ID Atención:");

        jLabel2.setText("Cita Médica:");

        jLabel3.setText("Síntomas:");

        txtSintomas.setColumns(20);
        txtSintomas.setRows(3);
        jScrollPane1.setViewportView(txtSintomas);

        jLabel4.setText("Diagnóstico:");

        txtDiagnostico.setColumns(20);
        txtDiagnostico.setRows(3);
        jScrollPane2.setViewportView(txtDiagnostico);

        jLabel5.setText("Observaciones:");

        txtObservaciones.setColumns(20);
        txtObservaciones.setRows(3);
        jScrollPane3.setViewportView(txtObservaciones);

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
                        .addComponent(txtIdAtencion, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(30, 30, 30)
                        .addComponent(jLabel2)
                        .addGap(18, 18, 18)
                        .addComponent(cboCita, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5)
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(20, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(txtIdAtencion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(cboCita, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap(15, Short.MAX_VALUE))
        );

        btnNuevo.setText("NUEVO");
        btnNuevo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNuevoActionPerformed(evt);
            }
        });
        jPanel2.add(btnNuevo);

        btnGuardar.setText("GUARDAR");
        btnGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarActionPerformed(evt);
            }
        });
        jPanel2.add(btnGuardar);

        btnActualizar.setText("ACTUALIZAR");
        btnActualizar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnActualizarActionPerformed(evt);
            }
        });
        jPanel2.add(btnActualizar);

        btnEliminar.setText("ELIMINAR");
        btnEliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEliminarActionPerformed(evt);
            }
        });
        jPanel2.add(btnEliminar);

        btnCancelar.setText("CANCELAR");
        btnCancelar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelarActionPerformed(evt);
            }
        });
        jPanel2.add(btnCancelar);

        tblAtenciones.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        tblAtenciones.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblAtencionesMouseClicked(evt);
            }
        });
        jScrollPane4.setViewportView(tblAtenciones);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane4))
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
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(15, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>                        

    private void btnNuevoActionPerformed(java.awt.event.ActionEvent evt) {                                         
        limpiar();
        estadoNuevo();
        txtSintomas.requestFocus();
    }                                        

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {                                           
        guardarAtencion();
    }                                          

    private void btnActualizarActionPerformed(java.awt.event.ActionEvent evt) {                                              
        actualizarAtencion();
    }                                             

    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {                                            
        eliminarAtencion();
    }                                           

    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {                                            
        limpiar();
        estadoInicial();
    }                                           

    private void tblAtencionesMouseClicked(java.awt.event.MouseEvent evt) {                                           
        int fila = tblAtenciones.getSelectedRow();
        if (fila >= 0) {
            txtIdAtencion.setText(tblAtenciones.getValueAt(fila, 0).toString());
            txtSintomas.setText(tblAtenciones.getValueAt(fila, 3).toString());
            txtDiagnostico.setText(tblAtenciones.getValueAt(fila, 4).toString());
            txtObservaciones.setText(tblAtenciones.getValueAt(fila, 5).toString());
            estadoEdicion();
        }
    }                                          

    private javax.swing.JButton btnActualizar;
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnEliminar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnNuevo;
    private javax.swing.JComboBox<String> cboCita;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JTable tblAtenciones;
    private javax.swing.JTextField txtIdAtencion;
    private javax.swing.JTextArea txtSintomas;
    private javax.swing.JTextArea txtDiagnostico;
    private javax.swing.JTextArea txtObservaciones;
}