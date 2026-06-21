package org.Ezone.POO.TestVocabulario.model;

import javax.persistence.*;

import org.Ezone.POO.TestVocabulario.enums.*;
import org.openxava.annotations.*;
import org.openxava.model.*;

import lombok.*;

@Entity
@Getter @Setter
public class Institucion extends Identifiable {

    @Column(length=120) @Required
    String nombre;

    @Enumerated(EnumType.STRING)
    TipoInstitucion tipoInstitucion = TipoInstitucion.OTRO;

    @Column(length=160)
    String direccion;

    @Column(length=120)
    String correo;

    @Column(length=20)
    String telefono;
}
