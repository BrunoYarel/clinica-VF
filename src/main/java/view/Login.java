package view;

import java.awt.*;
import java.sql.SQLException;
import javax.swing.*;
import service.UsuarioService;

/**
 * Pantalla de inicio de sesión. Es el punto de entrada de la aplicación.
 *
 * Diseño: a la izquierda la marca de la clínica (degradado) y a la derecha el formulario.
 * Los colores y componentes salen de la clase Estilo.
 */
public class Login extends JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(Login.class.getName());

    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private JLabel lblError;
    private JButton btnEntrar;
    private JButton btnNuevoUsuario;

    public Login() {
        super("Clínica - Inicio de sesión");
        initComponents();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        // ---------- LADO IZQUIERDO: marca de la clínica ----------
        JPanel panelMarca = Estilo.panelDegradado(Estilo.PRIMARIO_OSCURO, Estilo.AZUL, 0);
        panelMarca.setLayout(new GridBagLayout());          // centra su contenido
        panelMarca.setPreferredSize(new Dimension(320, 520));

        JPanel marca = new JPanel(new GridBagLayout());
        marca.setOpaque(false);

        JPanel filaLogo = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        filaLogo.setOpaque(false);
        filaLogo.add(Estilo.logo(84, Color.WHITE, Estilo.PRIMARIO_OSCURO));

        JLabel lblNombre = Estilo.texto("Clínica", Font.BOLD, 34, Color.WHITE);
        lblNombre.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblLema = Estilo.texto("<html><center>Gestión de pacientes,<br>citas y atenciones médicas</center></html>",
                Font.PLAIN, 14, Estilo.TEXTO_CLARO);
        lblLema.setHorizontalAlignment(SwingConstants.CENTER);

        Estilo.agregar(marca, filaLogo, 0, 0);
        Estilo.agregar(marca, lblNombre, 1, 18);
        Estilo.agregar(marca, lblLema, 2, 8);
        panelMarca.add(marca);

        // ---------- LADO DERECHO: formulario ----------
        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setBackground(Color.WHITE);
        panelForm.setBorder(BorderFactory.createEmptyBorder(30, 45, 30, 45));

        Estilo.agregar(panelForm, Estilo.texto("¡Bienvenido!", Font.BOLD, 28, Estilo.TEXTO), 0, 0);
        Estilo.agregar(panelForm, Estilo.texto("Ingresa tus credenciales para continuar",
                Font.PLAIN, 13, Estilo.TEXTO_SUAVE), 1, 4);

        txtUsuario = Estilo.campoTexto();
        Estilo.agregar(panelForm, Estilo.campo("Usuario", txtUsuario), 2, 28);

        txtPassword = Estilo.campoPassword();
        Estilo.agregar(panelForm, Estilo.campo("Contraseña", txtPassword), 3, 14);

        Estilo.agregar(panelForm, Estilo.verPassword(txtPassword), 4, 8);

        lblError = Estilo.textoError();
        Estilo.agregar(panelForm, lblError, 5, 6);

        btnEntrar = Estilo.botonPrimario("Ingresar");
        btnEntrar.addActionListener(e -> entrar());
        Estilo.agregar(panelForm, btnEntrar, 6, 8);

        JLabel lblSinPerfil = Estilo.texto("¿Aún no tienes un perfil?", Font.PLAIN, 13, Estilo.TEXTO_SUAVE);
        lblSinPerfil.setHorizontalAlignment(SwingConstants.CENTER);
        Estilo.agregar(panelForm, lblSinPerfil, 7, 18);

        btnNuevoUsuario = Estilo.botonSecundario("Crear nuevo perfil");
        btnNuevoUsuario.addActionListener(e -> abrirRegistro());
        Estilo.agregar(panelForm, btnNuevoUsuario, 8, 8);

        // ---------- ARMADO DE LA VENTANA ----------
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(panelMarca, BorderLayout.WEST);
        raiz.add(panelForm, BorderLayout.CENTER);
        setContentPane(raiz);

        getRootPane().setDefaultButton(btnEntrar); // Enter = Ingresar
        setResizable(false);
        pack();
    }

    /** BOTÓN INGRESAR: valida los datos contra la base de datos y abre el menú principal. */
    private void entrar() {
        
        lblError.setText(" ");
        String usuario = txtUsuario.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (usuario.isEmpty() || password.isEmpty()) {
            lblError.setText("Complete todos los campos.");
            return;
        }

        try {
            if (UsuarioService.autenticar(usuario, password)) {
                new MenuPrincipal().setVisible(true);
                dispose();
            } else {
                lblError.setText("Usuario o contraseña incorrectos.");
                txtPassword.setText("");
                txtPassword.requestFocus();
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Error en el proceso de login", e);
            JOptionPane.showMessageDialog(this, "No se pudo iniciar sesión:\n" + e.getMessage(),
                    "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** BOTÓN CREAR NUEVO PERFIL: abre la ventana de registro. */
    private void abrirRegistro() {
        new NuevoUsuario().setVisible(true);
        dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Login().setVisible(true));
    }
}