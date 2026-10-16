package com.mycompany.clinica.vista;

import com.mycompany.clinica.Conexion;
import com.mycompany.clinica.servicio.Sesion;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

public class FrmReceta extends javax.swing.JInternalFrame {

    private Map<String, Integer> atencionesMap = new HashMap<>();

    // [FIX receta-medicamento] Una receta lleva uno o más medicamentos (tabla detalle_receta).
    // Antes este formulario solo guardaba las indicaciones y la receta quedaba sin medicamentos.
    private final List<Integer> idsMedicamento = new ArrayList<>();   // mismo orden que cboMedicamento
    private final List<DetalleMed> detalles = new ArrayList<>();      // medicamentos de la receta en pantalla

    /** Una línea de la receta: medicamento + dosis, unidad, frecuencia y duración. */
    private static class DetalleMed {
        final int idMedicamento;
        final String nombre;
        final BigDecimal dosis;
        final String unidad;
        final String frecuencia;
        final String duracion;

        DetalleMed(int idMedicamento, String nombre, BigDecimal dosis, String unidad, String frecuencia, String duracion) {
            this.idMedicamento = idMedicamento;
            this.nombre = nombre;
            this.dosis = dosis;
            this.unidad = unidad;
            this.frecuencia = frecuencia;
            this.duracion = duracion;
        }
    }

    public FrmReceta() {
        initComponents();
        txtIdReceta.setEnabled(false);
        tblRecetas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblDetalle.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        cargarAtenciones();
        cargarMedicamentos();
        mostrarDetalles();
        listarRecetas();
        estadoInicial();
    }

    private void habilitarCampos(boolean estado) {
        cboAtencion.setEnabled(estado);
        txtIndicaciones.setEnabled(estado);
        cboMedicamento.setEnabled(estado);
        txtDosis.setEnabled(estado);
        cboUnidad.setEnabled(estado);
        txtFrecuencia.setEnabled(estado);
        txtDuracion.setEnabled(estado);
        btnAgregarMed.setEnabled(estado);
        btnQuitarMed.setEnabled(estado);
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
        detalles.clear();
        mostrarDetalles();
        limpiarCamposMedicamento();
    }

    private void cargarAtenciones() {
        cboAtencion.removeAllItems();
        atencionesMap.clear();
        // Restricción por rol: el médico solo puede recetar sobre SUS atenciones
        String sql = "SELECT a.id_atencion, CONCAT('Atención #', a.id_atencion, ' - Diagnóstico: ', a.diagnostico) AS descripcion " +
                "FROM atencion a INNER JOIN cita c ON a.id_cita = c.id_cita " +
                "WHERE c.id_medico = ?";

        try (Connection cn = Conexion.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, Sesion.getIdMedico());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idAtencion = rs.getInt("id_atencion");
                    String desc = rs.getString("descripcion");
                    cboAtencion.addItem(desc);
                    atencionesMap.put(desc, idAtencion);
                }
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

        // Restricción por rol: solo las recetas de las atenciones del médico en sesión
        String sql = "SELECT r.id_receta, r.id_atencion, r.fecha_emision, r.indicaciones " +
                "FROM receta r INNER JOIN atencion a ON r.id_atencion = a.id_atencion " +
                "INNER JOIN cita c ON a.id_cita = c.id_cita " +
                "WHERE c.id_medico = ? ORDER BY r.id_receta DESC";

        try (Connection cn = Conexion.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, Sesion.getIdMedico());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object[] fila = {
                            rs.getInt("id_receta"),
                            rs.getInt("id_atencion"),
                            rs.getString("fecha_emision"),
                            rs.getString("indicaciones")
                    };
                    modelo.addRow(fila);
                }
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
        if (detalles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Agregue al menos un medicamento a la receta.");
            cboMedicamento.requestFocus();
            return false;
        }
        return true;
    }

    private void guardarReceta() {
        if (!validarCampos()) return;

        int idAtencion = atencionesMap.get(cboAtencion.getSelectedItem().toString());
        String sql = "INSERT INTO receta (id_atencion, indicaciones) VALUES (?, ?)";

        // [FIX receta-medicamento] La receta y sus medicamentos se guardan juntos (transacción):
        // si falla una de las dos cosas no queda una receta a medias.
        try (Connection cn = Conexion.conectar()) {
            cn.setAutoCommit(false);
            try (PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setInt(1, idAtencion);
                ps.setString(2, txtIndicaciones.getText().trim());
                ps.executeUpdate();

                int idReceta;
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) {
                        throw new SQLException("No se pudo obtener el id de la receta.");
                    }
                    idReceta = rs.getInt(1);
                }
                insertarDetalles(cn, idReceta);
                cn.commit();
            } catch (SQLException e) {
                cn.rollback();
                throw e;
            }
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
        String sqlBorrar = "DELETE FROM detalle_receta WHERE id_receta = ?";

        // [FIX receta-medicamento] Indicaciones y medicamentos se actualizan juntos (transacción).
        try (Connection cn = Conexion.conectar()) {
            cn.setAutoCommit(false);
            int idReceta = Integer.parseInt(txtIdReceta.getText());
            try (PreparedStatement ps = cn.prepareStatement(sql);
                 PreparedStatement psBorrar = cn.prepareStatement(sqlBorrar)) {

                ps.setString(1, txtIndicaciones.getText().trim());
                ps.setInt(2, idReceta);
                ps.executeUpdate();

                psBorrar.setInt(1, idReceta);
                psBorrar.executeUpdate();
                insertarDetalles(cn, idReceta);
                cn.commit();
            } catch (SQLException e) {
                cn.rollback();
                throw e;
            }
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

    // =====================================================================
    //  [FIX receta-medicamento] Medicamentos de la receta (tabla detalle_receta)
    // =====================================================================

    /** Carga el catálogo de medicamentos (el que administra el Admin) en el combo. */
    private void cargarMedicamentos() {
        cboMedicamento.removeAllItems();
        idsMedicamento.clear();
        String sql = "SELECT id_medicamento, nombre, presentacion FROM medicamento ORDER BY nombre, presentacion";

        try (Connection cn = Conexion.conectar();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                idsMedicamento.add(rs.getInt("id_medicamento"));
                cboMedicamento.addItem(rs.getString("nombre") + " - " + rs.getString("presentacion"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar medicamentos: " + e.getMessage());
        }
    }

    private void limpiarCamposMedicamento() {
        if (cboMedicamento.getItemCount() > 0) {
            cboMedicamento.setSelectedIndex(0);
        }
        txtDosis.setText("");
        if (cboUnidad.getItemCount() > 0) {
            cboUnidad.setSelectedIndex(0);
        }
        txtFrecuencia.setText("");
        txtDuracion.setText("");
        tblDetalle.clearSelection();
    }

    /** Dibuja en la tabla de la derecha los medicamentos que lleva la receta en pantalla. */
    private void mostrarDetalles() {
        DefaultTableModel modelo = new DefaultTableModel(
                new Object[]{"Medicamento", "Dosis", "Unidad", "Frecuencia", "Duración"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (DetalleMed d : detalles) {
            modelo.addRow(new Object[]{d.nombre, d.dosis.stripTrailingZeros().toPlainString(),
                    d.unidad, d.frecuencia, d.duracion});
        }
        tblDetalle.setModel(modelo);
        int[] anchos = {200, 50, 70, 110, 90};
        for (int i = 0; i < anchos.length; i++) {
            tblDetalle.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
    }

    /** Trae de la base de datos los medicamentos de una receta ya guardada. */
    private void cargarDetallesDeReceta(int idReceta) {
        detalles.clear();
        String sql = "SELECT d.id_medicamento, m.nombre, m.presentacion, d.dosis, d.unidad_medida, d.frecuencia, d.duracion "
                + "FROM detalle_receta d INNER JOIN medicamento m ON d.id_medicamento = m.id_medicamento "
                + "WHERE d.id_receta = ? ORDER BY d.id_detalle_receta";

        try (Connection cn = Conexion.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idReceta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    detalles.add(new DetalleMed(
                            rs.getInt("id_medicamento"),
                            rs.getString("nombre") + " - " + rs.getString("presentacion"),
                            rs.getBigDecimal("dosis"),
                            rs.getString("unidad_medida"),
                            rs.getString("frecuencia"),
                            rs.getString("duracion")));
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar los medicamentos de la receta: " + e.getMessage());
        }
        mostrarDetalles();
    }

    /** Valida los campos del medicamento y lo agrega a la lista de la receta (todavía no toca la base de datos). */
    private void agregarMedicamento() {
        int pos = cboMedicamento.getSelectedIndex();
        if (pos < 0) {
            JOptionPane.showMessageDialog(this,
                    "No hay medicamentos en el catálogo.\nPida al administrador que los registre en el módulo Medicamentos.");
            return;
        }
        int idMedicamento = idsMedicamento.get(pos);

        BigDecimal dosis;
        try {
            dosis = new BigDecimal(txtDosis.getText().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Ingrese la dosis como número (ejemplo: 1 o 0.5).");
            txtDosis.requestFocus();
            return;
        }
        if (dosis.signum() <= 0 || dosis.stripTrailingZeros().scale() > 2
                || dosis.compareTo(new BigDecimal("9999.99")) > 0) {
            JOptionPane.showMessageDialog(this, "La dosis debe ser mayor que 0, hasta 9999.99 y con máximo 2 decimales.");
            txtDosis.requestFocus();
            return;
        }
        dosis = dosis.setScale(2);

        String unidad = cboUnidad.getSelectedItem() == null ? "" : cboUnidad.getSelectedItem().toString().trim();
        String frecuencia = txtFrecuencia.getText().trim();
        String duracion = txtDuracion.getText().trim();

        if (unidad.isEmpty() || unidad.length() > 20) {
            JOptionPane.showMessageDialog(this, "Indique la unidad de la dosis (máximo 20 caracteres).");
            cboUnidad.requestFocus();
            return;
        }
        if (frecuencia.isEmpty() || frecuencia.length() > 100) {
            JOptionPane.showMessageDialog(this, "Indique la frecuencia, por ejemplo \"Cada 8 horas\" (máximo 100 caracteres).");
            txtFrecuencia.requestFocus();
            return;
        }
        if (duracion.isEmpty() || duracion.length() > 50) {
            JOptionPane.showMessageDialog(this, "Indique la duración, por ejemplo \"7 días\" (máximo 50 caracteres).");
            txtDuracion.requestFocus();
            return;
        }
        for (DetalleMed d : detalles) {
            if (d.idMedicamento == idMedicamento) {
                JOptionPane.showMessageDialog(this, "Ese medicamento ya está en la receta. Quítelo si desea cambiar su dosis.");
                return;
            }
        }

        detalles.add(new DetalleMed(idMedicamento, cboMedicamento.getSelectedItem().toString(),
                dosis, unidad, frecuencia, duracion));
        mostrarDetalles();
        limpiarCamposMedicamento();
    }

    private void quitarMedicamento() {
        int fila = tblDetalle.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione en la lista el medicamento que desea quitar.");
            return;
        }
        detalles.remove(fila);
        mostrarDetalles();
    }

    /** Guarda en detalle_receta todos los medicamentos de la pantalla, usando la conexión (y transacción) recibida. */
    private void insertarDetalles(Connection cn, int idReceta) throws SQLException {
        String sql = "INSERT INTO detalle_receta (id_receta, id_medicamento, dosis, unidad_medida, frecuencia, duracion) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            for (DetalleMed d : detalles) {
                ps.setInt(1, idReceta);
                ps.setInt(2, d.idMedicamento);
                ps.setBigDecimal(3, d.dosis);
                ps.setString(4, d.unidad);
                ps.setString(5, d.frecuencia);
                ps.setString(6, d.duracion);
                ps.addBatch();
            }
            ps.executeBatch();
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

        // [FIX receta-medicamento] Componentes para elegir los medicamentos de la receta
        jPanel3 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        cboMedicamento = Estilo.combo(new String[0]);
        txtDosis = Estilo.campoTexto();
        cboUnidad = Estilo.combo(new String[]{"Tableta", "Cápsula", "ml", "mg", "Gotas", "Sobre", "Ampolla", "Aplicación"});
        cboUnidad.setEditable(true);   // si la unidad no está en la lista, se puede escribir
        txtFrecuencia = Estilo.campoTexto();
        txtDuracion = Estilo.campoTexto();
        btnAgregarMed = Estilo.botonPrimario("Agregar");
        btnQuitarMed = Estilo.botonSecundario("Quitar");
        jScrollPane3 = new javax.swing.JScrollPane();
        tblDetalle = new javax.swing.JTable();

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
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
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
                                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 500, Short.MAX_VALUE)))
                                .addContainerGap(20, 20))
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

        // --- [FIX receta-medicamento] Panel "Agregar medicamento" (una sola fila de campos) ---
        jPanel3.setBackground(java.awt.Color.WHITE);
        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(
                javax.swing.BorderFactory.createLineBorder(Estilo.BORDE, 1), "Agregar medicamento a la receta",
                javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                Estilo.fuente(java.awt.Font.BOLD, 13), Estilo.TEXTO));
        jPanel3.setLayout(new java.awt.GridBagLayout());
        cboMedicamento.setPreferredSize(new java.awt.Dimension(250, 40));
        // setColumns(0): un JTextField con columnas ignora el ancho que se le pida con setPreferredSize
        txtDosis.setColumns(0);
        txtFrecuencia.setColumns(0);
        txtDuracion.setColumns(0);
        txtDosis.setPreferredSize(new java.awt.Dimension(80, 40));
        cboUnidad.setPreferredSize(new java.awt.Dimension(120, 40));
        txtFrecuencia.setPreferredSize(new java.awt.Dimension(150, 40));
        txtDuracion.setPreferredSize(new java.awt.Dimension(100, 40));
        colocar(jPanel3, Estilo.texto("Medicamento", java.awt.Font.BOLD, 12, Estilo.TEXTO_SUAVE), 0, 0, 1.0);
        colocar(jPanel3, Estilo.texto("Dosis", java.awt.Font.BOLD, 12, Estilo.TEXTO_SUAVE), 1, 0, 0.0);
        colocar(jPanel3, Estilo.texto("Unidad", java.awt.Font.BOLD, 12, Estilo.TEXTO_SUAVE), 2, 0, 0.0);
        colocar(jPanel3, Estilo.texto("Frecuencia", java.awt.Font.BOLD, 12, Estilo.TEXTO_SUAVE), 3, 0, 0.0);
        colocar(jPanel3, Estilo.texto("Duración", java.awt.Font.BOLD, 12, Estilo.TEXTO_SUAVE), 4, 0, 0.0);
        colocar(jPanel3, cboMedicamento, 0, 1, 1.0);
        colocar(jPanel3, txtDosis, 1, 1, 0.0);
        colocar(jPanel3, cboUnidad, 2, 1, 0.0);
        colocar(jPanel3, txtFrecuencia, 3, 1, 0.0);
        colocar(jPanel3, txtDuracion, 4, 1, 0.0);
        btnAgregarMed.setText("Agregar");
        btnAgregarMed.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                agregarMedicamento();
            }
        });
        colocar(jPanel3, btnAgregarMed, 5, 1, 0.0);
        btnQuitarMed.setText("Quitar");
        btnQuitarMed.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quitarMedicamento();
            }
        });
        colocar(jPanel3, btnQuitarMed, 6, 1, 0.0);

        // --- Tabla con los medicamentos de la receta (al lado de la lista de recetas) ---
        tblDetalle.setModel(new javax.swing.table.DefaultTableModel(
                new Object [][] {},
                new String [] {}
        ));
        jScrollPane3.setViewportView(tblDetalle);
        Estilo.tabla(tblDetalle, jScrollPane3);
        tblDetalle.setRowHeight(26);   // filas y encabezado mas bajos para que quepan ~3 medicamentos a la vista
        tblDetalle.getTableHeader().setPreferredSize(new java.awt.Dimension(0, 30));
        jPanel4.setOpaque(false);
        jPanel4.setLayout(new java.awt.BorderLayout());
        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(
                javax.swing.BorderFactory.createEmptyBorder(), "Medicamentos de la receta",
                javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                Estilo.fuente(java.awt.Font.BOLD, 13), Estilo.TEXTO));
        jPanel4.add(jScrollPane3, java.awt.BorderLayout.CENTER);

        jPanel5.setOpaque(false);
        jPanel5.setLayout(new java.awt.BorderLayout(10, 0));
        jScrollPane2.setPreferredSize(new java.awt.Dimension(380, 125));   // lista de recetas (izquierda)
        jPanel5.add(jScrollPane2, java.awt.BorderLayout.WEST);
        jPanel5.add(jPanel4, java.awt.BorderLayout.CENTER);                // medicamentos de la receta (derecha, ocupa el resto)

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(15, 15, 15)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap(15, 15))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(6, 6, 6)
                                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(6, 6, 6)
                                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(6, 6, 6)
                                .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, 125, Short.MAX_VALUE)
                                .addContainerGap(10, 10))
        );

        Estilo.ventanaModulo(this, "Gestión de recetas médicas",
                "Emite y consulta las indicaciones y los medicamentos de cada atención");

        pack();
    }

    /** Coloca un componente en la fila 'y', columna 'x' del panel de medicamentos (GridBagLayout). */
    private void colocar(javax.swing.JPanel panel, java.awt.Component componente, int x, int y, double pesoX) {
        java.awt.GridBagConstraints g = new java.awt.GridBagConstraints();
        g.gridx = x;
        g.gridy = y;
        g.weightx = pesoX;
        g.fill = java.awt.GridBagConstraints.HORIZONTAL;
        g.anchor = java.awt.GridBagConstraints.WEST;
        g.insets = new java.awt.Insets(y == 0 ? 0 : 2, x == 0 ? 15 : 6, y == 0 ? 0 : 6, x == 6 ? 15 : 0);
        panel.add(componente, g);
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
            cargarDetallesDeReceta(Integer.parseInt(txtIdReceta.getText()));   // [FIX receta-medicamento]
            // [FIX receta-medicamento] Mostrar la atención (diagnóstico) a la que pertenece la receta elegida
            int idAtencionReceta = Integer.parseInt(tblRecetas.getValueAt(fila, 1).toString());
            for (Map.Entry<String, Integer> e : atencionesMap.entrySet()) {
                if (e.getValue() == idAtencionReceta) {
                    cboAtencion.setSelectedItem(e.getKey());
                    break;
                }
            }
            estadoEdicion();
        }
    }

    private javax.swing.JButton btnActualizar;
    private javax.swing.JButton btnAgregarMed;
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnEliminar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnNuevo;
    private javax.swing.JButton btnQuitarMed;
    private javax.swing.JComboBox<String> cboAtencion;
    private javax.swing.JComboBox<String> cboMedicamento;
    private javax.swing.JComboBox<String> cboUnidad;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable tblDetalle;
    private javax.swing.JTable tblRecetas;
    private javax.swing.JTextField txtDosis;
    private javax.swing.JTextField txtDuracion;
    private javax.swing.JTextField txtFrecuencia;
    private javax.swing.JTextField txtIdReceta;
    private javax.swing.JTextArea txtIndicaciones;
}