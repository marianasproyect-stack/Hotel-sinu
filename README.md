# Hotel Sinu

Sistema de gestion hotelera con un backend REST en Spring Boot, un frontend web estatico y persistencia en Oracle.

## Estructura

- `hotel-api/hotel-api`: API desarrollada con Spring Boot, Java 21, Maven y Oracle.
- `static/static`: frontend estatico servido por Nginx.
- `docker-compose.yml`: orquesta el backend y el frontend.

## Requisitos

Para ejecutar el proyecto con Docker necesitas:

- Docker Desktop con Docker Compose.
- Una instancia Oracle accesible desde el backend.

El backend usa por defecto:

```text
URL: jdbc:oracle:thin:@host.docker.internal:1521:XE
Usuario: hotel
Contrasena: ho123
```

Cambia estos valores si tu instancia Oracle utiliza otra direccion, servicio, usuario o contrasena.

## Ejecutar con Docker Compose

Desde la raiz del proyecto:

```bash
docker compose up --build
```

Servicios disponibles:

- Frontend: http://localhost:8080
- Backend: http://localhost:8081

Para detener los servicios:

```bash
docker compose down
```

### Variables de entorno

Puedes personalizar los puertos y la conexion a Oracle antes de iniciar. En PowerShell:

```powershell
$env:BACKEND_PORT="8081"
$env:FRONTEND_PORT="8080"
$env:SPRING_DATASOURCE_URL="jdbc:oracle:thin:@host.docker.internal:1521:XE"
$env:SPRING_DATASOURCE_USERNAME="hotel"
$env:SPRING_DATASOURCE_PASSWORD="tu_password"
docker compose up --build
```

`BACKEND_PORT` y `FRONTEND_PORT` son los puertos publicados en el equipo local. La aplicacion Spring Boot escucha internamente en el puerto `8081`.

## Ejecutar el backend sin Docker

Desde `hotel-api/hotel-api`:

```bash
mvn spring-boot:run
```

En este caso, configura primero la conexion a Oracle en `src/main/resources/application.properties`.

## Funcionalidades

- Registro e inicio de sesion.
- Gestion de clientes, habitaciones, reservas y pagos.
- Check-in y check-out.
- Resenas.
- Dashboard administrativo.
- Puntos de fidelidad y canje de descuentos.

## API

Endpoints principales:

- `/api/auth`
- `/api/clientes`
- `/api/habitaciones`
- `/api/reservas`
- `/api/pagos`
- `/api/resenas`
- `/api/dashboard`

La documentacion detallada del backend se encuentra en `hotel-api/hotel-api/README.md`.

## Dockerfiles

Cada parte mantiene su propio Dockerfile:

- Backend: `hotel-api/hotel-api/Dockerfile`
- Frontend: `static/static/Dockerfile`

El compose raiz utiliza esos Dockerfiles mediante sus respectivos contextos de construccion.

## Despliegue en Render

Render debe desplegar el backend como un **Web Service** de tipo Docker:

1. Usa este repositorio y configura como **Root Directory** `hotel-api/hotel-api`.
2. Selecciona **Docker** y usa `hotel-api/hotel-api/Dockerfile`.
3. No establezcas manualmente `PORT`; Render lo proporciona y Spring Boot lo utiliza automaticamente.
4. Agrega `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` en las variables de entorno.
5. La base Oracle debe estar alojada externamente, ser accesible desde Internet y permitir conexiones desde Render. `localhost` y `host.docker.internal` no funcionan para una base ubicada en tu computador.

El error `502 Bad Gateway` normalmente significa que el proceso no esta escuchando en el puerto `PORT` o que se detuvo durante el arranque. Revisa los logs de Render: si aparece `UnknownHostException`, `Network is unreachable` o `The Network Adapter could not establish the connection`, corrige primero la URL, el listener, el firewall o la disponibilidad de Oracle.

El frontend estatico debe desplegarse como un servicio separado con directorio publicado `static/static`. Antes de desplegarlo, cambia `static/static/js/app.js` para que `API` apunte a la URL publica del Web Service del backend, por ejemplo:

```javascript
const API = "https://tu-backend.onrender.com/api";
```
