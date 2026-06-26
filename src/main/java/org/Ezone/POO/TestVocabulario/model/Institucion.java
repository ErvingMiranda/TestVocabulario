package org.Ezone.POO.TestVocabulario.model;

import javax.persistence.*;
import javax.validation.constraints.*;

import org.Ezone.POO.TestVocabulario.enums.*;
import org.openxava.annotations.*;
import org.openxava.model.*;

import lombok.*;

@Entity
@Getter @Setter
public class Institucion extends Identifiable {

    @Column(length=120) @Required
    @NotBlank(message="El nombre de la institución es requerido")
    @Size(max=120, message="El nombre de la institución no puede exceder 120 caracteres")
    String nombre;

    @Enumerated(EnumType.STRING)
    TipoInstitucion tipoInstitucion = TipoInstitucion.OTRO;

    @Column(length=160)
    @Size(max=160, message="La dirección no puede exceder 160 caracteres")
    String direccion;

    @Column(length=120)
    @Size(max=120, message="El correo no puede exceder 120 caracteres")
    @Email(message="El correo debe tener un formato válido")
    String correo;

    @Column(length=20)
    @Size(max=20, message="El teléfono no puede exceder 20 caracteres")
    @Pattern(regexp=Usuario.TELEFONO, message="El teléfono solo puede contener números y debe tener entre 7 y 15 dígitos")
    String telefono;
}
