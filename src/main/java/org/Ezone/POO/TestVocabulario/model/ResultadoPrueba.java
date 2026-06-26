package org.Ezone.POO.TestVocabulario.model;

import java.math.*;
import java.time.*;

import javax.persistence.*;

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
    int totalPreguntas;

    @Hidden
    int respuestasCorrectas;

    @Hidden
    int respuestasIncorrectas;

    @Hidden
    int respuestasOmitidas;

    @ReadOnly
    int puntajeDirecto;

    @ReadOnly
    BigDecimal notaFinal;

    @ReadOnly
    int cantidadCorrectas;

    @ReadOnly
    int cantidadIncorrectas;

    @ReadOnly
    int cantidadNoSe;

    @ReadOnly
    int cantidadOmitidas;

    @Hidden
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
