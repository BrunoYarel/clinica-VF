package com.mycompany.clinica.vista;

public class Atencion {
    private int idAtencion;
    private int idCita;
    private String fechaHoraAtencion;
    private String sintomas;
    private String diagnostico;
    private String observaciones;

    public Atencion() {
    }

    public Atencion(int idAtencion, int idCita, String fechaHoraAtencion, String sintomas, String diagnostico, String observaciones) {
        this.idAtencion = idAtencion;
        this.idCita = idCita;
        this.fechaHoraAtencion = fechaHoraAtencion;
        this.sintomas = sintomas;
        this.diagnostico = diagnostico;
        this.observaciones = observaciones;
    }

    public int getIdAtencion() {
        return idAtencion;
    }

    public void setIdAtencion(int idAtencion) {
        this.idAtencion = idAtencion;
    }

    public int getIdCita() {
        return idCita;
    }

    public void setIdCita(int idCita) {
        this.idCita = idCita;
    }

    public String getFechaHoraAtencion() {
        return fechaHoraAtencion;
    }

    public void setFechaHoraAtencion(String fechaHoraAtencion) {
        this.fechaHoraAtencion = fechaHoraAtencion;
    }

    public String getSintomas() {
        return sintomas;
    }

    public void setSintomas(String sintomas) {
        this.sintomas = sintomas;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public void setDiagnostico(String diagnostico) {
        this.diagnostico = diagnostico;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}