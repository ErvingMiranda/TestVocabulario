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
    "Identificacion { nombre; codigo; descripcion };" +
    "ConfiguracionAplicacion { estadoPrueba; tiempoLimiteMinutos, permiteNoSe; psicologo; fechaCreacion };" +
    "BancoPreguntas { preguntas };" +
    "TablaBaremacion { rangosBaremacion }"
)
@Tab(properties="nombre, codigo, estadoPrueba, tiempoLimiteMinutos, permiteNoSe, psicologo.nombres, psicologo.apellidos, fechaCreacion")
public class PruebaVocabulario extends Identifiable {

    @OneToMany(mappedBy="prueba", cascade=CascadeType.ALL, orphanRemoval=true)
    @OrderBy("numero")
    @ListProperties("numero, enunciado, puntaje, activa, ejemplo, puntuable")
    Collection<PreguntaVocabulario> preguntas;

    @OneToMany(mappedBy="prueba", cascade=CascadeType.ALL, orphanRemoval=true)
    @OrderBy("puntajeMinimo")
    @ListProperties("puntajeMinimo, puntajeMaximo, notaCalculada, interpretacion")
    Collection<RangoBaremacion> rangosBaremacion;

    @Column(length=30, nullable=false) @Required
    @NotBlank(message="El código de la prueba es requerido")
    @Size(max=30, message="El código de la prueba no puede exceder 30 caracteres")
    @Pattern(regexp="^[A-Za-z0-9._-]+$", message="El código de la prueba solo puede contener letras, números, punto, guion y guion bajo")
    String codigo;

    @Column(length=100, nullable=false) @Required
    @NotBlank(message="El nombre de la prueba es requerido")
    @Size(max=100, message="El nombre de la prueba no puede exceder 100 caracteres")
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

    @Min(value=1, message="El tiempo límite debe ser mayor que cero") @Required
    int tiempoLimiteMinutos = 6;

    @Required
    boolean permiteNoSe = true;

    @ReadOnly
    LocalDate fechaCreacion = LocalDate.now();

    public boolean activa() {
        return EstadoPrueba.ACTIVA.equals(estadoPrueba);
    }
}
