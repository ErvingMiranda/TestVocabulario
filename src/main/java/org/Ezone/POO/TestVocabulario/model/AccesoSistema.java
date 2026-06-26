package org.Ezone.POO.TestVocabulario.model;

import javax.persistence.*;
import javax.validation.constraints.*;

import org.Ezone.POO.TestVocabulario.enums.*;
import org.openxava.annotations.*;
import org.openxava.model.*;

import lombok.*;

@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames="nombreUsuario"))
@Getter @Setter
public class AccesoSistema extends Identifiable {

    @Column(length=50, nullable=false) @Required
    @NotBlank(message="El nombre de usuario es requerido")
    @Size(max=50, message="El nombre de usuario no puede exceder 50 caracteres")
    @Pattern(regexp="^[A-Za-z0-9._-]+$", message="El nombre de usuario solo puede contener letras, números, punto, guion y guion bajo")
    String nombreUsuario;

    @Hidden
    @Column(length=120, nullable=false) @Required
    @NotBlank(message="La contraseña es requerida")
    @Size(max=120, message="La contraseña no puede exceder 120 caracteres")
    String hashContrasena;

    @Enumerated(EnumType.STRING)
    EstadoAcceso estadoAcceso = EstadoAcceso.ACTIVO;

    @OneToOne(fetch=FetchType.LAZY, optional=false)
    @DescriptionsList(descriptionProperties="nombres, apellidos")
    @Required
    Psicologo psicologo;
}
