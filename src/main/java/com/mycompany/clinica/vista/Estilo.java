package com.mycompany.clinica.vista;

import com.mycompany.clinica.servicio.Sesion;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.border.Border;

/**
 * Aquí vive TODO el diseño de la aplicación: colores, fuentes y los componentes
 * ya "decorados" (botones, campos, paneles con degradado...).
 *
 * Así las ventanas (Login, MenuPrincipal, etc.) solo arman su estructura y
 * llaman a estos métodos, sin repetir código de diseño en cada archivo.
 *
 * Si quieres cambiar el color de TODA la app, solo toca los colores de abajo.
 */
public final class Estilo {

    // ======================= COLORES =======================
    public static final Color PRIMARIO = new Color(13, 148, 136);         // turquesa (color principal)
    public static final Color PRIMARIO_OSCURO = new Color(15, 118, 110);  // turquesa oscuro (al pasar el mouse)
    public static final Color AZUL = new Color(30, 58, 138);              // azul profundo (fin de los degradados)
    public static final Color AZUL_OSCURO = new Color(15, 23, 42);        // menú lateral
    public static final Color FONDO = new Color(241, 245, 249);           // fondo gris claro
    public static final Color TEXTO = new Color(30, 41, 59);              // texto principal
    public static final Color TEXTO_SUAVE = new Color(100, 116, 139);     // texto secundario
    public static final Color BORDE = new Color(203, 213, 225);           // bordes de los campos
    public static final Color ERROR = new Color(220, 38, 38);             // rojo para errores
    public static final Color TEXTO_CLARO = new Color(204, 251, 241);     // texto claro sobre degradados

    // Altura de todos los campos de texto y combos
    private static final int ALTO_CAMPO = 40;

    // Borde normal (gris) y borde con foco (turquesa). Mismo tamaño total para que nada "salte".
    private static final Border BORDE_NORMAL = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDE, 1),
            BorderFactory.createEmptyBorder(9, 12, 9, 12));
    private static final Border BORDE_FOCO = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PRIMARIO, 2),
            BorderFactory.createEmptyBorder(8, 11, 8, 11));

    // Se ejecuta una sola vez, cuando se usa Estilo por primera vez:
    // deja los cuadros de mensaje (JOptionPane) con la misma tipografía y fondo blanco.
    static {
        UIManager.put("OptionPane.messageFont", fuente(Font.PLAIN, 14));
        UIManager.put("OptionPane.buttonFont", fuente(Font.BOLD, 13));
        UIManager.put("OptionPane.messageForeground", TEXTO);
        UIManager.put("OptionPane.background", Color.WHITE);
        UIManager.put("Panel.background", Color.WHITE);
    }

    private Estilo() {
    }

    // ======================= FUENTES Y TEXTOS =======================

    /** Fuente de toda la app (si el equipo no tiene Segoe UI, Java usa otra parecida). */
    public static Font fuente(int estilo, int tamanio) {
        return new Font("Segoe UI", estilo, tamanio);
    }

    /** Etiqueta de texto lista: Estilo.texto("Hola", Font.BOLD, 20, Color.WHITE) */
    public static JLabel texto(String texto, int estilo, int tamanio, Color color) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(fuente(estilo, tamanio));
        lbl.setForeground(color);
        return lbl;
    }

    /** Etiqueta roja para mostrar errores dentro del formulario (empieza vacía). */
    public static JLabel textoError() {
        return texto(" ", Font.BOLD, 12, ERROR);
    }

    /** Convierte el valor de la base de datos ("Medico") en un texto bonito ("Médico"). */
    public static String nombreRol(String rol) {
        if (Sesion.ADMIN.equals(rol)) {
            return "Administrador";
        }
        if (Sesion.MEDICO.equals(rol)) {
            return "Médico";
        }
        return rol == null ? "" : rol;
    }

    // ======================= CAMPOS DE FORMULARIO =======================

    /** Campo de texto con borde suave que se pinta turquesa al hacer clic. */
    public static JTextField campoTexto() {
        JTextField campo = new JTextField(20);
        estilizar(campo);
        return campo;
    }

    /** Igual que campoTexto, pero para contraseñas (puntitos en vez de letras). */
    public static JPasswordField campoPassword() {
        JPasswordField campo = new JPasswordField(20);
        estilizar(campo);
        return campo;
    }

    /** Lista desplegable con la misma altura que los campos de texto. */
    public static JComboBox<String> combo(String[] opciones) {
        JComboBox<String> combo = new JComboBox<>(opciones);
        combo.setFont(fuente(Font.PLAIN, 14));
        combo.setBackground(Color.WHITE);
        combo.setForeground(TEXTO);
        combo.setPreferredSize(new Dimension(combo.getPreferredSize().width, ALTO_CAMPO));
        return combo;
    }

    /** Apariencia común de JTextField y JPasswordField (JPasswordField es un JTextField). */
    private static void estilizar(JTextField campo) {
        campo.setFont(fuente(Font.PLAIN, 14));
        campo.setForeground(TEXTO);
        campo.setBackground(Color.WHITE);
        campo.setCaretColor(PRIMARIO);
        campo.setSelectionColor(new Color(153, 246, 228));
        campo.setSelectedTextColor(TEXTO);
        campo.setBorder(BORDE_NORMAL);
        campo.setPreferredSize(new Dimension(campo.getPreferredSize().width, ALTO_CAMPO));

        // Cambia el borde según tenga o no el foco
        campo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                campo.setBorder(BORDE_FOCO);
            }

            @Override
            public void focusLost(FocusEvent e) {
                campo.setBorder(BORDE_NORMAL);
            }
        });
    }

    /** Pequeño bloque "etiqueta arriba + campo abajo". */
    public static JPanel campo(String etiqueta, JComponent componente) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);
        panel.add(texto(etiqueta, Font.BOLD, 12, TEXTO_SUAVE), BorderLayout.NORTH);
        panel.add(componente, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Casilla "Mostrar contraseña": muestra u oculta el texto de los campos que le pases.
     * Ejemplo: Estilo.verPassword(txtActual, txtNueva, txtConfirmar)
     */
    public static JCheckBox verPassword(JPasswordField... campos) {
        JCheckBox chk = new JCheckBox("Mostrar contraseña");
        chk.setFont(fuente(Font.PLAIN, 12));
        chk.setForeground(TEXTO_SUAVE);
        chk.setOpaque(false);
        chk.setFocusPainted(false);
        chk.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        char puntitos = campos[0].getEchoChar();   // el caracter con que se oculta la contraseña
        chk.addActionListener(e -> {
            for (JPasswordField campo : campos) {
                // (char) 0 significa "mostrar el texto real"
                campo.setEchoChar(chk.isSelected() ? (char) 0 : puntitos);
            }
        });
        return chk;
    }

    // ======================= ORDEN DEL FORMULARIO =======================

    /**
     * Agrega un componente en una fila de un panel con GridBagLayout.
     * Ocupa todo el ancho y deja 'espacioArriba' píxeles de separación con la fila anterior.
     */
    public static void agregar(JPanel panel, Component componente, int fila, int espacioArriba) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(espacioArriba, 0, 0, 0);
        panel.add(componente, gbc);
    }

    /** Línea fina horizontal para separar secciones. */
    public static JPanel linea() {
        JPanel linea = new JPanel();
        linea.setBackground(BORDE);
        linea.setPreferredSize(new Dimension(1, 1));
        return linea;
    }

    // ======================= BOTONES =======================

    /** Botón turquesa, para la acción principal (Entrar, Guardar, Crear). */
    public static JButton botonPrimario(String texto) {
        return new Boton(texto, PRIMARIO, PRIMARIO_OSCURO, Color.WHITE);
    }

    /** Botón gris claro, para acciones secundarias (Cancelar, Limpiar). */
    public static JButton botonSecundario(String texto) {
        return new Boton(texto, new Color(226, 232, 240), new Color(203, 213, 225), TEXTO);
    }

    /** Botón translúcido blanco, para ponerlo sobre un fondo de color o degradado. */
    public static JButton botonClaro(String texto) {
        return new Boton(texto, new Color(255, 255, 255, 40), new Color(255, 255, 255, 80), Color.WHITE);
    }

    /** Botón del menú lateral: transparente, texto a la izquierda y se ilumina al pasar el mouse. */
    public static JButton botonMenu(String texto) {
        JButton boton = new Boton(texto, new Color(0, 0, 0, 0), new Color(255, 255, 255, 28), new Color(226, 232, 240));
        boton.setFont(fuente(Font.PLAIN, 14));
        boton.setHorizontalAlignment(SwingConstants.LEFT);
        boton.setBorder(BorderFactory.createEmptyBorder(11, 16, 11, 16));
        return boton;
    }

    /**
     * Botón redondeado. Swing dibuja los botones con el estilo del sistema, así que aquí
     * dibujamos el fondo nosotros mismos (un rectángulo redondeado) y dejamos que
     * Swing escriba solo el texto encima.
     */
    private static class Boton extends JButton {

        private final Color colorNormal;
        private final Color colorHover;

        Boton(String texto, Color colorNormal, Color colorHover, Color colorTexto) {
            super(texto);
            this.colorNormal = colorNormal;
            this.colorHover = colorHover;
            setForeground(colorTexto);
            setFont(fuente(Font.BOLD, 14));
            setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
            setContentAreaFilled(false);   // no pintar el fondo gris por defecto
            setOpaque(false);
            setFocusPainted(false);
            setRolloverEnabled(true);      // para detectar cuándo el mouse está encima
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean encima = getModel().isRollover() || getModel().isPressed();
            g2.setColor(encima ? colorHover : colorNormal);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g2.dispose();
            super.paintComponent(g);       // escribe el texto del botón
        }
    }

    // ======================= PANELES Y FIGURAS =======================

    /**
     * Panel con fondo degradado (de color1 a color2) y dos círculos suaves de adorno.
     * 'radio' es lo redondeadas que van las esquinas (0 = esquinas rectas).
     * Se usa como cualquier JPanel: panel.setLayout(...) y panel.add(...).
     */
    public static JPanel panelDegradado(Color color1, Color color2, int radio) {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // 1) Fondo degradado
                g2.setPaint(new GradientPaint(0, 0, color1, w, h, color2));
                g2.fillRoundRect(0, 0, w, h, radio, radio);

                // 2) Círculos de adorno (blanco casi transparente), recortados a la forma del panel
                g2.setClip(new RoundRectangle2D.Float(0, 0, w, h, radio, radio));
                g2.setColor(new Color(255, 255, 255, 20));
                int d = Math.min(w, h);
                g2.fillOval(w - d * 2 / 3, -d / 3, d, d);
                g2.fillOval(-d / 3, h - d / 2, d, d);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        return panel;
    }

    /** Logo de la clínica: cuadrado redondeado con una cruz médica dibujada en el centro. */
    public static JComponent logo(int tamanio, Color fondo, Color cruz) {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int d = Math.min(getWidth(), getHeight());   // lado del cuadrado
                int x = (getWidth() - d) / 2;
                int y = (getHeight() - d) / 2;

                g2.setColor(fondo);
                g2.fillRoundRect(x, y, d, d, d / 3, d / 3);

                int largo = d / 2;     // largo de cada brazo de la cruz
                int grosor = d / 5;    // grosor de la cruz
                g2.setColor(cruz);
                g2.fillRoundRect(x + (d - largo) / 2, y + (d - grosor) / 2, largo, grosor, grosor / 3, grosor / 3);
                g2.fillRoundRect(x + (d - grosor) / 2, y + (d - largo) / 2, grosor, largo, grosor / 3, grosor / 3);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(tamanio, tamanio));
        return panel;
    }

    /** Círculo de color con una letra dentro (como la foto de perfil o el icono de un módulo). */
    public static JLabel icono(String letra, Color color, int tamanio) {
        JLabel lbl = new JLabel(letra, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int d = Math.min(getWidth(), getHeight());
                g2.setColor(color);
                g2.fillOval((getWidth() - d) / 2, (getHeight() - d) / 2, d, d);
                g2.dispose();
                super.paintComponent(g);   // escribe la letra
            }
        };
        lbl.setFont(fuente(Font.BOLD, tamanio * 2 / 5));
        lbl.setForeground(Color.WHITE);
        lbl.setPreferredSize(new Dimension(tamanio, tamanio));
        return lbl;
    }
}