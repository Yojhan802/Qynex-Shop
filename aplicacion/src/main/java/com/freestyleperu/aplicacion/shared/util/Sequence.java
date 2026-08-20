package com.freestyleperu.aplicacion.shared.util;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Correlativos con nombre (SKU, código interno, código de barras...).
 * Se lee con bloqueo pesimista para que dos operaciones concurrentes nunca
 * obtengan el mismo número (ver docs/03-modelo-datos.md §11).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sequences")
public class Sequence {

    @Id
    @Column(name = "name", length = 40)
    private String name;

    @Column(name = "prefix", length = 10)
    private String prefix;

    @Column(name = "current_value", nullable = false)
    private long currentValue;

    @Column(name = "padding", nullable = false)
    private int padding;
}
