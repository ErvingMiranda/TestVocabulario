package org.Ezone.POO.TestVocabulario.model;

import javax.persistence.*;
import javax.validation.constraints.*;

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
    @Size(max=30, message="El número de licencia no puede exceder 30 caracteres")
    @Pattern(regexp="^$|[A-Za-z0-9._-]+$", message="El número de licencia solo puede contener letras, números, punto, guion y guion bajo")
    String numeroLicencia;

    @Column(length=80)
    @Size(max=80, message="La especialidad no puede exceder 80 caracteres")
    @Pattern(regexp="^$|[A-Za-zÁÉÍÓÚÜáéíóúüÑñ. ]+$", message="La especialidad solo puede contener letras, espacios y punto")
    String especialidad;

    @Required
    boolean activo = true;
}
