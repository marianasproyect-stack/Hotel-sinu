package com.hotelgestion.controller;

import com.hotelgestion.dto.CheckInRequestDTO;
import com.hotelgestion.dto.CheckOutRequestDTO;
import com.hotelgestion.dto.ReservaRequestDTO;
import com.hotelgestion.dto.ReservaResponseDTO;
import com.hotelgestion.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    // GET /api/reservas
    @GetMapping
    public ResponseEntity<List<ReservaResponseDTO>> listar() {
        return ResponseEntity.ok(reservaService.listar());
    }

    // GET /api/reservas/cliente/{clienteId} — reservas de un cliente
    // (usado por la vista "Mis Reservas" del panel de cliente).
    // OJO: esta ruta debe declararse ANTES que "/{id}", si no Spring
    // interpreta "cliente" como si fuera el {id} y explota.
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<ReservaResponseDTO>> obtenerPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(reservaService.obtenerPorCliente(clienteId));
    }

    // GET /api/reservas/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ReservaResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.obtenerPorId(id));
    }

    // POST /api/reservas
    // Body: { "clienteId": 1, "habitacionId": 2, "fechaEntrada": "2026-09-01", "fechaSalida": "2026-09-05" }
    @PostMapping
    public ResponseEntity<ReservaResponseDTO> crear(@Valid @RequestBody ReservaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.crear(dto));
    }

    // GET /api/reservas/llegadas-hoy — reservas CONFIRMADAS que entran hoy
    // (para que recepcion sepa a quien atender sin buscar en toda la tabla).
    // OJO: igual que "/cliente/{clienteId}", debe ir ANTES que "/{id}".
    @GetMapping("/llegadas-hoy")
    public ResponseEntity<List<ReservaResponseDTO>> llegadasHoy() {
        return ResponseEntity.ok(reservaService.obtenerLlegadasHoy());
    }

    // GET /api/reservas/salidas-hoy — reservas EN_CURSO que salen hoy.
    @GetMapping("/salidas-hoy")
    public ResponseEntity<List<ReservaResponseDTO>> salidasHoy() {
        return ResponseEntity.ok(reservaService.obtenerSalidasHoy());
    }

    // POST /api/reservas/{id}/checkin
    // Body: { "documentoHuesped": "123456", "numAcompanantes": 1, "observaciones": "..." }
    @PostMapping("/{id}/checkin")
    public ResponseEntity<ReservaResponseDTO> checkIn(@PathVariable Long id, @Valid @RequestBody CheckInRequestDTO dto) {
        return ResponseEntity.ok(reservaService.checkIn(id, dto));
    }

    // POST /api/reservas/{id}/checkout
    // Body (opcional): { "estadoHabitacion": "OK" | "DANOS", "observaciones": "..." }
    @PostMapping("/{id}/checkout")
    public ResponseEntity<ReservaResponseDTO> checkOut(@PathVariable Long id,
            @RequestBody(required = false) CheckOutRequestDTO dto) {
        return ResponseEntity.ok(reservaService.checkOut(id, dto != null ? dto : new CheckOutRequestDTO()));
    }

    // POST /api/reservas/{id}/cancelar
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ReservaResponseDTO> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.cancelar(id));
    }
}
