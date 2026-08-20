package com.hotelgestion.controller;

import com.hotelgestion.dto.CanjeDescuentoResponseDTO;
import com.hotelgestion.dto.PuntosNivelesResponseDTO;
import com.hotelgestion.dto.ReservaResponseDTO;
import com.hotelgestion.model.Cliente;
import com.hotelgestion.model.PuntosFidelidad;
import com.hotelgestion.service.ClienteService;
import com.hotelgestion.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;
    private final ReservaService reservaService;

    // GET /api/clientes
    @GetMapping
    public ResponseEntity<List<Cliente>> listar() {
        return ResponseEntity.ok(clienteService.listar());
    }

    // GET /api/clientes/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Cliente> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.obtenerPorId(id));
    }

    // POST /api/clientes
    @PostMapping
    public ResponseEntity<Cliente> crear(@Valid @RequestBody Cliente cliente) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.crear(cliente));
    }

    // PUT /api/clientes/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Cliente> actualizar(@PathVariable Long id, @Valid @RequestBody Cliente datos) {
        return ResponseEntity.ok(clienteService.actualizar(id, datos));
    }

    // DELETE /api/clientes/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        clienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/clientes/{id}/reservas — historial completo
    @GetMapping("/{id}/reservas")
    public ResponseEntity<List<ReservaResponseDTO>> reservas(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.obtenerPorCliente(id));
    }

    // GET /api/clientes/{id}/puntos — saldo de fidelidad
    @GetMapping("/{id}/puntos")
    public ResponseEntity<PuntosFidelidad> puntos(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.obtenerPuntos(id));
    }

    // POST /api/clientes/{id}/puntos/canjear — canjear puntos
    // Body: { "puntos": 100 }
    @PostMapping("/{id}/puntos/canjear")
    public ResponseEntity<PuntosFidelidad> canjearPuntos(@PathVariable Long id,
                                                          @RequestBody Map<String, Integer> body) {
        int puntos = body.getOrDefault("puntos", 0);
        return ResponseEntity.ok(clienteService.canjearPuntos(id, puntos));
    }

    // GET /api/clientes/{id}/puntos/niveles — niveles de descuento
    // disponibles para canjear con puntos, usado por el modal "Canjear"
    @GetMapping("/{id}/puntos/niveles")
    public ResponseEntity<PuntosNivelesResponseDTO> niveles(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.obtenerNiveles(id));
    }

    // POST /api/clientes/{id}/puntos/canjear-descuento — canjea puntos por
    // un descuento que se aplicara automaticamente en la siguiente reserva.
    // Body: { "nivel": 1 }
    @PostMapping("/{id}/puntos/canjear-descuento")
    public ResponseEntity<CanjeDescuentoResponseDTO> canjearDescuento(@PathVariable Long id,
                                                                      @RequestBody Map<String, Integer> body) {
        int nivel = body.getOrDefault("nivel", 0);
        return ResponseEntity.ok(clienteService.canjearDescuento(id, nivel));
    }
}
