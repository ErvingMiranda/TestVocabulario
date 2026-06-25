package org.Ezone.POO.TestVocabulario.model;

import java.time.*;
import java.util.Collection;

import javax.persistence.*;

import org.Ezone.POO.TestVocabulario.enums.*;
import org.openxava.annotations.*;
import org.openxava.model.*;

import lombok.*;

@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames="codigoAplicacion"))
@Getter @Setter
@View(members =
        "codigoAplicacion, estadoIntento;" +
                "fechaInicio, fechaFin;" +
                "respuestas { respuestas }" // CAMBIADO: 'respuestas' coincide con el nombre de la variable
)
public class IntentoPrueba extends Identifiable {

    @OneToMany(mappedBy="intento", cascade=CascadeType.ALL)
    Collection<RespuestaEvaluado> respuestas;

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
