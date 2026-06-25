package org.Ezone.POO.TestVocabulario.model;

import java.math.*;

import javax.persistence.*;
import javax.validation.constraints.*;

import org.openxava.annotations.*;
import org.openxava.model.*;

import lombok.*;

@Entity
@Getter @Setter
@View(members = "prueba; puntajeMinimo, puntajeMaximo; notaCalculada")
public class RangoBaremacion extends Identifiable {

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @DescriptionsList(descriptionProperties="nombre")
    @Required
    PruebaVocabulario prueba;

    @Min(0) @Required
    int puntajeMinimo;

    @Min(0) @Required
    int puntajeMaximo;

    @Column(precision=6, scale=2, nullable=false)
    @DecimalMin("0.00") @Required
    BigDecimal notaCalculada;

    @Hidden
    @AssertTrue(message="El puntaje minimo no puede ser mayor que el puntaje maximo")
    public boolean isRangoValido() {
        return puntajeMinimo <= puntajeMaximo;
    }
}
