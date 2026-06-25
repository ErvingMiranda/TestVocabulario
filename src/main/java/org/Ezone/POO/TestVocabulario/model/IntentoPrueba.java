package org.Ezone.POO.TestVocabulario.model;

import java.time.*;

import javax.persistence.*;

import org.Ezone.POO.TestVocabulario.enums.*;
import org.openxava.annotations.*;
import org.openxava.model.*;

import lombok.*;

@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames="codigoAplicacion"))
@View(members =
        "codigoAplicacion, estadoIntento;" +
                "fechaInicio, fechaFin;" +
                "respuestas { respuestas }" // Aquí se listarán las 77 preguntas
)
@Getter @Setter
public class IntentoPrueba extends Identifiable {

    @Column(length=40, nullable=false) @Required
    String codigoAplicacion;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @DescriptionsList(descriptionProperties="nombre")
    @Required
    PruebaVocabulario prueba;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @DescriptionsList(descriptionProperties="nombres, apellidos")
    @Required
    Evaluado evaluado;

    @Enumerated(EnumType.STRING)
    EstadoIntento estadoIntento = EstadoIntento.PENDIENTE;

    LocalDateTime fechaCreacion = LocalDateTime.now();

    LocalDateTime fechaInicio;

    LocalDateTime fechaFin;

    int numeroRespuestas;
}
