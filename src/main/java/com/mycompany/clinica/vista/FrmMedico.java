package com.mycompany.clinica.vista;

import com.mycompany.clinica.Conexion;
import com.mycompany.clinica.servicio.Permisos;

import javax.swing.GroupLayout;
import javax.swing.LayoutStyle;
import javax.swing.JOptionPane;
import javax.swing.JDesktopPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Color;
import java.awt.Font;
import com.mycompany.clinica.vista.Estilo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FrmMedico extends javax.swing.JInternalFrame {

    private DefaultTableModel modeloTabla;
    private int idMedicoSeleccionado = -1;
    private VentanaEspecialidades ventanaEsp;

    public FrmMedico() {
        initComponents();
        cargarComboEspecialidades();
        listarMedicos();
        this.setSize(1080, 600);

        // [FIX permisos] Permisos.puedeGestionarMedicos() existía pero nadie lo usaba: el Recepcionista (que
        // ve este módulo solo para consultar el ID de médico al agendar citas) podía crear, editar y eliminar
        // médicos. Si no es Admin se ocultan los botones que modifican datos; la tabla y "Ver vínculos" siguen.
        if (!Permisos.puedeGestionarMedicos()) {
            btnGuardar.setVisible(false);
            btnEditar.setVisible(false);
            btnEliminar.setVisible(false);
            btnGestionarEspecialidad.setVisible(false);
        }
    }

    public void cargarComboEspecialidades() {
        String sql = "SELECT id_especialidad, nombre FROM especialidad";

        // Recordar la especialidad seleccionada para no perderla al recargar
        int idPrevio = -1;
        Object sel = cboEspecialidad.getSelectedItem();
        if (sel instanceof Especialidad) {
            idPrevio = ((Especialidad) sel).getId();
        }

        try (Connection con = Conexion.conectar();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            cboEspecialidad.removeAllItems();
            while (rs.next()) {
                Especialidad item = new Especialidad(
                        rs.getInt("id_especialidad"),
                        rs.getString("nombre")
                );
                cboEspecialidad.addItem(item);
                if (item.getId() == idPrevio) {
                    cboEspecialidad.setSelectedItem(item);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar: " + e.getMessage());
        }
    }

    public void listarMedicos() {
        String sql = "SELECT m.id_medico, m.nombres, m.apellidos, m.cmp, "
                + "m.telefono, m.email, e.nombre AS especialidad "
                + "FROM medico m INNER JOIN especialidad e "
                + "ON m.id_especialidad = e.id_especialidad";
        modeloTabla.setRowCount(0);
        try (Connection con = Conexion.conectar();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Object[] fila = {
                        rs.getInt("id_medico"),
                        rs.getString("nombres"),
                        rs.getString("apellidos"),
                        rs.getString("cmp"),
                        rs.getString("telefono"),
                        rs.getString("email"),
                        rs.getString("especialidad")
                };
                modeloTabla.addRow(fila);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al listar: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        txtNombres = Estilo.campoTexto();
        txtApellidos = Estilo.campoTexto();
        txtCmp = Estilo.campoTexto();
        txtTelefono = Estilo.campoTexto();
        txtEmail = Estilo.campoTexto();
        cboEspecialidad = new javax.swing.JComboBox<>();
        btnGestionarEspecialidad = Estilo.botonSecundario("");
        btnGuardar = Estilo.botonPrimario("");
        btnEditar = Estilo.botonPrimario("");
        btnEliminar = Estilo.botonSecundario("");
        btnLimpiar = Estilo.botonSecundario("");
        btnVerVinculos = Estilo.botonSecundario("");
        jTable1 = new JTable();
        jScrollPane1 = new JScrollPane(jTable1);
        String[] columnas = {"ID", "Nombres", "Apellidos", "CMP", "Teléfono", "Email", "Especialidad"};
        modeloTabla = new DefaultTableModel(null, columnas) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        jTable1.setModel(modeloTabla);
        Estilo.tabla(jTable1, jScrollPane1);
        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("MÓDULO DE GESTIÓN DE MÉDICOS");
        Font fontLabels = Estilo.fuente(Font.BOLD, 12);
        Color colorTexto = Estilo.TEXTO_SUAVE;
        jLabel1.setFont(fontLabels); jLabel1.setForeground(colorTexto); jLabel1.setText("Nombres:");
        jLabel2.setFont(fontLabels); jLabel2.setForeground(colorTexto); jLabel2.setText("Apellidos:");
        jLabel3.setFont(fontLabels); jLabel3.setForeground(colorTexto); jLabel3.setText("CMP:");
        jLabel4.setFont(fontLabels); jLabel4.setForeground(colorTexto); jLabel4.setText("Teléfono:");
        jLabel5.setFont(fontLabels); jLabel5.setForeground(colorTexto); jLabel5.setText("Email:");
        jLabel6.setFont(fontLabels); jLabel6.setForeground(colorTexto); jLabel6.setText("Especialidad:");
        Font fontInputs = Estilo.fuente(Font.PLAIN, 14);
        txtNombres.setFont(fontInputs);
        txtApellidos.setFont(fontInputs);
        txtCmp.setFont(fontInputs);
        txtTelefono.setFont(fontInputs);
        txtEmail.setFont(fontInputs);
        cboEspecialidad.setFont(fontInputs);
        cboEspecialidad.setBackground(Color.WHITE);
        cboEspecialidad.setForeground(Estilo.TEXTO);

        btnGestionarEspecialidad.setFocusPainted(false);
        btnGestionarEspecialidad.setText("Especialidades");
        btnGestionarEspecialidad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGestionarEspecialidadActionPerformed(evt);
            }
        });
        btnGuardar.setFocusPainted(false);
        btnGuardar.setText("Guardar");
        btnGuardar.setEnabled(true);
        btnGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarActionPerformed(evt);
            }
        });
        btnEditar.setFocusPainted(false);
        btnEditar.setText("Editar");
        btnEditar.setEnabled(false);
        btnEditar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditarActionPerformed(evt);
            }
        });
        btnEliminar.setFocusPainted(false);
        btnEliminar.setText("Eliminar");
        btnEliminar.setEnabled(false);
        btnEliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEliminarActionPerformed(evt);
            }
        });
        btnLimpiar.setFocusPainted(false);
        btnLimpiar.setText("Limpiar");
        btnLimpiar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                limpiarFormulario();
            }
        });
        btnVerVinculos.setFocusPainted(false);
        btnVerVinculos.setText("Ver Vínculos");
        btnVerVinculos.setEnabled(false);
        btnVerVinculos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnVerVinculosActionPerformed(evt);
            }
        });
        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTable1MouseClicked(evt);
            }
        });

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(35, 35, 35)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel1)
                                                        .addComponent(jLabel2)
                                                        .addComponent(jLabel3)
                                                        .addComponent(jLabel4)
                                                        .addComponent(jLabel5)
                                                        .addComponent(jLabel6))
                                                .addGap(25, 25, 25)
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                                        .addComponent(txtNombres, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                                        .addComponent(txtApellidos)
                                                        .addComponent(txtCmp)
                                                        .addComponent(txtTelefono)
                                                        .addComponent(txtEmail)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(cboEspecialidad, GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(btnGestionarEspecialidad, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))))
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                                        .addComponent(btnGuardar, GroupLayout.DEFAULT_SIZE, 110, Short.MAX_VALUE)
                                                        .addComponent(btnEliminar, GroupLayout.DEFAULT_SIZE, 110, Short.MAX_VALUE))
                                                .addGap(10, 10, 10)
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                                        .addComponent(btnEditar, GroupLayout.DEFAULT_SIZE, 110, Short.MAX_VALUE)
                                                        .addComponent(btnLimpiar, GroupLayout.DEFAULT_SIZE, 110, Short.MAX_VALUE))
                                                .addGap(10, 10, 10)
                                                .addComponent(btnVerVinculos, GroupLayout.PREFERRED_SIZE, 110, GroupLayout.PREFERRED_SIZE)))
                                .addGap(35, 35, 35)
                                .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 570, Short.MAX_VALUE)
                                .addGap(35, 35, 35))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(35, 35, 35)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane1, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel1)
                                                        .addComponent(txtNombres, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                                .addGap(18, 18, 18)
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel2)
                                                        .addComponent(txtApellidos, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                                .addGap(18, 18, 18)
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel3)
                                                        .addComponent(txtCmp, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                                .addGap(18, 18, 18)
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel4)
                                                        .addComponent(txtTelefono, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                                .addGap(18, 18, 18)
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel5)
                                                        .addComponent(txtEmail, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                                .addGap(18, 18, 18)
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel6)
                                                        .addComponent(cboEspecialidad, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(btnGestionarEspecialidad, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                                .addGap(25, 25, 25)
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                        .addComponent(btnGuardar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(btnEditar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                                .addGap(10, 10, 10)
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                        .addComponent(btnEliminar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(btnLimpiar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(btnVerVinculos, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 15, Short.MAX_VALUE)))
                                .addGap(35, 35, 35))
        );
        Estilo.ventanaModulo(this, "Gestión de médicos", "Registra o edita médicos");
        pack();
    }

    // ============================================================
    //  Botón "Especialidades": abre la ventana de gestión
    // ============================================================
    private void btnGestionarEspecialidadActionPerformed(java.awt.event.ActionEvent evt) {
        // Si ya está abierta, solo la traemos al frente
        if (ventanaEsp != null && !ventanaEsp.isClosed()) {
            ventanaEsp.toFront();
            try {
                ventanaEsp.setSelected(true);
            } catch (java.beans.PropertyVetoException ex) {
                // se ignora
            }
            return;
        }

        JDesktopPane desktopPane = this.getDesktopPane();
        if (desktopPane != null) {
            ventanaEsp = new VentanaEspecialidades();
            ventanaEsp.addInternalFrameListener(new InternalFrameAdapter() {
                @Override
                public void internalFrameClosed(InternalFrameEvent e) {
                    ventanaEsp = null;
                }
            });
            desktopPane.add(ventanaEsp);
            int x = Math.max(0, (desktopPane.getWidth() - ventanaEsp.getWidth()) / 2);
            int y = Math.max(0, (desktopPane.getHeight() - ventanaEsp.getHeight()) / 2);
            ventanaEsp.setLocation(x, y);
            ventanaEsp.toFront();
            ventanaEsp.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Error: No se encontró el contenedor principal.");
        }
    }

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {
        String nombres = txtNombres.getText().trim();
        String apellidos = txtApellidos.getText().trim();
        String cmp = txtCmp.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String email = txtEmail.getText().trim();
        Especialidad especialidadSeleccionada = (Especialidad) cboEspecialidad.getSelectedItem();
        if (nombres.isEmpty() || apellidos.isEmpty() || cmp.isEmpty() || especialidadSeleccionada == null) {
            JOptionPane.showMessageDialog(this, "Campos obligatorios vacíos.");
            return;
        }
        String sql = "INSERT INTO medico (nombres, apellidos, cmp, telefono, email, id_especialidad) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombres);
            ps.setString(2, apellidos);
            ps.setString(3, cmp);
            ps.setString(4, telefono);
            ps.setString(5, email);
            ps.setInt(6, especialidadSeleccionada.getId());
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "¡Médico registrado correctamente!");
            limpiarFormulario();
            listarMedicos();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error de BD: " + e.getMessage());
        }
    }

    private void btnEditarActionPerformed(java.awt.event.ActionEvent evt) {
        if (idMedicoSeleccionado == -1) return;
        String nombres = txtNombres.getText().trim();
        String apellidos = txtApellidos.getText().trim();
        String cmp = txtCmp.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String email = txtEmail.getText().trim();
        Especialidad especialidadSeleccionada = (Especialidad) cboEspecialidad.getSelectedItem();
        if (nombres.isEmpty() || apellidos.isEmpty() || cmp.isEmpty() || especialidadSeleccionada == null) {
            JOptionPane.showMessageDialog(this, "Campos obligatorios vacíos.");
            return;
        }
        String sql = "UPDATE medico SET nombres=?, apellidos=?, cmp=?, telefono=?, email=?, id_especialidad=? WHERE id_medico=?";
        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombres);
            ps.setString(2, apellidos);
            ps.setString(3, cmp);
            ps.setString(4, telefono);
            ps.setString(5, email);
            ps.setInt(6, especialidadSeleccionada.getId());
            ps.setInt(7, idMedicoSeleccionado);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "¡Médico actualizado correctamente!");
            limpiarFormulario();
            listarMedicos();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al actualizar: " + e.getMessage());
        }
    }

    private void btnVerVinculosActionPerformed(java.awt.event.ActionEvent evt) {
        if (idMedicoSeleccionado == -1) return;
        String sql = "SELECT COUNT(*) AS total FROM cita WHERE id_medico = ?";
        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idMedicoSeleccionado);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int totalCitas = rs.getInt("total");
                    if (totalCitas > 0) {
                        JOptionPane.showMessageDialog(this,
                                "El médico tiene " + totalCitas + " cita(s) vinculada(s).\n"
                                        + "Debe reasignar o eliminar esas citas primero.",
                                "Restricción de Eliminación", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "Este médico no tiene citas asociadas. Puede eliminarlo.",
                                "Sin Vínculos", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error de consulta: " + e.getMessage());
        }
    }

    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {
        if (idMedicoSeleccionado == -1) return;
        int confirmar = JOptionPane.showConfirmDialog(this,
                "¿Desea eliminar a este médico?", "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmar == JOptionPane.YES_OPTION) {
            String sql = "DELETE FROM medico WHERE id_medico=?";
            try (Connection con = Conexion.conectar();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idMedicoSeleccionado);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Médico eliminado.");
                limpiarFormulario();
                listarMedicos();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error al eliminar: Medico vinculado no se puede eliminar");
            }
        }
    }

    private void jTable1MouseClicked(java.awt.event.MouseEvent evt) {
        int filaSeleccionada = jTable1.getSelectedRow();
        if (filaSeleccionada >= 0) {
            idMedicoSeleccionado = Integer.parseInt(jTable1.getValueAt(filaSeleccionada, 0).toString());
            txtNombres.setText(jTable1.getValueAt(filaSeleccionada, 1).toString());
            txtApellidos.setText(jTable1.getValueAt(filaSeleccionada, 2).toString());
            txtCmp.setText(jTable1.getValueAt(filaSeleccionada, 3).toString());
            Object telObj = jTable1.getValueAt(filaSeleccionada, 4);
            txtTelefono.setText(telObj != null ? telObj.toString() : "");
            Object mailObj = jTable1.getValueAt(filaSeleccionada, 5);
            txtEmail.setText(mailObj != null ? mailObj.toString() : "");
            String nombreEspecialidad = jTable1.getValueAt(filaSeleccionada, 6).toString();
            for (int i = 0; i < cboEspecialidad.getItemCount(); i++) {
                Especialidad esp = (Especialidad) cboEspecialidad.getItemAt(i);
                if (esp != null && esp.getNombre().equalsIgnoreCase(nombreEspecialidad)) {
                    cboEspecialidad.setSelectedIndex(i);
                    break;
                }
            }
            btnGuardar.setEnabled(false);
            btnEditar.setEnabled(true);
            btnEliminar.setEnabled(true);
            btnVerVinculos.setEnabled(true);
        }
    }

    private void limpiarFormulario() {
        idMedicoSeleccionado = -1;
        txtNombres.setText("");
        txtApellidos.setText("");
        txtCmp.setText("");
        txtTelefono.setText("");
        txtEmail.setText("");
        if (cboEspecialidad.getItemCount() > 0) {
            cboEspecialidad.setSelectedIndex(0);
        }
        jTable1.clearSelection();
        btnGuardar.setEnabled(true);
        btnEditar.setEnabled(false);
        btnEliminar.setEnabled(false);
        btnVerVinculos.setEnabled(false);
    }

    // ============================================================
    //  Ventana interna de ESPECIALIDADES
    //  Formulario + historial (tabla) + Guardar / Editar / Eliminar
    // ============================================================
    private class VentanaEspecialidades extends javax.swing.JInternalFrame {

        private DefaultTableModel modeloEsp;
        private JTable tablaEsp;
        private JScrollPane scrollEsp;
        private javax.swing.JTextField txtNombreEsp;
        private javax.swing.JTextField txtDescripcionEsp;
        private javax.swing.JLabel lblNombreEsp;
        private javax.swing.JLabel lblDescripcionEsp;
        private javax.swing.JLabel lblHistorialEsp;
        private javax.swing.JButton btnGuardarEsp;
        private javax.swing.JButton btnEditarEsp;
        private javax.swing.JButton btnEliminarEsp;
        private javax.swing.JButton btnLimpiarEsp;
        private int idEspecialidadSeleccionada = -1;

        VentanaEspecialidades() {
            initComponentsEsp();
            listarEspecialidades();
        }

        private void initComponentsEsp() {
            lblNombreEsp = Estilo.texto("", Font.BOLD, 12, Estilo.TEXTO_SUAVE);
            lblDescripcionEsp = Estilo.texto("", Font.BOLD, 12, Estilo.TEXTO_SUAVE);
            lblHistorialEsp = Estilo.texto("", Font.BOLD, 12, Estilo.TEXTO_SUAVE);
            txtNombreEsp = Estilo.campoTexto();
            txtDescripcionEsp = Estilo.campoTexto();
            btnGuardarEsp = Estilo.botonPrimario("");
            btnEditarEsp = Estilo.botonPrimario("");
            btnEliminarEsp = Estilo.botonSecundario("");
            btnLimpiarEsp = Estilo.botonSecundario("");

            tablaEsp = new JTable();
            scrollEsp = new JScrollPane(tablaEsp);
            String[] columnas = {"ID", "Nombre", "Descripción", "Médicos"};
            modeloEsp = new DefaultTableModel(null, columnas) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            tablaEsp.setModel(modeloEsp);
            tablaEsp.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
            Estilo.tabla(tablaEsp, scrollEsp);

            setClosable(true);
            setIconifiable(true);
            setMaximizable(true);
            setResizable(true);
            setTitle("Especialidades Médicas");

            lblNombreEsp.setText("Nombre:");
            lblDescripcionEsp.setText("Descripción:");
            lblHistorialEsp.setText("Historial de especialidades");

            Font fontInputs = Estilo.fuente(Font.PLAIN, 14);
            txtNombreEsp.setFont(fontInputs);
            txtDescripcionEsp.setFont(fontInputs);

            btnGuardarEsp.setFocusPainted(false);
            btnGuardarEsp.setText("Guardar");
            btnGuardarEsp.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    guardarEspecialidad();
                }
            });
            btnEditarEsp.setFocusPainted(false);
            btnEditarEsp.setText("Editar");
            btnEditarEsp.setEnabled(false);
            btnEditarEsp.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    editarEspecialidad();
                }
            });
            btnEliminarEsp.setFocusPainted(false);
            btnEliminarEsp.setText("Eliminar");
            btnEliminarEsp.setEnabled(false);
            btnEliminarEsp.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    eliminarEspecialidad();
                }
            });
            btnLimpiarEsp.setFocusPainted(false);
            btnLimpiarEsp.setText("Limpiar");
            btnLimpiarEsp.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    limpiarEsp();
                }
            });
            tablaEsp.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    seleccionarFilaEsp();
                }
            });

            GroupLayout layout = new GroupLayout(getContentPane());
            getContentPane().setLayout(layout);
            layout.setHorizontalGroup(
                    layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                    .addGap(30, 30, 30)
                                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                            .addGroup(layout.createSequentialGroup()
                                                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                            .addComponent(lblNombreEsp)
                                                            .addComponent(lblDescripcionEsp))
                                                    .addGap(20, 20, 20)
                                                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                            .addComponent(txtNombreEsp, GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
                                                            .addComponent(txtDescripcionEsp, GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)))
                                            .addGroup(layout.createSequentialGroup()
                                                    .addComponent(btnGuardarEsp, GroupLayout.PREFERRED_SIZE, 110, GroupLayout.PREFERRED_SIZE)
                                                    .addGap(10, 10, 10)
                                                    .addComponent(btnEditarEsp, GroupLayout.PREFERRED_SIZE, 110, GroupLayout.PREFERRED_SIZE)
                                                    .addGap(10, 10, 10)
                                                    .addComponent(btnEliminarEsp, GroupLayout.PREFERRED_SIZE, 110, GroupLayout.PREFERRED_SIZE)
                                                    .addGap(10, 10, 10)
                                                    .addComponent(btnLimpiarEsp, GroupLayout.PREFERRED_SIZE, 110, GroupLayout.PREFERRED_SIZE))
                                            .addComponent(lblHistorialEsp)
                                            .addComponent(scrollEsp, GroupLayout.DEFAULT_SIZE, 560, Short.MAX_VALUE))
                                    .addGap(30, 30, 30))
            );
            layout.setVerticalGroup(
                    layout.createSequentialGroup()
                            .addGap(30, 30, 30)
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                    .addComponent(lblNombreEsp)
                                    .addComponent(txtNombreEsp, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                            .addGap(15, 15, 15)
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                    .addComponent(lblDescripcionEsp)
                                    .addComponent(txtDescripcionEsp, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                            .addGap(20, 20, 20)
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                    .addComponent(btnGuardarEsp, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(btnEditarEsp, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(btnEliminarEsp, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(btnLimpiarEsp, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                            .addGap(25, 25, 25)
                            .addComponent(lblHistorialEsp)
                            .addGap(8, 8, 8)
                            .addComponent(scrollEsp, 180, 220, Short.MAX_VALUE)
                            .addGap(30, 30, 30)
            );

            Estilo.ventanaModulo(this, "Registro de especialidades",
                    "Agrega, edita o elimina especialidades médicas");
            pack();
        }

        // ---------- Historial ----------
        private void listarEspecialidades() {
            String sql = "SELECT e.id_especialidad, e.nombre, e.descripcion, "
                    + "COUNT(m.id_medico) AS medicos "
                    + "FROM especialidad e LEFT JOIN medico m "
                    + "ON m.id_especialidad = e.id_especialidad "
                    + "GROUP BY e.id_especialidad, e.nombre, e.descripcion "
                    + "ORDER BY e.id_especialidad DESC";
            modeloEsp.setRowCount(0);
            try (Connection con = Conexion.conectar();
                 Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    Object[] fila = {
                            rs.getInt("id_especialidad"),
                            rs.getString("nombre"),
                            rs.getString("descripcion"),
                            rs.getInt("medicos")
                    };
                    modeloEsp.addRow(fila);
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error al listar especialidades: " + e.getMessage());
            }
        }

        private void seleccionarFilaEsp() {
            int fila = tablaEsp.getSelectedRow();
            if (fila >= 0) {
                idEspecialidadSeleccionada = Integer.parseInt(tablaEsp.getValueAt(fila, 0).toString());
                txtNombreEsp.setText(tablaEsp.getValueAt(fila, 1).toString());
                Object desc = tablaEsp.getValueAt(fila, 2);
                txtDescripcionEsp.setText(desc != null ? desc.toString() : "");
                btnGuardarEsp.setEnabled(false);
                btnEditarEsp.setEnabled(true);
                btnEliminarEsp.setEnabled(true);
            }
        }

        private void limpiarEsp() {
            idEspecialidadSeleccionada = -1;
            txtNombreEsp.setText("");
            txtDescripcionEsp.setText("");
            tablaEsp.clearSelection();
            btnGuardarEsp.setEnabled(true);
            btnEditarEsp.setEnabled(false);
            btnEliminarEsp.setEnabled(false);
        }

        // Actualiza esta tabla, el combo y la tabla de médicos de la ventana padre
        private void refrescarTodo() {
            listarEspecialidades();
            cargarComboEspecialidades();
            listarMedicos();
        }

        // Verifica si ya existe una especialidad con ese nombre (excluyendo un id)
        private boolean existeNombre(String nombre, int idExcluir) {
            String sql = "SELECT COUNT(*) FROM especialidad WHERE LOWER(nombre) = LOWER(?) AND id_especialidad <> ?";
            try (Connection con = Conexion.conectar();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, nombre);
                ps.setInt(2, idExcluir);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            } catch (SQLException e) {
                return false;
            }
        }

        // ---------- Guardar ----------
        private void guardarEspecialidad() {
            String nombre = txtNombreEsp.getText().trim();
            String descripcion = txtDescripcionEsp.getText().trim();
            if (nombre.isEmpty()) {
                JOptionPane.showMessageDialog(this, "El nombre de la especialidad es obligatorio.");
                return;
            }
            if (existeNombre(nombre, -1)) {
                JOptionPane.showMessageDialog(this, "Ya existe una especialidad con ese nombre.");
                return;
            }
            String sql = "INSERT INTO especialidad (nombre, descripcion) VALUES (?, ?)";
            try (Connection con = Conexion.conectar();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, nombre);
                ps.setString(2, descripcion);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "¡Especialidad guardada correctamente!");
                limpiarEsp();
                refrescarTodo();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error de base de datos al insertar: " + e.getMessage());
            }
        }

        // ---------- Editar ----------
        private void editarEspecialidad() {
            if (idEspecialidadSeleccionada == -1) return;
            String nombre = txtNombreEsp.getText().trim();
            String descripcion = txtDescripcionEsp.getText().trim();
            if (nombre.isEmpty()) {
                JOptionPane.showMessageDialog(this, "El nombre de la especialidad es obligatorio.");
                return;
            }
            if (existeNombre(nombre, idEspecialidadSeleccionada)) {
                JOptionPane.showMessageDialog(this, "Ya existe otra especialidad con ese nombre.");
                return;
            }
            String sql = "UPDATE especialidad SET nombre=?, descripcion=? WHERE id_especialidad=?";
            try (Connection con = Conexion.conectar();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, nombre);
                ps.setString(2, descripcion);
                ps.setInt(3, idEspecialidadSeleccionada);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "¡Especialidad actualizada correctamente!");
                limpiarEsp();
                refrescarTodo();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error al actualizar: " + e.getMessage());
            }
        }

        // ---------- Eliminar ----------
        private void eliminarEspecialidad() {
            if (idEspecialidadSeleccionada == -1) return;

            // No permitir borrar si hay médicos con esta especialidad
            String sqlCuenta = "SELECT COUNT(*) FROM medico WHERE id_especialidad = ?";
            try (Connection con = Conexion.conectar();
                 PreparedStatement ps = con.prepareStatement(sqlCuenta)) {
                ps.setInt(1, idEspecialidadSeleccionada);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        JOptionPane.showMessageDialog(this,
                                "Hay " + rs.getInt(1) + " médico(s) con esta especialidad.\n"
                                        + "Reasígnalos a otra especialidad antes de eliminarla.",
                                "Restricción de Eliminación", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error de consulta: " + e.getMessage());
                return;
            }

            int confirmar = JOptionPane.showConfirmDialog(this,
                    "¿Desea eliminar esta especialidad?", "Confirmar",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirmar != JOptionPane.YES_OPTION) return;

            String sql = "DELETE FROM especialidad WHERE id_especialidad = ?";
            try (Connection con = Conexion.conectar();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idEspecialidadSeleccionada);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Especialidad eliminada.");
                limpiarEsp();
                refrescarTodo();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error al eliminar: " + e.getMessage());
            }
        }
    }

    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnEditar;
    private javax.swing.JButton btnEliminar;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnVerVinculos;
    private javax.swing.JButton btnGestionarEspecialidad;
    private javax.swing.JComboBox cboEspecialidad;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField txtApellidos;
    private javax.swing.JTextField txtCmp;
    private javax.swing.JTextField txtEmail;
    private javax.swing.JTextField txtNombres;
    private javax.swing.JTextField txtTelefono;
}