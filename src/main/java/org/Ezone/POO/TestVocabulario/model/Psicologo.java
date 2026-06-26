package org.Ezone.POO.TestVocabulario.model;

import javax.persistence.*;

import org.openxava.annotations.*;

import lombok.*;

@Entity
@Getter @Setter
@View(members =
    "DatosProfesionales { nombres, apellidos; numeroLicencia, especialidad; activo };" +
    "Contacto { correo, telefono; fechaNacimiento }"
)
@Tab(properties="apellidos, nombres, numeroLicencia, especialidad, correo, activo")
public class Psicologo extends Usuario {

    @Column(length=30)
    String numeroLicencia;

    @Column(length=80)
    String especialidad;

    @Required
    boolean activo = true;
}
