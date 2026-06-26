package org.Ezone.POO.TestVocabulario.model;

import java.util.*;

import javax.persistence.*;
import javax.validation.constraints.*;

import org.Ezone.POO.TestVocabulario.enums.*;
import org.openxava.annotations.*;

import lombok.*;

@Entity
@Getter @Setter
@View(members =
    "DatosPersonales { nombres, apellidos; codigoEvaluado; fechaNacimiento };" +
    "Contacto { correo, telefono };" +
    "ContextoAcademico { nivelAcademico; institucion }"
)
@Tab(properties="nombres, apellidos, codigoEvaluado, nivelAcademico, institucion.nombre, correo, telefono")
public class Evaluado extends Usuario {

    @OneToMany(mappedBy="evaluado", cascade=CascadeType.REMOVE, orphanRemoval=true)
    Collection<IntentoPrueba> intentos;

    @Column(length=30)
    @Size(max=30, message="El código de evaluado no puede exceder 30 caracteres")
    @Pattern(regexp="^$|[A-Za-z0-9._-]+$", message="El código de evaluado solo puede contener letras, números, punto, guion y guion bajo")
    String codigoEvaluado;

    @Enumerated(EnumType.STRING)
    NivelAcademico nivelAcademico = NivelAcademico.OTRO;

    @ManyToOne(fetch=FetchType.LAZY, optional=true)
    @DescriptionsList(descriptionProperties="nombre")
    Institucion institucion;
}
