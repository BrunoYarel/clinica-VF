package com.mycompany.clinica;

public class Especialidad {
    private int id_especialidad;
    private String nombre;
    private String descripcion;

    // Constructor por defecto
    public Especialidad() {
    }

    // Constructor cargado para el ComboBox
    public Especialidad(int id_especialidad, String nombre) {
        this.id_especialidad = id_especialidad;
        this.nombre = nombre;
    }

    // Método que necesita FrmMedico para guardar en la Base de Datos
    public int getId() {
        return id_especialidad;
    }

    public void setId(int id_especialidad) {
        this.id_especialidad = id_especialidad;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    // Método para que el JComboBox muestre el texto de la especialidad
    @Override
    public String toString() {
        return nombre;
    }
}

