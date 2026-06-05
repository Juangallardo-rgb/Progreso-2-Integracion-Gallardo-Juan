Examen Práctico Progreso 2 - Integración de Sistemas
1. Nombre del estudiante

Juan Esteban Gallardo Alava

2. Descripción breve de la solución

Este proyecto implementa una solución mínima de integración para el caso de la organización Salud360.

La solución permite registrar solicitudes de cita médica mediante una API REST. Cuando la cita es válida, el sistema inicia un flujo de integración usando Apache Camel, envía un mensaje de facturación a RabbitMQ mediante el patrón Point-to-Point, publica un evento de cita confirmada para notificaciones y analítica mediante Publish/Subscribe, y genera un archivo CSV para integrar con un sistema legado de auditoría.

También se implementa validación de datos y registro de errores para solicitudes inválidas.

3. Tecnologías utilizadas
Java 17
Spring Boot
Apache Camel
RabbitMQ
Docker Compose
Maven
Postman / curl
GitHub
4. Instrucciones para levantar RabbitMQ

Para levantar RabbitMQ se utiliza Docker Compose.

Desde la raíz del proyecto, ejecutar:

docker compose up -d

Verificar que el contenedor esté corriendo:

docker ps

Luego abrir el panel web de RabbitMQ:

http://localhost:15672

Credenciales:

Usuario: guest
Contraseña: guest

Al ejecutar la aplicación, se crean automáticamente las siguientes colas:

billing.queue
notifications.queue
analytics.queue

También se crean los exchanges:

billing.exchange
appointments.events
5. Instrucciones para ejecutar la aplicación

Desde la raíz del proyecto, ejecutar:

mvn clean spring-boot:run -U

La aplicación se ejecuta en el puerto:

http://localhost:8080
6. Endpoint disponible
Registrar cita médica
POST /api/citas

URL completa:

http://localhost:8080/api/citas
7. Ejemplo de request válido
{
  "idCita": "CITA-1004",
  "paciente": "Ana Torres",
  "correo": "ana.torres@email.com",
  "especialidad": "Cardiologia",
  "fechaCita": "2026-06-15",
  "sede": "Centro Norte",
  "valor": 45.50
}

Ejemplo usando curl:

curl -X POST http://localhost:8080/api/citas -H "Content-Type: application/json" -d "{\"idCita\":\"CITA-1004\",\"paciente\":\"Ana Torres\",\"correo\":\"ana.torres@email.com\",\"especialidad\":\"Cardiologia\",\"fechaCita\":\"2026-06-15\",\"sede\":\"Centro Norte\",\"valor\":45.50}"

Respuesta esperada:

{
  "estado": "PROCESADA",
  "mensaje": "La cita fue registrada y enviada al flujo de integración.",
  "idCita": "CITA-1004"
}
8. Ejemplo de request inválido

En este ejemplo el campo paciente está vacío.

{
  "idCita": "CITA-1005",
  "paciente": "",
  "correo": "paciente@email.com",
  "especialidad": "Dermatologia",
  "fechaCita": "2026-06-18",
  "sede": "Centro Sur",
  "valor": 30.00
}

Ejemplo usando curl:

curl -X POST http://localhost:8080/api/citas -H "Content-Type: application/json" -d "{\"idCita\":\"CITA-1005\",\"paciente\":\"\",\"correo\":\"paciente@email.com\",\"especialidad\":\"Dermatologia\",\"fechaCita\":\"2026-06-18\",\"sede\":\"Centro Sur\",\"valor\":30.00}"

Respuesta esperada:

{
  "estado": "RECHAZADA",
  "mensaje": "El campo paciente es obligatorio.",
  "idCita": "CITA-1005"
}
9. Explicación de los estilos y patrones de integración
API REST

La API REST se utiliza para exponer el registro de citas médicas mediante el endpoint:

POST /api/citas

Este endpoint recibe los datos de la cita, valida la información y, si la solicitud es válida, inicia el flujo de integración.

Point-to-Point

El patrón Point-to-Point se aplica en la integración con el sistema de facturación.

La cita válida genera un mensaje que se envía a la cola:

billing.queue

Este patrón se utiliza porque la solicitud de facturación debe ser procesada por un solo consumidor. Esto evita que se genere más de una orden de cobro para la misma cita.

Mensaje enviado a facturación:

{
  "idCita": "CITA-1004",
  "paciente": "Ana Torres",
  "especialidad": "Cardiologia",
  "valor": 45.5,
  "tipoMensaje": "COMANDO_FACTURAR_CITA"
}
Publish/Subscribe

El patrón Publish/Subscribe se aplica para distribuir el evento de cita confirmada a varios sistemas interesados.

El evento se publica en el exchange:

appointments.events

Y llega a las siguientes colas:

notifications.queue
analytics.queue

Este patrón se utiliza porque el mismo evento debe ser recibido por más de un sistema: el sistema de notificaciones y el sistema de analítica.

Evento publicado:

{
  "idCita": "CITA-1004",
  "paciente": "Ana Torres",
  "correo": "ana.torres@email.com",
  "especialidad": "Cardiologia",
  "fechaCita": "2026-06-15",
  "sede": "Centro Norte",
  "tipoEvento": "CITA_CONFIRMADA"
}
Transferencia de archivos

La transferencia de archivos se aplica para integrar con el sistema legado de auditoría.

Como el sistema legado no cuenta con API ni conexión directa con RabbitMQ, la solución genera un archivo CSV en la ruta:

data/outbox/auditoria-citas.csv

Formato generado:

idCita,paciente,correo,especialidad,fechaCita,sede,valor
CITA-1004,Ana Torres,ana.torres@email.com,Cardiologia,2026-06-15,Centro Norte,45.50
Manejo de errores

La solución valida los datos recibidos antes de iniciar el flujo de integración.

Se validan los siguientes campos:

idCita obligatorio.
paciente obligatorio.
correo obligatorio.
especialidad obligatoria.
fechaCita obligatoria.
sede obligatoria.
valor mayor a 0.

Si la solicitud es inválida, la cita no se envía a RabbitMQ ni se registra en el CSV de auditoría.

Los errores se registran en:

data/errors/citas-rechazadas.log

Cada error incluye:

Fecha y hora.
idCita, si existe.
Motivo del rechazo.
Payload recibido.
10. Evidencia esperada para verificar el funcionamiento

Para verificar el funcionamiento de la solución se debe revisar lo siguiente:

La aplicación Spring Boot inicia correctamente en el puerto 8080.
RabbitMQ se levanta correctamente mediante Docker Compose.
RabbitMQ muestra las colas:
billing.queue
notifications.queue
analytics.queue
Un request válido a POST /api/citas devuelve estado PROCESADA.
El mensaje de facturación llega a billing.queue.
El evento de cita confirmada llega a notifications.queue.
El evento de cita confirmada llega a analytics.queue.
Se genera una línea en el archivo:
data/outbox/auditoria-citas.csv
Un request inválido devuelve estado RECHAZADA.
El error se registra en:
data/errors/citas-rechazadas.log

Las capturas de evidencia se encuentran en:

docs/capturas/
11. Estructura del proyecto
progreso2-integracion-gallardo-juan/
│
├── README.md
├── docker-compose.yml
├── pom.xml
│
├── src/
│   └── main/
│       ├── java/
│       │   └── edu/udla/integracion/progreso2/
│       │       ├── Progreso2Application.java
│       │       ├── config/
│       │       │   └── RabbitMQConfig.java
│       │       ├── controller/
│       │       │   └── CitaController.java
│       │       ├── model/
│       │       │   └── CitaRequest.java
│       │       ├── routes/
│       │       │   └── CitaIntegrationRoute.java
│       │       └── service/
│       │           └── CitaValidationService.java
│       │
│       └── resources/
│           └── application.properties
│
├── data/
│   ├── outbox/
│   │   └── auditoria-citas.csv
│   └── errors/
│       └── citas-rechazadas.log
│
└── docs/
    └── capturas/
12. Comandos principales

Levantar RabbitMQ:

docker compose up -d

Detener RabbitMQ:

docker compose down

Ejecutar la aplicación:

mvn clean spring-boot:run -U

Probar cita válida:

curl -X POST http://localhost:8080/api/citas -H "Content-Type: application/json" -d "{\"idCita\":\"CITA-1004\",\"paciente\":\"Ana Torres\",\"correo\":\"ana.torres@email.com\",\"especialidad\":\"Cardiologia\",\"fechaCita\":\"2026-06-15\",\"sede\":\"Centro Norte\",\"valor\":45.50}"

Probar cita inválida:

curl -X POST http://localhost:8080/api/citas -H "Content-Type: application/json" -d "{\"idCita\":\"CITA-1005\",\