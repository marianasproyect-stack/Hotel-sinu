package com.hotelgestion.repository;

import com.hotelgestion.model.PuntosFidelidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PuntosFidelidadRepository extends JpaRepository<PuntosFidelidad, Long> {

    Optional<PuntosFidelidad> findByClienteId(Long clienteId);

    List<PuntosFidelidad> findByCategoriaIn(List<String> categorias);
}
