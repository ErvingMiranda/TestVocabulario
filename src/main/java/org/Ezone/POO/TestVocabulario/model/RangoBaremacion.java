package org.Ezone.POO.TestVocabulario.model;

import java.math.*;

import javax.persistence.*;
import javax.validation.constraints.*;

import org.openxava.annotations.*;
import org.openxava.jpa.*;
import org.openxava.model.*;

import lombok.*;

@Entity
@Getter @Setter
@View(members = "prueba; puntajeMinimo, puntajeMaximo; notaCalculada")
public class RangoBaremacion extends Identifiable {

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="prueba_id", nullable=false)
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

    @Hidden
    @AssertTrue(message="No puede haber rangos de baremacion solapados para la misma prueba")
    public boolean isSinSolapamiento() {
        if (prueba == null || puntajeMinimo > puntajeMaximo) return true;

        Long total = XPersistence.getManager()
            .createQuery(
                "select count(r) from RangoBaremacion r " +
                    "where r.prueba = :prueba " +
                    "and r.puntajeMinimo <= :puntajeMaximo " +
                    "and r.puntajeMaximo >= :puntajeMinimo " +
                    "and (:id is null or r.id <> :id)",
                Long.class)
            .setParameter("prueba", prueba)
            .setParameter("puntajeMinimo", puntajeMinimo)
            .setParameter("puntajeMaximo", puntajeMaximo)
            .setParameter("id", getId())
            .getSingleResult();
        return total == 0;
    }
}
