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
        "Intento { intento; fechaCalculo; tiempoDuracionMinutos };" +
        "Calificacion { puntajeDirecto, notaFinal; cantidadCorrectas, cantidadIncorrectas; cantidadNoSe, cantidadOmitidas };" +
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

    int puntajeDirecto;

    BigDecimal notaFinal;

    int cantidadCorrectas;

    int cantidadIncorrectas;

    int cantidadNoSe;

    int cantidadOmitidas;

    @Hidden
    BigDecimal porcentaje;

    @Stereotype("MEMO")
    String interpretacion;

    LocalDateTime fechaCalculo = LocalDateTime.now();

    @Depends("intento.fechaInicio, intento.fechaFin")
    public long getTiempoDuracionMinutos() {
        if (intento == null || intento.getFechaInicio() == null || intento.getFechaFin() == null) return 0;
        return java.time.Duration.between(intento.getFechaInicio(), intento.getFechaFin()).toMinutes();
    }
}
