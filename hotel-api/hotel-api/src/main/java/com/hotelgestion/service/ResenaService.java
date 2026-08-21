package com.hotelgestion.service;

import com.hotelgestion.dto.ResenaRequestDTO;
import com.hotelgestion.dto.ResenaResponseDTO;
import com.hotelgestion.enums.EstadoReserva;
import com.hotelgestion.model.Cliente;
import com.hotelgestion.model.Resena;
import com.hotelgestion.model.Reserva;
import com.hotelgestion.repository.ClienteRepository;
import com.hotelgestion.repository.ResenaRepository;
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
public class ResenaService {

    private final ResenaRepository resenaRepository;
    private final ClienteRepository clienteRepository;
    private final ReservaRepository reservaRepository;
    private final ClienteService clienteService;

    private static final int PUNTOS_RESENA = 20;

    public List<ResenaResponseDTO> listar() {
        return resenaRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public ResenaResponseDTO obtenerPorId(Long id) {
        return toDTO(buscarResena(id));
    }

    public List<ResenaResponseDTO> obtenerPorCliente(Long clienteId) {
        return resenaRepository.findByClienteId(clienteId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ResenaResponseDTO crear(ResenaRequestDTO dto) {
        Cliente cliente = clienteRepository.findById(dto.getClienteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));

        Reserva reserva = reservaRepository.findById(dto.getReservaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));

        // Solo se puede reseñar una reserva finalizada
        if (reserva.getEstado() != EstadoReserva.FINALIZADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Solo se puede reseñar una reserva FINALIZADA. Estado actual: " + reserva.getEstado());
        }

        // Verificar que el cliente sea el de la reserva
        if (!reserva.getCliente().getId().equals(cliente.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El cliente no pertenece a esta reserva");
        }

        // Una reserva solo puede tener una reseña
        if (resenaRepository.findByClienteIdAndReservaId(dto.getClienteId(), dto.getReservaId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una reseña para esta reserva");
        }

        Resena resena = new Resena();
        resena.setCliente(cliente);
        resena.setReserva(reserva);
        resena.setCalifGeneral(dto.getCalifGeneral());
        resena.setCalifLimpieza(dto.getCalifLimpieza());
        resena.setCalifAtencion(dto.getCalifAtencion());
        resena.setCalifDesayuno(dto.getCalifDesayuno());
        resena.setCalifInstalaciones(dto.getCalifInstalaciones());
        resena.setComentario(dto.getComentario());

        Resena guardada = resenaRepository.save(resena);

        // Sumar puntos por dejar reseña
        clienteService.sumarPuntos(cliente.getId(), PUNTOS_RESENA);

        return toDTO(guardada);
    }

    public Double promedioGeneral() {
        Double promedio = resenaRepository.promedioGeneral();
        return promedio != null ? Math.round(promedio * 10.0) / 10.0 : 0.0;
    }

    private Resena buscarResena(Long id) {
        return resenaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reseña no encontrada"));
    }

    private ResenaResponseDTO toDTO(Resena r) {
        ResenaResponseDTO dto = new ResenaResponseDTO();
        dto.setId(r.getId());
        dto.setClienteId(r.getCliente().getId());
        dto.setClienteNombre(r.getCliente().getNombre() + " " + r.getCliente().getApellido());
        dto.setReservaId(r.getReserva().getId());
        dto.setCalifGeneral(r.getCalifGeneral());
        dto.setCalifLimpieza(r.getCalifLimpieza());
        dto.setCalifAtencion(r.getCalifAtencion());
        dto.setCalifDesayuno(r.getCalifDesayuno());
        dto.setCalifInstalaciones(r.getCalifInstalaciones());
        dto.setComentario(r.getComentario());
        dto.setFechaResena(r.getFechaResena());
        return dto;
    }
}
