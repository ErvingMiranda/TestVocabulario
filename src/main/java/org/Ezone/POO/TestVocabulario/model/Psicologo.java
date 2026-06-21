package org.Ezone.POO.TestVocabulario.model;

import javax.persistence.*;

import org.openxava.annotations.*;

import lombok.*;

@Entity
@Getter @Setter
public class Psicologo extends Usuario {

    @Column(length=30)
    String numeroLicencia;

    @Column(length=80)
    String especialidad;

    @Required
    boolean activo = true;
}
