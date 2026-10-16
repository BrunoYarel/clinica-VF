# Clínica Valmont — Sistema de gestión de clínica

Aplicación de escritorio para administrar una clínica: pacientes, médicos, citas, atenciones, recetas, historial clínico y reportes. Está hecha en **Java Swing** y guarda todo en **MySQL**. Cada usuario entra con un **rol** (Admin, Médico o Recepcionista) y solo ve y hace lo que ese rol permite.

Este documento da una visión general de la arquitectura. No describe cada pantalla: explica **cómo está organizado el código, cómo fluye una operación y cómo se conectan las piezas**.

---

## 1. Stack y requisitos

| Componente | Detalle |
|---|---|
| Lenguaje | Java 25 (`maven.compiler.release` = 25) |
| Interfaz | Swing (formularios `.form` de NetBeans + paneles escritos a mano) |
| Base de datos | MySQL / MariaDB (pensado para XAMPP) |
| Driver | `mysql-connector-j` 9.7.0 (única dependencia de Maven) |
| Build | Maven (`pom.xml`), clase principal: `com.mycompany.clinica.vista.Login` |
| Seguridad de claves | PBKDF2 con clases del JDK (sin librerías extra) |

### Puesta en marcha

1. Encender MySQL (XAMPP).
2. Ejecutar completo `src/main/java/com/mycompany/clinica/script.sql`. **Borra y recrea** la base `clinica` con datos de prueba.
3. Ejecutar la clase `com.mycompany.clinica.vista.Login`.

### Usuarios de prueba

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `admin123` | Admin |
| `medico1` | `medico123` | Médico (Dr. Carlos Mendoza, Cardiología) |
| `medico2` | `medico123` | Médico (Dra. Ana Gómez, Pediatría) |
| `recep1` | `recep123` | Recepcionista |

---

## 2. Vista general de la arquitectura

El proyecto es una aplicación **monolítica de escritorio** en capas. No hay servidor ni API: cada ventana habla directamente con MySQL a través de la clase `Conexion`.

```
┌──────────────────────────────────────────────────────────────┐
│  VISTA  (vista/)                                             │
│  Login · MenuPrincipal · Frm* (pantallas por módulo)         │
└───────┬───────────────────────────┬──────────────────────────┘
        │ consulta permisos         │ pide datos / ejecuta acciones
        ▼                           ▼
┌───────────────┐   ┌──────────────────────────────────────────┐
│ Sesion        │   │  LÓGICA + ACCESO A DATOS                 │
│ Permisos      │   │  servicio/     (reglas de negocio + SQL) │
│ (servicio/)   │   │  dao/          (SQL de citas, espec.)    │
└───────────────┘   │  controlador/  (SQL de pacientes)        │
                    │  (algunas vistas ejecutan SQL directo)   │
                    └───────────────────┬──────────────────────┘
                                        │ Conexion.conectar()
                                        ▼
                              ┌───────────────────┐
                              │  MySQL  "clinica" │
                              └───────────────────┘

        modelo/ (POJOs: Cita, Paciente, Especialidad, EstadoCita)
        utilidades/ (ModeloTabla, AjustarCelda, ExportadorCsv)
        — usados de forma transversal por todas las capas
```

### Principios de diseño que se repiten en todo el código

- **La base de datos es la fuente de verdad.** Los módulos no se llaman entre sí ni comparten objetos en memoria: se comunican guardando y leyendo en MySQL. Por ejemplo, *Atención* guarda una atención y *Recetas*, *Historial*, *Mi agenda* y *Reportes* la leen después sin conocerse.
- **Sin conexión compartida.** Cada operación abre su conexión y la cierra al terminar (`try-with-resources`), así no hay estado de conexión que se corrompa.
- **Estado de sesión global.** Quién inició sesión se guarda en campos estáticos de `Sesion`; cualquier ventana puede consultarlo sin pasar parámetros.
- **Permisos centralizados.** Qué puede hacer cada rol se define solo en `Permisos`. Las ventanas preguntan, no deciden.
- **Seguridad en dos niveles.** Ocultar un botón es una comodidad de la interfaz; el permiso se **vuelve a validar** al abrir el módulo y, en los servicios, antes de operar (ver observaciones al final sobre las excepciones).
- **SQL siempre parametrizado** (`PreparedStatement`), nunca concatenando datos del usuario.

---

## 3. Estructura de carpetas

```
clinica-VF/
├── pom.xml
└── src/main/java/com/mycompany/clinica/
    ├── Conexion.java               ← única puerta a MySQL
    ├── script.sql                  ← esquema + datos de prueba
    ├── vista/                      ← ventanas Swing
    ├── servicio/                   ← sesión, permisos, reglas de negocio
    ├── dao/                        ← acceso a datos (citas, especialidades)
    ├── controlador/                ← acceso a datos (pacientes / historial)
    ├── modelo/                     ← clases de datos
    └── utilidades/                 ← tablas, renderizadores, exportación CSV
```

---

## 4. Archivos principales

### 4.1 Conexión y base de datos

| Archivo | Qué hace |
|---|---|
| `Conexion.java` | `Conexion.conectar()` devuelve una `Connection` nueva. Prueba en orden: puerto 3306 con clave vacía → 3306 con `root` → 3307 con clave vacía → 3307 con `root` (configuraciones comunes de XAMPP). Si ninguna funciona lanza una `SQLException` con un mensaje claro, así **nadie recibe una conexión nula**. Base: `clinica`, usuario `root`. |
| `script.sql` | Crea la base desde cero (`DROP DATABASE` + `CREATE`), las 9 tablas, y carga datos de prueba (2 médicos, 2 pacientes, citas, una atención con receta y los 4 usuarios). |

### 4.2 Servicios (`servicio/`)

| Archivo | Responsabilidad |
|---|---|
| `Sesion` | Guarda usuario, rol e `idMedico` del usuario conectado (campos estáticos). Métodos `iniciar`, `cerrar`, `esAdmin()`, `esMedico()`, `esRecepcionista()`. El `idMedico` **sale de la sesión, nunca de la pantalla**: así un médico solo trabaja con sus datos. |
| `Permisos` | Enum `Pantalla` con los 13 módulos y métodos como `puedeVer(...)`, `pantallasDelRol()`, `puedeGestionarCitas()`, `puedeEliminarPaciente()`, etc. Es el único lugar donde se define la matriz de permisos. |
| `UsuarioService` | Todo el SQL de la tabla `usuario`: login (`autenticar`), crear usuario (con o sin médico nuevo), cambiar contraseña, activar/desactivar. Contiene reglas: no desactivarse a sí mismo y no desactivar al último administrador. |
| `PasswordUtil` | Hash de contraseñas con PBKDF2-HMAC-SHA256, 120 000 iteraciones y sal aleatoria. Formato guardado: `pbkdf2$iteraciones$sal$hash`. |
| `AgendaService` | Consulta de solo lectura de "Mi agenda": las citas del médico en sesión, con filtros de fecha y estado. |
| `ReporteService` | Genera los 7 reportes del sistema (solo Admin) y el resumen de la portada. |
| `CitaService` | Validaciones básicas de citas (datos completos) antes de llamar a `CitaDAO`. |
| `ReglaNegocioException` | Excepción para reglas de negocio incumplidas; su mensaje se muestra tal cual al usuario. Los fallos técnicos siguen siendo `SQLException`. |

### 4.3 Acceso a datos (`dao/` y `controlador/`)

| Archivo | Responsabilidad |
|---|---|
| `dao/CitaDAO` | Registrar cita (nace `Pendiente`), listar y cambiar estado. |
| `dao/EspecialidadDAO` | Listar y registrar especialidades (alimenta el combo al crear un médico). |
| `controlador/PacienteControlador` | CRUD de pacientes, búsqueda y consulta del historial clínico (une `paciente` + `cita` + `atencion`; si el rol es Médico, filtra por su `id_medico`). |

### 4.4 Vistas (`vista/`)

| Archivo | Rol |
|---|---|
| `Login` | **Punto de entrada.** Valida credenciales con `UsuarioService.autenticar` y abre `MenuPrincipal`. |
| `MenuPrincipal` | Núcleo de la navegación. Construye el menú según el rol y abre cada módulo (ver sección 6). |
| `Estilo` | Paleta de colores, fuentes y fábricas de componentes (botones, campos, paneles) para que todas las ventanas luzcan igual. |
| `FrmPacientes`, `FrmCitas`, `FrmMedico`, `FrmEspecialidad`, `FrmMedicamento` | Gestión de datos maestros y citas. |
| `FrmAgenda`, `FrmAtencion`, `FrmReceta`, `FrmHistorial` | Módulos del médico. |
| `FrmReportes`, `FrmUsuarios`, `NuevoUsuario`, `configuracion` | Módulos del administrador. |
| `CambiarPassword` | Diálogo para que cualquier rol cambie su propia contraseña. |

### 4.5 Modelo y utilidades

| Archivo | Qué hace |
|---|---|
| `modelo/Cita`, `Paciente`, `Especialidad`, `EstadoCita` | Clases simples de datos. |
| `utilidades/ModeloTabla` | `TableModel` de solo lectura para las tablas Swing. |
| `utilidades/AjustarCelda` | Renderizador multilínea para textos largos (diagnósticos, observaciones). |
| `utilidades/ExportadorCsv` | Exporta tablas a CSV compatible con Excel (UTF-8 con BOM, separador `;`, y neutraliza celdas que empiecen con `=`, `+`, `-`, `@` para evitar inyección de fórmulas). |

---

## 5. Modelo de datos

```
especialidad ──< medico ──< cita >── paciente
                   │          │
                   │          └──── atencion ──< receta ──< detalle_receta >── medicamento
                   │
                   └── usuario   (id_medico opcional, solo rol Médico)
```

| Tabla | Descripción | Restricciones relevantes |
|---|---|---|
| `especialidad` | Catálogo de especialidades. | `nombre` único. |
| `paciente` | Datos del paciente. | `documento` único. |
| `medico` | Datos del médico. | `cmp` único; FK a `especialidad`. |
| `cita` | Cita de un paciente con un médico (fecha y hora en columnas separadas). | `estado` ENUM; FK a `paciente` y `medico`. |
| `atencion` | Consulta realizada (síntomas, diagnóstico, observaciones). | `id_cita` **único**: una cita tiene como máximo una atención. |
| `receta` | Receta emitida a partir de una atención. | FK a `atencion`. |
| `detalle_receta` | Medicamentos de una receta (dosis, unidad, frecuencia, duración). | Un medicamento no se repite en la misma receta; borrado en cascada con la receta. |
| `medicamento` | Catálogo de medicamentos. | — |
| `usuario` | Cuentas de acceso. | `username` único; `id_medico` único (un médico = un usuario); `rol` ENUM; `estado` 1/0. |

> **Importante:** los nombres de tablas van en **minúsculas** porque así los escribe el código. En Linux, MySQL distingue mayúsculas y las consultas fallarían con otro formato.

---

## 6. Flujo de la aplicación

### 6.1 Inicio de sesión

```
Login.entrar()
  └─ UsuarioService.autenticar(usuario, clave)
        ├─ SELECT ... FROM usuario WHERE username = ? AND estado = 1
        ├─ PasswordUtil.verificar(clave, password_hash)
        └─ si es correcto → Sesion.iniciar(id, username, rol, idMedico)
  └─ new MenuPrincipal().setVisible(true)  +  Login.dispose()
```

Los usuarios con `estado = 0` (desactivados) no pueden entrar.

### 6.2 Menú principal y navegación

`MenuPrincipal` lee `Permisos.pantallasDelRol()` y construye su interfaz según el rol:

| Rol | Pantallas que ve | Presentación |
|---|---|---|
| **Admin** | Pacientes, Citas, Médicos, Especialidades, Medicamentos, Historial (solo lectura), Reportes, Usuarios, Configuración | Tarjetas de módulos |
| **Recepcionista** | Pacientes, Citas, Médicos (solo consulta) | Tarjetas de módulos |
| **Médico** | Mi agenda, Atención, Recetas, Historial | Agenda como vista por defecto; los demás módulos se muestran **dentro de la misma ventana** (menú lateral) |

Al hacer clic en una tarjeta, `abrirModulo(titulo)`:

1. Convierte el título en un `Permisos.Pantalla` y **vuelve a validar el permiso** (aunque la tarjeta ya estuviera oculta).
2. Crea la ventana del módulo según el tipo de contenedor que use:

| Tipo de módulo | Cómo se abre | Ejemplos |
|---|---|---|
| `JFrame` | `abrirModuloEn(...)` | `FrmUsuarios`, `FrmReportes`, `configuracion` |
| `JInternalFrame` | `abrirInternoEn(...)`: lo envuelve en un `JFrame` con `JDesktopPane` | `FrmMedico`, `FrmEspecialidad`, `FrmMedicamento`, `FrmAtencion`, `FrmReceta` |
| `JPanel` | `abrirPanelComoVentana(...)` | `FrmPacientes`, `FrmHistorial` |

3. **Oculta el menú** mientras el módulo está abierto y lo **vuelve a mostrar** al cerrarlo (`windowClosed`). Solo hay una ventana de trabajo activa a la vez.

`Cerrar sesión` llama a `Sesion.cerrar()` y regresa a `Login`.

### 6.3 Ciclo de vida de una cita (el flujo central del negocio)

```
 Recepción agenda ──► Pendiente ──(confirmar)──► Programada ──(el médico atiende)──► Atendida
                          │                           │
                          └────────(cancelar)─────────┴──► Cancelada
```

| Paso | Quién | Dónde | Efecto en la base de datos |
|---|---|---|---|
| 1. Agendar | Admin / Recepcionista | `FrmCitas` → `CitaService` → `CitaDAO` | `INSERT` en `cita` con estado `Pendiente` |
| 2. Confirmar o cancelar | Admin / Recepcionista | `FrmCitas` | `UPDATE cita SET estado` → `Programada` o `Cancelada` |
| 3. Ver mi agenda | Médico | `FrmAgenda` → `AgendaService` | Solo lectura; filtra por `Sesion.getIdMedico()` |
| 4. Registrar atención | Médico | `FrmAtencion` | **Transacción**: `INSERT` en `atencion` + `UPDATE cita` a `Atendida` (si falla una, no se guarda ninguna) |
| 5. Emitir receta | Médico | `FrmReceta` | **Transacción**: `INSERT` en `receta` y sus `detalle_receta` |
| 6. Consultar historial | Médico / Admin | `FrmHistorial` → `PacienteControlador` | Solo lectura (`paciente` + `cita` + `atencion`) |
| 7. Reportes | Admin | `FrmReportes` → `ReporteService` | Solo lectura; exporta a CSV |

Detalles que conviene conocer:

- Al **eliminar una atención**, la cita vuelve a `Programada` (también en transacción) para no dejar una cita "Atendida" sin atención.
- Una cita cuenta como **Atendida** si su estado lo dice **o** si ya tiene una atención registrada. Esa regla (`ESTADO_REAL`) se repite en `AgendaService` y `ReporteService` para que ambos muestren cifras coherentes.
- El médico solo ve y atiende **sus** citas: toda consulta de los módulos médicos usa `Sesion.getIdMedico()`.

### 6.4 Alta de usuarios

1. Un Admin abre **Nuevo usuario** (`NuevoUsuario`).
2. Debe escribir la **contraseña de un administrador** (`UsuarioService.verificarPasswordAdmin`) como confirmación.
3. Si el rol es Médico, puede vincularlo a un médico existente sin usuario, o registrar un médico nuevo en el mismo paso (`crearUsuarioConMedicoNuevo`, transaccional).
4. La contraseña se guarda siempre como hash PBKDF2.

`FrmUsuarios` permite activar o desactivar cuentas (no se borran).

---

## 7. Permisos por rol

Definidos únicamente en `servicio/Permisos.java`:

| Acción | Admin | Médico | Recepcionista |
|---|:-:|:-:|:-:|
| Gestionar pacientes (buscar, registrar, editar) | ✔ | — | ✔ |
| Eliminar pacientes | ✔ | — | — |
| Agendar, confirmar y cancelar citas | ✔ | — | ✔ |
| Gestionar médicos | ✔ | — | consulta |
| Gestionar especialidades y medicamentos | ✔ | — | — |
| Ver historial clínico | ✔ (lectura) | ✔ (solo sus pacientes atendidos) | — |
| Ver su agenda | — | ✔ | — |
| Registrar atenciones y emitir recetas | — | ✔ | — |
| Reportes | ✔ | — | — |
| Gestionar usuarios y configuración | ✔ | — | — |

Todos los roles pueden cambiar su propia contraseña.

---

## 8. Estilos de acceso a datos: cómo está escrito cada módulo

El proyecto fue construido por varias personas, así que **no todos los módulos siguen la misma estructura**. Conviene saberlo antes de modificar algo:

| Estilo | Módulos | Dónde está el SQL |
|---|---|---|
| **Servicio estático** (el más limpio) | Usuarios, Reportes, Mi agenda | En `servicio/`; la ventana solo muestra resultados. Lanzan `ReglaNegocioException` y `SQLException`. |
| **Servicio + DAO** | Citas (y el combo de especialidades de `NuevoUsuario`) | `CitaService` valida → `CitaDAO` ejecuta. Los DAO de citas capturan el error y devuelven `false`/lista vacía. |
| **Controlador** | Pacientes, Historial | `PacienteControlador`. |
| **SQL dentro de la vista** | Médicos, Especialidades, Medicamentos, Atención, Recetas | Directamente en el formulario (`FrmMedico`, `FrmAtencion`, `FrmReceta`, etc.), usando `Conexion.conectar()`. |

Para un módulo nuevo se recomienda seguir el primer estilo (servicio estático + `ReglaNegocioException`), como `AgendaService` o `ReporteService`.

---

## 9. Cómo agregar un módulo nuevo

1. **Pantalla:** crear la ventana en `vista/` (`JPanel`, `JFrame` o `JInternalFrame`).
2. **Permiso:** agregar una entrada al enum `Permisos.Pantalla` y, si hace falta, un método `puedeXxx()`. Incluirla en `Permisos.pantallasDelRol()` para los roles que correspondan.
3. **Menú:** agregar un `if (modulo.equals("Mi módulo"))` en `MenuPrincipal.abrirModulo(...)` usando `abrirModuloEn`, `abrirInternoEn` o `abrirPanelComoVentana` según el tipo de ventana.
4. **Datos:** poner el SQL en un servicio (`servicio/`), abriendo la conexión con `Conexion.conectar()` y `try-with-resources`.
5. **Seguridad:** validar el permiso también dentro del servicio, no solo en la interfaz.
6. Si el módulo usa tablas nuevas, agregarlas a `script.sql` (en minúsculas).

Mientras un módulo no tenga ventana asociada, su tarjeta muestra "estará disponible pronto".

---

## 10. Observaciones técnicas

Puntos a tener presentes al mantener el sistema:

- **Credenciales de BD fijas:** `Conexion` usa el usuario `root` y prueba claves comunes. Está pensado para desarrollo local; para producción convendría externalizar la configuración.
- **Estado `En Espera`:** existe en el `ENUM` de la tabla `cita` y se contempla en consultas, pero **no está** en el enum Java `EstadoCita`; los estados viajan como `String`.
- **Dos clases `Especialidad`:** una en `modelo/` (usada por `EspecialidadDAO`) y otra en `vista/` (usada por `FrmMedico`).
- **Errores en DAO/Controlador:** `CitaDAO` y `PacienteControlador` imprimen el error por consola y devuelven `false`/vacío, por lo que la pantalla no distingue "sin datos" de "fallo de BD". Los servicios (`servicio/`) sí propagan `SQLException`.
- **Validación de permisos desigual:** `MenuPrincipal` siempre valida el acceso al módulo, y `FrmPacientes`, `FrmMedico`, `FrmHistorial` y los servicios lo vuelven a validar internamente; `FrmCitas`, `FrmAtencion`, `FrmReceta`, `FrmMedicamento` y `FrmEspecialidad` dependen solo del control del menú.
- **Sesión estática:** simple y suficiente para una app de escritorio de un solo usuario por proceso; no sirve si se migrara a una arquitectura multiusuario en servidor.
- **Contraseñas de prueba:** cambiarlas desde la app (`CambiarPassword`) antes de cualquier uso real.