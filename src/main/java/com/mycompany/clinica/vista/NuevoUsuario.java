package com.mycompany.clinica.vista;

import com.mycompany.clinica.dao.EspecialidadDAO;
import com.mycompany.clinica.modelo.Especialidad;
import com.mycompany.clinica.servicio.Permisos;
import com.mycompany.clinica.servicio.ReglaNegocioException;
import com.mycompany.clinica.servicio.Sesion;
import com.mycompany.clinica.servicio.UsuarioService;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * Registro de nuevos usuarios (crear perfil). Para crear uno hay que escribir
 * la contraseña de un administrador, que se verifica contra la base de datos.
 *
 * @author PC-03
 */
public class NuevoUsuario extends JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(NuevoUsuario.class.getName());

    // Opciones del combo y su valor exacto en el ENUM de la base de datos (mismo orden)
    private static final String[] ROLES_MOSTRADOS = {"Seleccione un rol...", "Administrador", "Recepcionista", "Médico"};
    private static final String[] ROLES_BD = {"", Sesion.ADMIN, Sesion.RECEPCIONISTA, Sesion.MEDICO};

    private static final String OPCION_MEDICO_NUEVO = "Registrar un médico nuevo";

    private JTextField txtNombre;
    private JComboBox<String> cbxRol;
    // Bloque "Médico" (solo con rol Médico): elegir un médico existente sin usuario, o registrar uno nuevo
    private JComboBox<String> cbxMedico;
    private JPanel panelMedico;
    private JPanel panelDatosMedico;
    private JTextField txtMedNombres;
    private JTextField txtMedApellidos;
    private JTextField txtMedCmp;
    private JTextField txtMedTelefono;
    private JTextField txtMedEmail;
    private JComboBox<String> cbxEspecialidad;
    // id_medico de cada médico del combo (índice del combo - 1, por la opción "Registrar un médico nuevo")
    private final List<Integer> idsMedicos = new ArrayList<>();
    // especialidades del combo, en el mismo orden
    private List<Especialidad> especialidades = new ArrayList<>();
    private JPasswordField txtPasswordUsuario;
    private JPasswordField txtPasswordConfirmar;
    private JPasswordField txtPasswordAdmin;
    private JLabel lblError;
    private JButton btnCrear;
    private JButton btnLimpiar;
    private JButton btnVolver;

    public NuevoUsuario() {
        super("Registro de Usuarios");
        initComponents();
        // Al cerrar con la X también se vuelve a la pantalla anterior (no se cierra todo el programa)
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                volver();
            }
        });
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        // ---------- CABECERA ----------
        JPanel titulos = new JPanel(new GridLayout(2, 1, 0, 3));
        titulos.setOpaque(false);
        titulos.add(Estilo.texto("Crear nuevo perfil", Font.BOLD, 24, Color.WHITE));
        titulos.add(Estilo.texto("Registra a un usuario del sistema y asígnale un rol",
                Font.PLAIN, 13, Estilo.TEXTO_CLARO));

        // Si un admin ya inició sesión vuelve al menú; si no, al login
        btnVolver = Estilo.botonClaro(Sesion.estaActiva() ? "Volver al menú" : "Volver al login");
        btnVolver.addActionListener(e -> volver());
        JPanel envoltura = new JPanel(new GridBagLayout());   // centra el botón sin estirarlo
        envoltura.setOpaque(false);
        envoltura.add(btnVolver);

        JPanel cabecera = Estilo.panelDegradado(Estilo.PRIMARIO_OSCURO, Estilo.AZUL, 0);
        cabecera.setLayout(new BorderLayout(14, 0));
        cabecera.setBorder(BorderFactory.createEmptyBorder(22, 30, 22, 30));
        cabecera.add(Estilo.logo(52, Color.WHITE, Estilo.PRIMARIO_OSCURO), BorderLayout.WEST);
        cabecera.add(titulos, BorderLayout.CENTER);
        cabecera.add(envoltura, BorderLayout.EAST);

        // ---------- FORMULARIO ----------
        JPanel cuerpo = new JPanel(new GridBagLayout());
        cuerpo.setBackground(Color.WHITE);
        cuerpo.setBorder(BorderFactory.createEmptyBorder(26, 30, 26, 30));

        txtNombre = Estilo.campoTexto();
        cbxRol = Estilo.combo(ROLES_MOSTRADOS);
        cbxRol.addActionListener(e -> cambiarRol());

        // Bloque "Médico": oculto hasta que se elige el rol Médico (va dentro de la fila 0, así no se renumeran las demás)
        cbxMedico = Estilo.combo(new String[]{OPCION_MEDICO_NUEVO});
        cbxMedico.addActionListener(e -> cambiarMedico());
        txtMedNombres = Estilo.campoTexto();
        txtMedApellidos = Estilo.campoTexto();
        txtMedCmp = Estilo.campoTexto();
        txtMedTelefono = Estilo.campoTexto();
        txtMedEmail = Estilo.campoTexto();
        cbxEspecialidad = Estilo.combo(new String[0]);

        panelDatosMedico = new JPanel(new GridLayout(3, 2, 16, 14));
        panelDatosMedico.setOpaque(false);
        panelDatosMedico.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        panelDatosMedico.add(Estilo.campo("Nombres del médico", txtMedNombres));
        panelDatosMedico.add(Estilo.campo("Apellidos del médico", txtMedApellidos));
        panelDatosMedico.add(Estilo.campo("CMP", txtMedCmp));
        panelDatosMedico.add(Estilo.campo("Especialidad", cbxEspecialidad));
        panelDatosMedico.add(Estilo.campo("Teléfono (opcional)", txtMedTelefono));
        panelDatosMedico.add(Estilo.campo("Email (opcional)", txtMedEmail));

        panelMedico = new JPanel(new BorderLayout());
        panelMedico.setOpaque(false);
        panelMedico.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        panelMedico.add(Estilo.campo("Médico", cbxMedico), BorderLayout.NORTH);
        panelMedico.add(panelDatosMedico, BorderLayout.CENTER);
        panelMedico.setVisible(false);

        JPanel filaRol = new JPanel(new BorderLayout());
        filaRol.setOpaque(false);
        filaRol.add(dosColumnas(
                Estilo.campo("Nombre de usuario", txtNombre),
                Estilo.campo("Rol", cbxRol)), BorderLayout.CENTER);
        filaRol.add(panelMedico, BorderLayout.SOUTH);
        Estilo.agregar(cuerpo, filaRol, 0, 0);

        txtPasswordUsuario = Estilo.campoPassword();
        txtPasswordConfirmar = Estilo.campoPassword();
        Estilo.agregar(cuerpo, dosColumnas(
                Estilo.campo("Contraseña", txtPasswordUsuario),
                Estilo.campo("Confirmar contraseña", txtPasswordConfirmar)), 1, 16);

        // Sección de autorización del administrador
        Estilo.agregar(cuerpo, Estilo.linea(), 2, 22);
        Estilo.agregar(cuerpo, Estilo.texto("Autorización del administrador", Font.BOLD, 15, Estilo.TEXTO), 3, 18);
        Estilo.agregar(cuerpo, Estilo.texto("Para crear un perfil se necesita la contraseña de un administrador.",
                Font.PLAIN, 12, Estilo.TEXTO_SUAVE), 4, 2);

        txtPasswordAdmin = Estilo.campoPassword();
        Estilo.agregar(cuerpo, Estilo.campo("Contraseña de administrador", txtPasswordAdmin), 5, 12);

        Estilo.agregar(cuerpo, Estilo.verPassword(txtPasswordUsuario, txtPasswordConfirmar, txtPasswordAdmin), 6, 10);

        lblError = Estilo.textoError();
        Estilo.agregar(cuerpo, lblError, 7, 6);

        // ---------- BOTONES ----------
        btnLimpiar = Estilo.botonSecundario("Limpiar");
        btnLimpiar.addActionListener(e -> limpiar());

        btnCrear = Estilo.botonPrimario("Crear perfil");
        btnCrear.addActionListener(e -> crear());

        JPanel botones = new JPanel(new GridLayout(1, 2, 14, 0));
        botones.setOpaque(false);
        botones.add(btnLimpiar);
        botones.add(btnCrear);
        Estilo.agregar(cuerpo, botones, 8, 8);

        // ---------- ARMADO ----------
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(cabecera, BorderLayout.NORTH);
        raiz.add(cuerpo, BorderLayout.CENTER);
        setContentPane(raiz);

        getRootPane().setDefaultButton(btnCrear);
        setResizable(false);
        pack();
    }

    /** Pone dos componentes lado a lado, cada uno con la mitad del ancho. */
    private JPanel dosColumnas(Component izquierda, Component derecha) {
        JPanel fila = new JPanel(new GridLayout(1, 2, 16, 0));
        fila.setOpaque(false);
        fila.add(izquierda);
        fila.add(derecha);
        return fila;
    }

    /** true si el rol elegido en el combo es Médico. */
    private boolean esRolMedico() {
        int indice = cbxRol.getSelectedIndex();
        return indice >= 0 && Sesion.MEDICO.equals(ROLES_BD[indice]);
    }

    /** true si en el combo de médico está elegida la opción "Registrar un médico nuevo". */
    private boolean esMedicoNuevo() {
        return cbxMedico.getSelectedIndex() <= 0;
    }

    /** Al cambiar el rol: con Médico se muestra el bloque de médico; con otro rol se oculta. */
    private void cambiarRol() {
        lblError.setText(" ");
        if (esRolMedico()) {
            cargarEspecialidades();
            cargarMedicosSinUsuario();
            panelMedico.setVisible(true);
            panelDatosMedico.setVisible(esMedicoNuevo());
            pack();
        } else if (panelMedico.isVisible()) {
            panelMedico.setVisible(false);
            pack();
        }
    }

    /** Al cambiar el médico: los datos del médico solo se piden cuando se va a registrar uno nuevo. */
    private void cambiarMedico() {
        lblError.setText(" ");
        panelDatosMedico.setVisible(esMedicoNuevo());
        pack();
    }

    /** Llena el combo de especialidades (las mismas que usa el módulo Médicos). */
    private void cargarEspecialidades() {
        String[] nombres = new String[0];
        try {
            especialidades = new EspecialidadDAO().listarEspecialidades();
            nombres = new String[especialidades.size()];
            for (int i = 0; i < nombres.length; i++) {
                nombres[i] = especialidades.get(i).getNombre();
            }
            if (especialidades.isEmpty()) {
                lblError.setText("No hay especialidades registradas. Créelas primero en el módulo Especialidades.");
            }
        } catch (SQLException e) {
            especialidades = new ArrayList<>();
            logger.log(java.util.logging.Level.SEVERE, "Error al cargar las especialidades", e);
            lblError.setText("No se pudieron cargar las especialidades: " + e.getMessage());
        }
        cbxEspecialidad.setModel(new DefaultComboBoxModel<>(nombres));
    }

    /** Llena el combo de médico: "Registrar un médico nuevo" + los médicos que aún no tienen usuario. */
    private void cargarMedicosSinUsuario() {
        List<String> etiquetas = new ArrayList<>();
        etiquetas.add(OPCION_MEDICO_NUEVO);
        idsMedicos.clear();
        try {
            for (UsuarioService.MedicoSinUsuario m : UsuarioService.listarMedicosSinUsuario()) {
                etiquetas.add(m.etiqueta());
                idsMedicos.add(m.id());
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Error al cargar los médicos", e);
            lblError.setText("No se pudieron cargar los médicos: " + e.getMessage());
        }
        cbxMedico.setModel(new DefaultComboBoxModel<>(etiquetas.toArray(new String[0])));
    }

    /** BOTÓN LIMPIAR: solo vacía los campos (no usa la base de datos). */
    private void limpiar() {
        txtNombre.setText("");
        txtPasswordUsuario.setText("");
        txtPasswordConfirmar.setText("");
        txtPasswordAdmin.setText("");
        txtMedNombres.setText("");
        txtMedApellidos.setText("");
        txtMedCmp.setText("");
        txtMedTelefono.setText("");
        txtMedEmail.setText("");
        cbxRol.setSelectedIndex(0);   // al volver a "Seleccione un rol..." se oculta el bloque de médico
        lblError.setText(" ");
        txtNombre.requestFocus();
    }

    /** BOTÓN CREAR PERFIL: valida los datos, verifica al admin y guarda el usuario en la base de datos. */
    private void crear() {
        if (!Permisos.puedeGestionarUsuarios()) {
            lblError.setText("No tiene permiso para crear usuarios.");
            return;
        }
        lblError.setText(" ");
        String nombre = txtNombre.getText().trim();
        String passUsuario = new String(txtPasswordUsuario.getPassword());
        String passConfirmar = new String(txtPasswordConfirmar.getPassword());
        String passAdmin = new String(txtPasswordAdmin.getPassword());
        int indiceRol = cbxRol.getSelectedIndex();

        // 1. Validaciones (antes de tocar la base de datos)
        if (nombre.isEmpty() || passUsuario.isEmpty() || passConfirmar.isEmpty()
                || passAdmin.isEmpty() || indiceRol == 0) {
            lblError.setText("Por favor, complete todos los campos requeridos.");
            return;
        }
        if (nombre.length() < UsuarioService.MIN_USERNAME || nombre.length() > UsuarioService.MAX_USERNAME) {
            lblError.setText("El nombre de usuario debe tener entre " + UsuarioService.MIN_USERNAME
                    + " y " + UsuarioService.MAX_USERNAME + " caracteres.");
            return;
        }
        if (passUsuario.length() < UsuarioService.MIN_PASSWORD) {
            lblError.setText("La contraseña debe tener al menos " + UsuarioService.MIN_PASSWORD + " caracteres.");
            return;
        }
        if (!passUsuario.equals(passConfirmar)) {
            lblError.setText("Las contraseñas no coinciden.");
            return;
        }

        // Con rol Médico el usuario siempre queda ligado a un médico: uno existente sin usuario o uno nuevo
        boolean medicoNuevo = false;
        int idMedico = 0;
        String medNombres = "";
        String medApellidos = "";
        String medCmp = "";
        String medTelefono = "";
        String medEmail = "";
        int idEspecialidad = 0;
        if (esRolMedico()) {
            if (esMedicoNuevo()) {
                medicoNuevo = true;
                medNombres = txtMedNombres.getText().trim();
                medApellidos = txtMedApellidos.getText().trim();
                medCmp = txtMedCmp.getText().trim();
                medTelefono = txtMedTelefono.getText().trim();
                medEmail = txtMedEmail.getText().trim();
                int indiceEsp = cbxEspecialidad.getSelectedIndex();
                if (medNombres.isEmpty() || medApellidos.isEmpty() || medCmp.isEmpty()
                        || indiceEsp < 0 || indiceEsp >= especialidades.size()) {
                    lblError.setText("Nombres, apellidos, CMP y especialidad del médico son obligatorios.");
                    return;
                }
                if (medNombres.length() > 100 || medApellidos.length() > 100 || medCmp.length() > 20
                        || medTelefono.length() > 20 || medEmail.length() > 150) {
                    lblError.setText("Algún dato del médico es demasiado largo (CMP y teléfono: máx. 20 caracteres).");
                    return;
                }
                idEspecialidad = especialidades.get(indiceEsp).getIdEspecialidad();
            } else {
                int indiceMedico = cbxMedico.getSelectedIndex();
                if (indiceMedico > idsMedicos.size()) {
                    lblError.setText("Seleccione el médico al que pertenece este usuario.");
                    return;
                }
                idMedico = idsMedicos.get(indiceMedico - 1);
            }
        }

        // 2. Base de datos: verificar al admin y guardar
        try {
            if (!UsuarioService.verificarPasswordAdmin(passAdmin)) {
                lblError.setText("La contraseña de administrador es incorrecta.");
                txtPasswordAdmin.setText("");
                txtPasswordAdmin.requestFocus();
                return;
            }

            boolean creado;
            if (medicoNuevo) {
                creado = UsuarioService.crearUsuarioConMedicoNuevo(nombre, passUsuario, medNombres, medApellidos,
                        medCmp, medTelefono, medEmail, idEspecialidad);
            } else if (idMedico > 0) {
                if (UsuarioService.medicoYaTieneUsuario(idMedico)) {
                    lblError.setText("Ese médico ya tiene un usuario asignado.");
                    return;
                }
                creado = UsuarioService.crearUsuario(nombre, passUsuario, ROLES_BD[indiceRol], idMedico);
            } else {
                creado = UsuarioService.crearUsuario(nombre, passUsuario, ROLES_BD[indiceRol]);
            }

            if (creado) {
                JOptionPane.showMessageDialog(this, "El usuario '" + nombre + "' ha sido registrado con éxito.",
                        "Registro exitoso", JOptionPane.INFORMATION_MESSAGE);
                limpiar();
                volver();
            } else {
                lblError.setText("No se pudo registrar el usuario. Intente nuevamente.");
            }

        } catch (ReglaNegocioException e) {
            lblError.setText(e.getMessage());
        } catch (SQLIntegrityConstraintViolationException e) {
            lblError.setText("El usuario '" + nombre + "' ya existe. Elija otro nombre.");
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Error al registrar usuario en la BD", e);
            JOptionPane.showMessageDialog(this, "Error de base de datos:\n" + e.getMessage(),
                    "Error SQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** BOTÓN VOLVER: regresa al menú (si hay sesión) o al login. */
    private void volver() {
        if (Sesion.estaActiva()) {
            new MenuPrincipal().setVisible(true);
        } else {
            new Login().setVisible(true);
        }
        dispose();
    }
}