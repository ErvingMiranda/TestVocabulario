package org.Ezone.POO.TestVocabulario.model;

import javax.persistence.*;

import org.Ezone.POO.TestVocabulario.enums.*;
import org.openxava.annotations.*;

import lombok.*;

@Entity
@Getter @Setter
@View(members = "nombres, apellidos; correo, telefono; fechaNacimiento; codigoEvaluado, nivelAcademico; institucion")
@Tab(properties="nombres, apellidos, correo, telefono, nivelAcademico, institucion.nombre")
public class Evaluado extends Usuario {

    @Column(length=30)
    String codigoEvaluado;

    @Enumerated(EnumType.STRING)
    NivelAcademico nivelAcademico = NivelAcademico.OTRO;

    @ManyToOne(fetch=FetchType.LAZY, optional=true)
    @DescriptionsList(descriptionProperties="nombre")
    Institucion institucion;
}
