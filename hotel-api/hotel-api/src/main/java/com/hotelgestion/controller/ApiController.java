package com.hotelgestion.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoint raiz de la API. Expone un mapa con todos los recursos
 * disponibles y su ruta base, a modo de indice/documentacion rapida
 * (igual que GET /api en la Concesionaria API).
 *
 * GET /api
 */
@RestController
public class ApiController {

    @GetMapping({"/", "/api"})
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, String> recursos = new LinkedHashMap<>();
        recursos.put("auth", "/api/auth (registro y login)");
        recursos.put("clientes", "/api/clientes");
        recursos.put("habitaciones", "/api/habitaciones");
        recursos.put("reservas", "/api/reservas");
        recursos.put("pagos", "/api/pagos");
        recursos.put("resenas", "/api/resenas");
        recursos.put("dashboard", "/api/dashboard");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("api", "Hotel API");
        body.put("version", "1.0.0");
        body.put("descripcion", "API REST - Sistema de Gestion Hotelera");
        body.put("recursos", recursos);

        return ResponseEntity.ok(body);
    }
}
