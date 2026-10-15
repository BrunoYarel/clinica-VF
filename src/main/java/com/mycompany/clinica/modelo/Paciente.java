package com.mycompany.clinica.modelo;

/** Datos de un paciente. La fecha se maneja como texto ISO yyyy-MM-dd en el formulario. */
public class Paciente {
    private int idPaciente;
    private String nombres;
    private String apellidos;
    private String documento;
    private String fechaNacimiento;
    private String telefono;
    private String email;

    // Constructor para registrar un paciente nuevo
    public Paciente(String nombres, String apellidos, String documento, String fechaNacimiento, String telefono, String email) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.documento = documento;
        this.fechaNacimiento = fechaNacimiento;
        this.telefono = telefono;
        this.email = email;
    }

    // Métodos Getters y Setters necesarios
    public int getIdPaciente() { return idPaciente; }
    public void setIdPaciente(int idPaciente) { this.idPaciente = idPaciente; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }

    public String getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(String fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
