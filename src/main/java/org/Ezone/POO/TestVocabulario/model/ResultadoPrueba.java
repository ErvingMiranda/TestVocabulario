package org.Ezone.POO.TestVocabulario.model;

import java.math.*;
import java.time.*;

import javax.persistence.*;
import javax.validation.constraints.*;

import org.openxava.annotations.*;
import org.openxava.model.*;

import lombok.*;

@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames="intento_id"))
@Getter @Setter
@View(members =
        "DatosResultado { intento; fechaCalculo; tiempoDuracionMinutos };" +
        "Calificacion { puntajeDirecto, notaFinal; cantidadCorrectas, cantidadIncorrectas; cantidadOmitidas, cantidadNoSe };" +
        "Interpretacion { interpretacion }"
)
@Tab(properties="intento.codigoAplicacion, intento.evaluado.nombres, intento.evaluado.apellidos, fechaCalculo, puntajeDirecto, notaFinal, cantidadCorrectas, cantidadIncorrectas, cantidadNoSe, cantidadOmitidas, interpretacion")
public class ResultadoPrueba extends Identifiable {

    @OneToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="intento_id", nullable=false)
    @DescriptionsList(descriptionProperties="codigoAplicacion")
    @Required
    IntentoPrueba intento;

    @Hidden
    @Min(value=0, message="El total de preguntas no puede ser negativo")
    int totalPreguntas;

    @Hidden
    @Min(value=0, message="Las respuestas correctas no pueden ser negativas")
    int respuestasCorrectas;

    @Hidden
    @Min(value=0, message="Las respuestas incorrectas no pueden ser negativas")
    int respuestasIncorrectas;

    @Hidden
    @Min(value=0, message="Las respuestas omitidas no pueden ser negativas")
    int respuestasOmitidas;

    @ReadOnly
    @Min(value=0, message="El puntaje directo no puede ser negativo")
    int puntajeDirecto;

    @ReadOnly
    @DecimalMin(value="0.00", message="La nota final no puede ser negativa")
    BigDecimal notaFinal;

    @ReadOnly
    @Min(value=0, message="La cantidad de correctas no puede ser negativa")
    int cantidadCorrectas;

    @ReadOnly
    @Min(value=0, message="La cantidad de incorrectas no puede ser negativa")
    int cantidadIncorrectas;

    @ReadOnly
    @Min(value=0, message="La cantidad de NO_SE no puede ser negativa")
    int cantidadNoSe;

    @ReadOnly
    @Min(value=0, message="La cantidad de omitidas no puede ser negativa")
    int cantidadOmitidas;

    @Hidden
    @DecimalMin(value="0.00", message="El porcentaje no puede ser negativo")
    BigDecimal porcentaje;

    @Stereotype("MEMO")
    @ReadOnly
    String interpretacion;

    @ReadOnly
    LocalDateTime fechaCalculo = LocalDateTime.now();

    @Depends("intento.fechaInicio, intento.fechaFin")
    public long getTiempoDuracionMinutos() {
        if (intento == null || intento.getFechaInicio() == null || intento.getFechaFin() == null) return 0;
        return java.time.Duration.between(intento.getFechaInicio(), intento.getFechaFin()).toMinutes();
    }
}
