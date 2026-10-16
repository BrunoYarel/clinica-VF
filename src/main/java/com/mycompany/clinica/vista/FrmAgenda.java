package com.mycompany.clinica.vista;

import com.mycompany.clinica.servicio.AgendaService;
import com.mycompany.clinica.servicio.AgendaService.FilaAgenda;
import com.mycompany.clinica.servicio.ReglaNegocioException;
import com.mycompany.clinica.utilidades.ModeloTabla;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Mi agenda (solo Médico): muestra las citas del médico en sesión, filtradas por período y estado.
 * Es un PANEL incrustable (MenuPrincipal lo muestra como vista por defecto del médico). Es de solo lectura
 * y se vuelve a leer de la base de datos al abrirla, al pulsar "Actualizar" y cuando el menú llama a
 * actualizar() (al volver de otro módulo); así refleja lo que hagan Citas (confirmar/cancelar) y Atención
 * (registrar una atención). Todo el SQL y las reglas viven en AgendaService.
 */
public class FrmAgenda extends JPanel {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(FrmAgenda.class.getName());

    private static final DateTimeFormatter MOSTRAR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] PERIODOS = {"Hoy", "Próximos 7 días", "Este mes", "Todas"};

    private final ModeloTabla modelo = new ModeloTabla("Fecha", "Hora", "Paciente", "Documento", "Motivo", "Estado");
    private List<FilaAgenda> filas = new ArrayList<>();   // mismo orden que las filas de la tabla

    private JComboBox<String> cboPeriodo;
    private JComboBox<String> cboEstado;
    private JTable tblAgenda;
    private JLabel lblResumen;
    private JTextArea txtDetalle;

    public FrmAgenda() {
        super(new BorderLayout());
        setOpaque(false);
        initComponents();
        cargar(false);
    }

    /** Vuelve a leer la BD sin cuadros de error (lo llama el menú al recuperar el foco o pulsar el atajo). */
    public void actualizar() {
        cargar(false);
    }

    private void initComponents() {
        // ---------- FILTROS ----------
        cboPeriodo = Estilo.combo(PERIODOS);
        cboEstado = Estilo.combo(estadosConTodos());
        cboPeriodo.addActionListener(e -> cargar());
        cboEstado.addActionListener(e -> cargar());

        JButton btnActualizar = Estilo.botonPrimario("Actualizar");
        btnActualizar.addActionListener(e -> cargar());

        JPanel filtros = new JPanel(new GridLayout(1, 3, 14, 0));
        filtros.setOpaque(false);
        filtros.add(Estilo.campo("Período", cboPeriodo));
        filtros.add(Estilo.campo("Estado", cboEstado));
        JPanel botonActualizar = new JPanel(new BorderLayout());
        botonActualizar.setOpaque(false);
        botonActualizar.add(btnActualizar, BorderLayout.SOUTH);   // alineado con los combos
        filtros.add(botonActualizar);

        lblResumen = Estilo.texto(" ", Font.BOLD, 13, Estilo.TEXTO_SUAVE);

        JPanel arriba = new JPanel(new BorderLayout(0, 10));
        arriba.setOpaque(false);
        arriba.add(filtros, BorderLayout.NORTH);
        arriba.add(lblResumen, BorderLayout.SOUTH);

        // ---------- TABLA ----------
        tblAgenda = new JTable(modelo);
        tblAgenda.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblAgenda.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tblAgenda.getTableHeader().setReorderingAllowed(false);
        JScrollPane scroll = new JScrollPane(tblAgenda);
        Estilo.tabla(tblAgenda, scroll);
        tblAgenda.getColumnModel().getColumn(0).setPreferredWidth(90);
        tblAgenda.getColumnModel().getColumn(1).setPreferredWidth(60);
        tblAgenda.getColumnModel().getColumn(2).setPreferredWidth(220);
        tblAgenda.getColumnModel().getColumn(3).setPreferredWidth(90);
        tblAgenda.getColumnModel().getColumn(4).setPreferredWidth(240);
        tblAgenda.getColumnModel().getColumn(5).setPreferredWidth(90);
        tblAgenda.getColumnModel().getColumn(5).setCellRenderer(new RenderEstado());
        tblAgenda.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarDetalle();
            }
        });

        // ---------- DETALLE ----------
        txtDetalle = new JTextArea(3, 40);
        txtDetalle.setEditable(false);
        txtDetalle.setLineWrap(true);
        txtDetalle.setWrapStyleWord(true);
        txtDetalle.setFont(Estilo.fuente(Font.PLAIN, 13));
        txtDetalle.setForeground(Estilo.TEXTO);
        txtDetalle.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JScrollPane scrollDetalle = new JScrollPane(txtDetalle);
        scrollDetalle.setBorder(BorderFactory.createLineBorder(Estilo.BORDE, 1));
        mostrarDetalle();

        JPanel abajo = new JPanel(new BorderLayout(0, 6));
        abajo.setOpaque(false);
        abajo.add(Estilo.texto("Detalle de la cita seleccionada", Font.BOLD, 12, Estilo.TEXTO_SUAVE), BorderLayout.NORTH);
        abajo.add(scrollDetalle, BorderLayout.CENTER);

        // ---------- ARMADO ----------
        setLayout(new BorderLayout(0, 12));
        add(arriba, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(abajo, BorderLayout.SOUTH);
    }

    private static String[] estadosConTodos() {
        String[] estados = AgendaService.estados();
        String[] opciones = new String[estados.length + 1];
        opciones[0] = "Todos";
        System.arraycopy(estados, 0, opciones, 1, estados.length);
        return opciones;
    }

    private void cargar() {
        cargar(true);
    }

    /** Vuelve a leer las citas del médico con los filtros elegidos y rellena la tabla. */
    private void cargar(boolean avisarError) {
        LocalDate hoy = LocalDate.now();
        LocalDate desde = null;
        LocalDate hasta = null;
        switch (cboPeriodo.getSelectedIndex()) {
            case 0 -> {                       // Hoy
                desde = hoy;
                hasta = hoy;
            }
            case 1 -> {                       // Próximos 7 días (hoy incluido)
                desde = hoy;
                hasta = hoy.plusDays(6);
            }
            case 2 -> {                       // Este mes
                desde = hoy.withDayOfMonth(1);
                hasta = hoy.withDayOfMonth(hoy.lengthOfMonth());
            }
            default -> {                      // Todas: sin límite
            }
        }
        String estado = cboEstado.getSelectedIndex() == 0 ? null : (String) cboEstado.getSelectedItem();

        int idSeleccionada = obtenerIdSeleccionado();
        try {
            filas = AgendaService.listar(desde, hasta, estado);
        } catch (ReglaNegocioException e) {
            filas = new ArrayList<>();
            lblResumen.setText(e.getMessage());
            modelo.setRowCount(0);
            mostrarDetalle();
            return;
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Error al cargar la agenda", e);
            if (avisarError) {
                JOptionPane.showMessageDialog(this, "No se pudo cargar la agenda:\n" + e.getMessage(),
                        "Error de base de datos", JOptionPane.ERROR_MESSAGE);
            } else {
                lblResumen.setText("No se pudo actualizar la agenda (revise la conexión con MySQL).");
            }
            return;
        }

        modelo.setRowCount(0);
        int[] porEstado = new int[AgendaService.estados().length];
        String[] nombres = AgendaService.estados();
        int filaASeleccionar = -1;
        for (int i = 0; i < filas.size(); i++) {
            FilaAgenda f = filas.get(i);
            modelo.addRow(new Object[]{f.fecha() == null ? "" : f.fecha().format(MOSTRAR), f.hora(),
                    f.paciente(), f.documento(), f.motivo(), f.estado()});
            for (int k = 0; k < nombres.length; k++) {
                if (nombres[k].equals(f.estado())) {
                    porEstado[k]++;
                }
            }
            if (f.idCita() == idSeleccionada) {
                filaASeleccionar = i;
            }
        }
        if (filaASeleccionar >= 0) {
            tblAgenda.setRowSelectionInterval(filaASeleccionar, filaASeleccionar);
        }
        lblResumen.setText("Total: " + filas.size() + "   ·   Pendientes: " + porEstado[0]
                + "   ·   Programadas: " + porEstado[1] + "   ·   En espera: " + porEstado[2]
                + "   ·   Atendidas: " + porEstado[3] + "   ·   Canceladas: " + porEstado[4]);
        mostrarDetalle();
    }

    private int obtenerIdSeleccionado() {
        int fila = tblAgenda == null ? -1 : tblAgenda.getSelectedRow();
        return fila >= 0 && fila < filas.size() ? filas.get(fila).idCita() : -1;
    }

    /** Muestra debajo de la tabla los datos de la cita seleccionada. */
    private void mostrarDetalle() {
        int fila = tblAgenda == null ? -1 : tblAgenda.getSelectedRow();
        if (fila < 0 || fila >= filas.size()) {
            txtDetalle.setText("Seleccione una cita de la tabla para ver su detalle.");
            return;
        }
        FilaAgenda f = filas.get(fila);
        StringBuilder sb = new StringBuilder();
        sb.append("Cita #").append(f.idCita()).append("  ·  ")
                .append(f.fecha() == null ? "" : f.fecha().format(MOSTRAR)).append(" ").append(f.hora()).append('\n');
        sb.append("Paciente: ").append(f.paciente()).append("  (Doc. ").append(f.documento()).append(")");
        if (!f.telefono().isEmpty()) {
            sb.append("  ·  Tel. ").append(f.telefono());
        }
        sb.append('\n');
        sb.append("Motivo: ").append(f.motivo().isEmpty() ? "-" : f.motivo()).append('\n');
        sb.append("Estado: ").append(f.estado());
        if (!f.diagnostico().isEmpty()) {
            sb.append("  ·  Diagnóstico: ").append(f.diagnostico());
        }
        txtDetalle.setText(sb.toString());
        txtDetalle.setCaretPosition(0);
    }

    /** Pinta el estado de la cita con un color suave. */
    private static final class RenderEstado extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                                                       boolean foco, int fila, int columna) {
            Component c = super.getTableCellRendererComponent(tabla, valor, seleccionada, foco, fila, columna);
            setFont(Estilo.fuente(Font.BOLD, 13));
            if (!seleccionada) {
                String estado = String.valueOf(valor);
                Color color = switch (estado) {
                    case "Atendida" -> new Color(22, 163, 74);
                    case "Cancelada" -> Estilo.ERROR;
                    case "Programada" -> new Color(37, 99, 235);
                    case "En Espera" -> new Color(217, 119, 6);
                    default -> Estilo.TEXTO_SUAVE;      // Pendiente
                };
                setForeground(color);
            }
            return c;
        }
    }
}