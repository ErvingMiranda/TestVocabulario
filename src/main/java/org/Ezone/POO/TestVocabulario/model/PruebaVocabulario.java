package org.Ezone.POO.TestVocabulario.model;

import java.time.*;

import javax.persistence.*;
import javax.validation.constraints.*;

import org.Ezone.POO.TestVocabulario.enums.*;
import org.openxava.annotations.*;
import org.openxava.model.*;

import lombok.*;

@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames="codigo"))
@Getter @Setter
public class PruebaVocabulario extends Identifiable {

    @Column(length=30, nullable=false) @Required
    String codigo;

    @Column(length=100, nullable=false) @Required
    String nombre;

    @Stereotype("MEMO")
    String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    @NotNull @Required
    EstadoPrueba estadoPrueba = EstadoPrueba.BORRADOR;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @DescriptionsList(descriptionProperties="nombres, apellidos")
    @Required
    Psicologo psicologo;

    @Min(1) @Required
    int tiempoLimiteMinutos;

    LocalDate fechaCreacion = LocalDate.now();

    public boolean activa() {
        return EstadoPrueba.ACTIVA.equals(estadoPrueba);
    }
}
