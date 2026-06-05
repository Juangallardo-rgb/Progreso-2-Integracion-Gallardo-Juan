package edu.udla.integracion.progreso2.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.udla.integracion.progreso2.config.RabbitMQConfig;
import edu.udla.integracion.progreso2.model.CitaRequest;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CitaIntegrationRoute extends RouteBuilder {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public CitaIntegrationRoute(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void configure() {

        onException(Exception.class)
                .handled(true)
                .process(exchange -> {
                    Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    CitaRequest cita = exchange.getIn().getBody(CitaRequest.class);

                    String idCita = cita != null ? cita.getIdCita() : "SIN_ID";
                    String motivo = exception != null ? exception.getMessage() : "Error desconocido";

                    String errorLog =
                            "FechaHora: " + LocalDateTime.now() + "\n" +
                            "idCita: " + idCita + "\n" +
                            "Motivo: Error al procesar el mensaje en Apache Camel: " + motivo + "\n" +
                            "Payload: " + construirPayloadTexto(cita) + "\n" +
                            "--------------------------------------------------\n";

                    exchange.getIn().setBody(errorLog);
                })
                .to("file:data/errors?fileName=citas-rechazadas.log&fileExist=Append");

        from("direct:procesarCita")
                .routeId("ruta-integracion-cita-confirmada")
                .log("Procesando cita confirmada con id: ${body.idCita}")

                .wireTap("direct:enviarFacturacion")
                .wireTap("direct:publicarEventoCita")
                .wireTap("direct:generarCsvAuditoria")

                .log("Cita procesada correctamente: ${body.idCita}");

        from("direct:enviarFacturacion")
                .routeId("ruta-point-to-point-facturacion")
                .process(exchange -> {
                    CitaRequest cita = exchange.getIn().getBody(CitaRequest.class);

                    Map<String, Object> mensajeFacturacion = new LinkedHashMap<>();
                    mensajeFacturacion.put("idCita", cita.getIdCita());
                    mensajeFacturacion.put("paciente", cita.getPaciente());
                    mensajeFacturacion.put("especialidad", cita.getEspecialidad());
                    mensajeFacturacion.put("valor", cita.getValor());
                    mensajeFacturacion.put("tipoMensaje", "COMANDO_FACTURAR_CITA");

                    String json = objectMapper.writeValueAsString(mensajeFacturacion);

                    rabbitTemplate.convertAndSend(
                            RabbitMQConfig.BILLING_EXCHANGE,
                            RabbitMQConfig.BILLING_QUEUE,
                            json
                    );

                    exchange.getIn().setBody(json);
                })
                .log("Mensaje Point-to-Point enviado a billing.queue: ${body}");

        from("direct:publicarEventoCita")
                .routeId("ruta-publish-subscribe-cita-confirmada")
                .process(exchange -> {
                    CitaRequest cita = exchange.getIn().getBody(CitaRequest.class);

                    Map<String, Object> eventoCita = new LinkedHashMap<>();
                    eventoCita.put("idCita", cita.getIdCita());
                    eventoCita.put("paciente", cita.getPaciente());
                    eventoCita.put("correo", cita.getCorreo());
                    eventoCita.put("especialidad", cita.getEspecialidad());
                    eventoCita.put("fechaCita", cita.getFechaCita());
                    eventoCita.put("sede", cita.getSede());
                    eventoCita.put("tipoEvento", "CITA_CONFIRMADA");

                    String json = objectMapper.writeValueAsString(eventoCita);

                    rabbitTemplate.convertAndSend(
                            RabbitMQConfig.APPOINTMENTS_EVENTS_EXCHANGE,
                            "",
                            json
                    );

                    exchange.getIn().setBody(json);
                })
                .log("Evento Publish/Subscribe publicado en appointments.events: ${body}");

        from("direct:generarCsvAuditoria")
                .routeId("ruta-archivo-csv-auditoria")
                .process(exchange -> {
                    CitaRequest cita = exchange.getIn().getBody(CitaRequest.class);

                    String lineaCsv = cita.getIdCita() + ","
                            + cita.getPaciente() + ","
                            + cita.getCorreo() + ","
                            + cita.getEspecialidad() + ","
                            + cita.getFechaCita() + ","
                            + cita.getSede() + ","
                            + String.format("%.2f", cita.getValor());

                    exchange.getIn().setBody(lineaCsv + "\n");
                })
                .to("file:data/outbox?fileName=auditoria-citas.csv&fileExist=Append")
                .log("Archivo CSV de auditoria actualizado");
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