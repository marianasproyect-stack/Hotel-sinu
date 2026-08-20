# Hotel Sinú - Proyecto base (CRUD JPA + Oracle + login web)

## Antes de correr el proyecto

1. Abre `src/main/resources/application.properties` y coloca tu usuario, contraseña y el `SID`/`SERVICE_NAME` real de tu Oracle.
2. No necesitas crear tablas manualmente: `spring.jpa.hibernate.ddl-auto=update` hace que Hibernate cree/actualice las tablas automáticamente al arrancar la app (usa `CLIENTES`, `HABITACIONES`, `RESERVAS`, `PAGOS`, `ROLES`, `USUARIOS`, más sus secuencias). Al arrancar también se ejecuta `data.sql`, que crea los roles `CLIENTE`/`ADMINISTRADOR` y 4 habitaciones de ejemplo si todavía no existen.
3. Corre el proyecto: `mvn spring-boot:run` (o el botón Run desde VS Code/tu IDE).
4. Abre el navegador en **http://localhost:8081** (el puerto está configurado en `application.properties`, línea `server.port`). Ahí está la página con el login, el registro, el panel del cliente y el panel del administrador — no hace falta Postman para probar esta parte, aunque los endpoints REST siguen funcionando igual si quieres usarlos ahí.

## Docker y Render

El proyecto incluye `Dockerfile`, `docker-compose.yml` y `.dockerignore`.

### Ejecutar con Docker Compose

Desde la carpeta que contiene `pom.xml`:

```bash
docker compose up --build
```

La aplicación quedará disponible en `http://localhost:8081`. Compose usa por defecto una instancia Oracle en el host (`host.docker.internal:1521:XE`). Para cambiarla, define estas variables antes de iniciar:

```bash
SPRING_DATASOURCE_URL=jdbc:oracle:thin:@host.docker.internal:1521:XE
SPRING_DATASOURCE_USERNAME=hotel
SPRING_DATASOURCE_PASSWORD=tu_password
docker compose up --build
```

### Publicar en Render

1. Sube este repositorio a GitHub y crea un servicio **New > Web Service** en Render.
2. Selecciona el repositorio y el entorno **Docker**. Render detectará el `Dockerfile` automáticamente.
3. En **Environment**, agrega las variables `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` con los datos de una base Oracle accesible desde Internet.
4. No fijes `PORT`: Render la proporciona automáticamente y la aplicación ya la utiliza.
5. Crea el servicio. Render construirá la imagen y ejecutará el JAR incluido en el `Dockerfile`.

Render no proporciona una base de datos Oracle administrada; la instancia Oracle debe estar alojada externamente y permitir conexiones desde Render.

## Login y registro (nuevo)

- El frontend está en `src/main/resources/static` (`index.html`, `css/styles.css`, `js/app.js`). Spring Boot lo sirve automáticamente en la raíz `/`, en el mismo puerto que la API — por eso no hace falta configurar CORS ni otro servidor aparte.
- Al registrarte eliges el rol (Cliente o Administrador). Si eliges Cliente, por debajo también se crea un registro en `CLIENTES` (con sus puntos de fidelidad en cero) para poder reservar.
- Endpoints nuevos:
  - `POST /api/auth/registro` → `{ "nombre", "apellido", "correo", "contrasena", "rol" }`
  - `POST /api/auth/login` → `{ "correo", "contrasena" }`
- La contraseña se guarda con un hash SHA-256 (sencillo, para el alcance del proyecto — no es lo que se usaría en un sistema real en producción).

## Entidades incluidas

- **Cliente**: nombre, apellido, correo (único), teléfono, documento.
- **Habitacion**: numero (único), tipo, capacidad, precioNoche, descripcion, estado (enum).
- **Reserva**: cliente, habitacion, fechaEntrada, fechaSalida, estado (enum).
- **Pago**: reserva, monto, metodoPago, estado (enum).

## Endpoints disponibles (todos con GET, GET/{id}, POST, PUT/{id}, DELETE/{id})

- `/api/clientes`
- `/api/habitaciones`
- `/api/reservas`
- `/api/pagos`

## Ejemplos de body para Postman

### POST /api/clientes
```json
{
  "nombre": "Juan",
  "apellido": "Perez",
  "correo": "juan.perez@correo.com",
  "telefono": "3001234567",
  "documento": "1002003004"
}
```

### POST /api/habitaciones
```json
{
  "numero": "101",
  "tipo": "Doble",
  "capacidad": 2,
  "precioNoche": 150000,
  "descripcion": "Habitacion doble con balcon",
  "estado": "DISPONIBLE"
}
```
Valores válidos de `estado`: `DISPONIBLE`, `RESERVADA`, `OCUPADA`, `PENDIENTE_LIMPIEZA`, `EN_LIMPIEZA`, `FUERA_DE_SERVICIO`.

### POST /api/reservas
```json
{
  "cliente": { "id": 1 },
  "habitacion": { "id": 1 },
  "fechaEntrada": "2026-08-20",
  "fechaSalida": "2026-08-25",
  "estado": "PENDIENTE"
}
```
Valores válidos de `estado`: `PENDIENTE`, `CONFIRMADA`, `EN_CURSO`, `FINALIZADA`, `CANCELADA`.

### POST /api/pagos
```json
{
  "reserva": { "id": 1 },
  "monto": 750000,
  "metodoPago": "Tarjeta",
  "estado": "PAGADO"
}
```
Valores válidos de `estado`: `PENDIENTE`, `PAGADO`, `RECHAZADO`, `REEMBOLSADO`.

## Pruebas típicas en Postman

- `201 Created` -> creas cliente/habitacion/reserva/pago correctamente.
- `409 Conflict` -> intentas crear un cliente con correo repetido, o una habitacion con numero repetido.
- `400 Bad Request` -> faltan campos obligatorios, o fechaSalida <= fechaEntrada en una reserva.
- `204 No Content` -> DELETE exitoso.
- `404 Not Found` -> consultas o editas un id que no existe.

## Notas sobre Oracle

- Se usa `GenerationType.SEQUENCE` (no `IDENTITY`) en todos los IDs para evitar los problemas de compatibilidad con Oracle que tuviste en el taller de biblioteca.
- Si tu Oracle es una version antigua (pre-12c), este esquema funciona igual sin cambios.
