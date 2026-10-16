package com.mycompany.clinica.vista;

import com.mycompany.clinica.servicio.Permisos;
import com.mycompany.clinica.servicio.ReglaNegocioException;
import com.mycompany.clinica.servicio.ReporteService;
import com.mycompany.clinica.servicio.ReporteService.Reporte;
import com.mycompany.clinica.servicio.ReporteService.TipoReporte;
import com.mycompany.clinica.utilidades.ExportadorCsv;
import com.mycompany.clinica.utilidades.ModeloTabla;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * Reportes del sistema (solo Admin). Elige un reporte, un período y (según el reporte) un médico
 * o un estado; pulsa "Generar" y exporta lo que ves a CSV (se abre en Excel).
 * Todo el SQL y la verificación de permiso viven en ReporteService.
 */
public class FrmReportes extends JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(FrmReportes.class.getName());

    private static final DateTimeFormatter LEER = DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter MOSTRAR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final int P_TODO = 0, P_MES = 1, P_HOY = 2, P_7 = 3, P_30 = 4, P_ANIO = 5, P_PERSONALIZADO = 6;
    private static final String[] PERIODOS = {"Todo el historial", "Este mes", "Hoy", "Últimos 7 días",
            "Últimos 30 días", "Este año", "Personalizado"};

    private final TipoReporte[] tipos = TipoReporte.values();
    private List<ReporteService.OpcionMedico> medicos = new ArrayList<>();
    private Reporte actual;                      // el reporte que se está mostrando (y se exporta)
    private boolean silencio;                    // true mientras se ajustan filtros por código (no regenerar)

    private JComboBox<String> cboReporte, cboPeriodo, cboMedico, cboEstado;
    private JTextField txtDesde, txtHasta;
    private JButton btnGenerar, btnExportar;
    private JTable tblReporte;
    private JLabel lblPie;
    private final JLabel[] valoresResumen = new JLabel[6];

    public FrmReportes() {
        super("Reportes del sistema");
        initComponents();
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(1180, 720);
        setMinimumSize(new Dimension(960, 620));
        setLocationRelativeTo(null);

        if (!Permisos.puedeVerReportes()) {
            JOptionPane.showMessageDialog(this, "Solo el administrador puede ver los reportes.",
                    "Reportes", JOptionPane.WARNING_MESSAGE);
            btnGenerar.setEnabled(false);
            btnExportar.setEnabled(false);
            return;
        }
        cargarMedicos();
        aplicarPeriodo();
        actualizarFiltros();
        generar();
        // Los listeners se agregan al final para que la carga inicial no los dispare
        cboPeriodo.addActionListener(e -> {
            aplicarPeriodo();
            if (cboPeriodo.getSelectedIndex() != P_PERSONALIZADO) {   // Personalizado espera a "Generar"
                generar();
            }
        });
        cboReporte.addActionListener(e -> {
            actualizarFiltros();
            generar();
        });
        cboMedico.addActionListener(e -> {
            if (!silencio) {
                generar();
            }
        });
        cboEstado.addActionListener(e -> {
            if (!silencio) {
                generar();
            }
        });
    }

    // =====================================================================
    //  ARMADO DE LA VENTANA
    // =====================================================================
    private void initComponents() {
        // ---------- CABECERA ----------
        JPanel titulos = new JPanel(new GridLayout(2, 1, 0, 3));
        titulos.setOpaque(false);
        titulos.add(Estilo.texto("Reportes del sistema", Font.BOLD, 24, Color.WHITE));
        titulos.add(Estilo.texto("Consulta y exporta la información de la clínica", Font.PLAIN, 13, Estilo.TEXTO_CLARO));

        JButton btnVolver = Estilo.botonClaro("Volver al menú");
        btnVolver.addActionListener(e -> dispose());
        JPanel envoltura = new JPanel(new GridBagLayout());
        envoltura.setOpaque(false);
        envoltura.add(btnVolver);

        JPanel cabecera = Estilo.panelDegradado(Estilo.PRIMARIO_OSCURO, Estilo.AZUL, 0);
        cabecera.setLayout(new BorderLayout(14, 0));
        cabecera.setBorder(BorderFactory.createEmptyBorder(18, 30, 18, 30));
        cabecera.add(Estilo.logo(52, Color.WHITE, Estilo.PRIMARIO_OSCURO), BorderLayout.WEST);
        cabecera.add(titulos, BorderLayout.CENTER);
        cabecera.add(envoltura, BorderLayout.EAST);

        // ---------- TARJETAS DE RESUMEN ----------
        String[] nombresResumen = {"Pacientes", "Médicos", "Citas de hoy", "Por confirmar", "Atenciones", "Recetas"};
        JPanel tarjetas = new JPanel(new GridLayout(1, 6, 12, 0));
        tarjetas.setOpaque(false);
        for (int i = 0; i < nombresResumen.length; i++) {
            valoresResumen[i] = Estilo.texto("-", Font.BOLD, 22, Estilo.PRIMARIO_OSCURO);
            JPanel t = new JPanel(new GridLayout(2, 1, 0, 0));
            t.setBackground(Color.WHITE);
            t.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Estilo.BORDE, 1),
                    BorderFactory.createEmptyBorder(8, 14, 8, 14)));
            t.add(valoresResumen[i]);
            t.add(Estilo.texto(nombresResumen[i], Font.PLAIN, 12, Estilo.TEXTO_SUAVE));
            tarjetas.add(t);
        }

        // ---------- FILTROS ----------
        String[] titulosReportes = new String[tipos.length];
        for (int i = 0; i < tipos.length; i++) {
            titulosReportes[i] = tipos[i].getTitulo();
        }
        cboReporte = Estilo.combo(titulosReportes);
        cboPeriodo = Estilo.combo(PERIODOS);
        txtDesde = Estilo.campoTexto();
        txtHasta = Estilo.campoTexto();
        cboMedico = Estilo.combo(new String[]{"Todos"});
        String[] estados = ReporteService.estados();
        String[] opcionesEstado = new String[estados.length + 1];
        opcionesEstado[0] = "Todos";
        System.arraycopy(estados, 0, opcionesEstado, 1, estados.length);
        cboEstado = Estilo.combo(opcionesEstado);

        btnGenerar = Estilo.botonPrimario("Generar");
        btnGenerar.addActionListener(e -> generar());
        txtDesde.addActionListener(e -> generar());
        txtHasta.addActionListener(e -> generar());
        btnExportar = Estilo.botonSecundario("Exportar CSV");
        btnExportar.addActionListener(e -> exportar());
        btnExportar.setEnabled(false);

        JPanel filtros = new JPanel(new GridLayout(2, 4, 14, 10));
        filtros.setBackground(Color.WHITE);
        filtros.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BORDE, 1),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        filtros.add(Estilo.campo("Reporte", cboReporte));
        filtros.add(Estilo.campo("Período", cboPeriodo));
        filtros.add(Estilo.campo("Desde (dd/mm/aaaa)", txtDesde));
        filtros.add(Estilo.campo("Hasta (dd/mm/aaaa)", txtHasta));
        filtros.add(Estilo.campo("Médico", cboMedico));
        filtros.add(Estilo.campo("Estado de la cita", cboEstado));
        filtros.add(conBoton(btnGenerar));
        filtros.add(conBoton(btnExportar));

        JPanel arriba = new JPanel(new BorderLayout(0, 12));
        arriba.setOpaque(false);
        arriba.add(tarjetas, BorderLayout.NORTH);
        arriba.add(filtros, BorderLayout.CENTER);

        // ---------- TABLA ----------
        tblReporte = new JTable(new ModeloTabla()) {
            @Override
            public String getToolTipText(MouseEvent e) {
                // Si el texto no cabe en la columna, al pasar el mouse se muestra completo
                int fila = rowAtPoint(e.getPoint());
                int col = columnAtPoint(e.getPoint());
                if (fila < 0 || col < 0 || getValueAt(fila, col) == null) {
                    return null;
                }
                String texto = getValueAt(fila, col).toString();
                boolean cortado = getFontMetrics(getFont()).stringWidth(texto) + 16 > getCellRect(fila, col, false).width;
                return cortado ? texto : null;
            }
        };
        JScrollPane scroll = new JScrollPane(tblReporte);
        Estilo.tabla(tblReporte, scroll);
        tblReporte.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblReporte.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tblReporte.getTableHeader().setReorderingAllowed(false);
        tblReporte.setDefaultRenderer(Object.class, new Renderizador());

        // ---------- PIE ----------
        lblPie = Estilo.texto(" ", Font.PLAIN, 12, Estilo.TEXTO_SUAVE);

        // ---------- ARMADO ----------
        JPanel cuerpo = new JPanel(new BorderLayout(0, 12));
        cuerpo.setBackground(Estilo.FONDO);
        cuerpo.setBorder(BorderFactory.createEmptyBorder(16, 30, 14, 30));
        cuerpo.add(arriba, BorderLayout.NORTH);
        cuerpo.add(scroll, BorderLayout.CENTER);
        cuerpo.add(lblPie, BorderLayout.SOUTH);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(cabecera, BorderLayout.NORTH);
        raiz.add(cuerpo, BorderLayout.CENTER);
        setContentPane(raiz);
    }

    /** Alinea un botón con los campos (deja el espacio de la etiqueta vacío). */
    private JPanel conBoton(JButton boton) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.add(Estilo.texto(" ", Font.BOLD, 12, Estilo.TEXTO_SUAVE), BorderLayout.NORTH);
        p.add(boton, BorderLayout.CENTER);
        return p;
    }

    // =====================================================================
    //  FILTROS
    // =====================================================================

    private void cargarMedicos() {
        try {
            medicos = ReporteService.listarMedicos();
            cboMedico.removeAllItems();
            cboMedico.addItem("Todos");
            for (ReporteService.OpcionMedico m : medicos) {
                cboMedico.addItem(m.etiqueta());
            }
        } catch (ReglaNegocioException | SQLException e) {
            logger.log(java.util.logging.Level.WARNING, "No se pudieron cargar los médicos", e);
        }
    }

    /** Habilita solo los filtros que usa el reporte elegido. */
    private void actualizarFiltros() {
        TipoReporte tipo = tipos[cboReporte.getSelectedIndex()];
        silencio = true;
        try {
            cboMedico.setEnabled(tipo.usaMedico());
            if (!tipo.usaMedico()) {
                cboMedico.setSelectedIndex(0);
            }
            cboEstado.setEnabled(tipo.usaEstado());
            if (!tipo.usaEstado()) {
                cboEstado.setSelectedIndex(0);
            }
        } finally {
            silencio = false;
        }
    }

    /** Al cambiar el período rellena Desde/Hasta; solo "Personalizado" deja escribirlos. */
    private void aplicarPeriodo() {
        int p = cboPeriodo.getSelectedIndex();
        boolean libre = p == P_PERSONALIZADO;
        txtDesde.setEnabled(libre);
        txtHasta.setEnabled(libre);
        if (libre) {
            return;
        }
        LocalDate[] r = rangoPredefinido(p);
        txtDesde.setText(r[0] == null ? "" : r[0].format(MOSTRAR));
        txtHasta.setText(r[1] == null ? "" : r[1].format(MOSTRAR));
    }

    private LocalDate[] rangoPredefinido(int periodo) {
        LocalDate hoy = LocalDate.now();
        switch (periodo) {
            case P_MES:
                return new LocalDate[]{hoy.withDayOfMonth(1), hoy.withDayOfMonth(hoy.lengthOfMonth())};
            case P_HOY:
                return new LocalDate[]{hoy, hoy};
            case P_7:
                return new LocalDate[]{hoy.minusDays(6), hoy};
            case P_30:
                return new LocalDate[]{hoy.minusDays(29), hoy};
            case P_ANIO:
                return new LocalDate[]{hoy.withDayOfYear(1), hoy.withDayOfYear(hoy.lengthOfYear())};
            default:
                return new LocalDate[]{null, null};
        }
    }

    private ReporteService.Filtro leerFiltro() throws ReglaNegocioException {
        LocalDate desde = leerFecha(txtDesde.getText(), "Desde");
        LocalDate hasta = leerFecha(txtHasta.getText(), "Hasta");
        TipoReporte tipo = tipos[cboReporte.getSelectedIndex()];
        int idMedico = 0;
        int i = cboMedico.getSelectedIndex();
        if (tipo.usaMedico() && i > 0 && i - 1 < medicos.size()) {
            idMedico = medicos.get(i - 1).id();
        }
        String estado = tipo.usaEstado() && cboEstado.getSelectedIndex() > 0
                ? (String) cboEstado.getSelectedItem() : null;
        return new ReporteService.Filtro(desde, hasta, idMedico, estado);
    }

    private LocalDate leerFecha(String texto, String nombre) throws ReglaNegocioException {
        String t = texto == null ? "" : texto.trim();
        if (t.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(t, LEER);
        } catch (DateTimeParseException e) {
            throw new ReglaNegocioException("La fecha '" + nombre + "' no es válida. Use el formato dd/mm/aaaa, por ejemplo "
                    + LocalDate.now().format(MOSTRAR) + ".");
        }
    }

    // =====================================================================
    //  GENERAR Y MOSTRAR
    // =====================================================================

    private void generar() {
        ReporteService.Filtro filtro;
        try {
            filtro = leerFiltro();
        } catch (ReglaNegocioException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Reportes", JOptionPane.WARNING_MESSAGE);
            return;
        }
        TipoReporte tipo = tipos[cboReporte.getSelectedIndex()];
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            mostrar(ReporteService.generar(tipo, filtro));
            cargarResumen();
        } catch (ReglaNegocioException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Reportes", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Error al generar el reporte", e);
            JOptionPane.showMessageDialog(this, "No se pudo generar el reporte:\n" + e.getMessage(),
                    "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private void cargarResumen() {
        try {
            ReporteService.Resumen r = ReporteService.resumen();
            int[] v = {r.pacientes(), r.medicos(), r.citasHoy(), r.citasPorConfirmar(), r.atenciones(), r.recetas()};
            for (int i = 0; i < v.length; i++) {
                valoresResumen[i].setText(String.valueOf(v[i]));
            }
        } catch (ReglaNegocioException | SQLException e) {
            logger.log(java.util.logging.Level.WARNING, "No se pudo cargar el resumen", e);
        }
    }

    private void mostrar(Reporte r) {
        actual = r;
        ModeloTabla modelo = new ModeloTabla(r.columnas().toArray(new String[0]));
        boolean hayDatos = r.registros() > 0;
        if (hayDatos) {
            for (Object[] fila : r.filas()) {
                modelo.addRow(fila);
            }
        }
        tblReporte.setModel(modelo);
        ajustarColumnas();
        btnExportar.setEnabled(hayDatos);
        if (hayDatos) {
            lblPie.setText(r.registros() + " registros  ·  " + r.tipo().getTitulo()
                    + "  ·  Las fechas filtran por la fecha de la cita.");
        } else {
            lblPie.setText("No hay datos para los filtros seleccionados.");
        }
    }

    /** Ancho de cada columna según el contenido (máximo 360 px); JTable reparte el resto. */
    private void ajustarColumnas() {
        FontMetrics fm = tblReporte.getFontMetrics(tblReporte.getFont());
        FontMetrics fmCab = tblReporte.getFontMetrics(Estilo.fuente(Font.BOLD, 13));
        int filas = Math.min(tblReporte.getRowCount(), 200);
        for (int c = 0; c < tblReporte.getColumnCount(); c++) {
            TableColumn col = tblReporte.getColumnModel().getColumn(c);
            int ancho = fmCab.stringWidth(String.valueOf(col.getHeaderValue())) + 28;
            for (int f = 0; f < filas; f++) {
                Object v = tblReporte.getValueAt(f, c);
                if (v != null) {
                    ancho = Math.max(ancho, fm.stringWidth(v.toString()) + 28);
                }
            }
            col.setPreferredWidth(Math.min(ancho, 360));
        }
    }

    // =====================================================================
    //  EXPORTAR
    // =====================================================================

    private void exportar() {
        if (actual == null || actual.registros() == 0) {
            return;
        }
        String sugerido = "reporte_" + actual.tipo().name().toLowerCase() + "_"
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar reporte como CSV");
        selector.setFileFilter(new FileNameExtensionFilter("Archivo CSV (*.csv)", "csv"));
        selector.setSelectedFile(new File(sugerido));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File archivo = selector.getSelectedFile();
        if (!archivo.getName().toLowerCase().endsWith(".csv")) {
            archivo = new File(archivo.getParentFile(), archivo.getName() + ".csv");
        }
        if (archivo.exists()) {
            int r = JOptionPane.showConfirmDialog(this, "El archivo ya existe. ¿Desea reemplazarlo?",
                    "Exportar", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (r != JOptionPane.YES_OPTION) {
                return;
            }
        }
        try {
            ExportadorCsv.exportar(archivo, actual.columnas(), actual.filas());
            JOptionPane.showMessageDialog(this, "Reporte guardado en:\n" + archivo.getAbsolutePath(),
                    "Exportar", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            logger.log(java.util.logging.Level.SEVERE, "Error al exportar", e);
            JOptionPane.showMessageDialog(this, "No se pudo guardar el archivo:\n" + e.getMessage()
                            + "\n(Si está abierto en Excel, ciérrelo e intente de nuevo.)",
                    "Exportar", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =====================================================================
    //  APARIENCIA DE LAS CELDAS
    // =====================================================================

    /** Números a la derecha, filas alternadas y la fila de total en negrita. */
    private class Renderizador extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            boolean total = actual != null && actual.conTotal() && row == table.getRowCount() - 1;
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            setHorizontalAlignment(value instanceof Number ? SwingConstants.RIGHT : SwingConstants.LEFT);
            setFont(Estilo.fuente(total ? Font.BOLD : Font.PLAIN, 13));
            if (!isSelected) {
                setForeground(Estilo.TEXTO);
                setBackground(total ? new Color(226, 232, 240)
                        : (row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252)));
            }
            return this;
        }
    }
}