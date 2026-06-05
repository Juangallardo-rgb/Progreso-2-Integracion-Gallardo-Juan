package edu.udla.integracion.progreso2.controller;

import edu.udla.integracion.progreso2.model.CitaRequest;
import edu.udla.integracion.progreso2.service.CitaValidationService;
import org.apache.camel.ProducerTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/citas")
public class CitaController {

    private final ProducerTemplate producerTemplate;
    private final CitaValidationService validationService;

    public CitaController(ProducerTemplate producerTemplate, CitaValidationService validationService) {
        this.producerTemplate = producerTemplate;
        this.validationService = validationService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> registrarCita(@RequestBody CitaRequest cita) {

        String error = validationService.validar(cita);

        if (error != null) {
            registrarError(cita, error);

            Map<String, Object> respuestaError = new LinkedHashMap<>();
            respuestaError.put("estado", "RECHAZADA");
            respuestaError.put("mensaje", error);
            respuestaError.put("idCita", cita != null ? cita.getIdCita() : null);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuestaError);
        }

        try {
            producerTemplate.sendBody("direct:procesarCita", cita);

            Map<String, Object> respuesta = new LinkedHashMap<>();
            respuesta.put("estado", "PROCESADA");
            respuesta.put("mensaje", "La cita fue registrada y enviada al flujo de integración.");
            respuesta.put("idCita", cita.getIdCita());

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            registrarError(cita, "Error al procesar la cita: " + e.getMessage());

            Map<String, Object> respuestaError = new LinkedHashMap<>();
            respuestaError.put("estado", "ERROR");
            respuestaError.put("mensaje", "Ocurrió un error al procesar la cita.");
            respuestaError.put("detalle", e.getMessage());
            respuestaError.put("idCita", cita.getIdCita());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(respuestaError);
        }
    }

    private void registrarError(CitaRequest cita, String motivo) {
        try {
            FileWriter writer = new FileWriter("data/errors/citas-rechazadas.log", true);

            writer.write("FechaHora: " + LocalDateTime.now() + "\n");
            writer.write("idCita: " + (cita != null ? cita.getIdCita() : "SIN_ID") + "\n");
            writer.write("Motivo: " + motivo + "\n");
            writer.write("Payload: " + construirPayloadTexto(cita) + "\n");
            writer.write("--------------------------------------------------\n");

            writer.close();
        } catch (IOException e) {
            System.err.println("No se pudo registrar el error en archivo: " + e.getMessage());
        }
    }

    private String construirPayloadTexto(CitaRequest cita) {
        if (cita == null) {
            return "Payload vacío";
        }

        return "{"
                + "idCita='" + cita.getIdCita() + '\''
                + ", paciente='" + cita.getPaciente() + '\''
                + ", correo='" + cita.getCorreo() + '\''
                + ", especialidad='" + cita.getEspecialidad() + '\''
                + ", fechaCita='" + cita.getFechaCita() + '\''
                + ", sede='" + cita.getSede() + '\''
                + ", valor=" + cita.getValor()
                + '}';
    }
}