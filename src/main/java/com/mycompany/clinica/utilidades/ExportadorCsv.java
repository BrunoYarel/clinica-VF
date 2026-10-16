package com.mycompany.clinica.utilidades;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Guarda una tabla en un archivo CSV que Excel abre bien:
 *   - UTF-8 con BOM (las tildes y la ñ se ven correctas).
 *   - Separador ";" y primera línea "sep=;" para que Excel separe las columnas
 *     sin importar la configuración regional del equipo.
 *   - Las celdas de texto que empiezan con = + - @ se neutralizan con un apóstrofo
 *     (evita que Excel ejecute una fórmula escrita en un motivo o diagnóstico).
 */
public final class ExportadorCsv {

    private static final char SEP = ';';

    private ExportadorCsv() {
    }

    public static void exportar(File archivo, List<String> columnas, List<Object[]> filas) throws IOException {
        try (BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(Files.newOutputStream(archivo.toPath()), StandardCharsets.UTF_8))) {
            w.write('\uFEFF');
            w.write("sep=" + SEP + "\r\n");
            escribirLinea(w, columnas.toArray());
            for (Object[] fila : filas) {
                escribirLinea(w, fila);
            }
        }
    }

    private static void escribirLinea(BufferedWriter w, Object[] celdas) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < celdas.length; i++) {
            if (i > 0) {
                sb.append(SEP);
            }
            sb.append(celda(celdas[i]));
        }
        sb.append("\r\n");
        w.write(sb.toString());
    }

    /** Convierte un valor en una celda CSV segura. */
    static String celda(Object valor) {
        if (valor == null) {
            return "";
        }
        String t = valor.toString();
        if (!(valor instanceof Number) && !t.isEmpty()) {
            char c = t.charAt(0);
            if (c == '=' || c == '+' || c == '-' || c == '@' || c == '\t' || c == '\r') {
                t = "'" + t;
            }
        }
        if (t.indexOf(SEP) >= 0 || t.indexOf('"') >= 0 || t.indexOf('\n') >= 0 || t.indexOf('\r') >= 0) {
            t = "\"" + t.replace("\"", "\"\"") + "\"";
        }
        return t;
    }
}