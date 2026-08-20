package com.hotelgestion.service;

import com.hotelgestion.dto.PagoRequestDTO;
import com.hotelgestion.dto.PagoResponseDTO;
import com.hotelgestion.enums.EstadoPago;
import com.hotelgestion.enums.EstadoReserva;
import com.hotelgestion.model.Pago;
import com.hotelgestion.model.Reserva;
import com.hotelgestion.repository.PagoRepository;
import com.hotelgestion.repository.ReservaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;
    private final ClienteService clienteService;

    // Puntos otorgados cuando se confirma un pago (estado PAGADO).
    // Antes se daban al reservar, pero eso premiaba reservas que nunca se
    // llegaban a pagar; ahora quedan atados al pago real.
    private static final int PUNTOS_POR_PAGO = 50;

    public List<PagoResponseDTO> listar() {
        return pagoRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public PagoResponseDTO obtenerPorId(Long id) {
        return toDTO(buscarPago(id));
    }

    public List<PagoResponseDTO> obtenerPorReserva(Long reservaId) {
        return pagoRepository.findByReservaId(reservaId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public PagoResponseDTO crear(PagoRequestDTO dto) {
        Reserva reserva = reservaRepository.findById(dto.getReservaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));

        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se puede registrar pago de una reserva cancelada");
        }

        // Si la reserva sigue PENDIENTE pero ya paso el plazo de 24h, se
        // considera vencida (aunque el barrido automatico todavia no la
        // haya marcado CANCELADA) y no se acepta el pago.
        if (reserva.getEstado() == EstadoReserva.PENDIENTE
                && reserva.getFechaLimitePago() != null
                && reserva.getFechaLimitePago().isBefore(java.time.LocalDateTime.now())) {
            reserva.setEstado(EstadoReserva.CANCELADA);
            reservaRepository.save(reserva);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El plazo de 24h para pagar esta reserva ya vencio y se cancelo");
        }

        Pago pago = new Pago();
        pago.setReserva(reserva);
        pago.setMonto(dto.getMonto());
        pago.setMetodoPago(dto.getMetodoPago());
        pago.setEstado(EstadoPago.PAGADO);

        Pago guardado = pagoRepository.save(pago);

        // El pago confirma la reserva (pasa de PENDIENTE a CONFIRMADA).
        if (reserva.getEstado() == EstadoReserva.PENDIENTE) {
            reserva.setEstado(EstadoReserva.CONFIRMADA);
            reservaRepository.save(reserva);
        }

        // Puntos de fidelidad: se dan aqui, al confirmar el pago, no al
        // solo crear la reserva.
        clienteService.sumarPuntos(reserva.getCliente().getId(), PUNTOS_POR_PAGO);

        return toDTO(guardado);
    }

    public PagoResponseDTO actualizarEstado(Long id, EstadoPago nuevoEstado) {
        Pago pago = buscarPago(id);
        EstadoPago estadoAnterior = pago.getEstado();
        pago.setEstado(nuevoEstado);

        // Si se cambia a REEMBOLSADO desde PAGADO, revertir los puntos asignados
        if (estadoAnterior == EstadoPago.PAGADO && nuevoEstado == EstadoPago.REEMBOLSADO) {
            Reserva reserva = pago.getReserva();
            if (reserva != null && reserva.getCliente() != null) {
                // Se devuelven los 50 puntos que se asignaron al pagar
                clienteService.restarPuntos(reserva.getCliente().getId(), PUNTOS_POR_PAGO);
            }
        }

        return toDTO(pagoRepository.save(pago));
    }

    public void eliminar(Long id) {
        if (!pagoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no encontrado");
        }
        pagoRepository.deleteById(id);
    }

    private Pago buscarPago(Long id) {
        return pagoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no encontrado"));
    }

    private PagoResponseDTO toDTO(Pago p) {
        PagoResponseDTO dto = new PagoResponseDTO();
        dto.setId(p.getId());
        dto.setReservaId(p.getReserva().getId());
        dto.setMonto(p.getMonto());
        dto.setMetodoPago(p.getMetodoPago());
        dto.setEstado(p.getEstado());
        dto.setFechaPago(p.getFechaPago());
        return dto;
    }
}
