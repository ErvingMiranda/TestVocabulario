package org.Ezone.POO.TestVocabulario.model;

import java.time.*;
import java.util.Collection;

import javax.persistence.*;
import javax.validation.constraints.*;

import org.Ezone.POO.TestVocabulario.enums.*;
import org.openxava.annotations.*;
import org.openxava.model.*;

import lombok.*;

@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames="codigoAplicacion"))
@Getter @Setter
@View(members =
        "EvaluadoYPrueba { evaluado; prueba; codigoAplicacion };" +
        "Seguimiento { estadoIntento; numeroRespuestas; fechaInicio, fechaFin; fechaCreacion };" +
        "RespuestasRegistradas { respuestas }"
)
@Tab(properties="evaluado.nombres, evaluado.apellidos, prueba.nombre, estadoIntento, codigoAplicacion, fechaInicio, fechaFin, numeroRespuestas")
public class IntentoPrueba extends Identifiable {

    @OneToMany(mappedBy="intento", cascade=CascadeType.ALL, orphanRemoval=true)
    @ReadOnly
    @ListProperties("pregunta.numero, opcionSeleccionada.letra, clasificacionRespuesta, correcta, fechaRespuesta")
    Collection<RespuestaEvaluado> respuestas;

    @OneToOne(mappedBy="intento", cascade=CascadeType.REMOVE, orphanRemoval=true)
    ResultadoPrueba resultado;

    @Column(length=40, nullable=false) @Required
    @NotBlank(message="El código de aplicación es requerido")
    @Size(max=40, message="El código de aplicación no puede exceder 40 caracteres")
    @Pattern(regexp="^[A-Za-z0-9._-]+$", message="El código de aplicación solo puede contener letras, números, punto, guion y guion bajo")
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

    @Hidden
    @AssertTrue(message="La fecha de fin no puede ser anterior a la fecha de inicio")
    public boolean isFechasCoherentes() {
        return fechaInicio == null || fechaFin == null || !fechaFin.isBefore(fechaInicio);
    }
}
