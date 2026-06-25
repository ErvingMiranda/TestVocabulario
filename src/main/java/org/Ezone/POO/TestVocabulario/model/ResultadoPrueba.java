package org.Ezone.POO.TestVocabulario.model;

import java.math.*;
import java.time.*;

import javax.persistence.*;

import org.openxava.annotations.*;
import org.openxava.model.*;

import lombok.*;

@Entity
@Getter @Setter
@View(members =
        "intento;" +
                "resumen { totalPreguntas, respuestasCorrectas, respuestasIncorrectas, respuestasOmitidas };" +
                "desempeno { porcentaje, interpretacion };" +
                "fechaCalculo"
)
public class ResultadoPrueba extends Identifiable {

    @OneToOne(fetch=FetchType.LAZY, optional=false)
    @DescriptionsList(descriptionProperties="codigoAplicacion")
    @Required
    IntentoPrueba intento;

    int totalPreguntas;

    int respuestasCorrectas;

    int respuestasIncorrectas;

    int respuestasOmitidas;

    int puntajeDirecto;

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
