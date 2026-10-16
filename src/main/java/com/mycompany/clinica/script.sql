-- =========================================================================
-- BASE DE DATOS "clinica"  (nombre que usa Conexion.java)
-- ATENCIÓN: este script BORRA la base de datos y la vuelve a crear desde cero.
-- Ejecútalo completo (phpMyAdmin > SQL, o  mysql -u root < script.sql).
--
-- Los nombres de las tablas van en MINÚSCULAS porque así los escribe el código
-- Java (cita, paciente, usuario...). En Windows/XAMPP da igual, pero en Linux
-- MySQL distingue mayúsculas y con nombres en MAYÚSCULAS las consultas fallarían.
-- =========================================================================
DROP DATABASE IF EXISTS clinica;
CREATE DATABASE clinica CHARACTER SET utf8mb4;
USE clinica;

-- Evita que las tildes y la "ñ" de los datos de prueba se corrompan al importar.
SET NAMES utf8mb4;

-- =========================================================================
-- CREACIÓN DE TABLAS
-- =========================================================================

-- 1. ESPECIALIDAD
CREATE TABLE especialidad (
                              id_especialidad INT AUTO_INCREMENT,
                              nombre VARCHAR(100) NOT NULL,
                              descripcion VARCHAR(255),
                              CONSTRAINT pk_especialidad PRIMARY KEY (id_especialidad),
                              CONSTRAINT uq_especialidad_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. PACIENTE
CREATE TABLE paciente (
                          id_paciente INT AUTO_INCREMENT,
                          nombres VARCHAR(100) NOT NULL,
                          apellidos VARCHAR(100) NOT NULL,
                          documento VARCHAR(20) NOT NULL,
                          fecha_nacimiento DATE NOT NULL,
                          telefono VARCHAR(20),
                          email VARCHAR(100),
                          CONSTRAINT pk_paciente PRIMARY KEY (id_paciente),
                          CONSTRAINT uq_paciente_documento UNIQUE (documento)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. MEDICAMENTO
--  * Lo usa FrmMedicamento (módulo de Óscar): lista y guarda nombre + presentación.
CREATE TABLE medicamento (
                             id_medicamento INT AUTO_INCREMENT,
                             nombre VARCHAR(100) NOT NULL,
                             presentacion VARCHAR(100) NOT NULL,
                             CONSTRAINT pk_medicamento PRIMARY KEY (id_medicamento)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. MEDICO
CREATE TABLE medico (
                        id_medico INT AUTO_INCREMENT,
                        nombres VARCHAR(100) NOT NULL,
                        apellidos VARCHAR(100) NOT NULL,
                        cmp VARCHAR(20) NOT NULL,
                        telefono VARCHAR(20),
                        email VARCHAR(150),
                        id_especialidad INT NOT NULL,
                        CONSTRAINT pk_medico PRIMARY KEY (id_medico),
                        CONSTRAINT uq_medico_cmp UNIQUE (cmp),
                        CONSTRAINT fk_medico_especialidad FOREIGN KEY (id_especialidad) REFERENCES especialidad(id_especialidad)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. CITA  (fecha y hora separadas, como las usa el código Java)
--  * estado: los valores deben coincidir EXACTO con el enum EstadoCita.java
CREATE TABLE cita (
                      id_cita INT AUTO_INCREMENT,
                      fecha_cita DATE NOT NULL,
                      hora_cita TIME NOT NULL,
                      motivo VARCHAR(255),
                      estado ENUM('Pendiente', 'Programada', 'En Espera', 'Atendida', 'Cancelada') DEFAULT 'Pendiente',
                      id_paciente INT NOT NULL,
                      id_medico INT NOT NULL,
                      CONSTRAINT pk_cita PRIMARY KEY (id_cita),
                      CONSTRAINT fk_cita_paciente FOREIGN KEY (id_paciente) REFERENCES paciente(id_paciente),
                      CONSTRAINT fk_cita_medico FOREIGN KEY (id_medico) REFERENCES medico(id_medico)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. ATENCION
--  * uq_atencion_cita: una cita solo puede tener UNA atención (AgendaDAO captura este error).
CREATE TABLE atencion (
                          id_atencion INT AUTO_INCREMENT,
                          id_cita INT NOT NULL,
                          fecha_hora_atencion TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                          sintomas TEXT NOT NULL,
                          diagnostico TEXT NOT NULL,
                          observaciones TEXT,
                          CONSTRAINT pk_atencion PRIMARY KEY (id_atencion),
                          CONSTRAINT uq_atencion_cita UNIQUE (id_cita),
                          CONSTRAINT fk_atencion_cita FOREIGN KEY (id_cita) REFERENCES cita(id_cita)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. RECETA
CREATE TABLE receta (
                        id_receta INT AUTO_INCREMENT,
                        fecha_emision DATE DEFAULT (CURRENT_DATE) NOT NULL,
                        indicaciones TEXT NOT NULL,
                        id_atencion INT NOT NULL,
                        CONSTRAINT pk_receta PRIMARY KEY (id_receta),
                        CONSTRAINT fk_receta_atencion FOREIGN KEY (id_atencion) REFERENCES atencion(id_atencion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. DETALLE_RECETA
--  * Tamaños alineados con FrmReceta.java (módulo Recetas, que es quien llena esta tabla): dosis hasta 9999.99,
--    unidad 20, frecuencia 100, duración 50.
--  * uq_detalle_receta_medicamento: un medicamento no se repite dentro de la misma receta.
CREATE TABLE detalle_receta (
                                id_detalle_receta INT AUTO_INCREMENT,
                                id_receta INT NOT NULL,
                                id_medicamento INT NOT NULL,
                                dosis DECIMAL(6,2) NOT NULL,
                                unidad_medida VARCHAR(20) NOT NULL,
                                frecuencia VARCHAR(100) NOT NULL,
                                duracion VARCHAR(50) NOT NULL,
                                CONSTRAINT pk_detalle_receta PRIMARY KEY (id_detalle_receta),
                                CONSTRAINT uq_detalle_receta_medicamento UNIQUE (id_receta, id_medicamento),
                                CONSTRAINT fk_detalle_receta_receta FOREIGN KEY (id_receta) REFERENCES receta(id_receta) ON DELETE CASCADE,
                                CONSTRAINT fk_detalle_receta_medicamento FOREIGN KEY (id_medicamento) REFERENCES medicamento(id_medicamento)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. USUARIO
--  * rol: 'Medico' SIN tilde, para que coincida con la clase Sesion.
--  * password_hash: formato  pbkdf2$iteraciones$sal$hash  (PasswordUtil.java).
--  * id_medico: solo se llena para usuarios con rol 'Medico'; Admin y
--    Recepcionista lo tienen en NULL.
--  * uq_usuario_medico: un médico solo puede tener UN usuario (MySQL permite
--    varios NULL en una columna UNIQUE, así que Admin/Recepcionista no chocan).
CREATE TABLE usuario (
                         id_usuario INT AUTO_INCREMENT,
                         username VARCHAR(50) NOT NULL,
                         password_hash VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
                         rol ENUM('Admin', 'Medico', 'Recepcionista') NOT NULL,
                         id_medico INT NULL,
                         estado TINYINT(1) NOT NULL DEFAULT 1,
                         CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
                         CONSTRAINT uq_usuario_username UNIQUE (username),
                         CONSTRAINT uq_usuario_medico UNIQUE (id_medico),
                         CONSTRAINT fk_usuario_medico FOREIGN KEY (id_medico) REFERENCES medico(id_medico) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================================
-- DATOS DE PRUEBA
-- =========================================================================

-- Especialidad
INSERT INTO especialidad (nombre, descripcion) VALUES
                                                   ('Cardiología', 'Estudio, diagnóstico y tratamiento de enfermedades del corazón.'),
                                                   ('Pediatría', 'Atención médica de bebés, niños y adolescentes.');

-- Pacientes
INSERT INTO paciente (nombres, apellidos, documento, fecha_nacimiento, telefono, email) VALUES
                                                                                            ('Juan Carlos', 'Pérez Quispe', '45781236', '1990-05-15', '955123456', 'juan.perez@email.com'),
                                                                                            ('María Elena', 'Flores Choque', '70124589', '1985-11-22', '944789123', 'maria.flores@email.com');

-- Medicamentos
INSERT INTO medicamento (nombre, presentacion) VALUES
                                                   ('Paracetamol', 'Tableta 500mg'),
                                                   ('Amoxicilina', 'Cápsula 500mg'),
                                                   ('Losartán', 'Tableta 50mg');

-- Médicos
INSERT INTO medico (nombres, apellidos, cmp, telefono, email, id_especialidad) VALUES
                                                                                   ('Carlos Alberto', 'Mendoza Ruiz', 'CMP12345', '987654321', 'carlos.mendoza@clinica.com', 1),
                                                                                   ('Ana Sofía', 'Gómez Torres', 'CMP67890', '912345678', 'ana.gomez@clinica.com', 2);

-- Cita 1 (paciente 1 con médico 1, ya atendida)
INSERT INTO cita (fecha_cita, hora_cita, motivo, estado, id_paciente, id_medico) VALUES
    ('2026-05-14', '09:00:00', 'Control de presión arterial alta', 'Atendida', 1, 1);

-- Atención 1
INSERT INTO atencion (id_cita, fecha_hora_atencion, sintomas, diagnostico, observaciones) VALUES
    (LAST_INSERT_ID(), '2026-05-14 09:15:00', 'El paciente presenta cefalea leve y fatiga. Presión registrada: 140/90.', 'Hipertensión esencial primaria', 'Se solicita al paciente reducir el consumo de sodio y regresar en 15 días.');

-- Receta 1
INSERT INTO receta (indicaciones, id_atencion) VALUES
    ('Tomar el medicamento estrictamente en las horas señaladas. Evitar esfuerzos físicos.', LAST_INSERT_ID());

-- Detalle receta 1
INSERT INTO detalle_receta (id_receta, id_medicamento, dosis, unidad_medida, frecuencia, duracion) VALUES
    (LAST_INSERT_ID(), 3, 1.00, 'Tableta', 'Cada 24 horas', '30 días');

-- Cita 2 (paciente 2 con médico 2, programada)
INSERT INTO cita (fecha_cita, hora_cita, motivo, estado, id_paciente, id_medico) VALUES
    ('2026-06-15', '10:30:00', 'Fiebre persistente y tos', 'Programada', 2, 2);

-- Citas 3 y 4: para HOY (CURDATE), así "Mi agenda" y confirmar/cancelar tienen qué mostrar.
--   * Cita 3 (médico 1) nace Pendiente: recepción debe confirmarla antes de que el médico la atienda.
--   * Cita 4 (médico 2) ya está Programada: medico2 puede atenderla de inmediato.
-- Bloque opcional: puedes borrarlo si no lo quieres.
INSERT INTO cita (fecha_cita, hora_cita, motivo, estado, id_paciente, id_medico) VALUES
                                                                                     (CURDATE(), '10:00:00', 'Dolor de cabeza frecuente', 'Pendiente', 2, 1),
                                                                                     (CURDATE(), '11:00:00', 'Consulta general', 'Programada', 1, 2);

-- =========================================================================
-- USUARIOS DE PRUEBA  (cambia las contraseñas desde la app)
--   admin    / admin123    -> rol Admin
--   medico1  / medico123   -> rol Medico, vinculado al médico 1 (Carlos Mendoza)
--   medico2  / medico123   -> rol Medico, vinculado al médico 2 (Ana Gómez)
--   recep1   / recep123    -> rol Recepcionista
-- Los valores de password_hash son hashes PBKDF2, NO texto plano.
-- =========================================================================
INSERT INTO usuario (username, password_hash, rol, id_medico, estado) VALUES
                                                                          ('admin',   'pbkdf2$120000$YuBHJsTGZfOqkwwY+shyLw==$EyRbJSvOEr1DiP8VYa1xkC0pG3Rn5Gm28eIAhRwLqOs=', 'Admin', NULL, 1),
                                                                          ('medico1', 'pbkdf2$120000$bsMXwIQqaMaG+U9iviEoSg==$tFyg1KZeH9rAlv7jxJBwsRTCQTAW+X8NBmoCEDCjgi0=', 'Medico', 1, 1),
                                                                          ('medico2', 'pbkdf2$120000$bsMXwIQqaMaG+U9iviEoSg==$tFyg1KZeH9rAlv7jxJBwsRTCQTAW+X8NBmoCEDCjgi0=', 'Medico', 2, 1),
                                                                          ('recep1',  'pbkdf2$120000$j2d0iW+1avFw2kgnrvnjkQ==$yaTEmHhWSsVVWOx5dAtjCYI6DbJC881UstiNZA74swE=', 'Recepcionista', NULL, 1);