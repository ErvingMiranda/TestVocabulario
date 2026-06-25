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
    "ConfiguracionGeneral { codigo; nombre; descripcion; estadoPrueba; psicologo; tiempoLimiteMinutos; permiteNoSe; fechaCreacion };" +
    "Preguntas { preguntas };" +
    "Baremacion { rangosBaremacion }"
)
@Tab(properties="codigo, nombre, estadoPrueba, psicologo.nombres, psicologo.apellidos, tiempoLimiteMinutos, permiteNoSe, fechaCreacion")
public class PruebaVocabulario extends Identifiable {

    @OneToMany(mappedBy="prueba", cascade=CascadeType.ALL, orphanRemoval=true)
    @OrderBy("numero")
    @ListProperties("numero, enunciado, puntaje, ejemplo, puntuable, activa")
    Collection<PreguntaVocabulario> preguntas;

    @OneToMany(mappedBy="prueba", cascade=CascadeType.ALL, orphanRemoval=true)
    @OrderBy("puntajeMinimo")
    @ListProperties("puntajeMinimo, puntajeMaximo, notaCalculada, interpretacion")
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

    @Required
    boolean permiteNoSe = true;

    LocalDate fechaCreacion = LocalDate.now();

    public boolean activa() {
        return EstadoPrueba.ACTIVA.equals(estadoPrueba);
    }
}
