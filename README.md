# Hotel Sinu

Sistema de gestion hotelera con un backend REST en Spring Boot, un frontend web estatico y persistencia en PostgreSQL.

## Estructura

- `hotel-api/hotel-api`: API desarrollada con Spring Boot, Java 21, Maven y PostgreSQL.
- `static/static`: frontend estatico servido por Nginx.
- `docker-compose.yml`: orquesta el backend y el frontend.

## Requisitos

Para ejecutar el proyecto con Docker necesitas:

- Docker Desktop con Docker Compose.

Ya no necesitas una base de datos externa: `docker-compose.yml` levanta un contenedor de PostgreSQL (`hotel-db`) junto con el backend. El backend usa por defecto:

```text
URL: jdbc:postgresql://hotel-db:5432/hotel
Usuario: hotel
Contrasena: ho123
```

Cambia estos valores (o las variables `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`) si quieres usar otra base, usuario o contrasena.

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

Puedes personalizar los puertos y la conexion a la base antes de iniciar. En PowerShell:

```powershell
$env:BACKEND_PORT="8081"
$env:FRONTEND_PORT="8080"
$env:DB_PORT="5432"
$env:POSTGRES_DB="hotel"
$env:POSTGRES_USER="hotel"
$env:POSTGRES_PASSWORD="tu_password"
docker compose up --build
```

`BACKEND_PORT` y `FRONTEND_PORT` son los puertos publicados en el equipo local. La aplicacion Spring Boot escucha internamente en el puerto `8081`.

## Ejecutar el backend sin Docker

Desde `hotel-api/hotel-api`:

```bash
mvn spring-boot:run
```

En este caso, necesitas una instancia de PostgreSQL accesible (local o remota) y debes configurar primero la conexion en `src/main/resources/application.properties` (o mediante las variables de entorno `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`).

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
4. Crea una base de datos **PostgreSQL** en Render ("New +" -> "PostgreSQL"), gratuita o del plan que prefieras.
5. En el Web Service del backend, agrega las variables de entorno `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` con los datos de conexion **externa** ("External Database URL") que Render te da para esa base. La URL de JDBC tiene esta forma:

```text
jdbc:postgresql://<host-externo-de-render>:5432/<nombre_bd>?sslmode=require
```

`sslmode=require` es necesario porque las bases de Render exigen SSL.

El error `502 Bad Gateway` normalmente significa que el proceso no esta escuchando en el puerto `PORT` o que se detuvo durante el arranque. Revisa los logs de Render: si aparece `UnknownHostException`, `Network is unreachable`, `Connection refused` o errores de SSL, corrige primero la URL, las credenciales o el estado de la base de datos de Render.

El frontend estatico debe desplegarse como un servicio separado con directorio publicado `static/static`. Antes de desplegarlo, cambia `static/static/js/app.js` para que `API` apunte a la URL publica del Web Service del backend, por ejemplo:

```javascript
const API = "https://tu-backend.onrender.com/api";
```
