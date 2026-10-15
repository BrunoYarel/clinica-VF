package pe.senati.clinica.service;

import pe.senati.clinica.dao.CitaDAO;
import pe.senati.clinica.model.Cita;
import java.util.List;


public class CitaService {
    
    private final CitaDAO citaDAO;

    // Constructor que inicializa el acceso a datos
    public CitaService() {
        this.citaDAO = new CitaDAO();
    }

    /**
     * Regla de negocio para registrar una cita.
     * Valida que los campos esenciales no estén vacíos antes de enviarlos a la BD.
     */
    public boolean agendarNuevaCita(Cita cita) {
        if (cita.getFechaCita() == null || cita.getFechaCita().trim().isEmpty() ||
            cita.getHoraCita() == null || cita.getHoraCita().trim().isEmpty() ||
            cita.getIdPaciente() <= 0 || cita.getIdMedico() <= 0) {
            
            System.out.println("Error en Service: Datos de la cita incompletos o inválidos.");
            return false;
        }
        
        return citaDAO.registrarCita(
            cita.getIdPaciente(), 
            cita.getIdMedico(), 
            cita.getFechaCita(), 
            cita.getHoraCita(), 
            cita.getMotivo()
        );
    }
    
    public List<Cita> obtenerTodasLasCitas() {
        return citaDAO.listarCitas();
    }
    
    public boolean actualizarEstado(int idCita, String nuevoEstado) {
        if (idCita <= 0 || nuevoEstado == null || nuevoEstado.trim().isEmpty()) {
            System.out.println("Error en Service: ID o estado inválido.");
            return false;
        }
        return citaDAO.modificarEstadoCita(idCita, nuevoEstado);
    }
}


