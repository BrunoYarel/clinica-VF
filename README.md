# Clínica VF

Sistema de gestión de clínica (Java Swing + MySQL). Este repositorio parte con el módulo de **Login y Roles**; cada módulo se integra después sobre esta base.

## Requisitos
- JDK 25 y Maven (`maven.compiler.release` = 25 en el `pom.xml`)
- MySQL (XAMPP): `Conexion.java` prueba puertos 3306/3307 con clave vacía o `root`

## Puesta en marcha
1. Ejecutar completo `src/main/java/com/mycompany/clinica/script.sql` (crea la base `clinica` desde cero).
2. Ejecutar la clase `com.mycompany.clinica.vista.Login`.

## Usuarios de prueba
| Usuario  | Contraseña | Rol           |
|----------|------------|---------------|
| admin    | admin123   | Admin         |
| medico1  | medico123  | Medico        |
| medico2  | medico123  | Medico        |
| recep1   | recep123   | Recepcionista |

## Qué incluye
- `servicio/`: `Sesion` (quién inició sesión), `Permisos` (qué ve y qué puede hacer cada rol), `UsuarioService` (todo el SQL de `usuario`), `PasswordUtil` (PBKDF2), `ReglaNegocioException`.
- `vista/`: `Login`, `MenuPrincipal`, `NuevoUsuario`, `FrmUsuarios`, `CambiarPassword`, `Estilo`.
- `Conexion`, `utilidades/ModeloTabla`, y `dao/EspecialidadDAO` + `modelo/Especialidad` (los usa `NuevoUsuario` para registrar un médico).

## Cómo integrar un módulo al menú
En `MenuPrincipal.abrirModulo(...)` agregar una línea por módulo, por ejemplo:

```java
if (modulo.equals("Citas")) { abrirModuloEn(new FrmCitas()); return; }
```

Mientras un módulo no esté conectado, su tarjeta muestra "estará disponible pronto". Los permisos de cada pantalla se definen solo en `Permisos`.
