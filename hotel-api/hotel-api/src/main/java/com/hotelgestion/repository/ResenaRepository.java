package com.hotelgestion.repository;

import com.hotelgestion.model.Resena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ResenaRepository extends JpaRepository<Resena, Long> {

    List<Resena> findByClienteId(Long clienteId);

    List<Resena> findByReservaId(Long reservaId);

    // Verifica que el cliente ya haya reseñado esa reserva
    Optional<Resena> findByClienteIdAndReservaId(Long clienteId, Long reservaId);

    // Indica si una reserva ya tiene reseña (sin importar el cliente),
    // usado para no volver a mostrar el boton "Dejar reseña" en el frontend.
    // NOTA: se usa count() en vez de existsBy... porque existsBy genera un
    // SQL con "FETCH FIRST 1 ROWS ONLY", que Oracle 11g XE no soporta
    // (da ORA-00933). count() no necesita limitar filas, asi que evita eso.
    long countByReservaId(Long reservaId);

    // Promedio general de todas las reseñas
    @Query("SELECT AVG(r.califGeneral) FROM Resena r")
    Double promedioGeneral();

    // Promedio de desayuno en los últimos N días (para alertas)
    @Query("SELECT AVG(r.califDesayuno) FROM Resena r WHERE r.fechaResena >= :desde")
    Double promedioDesayunoDesde(LocalDate desde);

    @Query("SELECT AVG(r.califLimpieza) FROM Resena r WHERE r.fechaResena >= :desde")
    Double promedioLimpiezaDesde(LocalDate desde);

    @Query("SELECT AVG(r.califAtencion) FROM Resena r WHERE r.fechaResena >= :desde")
    Double promedioAtencionDesde(LocalDate desde);

    @Query("SELECT AVG(r.califInstalaciones) FROM Resena r WHERE r.fechaResena >= :desde")
    Double promedioInstalacionesDesde(LocalDate desde);
}
