package com.hotelgestion.controller;

import com.hotelgestion.enums.EstadoHabitacion;
import com.hotelgestion.model.Habitacion;
import com.hotelgestion.service.HabitacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/habitaciones")
@RequiredArgsConstructor
public class HabitacionController {

    private final HabitacionService habitacionService;

    // GET /api/habitaciones
    // GET /api/habitaciones?estado=DISPONIBLE  — filtra por estado
    @GetMapping
    public ResponseEntity<List<Habitacion>> listar(
            @RequestParam(required = false) EstadoHabitacion estado) {
        return ResponseEntity.ok(habitacionService.listar(estado));
    }

    // GET /api/habitaciones/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Habitacion> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(habitacionService.obtenerPorId(id));
    }

    // POST /api/habitaciones
    @PostMapping
    public ResponseEntity<Habitacion> crear(@Valid @RequestBody Habitacion habitacion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(habitacionService.crear(habitacion));
    }

    // PUT /api/habitaciones/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Habitacion> actualizar(@PathVariable Long id,
                                                  @Valid @RequestBody Habitacion datos) {
        return ResponseEntity.ok(habitacionService.actualizar(id, datos));
    }

    // PATCH /api/habitaciones/{id}/estado — cambio rápido de estado
    // Body: { "estado": "EN_LIMPIEZA" }
    @PatchMapping("/{id}/estado")
    public ResponseEntity<Habitacion> cambiarEstado(@PathVariable Long id,
                                                     @RequestBody Map<String, String> body) {
        EstadoHabitacion nuevoEstado = EstadoHabitacion.valueOf(body.get("estado").toUpperCase());
        return ResponseEntity.ok(habitacionService.cambiarEstado(id, nuevoEstado));
    }

    // DELETE /api/habitaciones/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        habitacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
