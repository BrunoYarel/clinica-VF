package com.mycompany.clinica.modelo;

public class Especialidad {
    private int idEspecialidad;
    private String nombre;
    private String descripcion;

    // Constructor por defecto
    public Especialidad() {
    }

    // Constructor cargado para el ComboBox
    public Especialidad(int idEspecialidad, String nombre) {
        this.idEspecialidad = idEspecialidad;
        this.nombre = nombre;
    }

    public int getIdEspecialidad() {
        return idEspecialidad;
    }

    public void setIdEspecialidad(int idEspecialidad) {
        this.idEspecialidad = idEspecialidad;
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
