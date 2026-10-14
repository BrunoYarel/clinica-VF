package com.mycompany.clinica;

public class FrmEspecialidad extends javax.swing.JInternalFrame {

    private FrmMedico frmMedicoPadre;

    public FrmEspecialidad() {
        initComponents();
    }

    public FrmEspecialidad(FrmMedico frmMedicoPadre) {
        initComponents();
        this.frmMedicoPadre = frmMedicoPadre;
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        txtNombre = new javax.swing.JTextField();
        txtDescripcion = new javax.swing.JTextField();
        btnGuardar = new javax.swing.JButton();

        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Registrar Especialidad Médica");

        jLabel1.setText("Nombre:");
        jLabel2.setText("Descripción:");

        btnGuardar.setText("GUARDAR");
        btnGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2))
                .addGap(30, 30, 30)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtNombre, javax.swing.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)
                    .addComponent(txtDescripcion))
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
                    .addComponent(txtDescripcion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(35, 35, 35)
                .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(40, Short.MAX_VALUE))
        );

        pack();
    }

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {                                           
        String nombre = txtNombre.getText().trim();
        String description = txtDescripcion.getText().trim();

        if (nombre.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "El nombre de la especialidad es obligatorio.");
            return;
        }

        // Corregido con corchetes [] para que compile en Java
        String[] urls = {
            "jdbc:mysql://localhost:3306/clinica?serverTimezone=UTC&useSSL=false",
            "jdbc:mysql://localhost:3307/clinica?serverTimezone=UTC&useSSL=false"
        };
        String[] claves = {"", "root"};

        java.sql.Connection con = null;

        for (String url : urls) {
            for (String clave : claves) {
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    con = java.sql.DriverManager.getConnection(url, "root", clave);
                    if (con != null) break;
                } catch (Exception e) {
                    // Sigue intentando si falla
                }
            }
            if (con != null) break;
        }

        if (con == null) {
            javax.swing.JOptionPane.showMessageDialog(this, "Error: No se pudo conectar a MySQL. Verifica tu XAMPP.");
            return;
        }

        String sql = "INSERT INTO especialidad (nombre, descripcion) VALUES (?, ?)";

        try (java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, description);
            ps.executeUpdate(); 
            
            javax.swing.JOptionPane.showMessageDialog(this, "¡Especialidad guardada correctamente!");
            txtNombre.setText("");
            txtDescripcion.setText("");
            
            if (frmMedicoPadre != null) {
                frmMedicoPadre.cargarComboEspecialidades();
            }
            
            con.close();
            this.dispose(); 
            
        } catch (java.sql.SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Error de base de datos al insertar: " + e.getMessage());
        }
    }                                          

    private javax.swing.JButton btnGuardar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JTextField txtDescripcion;
    private javax.swing.JTextField txtNombre;                 
}
