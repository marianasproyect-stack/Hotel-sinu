package com.hotelgestion.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PUNTOS_FIDELIDAD")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PuntosFidelidad {

    @Id
    @SequenceGenerator(name = "puntos_seq", sequenceName = "puntos_seq", allocationSize = 1, initialValue = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "puntos_seq")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLIENTE_ID", nullable = false, unique = true)
    private Cliente cliente;

    @Column(name = "PUNTOS_TOTALES", nullable = false)
    private Integer puntosTotales = 0;

    @Column(name = "PUNTOS_CANJEADOS", nullable = false)
    private Integer puntosCanjeados = 0;

    @Column(name = "CATEGORIA", length = 30)
    private String categoria = "ESTANDAR"; // ESTANDAR, PLATA, ORO, PLATINUM

    // Descuento (en fraccion, ej. 0.10 = 10%) que el cliente canjeo con sus
    // puntos y que todavia no ha usado en ninguna reserva. Se aplica
    // automaticamente en la SIGUIENTE reserva que haga (ver
    // ReservaService.crear) y luego vuelve a quedar en 0.
    @Column(name = "DESCUENTO_DISPONIBLE")
    private Double descuentoDisponible = 0.0;

    // Fecha del ultimo canje de puntos por descuento, para controlar el
    // tiempo de espera (cooldown) entre canjes.
    @Column(name = "FECHA_ULTIMO_CANJE")
    private java.time.LocalDate fechaUltimoCanje;
}
