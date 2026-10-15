-- =========================================================================
-- BASE DE DATOS "clinica"  (nombre que usa Conexion.java)
-- ATENCIÓN: este script BORRA la base de datos y la vuelve a crear desde cero.
-- Ejecútalo completo (phpMyAdmin > SQL, o  mysql -u root < script.sql).
--
-- Contiene solo las tablas del módulo de Login y Roles (usuario) y las dos de las que
-- depende (especialidad y medico, por la clave foránea usuario.id_medico). Cada módulo
-- agrega aquí sus propias tablas, respetando el mismo estándar de nombres.
--
-- Los nombres de las tablas van en MINÚSCULAS porque así los escribe el código
-- Java (usuario, medico...). En Windows/XAMPP da igual, pero en Linux
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

-- 2. MEDICO
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

-- 3. USUARIO
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

-- 4. MEDICAMENTO
--  * Lo usa FrmMedicamento (módulo de Óscar): lista y guarda nombre + presentación.
CREATE TABLE medicamento (
                             id_medicamento INT AUTO_INCREMENT,
                             nombre VARCHAR(100) NOT NULL,
                             presentacion VARCHAR(100) NOT NULL,
                             CONSTRAINT pk_medicamento PRIMARY KEY (id_medicamento)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================================
-- DATOS DE PRUEBA
-- =========================================================================

-- Especialidad
INSERT INTO especialidad (nombre, descripcion) VALUES
                                                   ('Cardiología', 'Estudio, diagnóstico y tratamiento de enfermedades del corazón.'),
                                                   ('Pediatría', 'Atención médica de bebés, niños y adolescentes.');

-- Médicos
INSERT INTO medico (nombres, apellidos, cmp, telefono, email, id_especialidad) VALUES
                                                                                   ('Carlos Alberto', 'Mendoza Ruiz', 'CMP12345', '987654321', 'carlos.mendoza@clinica.com', 1),
                                                                                   ('Ana Sofía', 'Gómez Torres', 'CMP67890', '912345678', 'ana.gomez@clinica.com', 2);

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