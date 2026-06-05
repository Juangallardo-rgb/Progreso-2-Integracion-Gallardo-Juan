package edu.udla.integracion.progreso2.service;

import edu.udla.integracion.progreso2.model.CitaRequest;
import org.springframework.stereotype.Service;

@Service
public class CitaValidationService {

    public String validar(CitaRequest cita) {

        if (cita == null) {
            return "El payload de la cita no puede estar vacío.";
        }

        if (estaVacio(cita.getIdCita())) {
            return "El campo idCita es obligatorio.";
        }

        if (estaVacio(cita.getPaciente())) {
            return "El campo paciente es obligatorio.";
        }

        if (estaVacio(cita.getCorreo())) {
            return "El campo correo es obligatorio.";
        }

        if (estaVacio(cita.getEspecialidad())) {
            return "El campo especialidad es obligatorio.";
        }

        if (estaVacio(cita.getFechaCita())) {
            return "El campo fechaCita es obligatorio.";
        }

        if (estaVacio(cita.getSede())) {
            return "El campo sede es obligatorio.";
        }

        if (cita.getValor() == null || cita.getValor() <= 0) {
            return "El campo valor debe ser mayor a 0.";
        }

        return null;
    }

    private boolean estaVacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }
}