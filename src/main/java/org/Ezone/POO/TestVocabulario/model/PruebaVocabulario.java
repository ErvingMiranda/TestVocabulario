package org.Ezone.POO.TestVocabulario.model;

import java.time.*;
import java.util.*;

import javax.persistence.*;
import javax.validation.constraints.*;

import org.Ezone.POO.TestVocabulario.enums.*;
import org.openxava.annotations.*;
import org.openxava.model.*;

import lombok.*;

@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames="codigo"))
@Getter @Setter
@View(members =
    "ConfiguracionGeneral { codigo; nombre; descripcion; estadoPrueba; psicologo; tiempoLimiteMinutos; fechaCreacion };" +
    "Preguntas { preguntas };" +
    "Baremacion { rangosBaremacion }"
)
@Tab(properties="codigo, nombre, estadoPrueba, psicologo.nombres, psicologo.apellidos, tiempoLimiteMinutos, fechaCreacion")
public class PruebaVocabulario extends Identifiable {

    @OneToMany(mappedBy="prueba")
    @OrderBy("numero")
    @ListProperties("numero, enunciado, ejemplo, puntuable, activa")
    Collection<PreguntaVocabulario> preguntas;

    @OneToMany(mappedBy="prueba")
    @OrderBy("puntajeMinimo")
    @ListProperties("puntajeMinimo, puntajeMaximo, notaCalculada")
    Collection<RangoBaremacion> rangosBaremacion;

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
    int tiempoLimiteMinutos = 6;

    LocalDate fechaCreacion = LocalDate.now();

    public boolean activa() {
        return EstadoPrueba.ACTIVA.equals(estadoPrueba);
    }
}
