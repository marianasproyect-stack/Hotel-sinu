package com.hotelgestion.controller;

import com.hotelgestion.dto.ResenaRequestDTO;
import com.hotelgestion.dto.ResenaResponseDTO;
import com.hotelgestion.service.ResenaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/resenas")
@RequiredArgsConstructor
public class ResenaController {

    private final ResenaService resenaService;

    // GET /api/resenas
    @GetMapping
    public ResponseEntity<List<ResenaResponseDTO>> listar() {
        return ResponseEntity.ok(resenaService.listar());
    }

    // GET /api/resenas/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ResenaResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(resenaService.obtenerPorId(id));
    }

    // GET /api/resenas/cliente/{clienteId} — reseñas de un cliente
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<ResenaResponseDTO>> porCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(resenaService.obtenerPorCliente(clienteId));
    }

    // GET /api/resenas/promedio — calificacion promedio general del hotel
    @GetMapping("/promedio")
    public ResponseEntity<Map<String, Double>> promedioGeneral() {
        return ResponseEntity.ok(Map.of("promedioGeneral", resenaService.promedioGeneral()));
    }

    // POST /api/resenas
    // Body: { "clienteId": 1, "reservaId": 3, "califGeneral": 5, "califLimpieza": 4,
    //         "califAtencion": 5, "califDesayuno": 3, "califInstalaciones": 4, "comentario": "..." }
    @PostMapping
    public ResponseEntity<ResenaResponseDTO> crear(@Valid @RequestBody ResenaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resenaService.crear(dto));
    }
}
