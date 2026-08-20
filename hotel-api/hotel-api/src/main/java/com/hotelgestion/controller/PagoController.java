package com.hotelgestion.controller;

import com.hotelgestion.dto.PagoRequestDTO;
import com.hotelgestion.dto.PagoResponseDTO;
import com.hotelgestion.enums.EstadoPago;
import com.hotelgestion.service.PagoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;

    // GET /api/pagos
    @GetMapping
    public ResponseEntity<List<PagoResponseDTO>> listar() {
        return ResponseEntity.ok(pagoService.listar());
    }

    // GET /api/pagos/{id}
    @GetMapping("/{id}")
    public ResponseEntity<PagoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pagoService.obtenerPorId(id));
    }

    // GET /api/pagos/reserva/{reservaId} — pagos de una reserva
    @GetMapping("/reserva/{reservaId}")
    public ResponseEntity<List<PagoResponseDTO>> porReserva(@PathVariable Long reservaId) {
        return ResponseEntity.ok(pagoService.obtenerPorReserva(reservaId));
    }

    // POST /api/pagos
    // Body: { "reservaId": 1, "monto": 500.00, "metodoPago": "Tarjeta" }
    @PostMapping
    public ResponseEntity<PagoResponseDTO> crear(@Valid @RequestBody PagoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagoService.crear(dto));
    }

    // PATCH /api/pagos/{id}/estado — actualizar estado del pago
    // Body: { "estado": "REEMBOLSADO" }
    @PatchMapping("/{id}/estado")
    public ResponseEntity<PagoResponseDTO> actualizarEstado(@PathVariable Long id,
                                                              @RequestBody Map<String, String> body) {
        EstadoPago nuevoEstado = EstadoPago.valueOf(body.get("estado").toUpperCase());
        return ResponseEntity.ok(pagoService.actualizarEstado(id, nuevoEstado));
    }

    // DELETE /api/pagos/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        pagoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
