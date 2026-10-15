package com.mycompany.clinica.vista;

import com.mycompany.clinica.Conexion;
import javax.swing.table.DefaultTableModel;

public class FrmMedicamento extends javax.swing.JInternalFrame {

    private DefaultTableModel modeloTabla;

    public FrmMedicamento() {
        initComponents();
        listarMedicamentos(); // Carga el historial al abrir la ventana
    }

    private void listarMedicamentos() {
        modeloTabla.setRowCount(0); // Limpia las filas viejas antes de cargar
        String sql = "SELECT nombre, presentacion FROM medicamento ORDER BY nombre ASC";

        try {
            java.sql.Connection con = Conexion.conectar();
            if (con != null) {
                try (java.sql.PreparedStatement ps = con.prepareStatement(sql);
                     java.sql.ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {
                        Object[] fila = new Object[2];
                        fila[0] = rs.getString("nombre");
                        fila[1] = rs.getString("presentacion");
                        modeloTabla.addRow(fila);
                    }
                }
                con.close();
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Error al listar medicamentos: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        jLabel1 = Estilo.texto("", java.awt.Font.BOLD, 12, Estilo.TEXTO_SUAVE);
        jLabel2 = Estilo.texto("", java.awt.Font.BOLD, 12, Estilo.TEXTO_SUAVE);
        txtNombre = Estilo.campoTexto();
        txtPresentacion = Estilo.campoTexto();
        btnGuardar = Estilo.botonPrimario("");

        jScrollPane1 = new javax.swing.JScrollPane();
        tablaMedicamentos = new javax.swing.JTable();

        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Gestión de Medicamentos");

        jLabel1.setText("Nombre:");

        jLabel2.setText("Presentación:");

        btnGuardar.setText("Guardar medicamento");
        btnGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarActionPerformed(evt);
            }
        });

        // Configuración estructural de la JTable
        modeloTabla = new DefaultTableModel(
                new Object [][] {},
                new String [] { "Nombre del Medicamento", "Presentación" }
        ) {
            boolean[] canEdit = new boolean [] { false, false };
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        };
        tablaMedicamentos.setModel(modeloTabla);
        jScrollPane1.setViewportView(tablaMedicamentos);
        Estilo.tabla(tablaMedicamentos, jScrollPane1);   // diseño de la tabla (viene de Estilo)

        // Layout editado para organizar las cajas y la tabla horizontalmente
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(40, 40, 40)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 330, Short.MAX_VALUE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel1)
                                                        .addComponent(jLabel2))
                                                .addGap(30, 30, 30)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                        .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(txtNombre, javax.swing.GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                                        .addComponent(txtPresentacion))))
                                .addContainerGap(40, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(40, 40, 40)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel1)
                                        .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(25, 25, 25)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel2)
                                        .addComponent(txtPresentacion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(35, 35, 35)
                                .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(25, 25, 25)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(40, Short.MAX_VALUE))
        );

        Estilo.ventanaModulo(this, "Gestión de medicamentos", "Registra medicamentos y consulta el catálogo");

        pack();
    }

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {
        String nombre = txtNombre.getText().trim();
        String presentacion = txtPresentacion.getText().trim();

        if (nombre.isEmpty() || presentacion.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "Por favor, llene todos los campos del medicamento.");
            return;
        }

        String sql = "INSERT INTO medicamento (nombre, presentacion) VALUES (?, ?)";

        try {
            java.sql.Connection con = Conexion.conectar();

            if (con == null) {
                javax.swing.JOptionPane.showMessageDialog(this, "Error: No se pudo conectar a MySQL. Verifica XAMPP.");
                return;
            }

            try (java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, nombre);
                ps.setString(2, presentacion);

                ps.executeUpdate(); // Inserta en la Base de Datos

                javax.swing.JOptionPane.showMessageDialog(this, "¡Medicamento guardado correctamente!");

                txtNombre.setText("");
                txtPresentacion.setText("");

                listarMedicamentos(); // Actualiza la lista en pantalla automáticamente
            }
            con.close();

        } catch (java.sql.SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Error de base de datos: " + e.getMessage());
        }
    }

    // Declaración final de componentes y variables
    private javax.swing.JButton btnGuardar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tablaMedicamentos;
    private javax.swing.JTextField txtPresentacion;
    private javax.swing.JTextField txtNombre;
}