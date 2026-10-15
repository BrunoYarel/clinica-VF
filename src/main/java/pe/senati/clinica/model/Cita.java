package pe.senati.clinica.model;

public class Cita {
    private int idCita;
    private String fechaCita;
    private String horaCita;
    private String motivo;
    private String estado;
    private int idPaciente;
    private int idMedico;

    // Constructor vacío
    public Cita() {
    }

    // Constructor completo
    public Cita(int idCita, String fechaCita, String horaCita, String motivo, String estado, int idPaciente, int idMedico) {
        this.idCita = idCita;
        this.fechaCita = fechaCita;
        this.horaCita = horaCita;
        this.motivo = motivo;
        this.estado = estado;
        this.idPaciente = idPaciente;
        this.idMedico = idMedico;
    }

    // Getters y Setters
    public int getIdCita() { return idCita; }
    public void setIdCita(int idCita) { this.idCita = idCita; }

    public String getFechaCita() { return fechaCita; }
    public void setFechaCita(String fechaCita) { this.fechaCita = fechaCita; }

    public String getHoraCita() { return horaCita; }
    public void setHoraCita(String horaCita) { this.horaCita = horaCita; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public int getIdPaciente() { return idPaciente; }
    public void setIdPaciente(int idPaciente) { this.idPaciente = idPaciente; }

    public int getIdMedico() { return idMedico; }
    public void setIdMedico(int idMedico) { this.idMedico = idMedico; }
}

