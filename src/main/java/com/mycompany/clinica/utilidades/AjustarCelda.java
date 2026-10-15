package com.mycompany.clinica.utilidades;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.TableCellRenderer;

/** Renderizador de texto multilínea para tablas, útil para diagnósticos y observaciones. */
public class AjustarCelda extends JTextArea implements TableCellRenderer {
    public AjustarCelda() {
        setLineWrap(true);
        setWrapStyleWord(true);
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        setText(value == null ? "" : value.toString());
        setFont(table.getFont());
        if (isSelected) {
            setBackground(table.getSelectionBackground());
            setForeground(table.getSelectionForeground());
        } else {
            setBackground(table.getBackground());
            setForeground(table.getForeground());
        }
        int ancho = Math.max(1, table.getColumnModel().getColumn(column).getWidth());
        setSize(ancho, Short.MAX_VALUE);
        int altura = Math.max(24, getPreferredSize().height);
        if (table.getRowHeight(row) != altura) {
            table.setRowHeight(row, altura);
        }
        return this;
    }
}
