package org.Ezone.POO.TestVocabulario.model;

import javax.persistence.*;

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

    @Column(length=30)
    String codigoEvaluado;

    @Enumerated(EnumType.STRING)
    NivelAcademico nivelAcademico = NivelAcademico.OTRO;

    @ManyToOne(fetch=FetchType.LAZY, optional=true)
    @DescriptionsList(descriptionProperties="nombre")
    Institucion institucion;
}
