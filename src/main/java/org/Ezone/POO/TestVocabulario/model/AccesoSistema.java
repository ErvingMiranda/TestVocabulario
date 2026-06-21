package org.Ezone.POO.TestVocabulario.model;

import javax.persistence.*;

import org.Ezone.POO.TestVocabulario.enums.*;
import org.openxava.annotations.*;
import org.openxava.model.*;

import lombok.*;

@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames="nombreUsuario"))
@Getter @Setter
public class AccesoSistema extends Identifiable {

    @Column(length=50, nullable=false) @Required
    String nombreUsuario;

    @Column(length=120, nullable=false) @Required
    String hashContrasena;

    @Enumerated(EnumType.STRING)
    EstadoAcceso estadoAcceso = EstadoAcceso.ACTIVO;

    @OneToOne(fetch=FetchType.LAZY, optional=false)
    @DescriptionsList(descriptionProperties="nombres, apellidos")
    @Required
    Psicologo psicologo;
}
