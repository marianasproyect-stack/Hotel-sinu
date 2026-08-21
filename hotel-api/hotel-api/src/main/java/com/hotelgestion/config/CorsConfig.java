package com.hotelgestion.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Habilita CORS para toda la API.
 *
 * Esto es necesario porque el frontend (carpeta "static") a veces se abre
 * con otro servidor distinto al de Spring Boot -por ejemplo con la
 * extension "Live Server" de VS Code, que sirve en 127.0.0.1:5500-,
 * mientras que la API corre en el puerto 8081 (ver application.properties).
 * Para el navegador esos son dos "origenes" distintos, y sin esta
 * configuracion bloquea las peticiones desde el frontend hacia la API
 * (aparece como error de conexion aunque la API si este corriendo).
 *
 * Nota: esto permite cualquier origen porque es un proyecto academico. En
 * un proyecto real de produccion se deberia restringir "allowedOrigins" al
 * dominio exacto del frontend.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false);
    }
}
