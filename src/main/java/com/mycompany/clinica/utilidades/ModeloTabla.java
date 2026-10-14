package com.mycompany.clinica.utilidades;

import javax.swing.table.DefaultTableModel;

/**
 * Modelo de tabla de solo lectura: las filas se llenan desde la base de datos
 * y el usuario no puede editar las celdas escribiendo sobre ellas.
 *
 * Uso: ModeloTabla modelo = new ModeloTabla("ID", "Nombre"); modelo.addRow(new Object[]{1, "Ana"});
 */
public class ModeloTabla extends DefaultTableModel {

    public ModeloTabla(String... columnas) {
        super(columnas, 0);
    }

    @Override
    public boolean isCellEditable(int fila, int columna) {
        return false;
    }
}
