package org.Ezone.POO.TestVocabulario.model;

import java.time.*;

import javax.persistence.*;
import javax.validation.constraints.*;

import org.openxava.annotations.*;
import org.openxava.model.*;

import lombok.*;

@MappedSuperclass
@Getter @Setter
public abstract class Usuario extends Identifiable {

    static final String SOLO_LETRAS_ESPACIOS = "^[A-Za-zÁÉÍÓÚÜáéíóúüÑñ]+(?: [A-Za-zÁÉÍÓÚÜáéíóúüÑñ]+)*$";
    static final String TELEFONO = "^$|\\d{7,15}$";

    @Column(length=80) @Required
    @NotBlank(message="Los nombres son requeridos")
    @Size(max=80, message="Los nombres no pueden exceder 80 caracteres")
    @Pattern(regexp=SOLO_LETRAS_ESPACIOS, message="Los nombres solo pueden contener letras y espacios")
    String nombres;

    @Column(length=80) @Required
    @NotBlank(message="Los apellidos son requeridos")
    @Size(max=80, message="Los apellidos no pueden exceder 80 caracteres")
    @Pattern(regexp=SOLO_LETRAS_ESPACIOS, message="Los apellidos solo pueden contener letras y espacios")
    String apellidos;

    @Column(length=120)
    @Size(max=120, message="El correo no puede exceder 120 caracteres")
    @Email(message="El correo debe tener un formato válido")
    String correo;

    @Column(length=20)
    @Size(max=20, message="El teléfono no puede exceder 20 caracteres")
    @Pattern(regexp=TELEFONO, message="El teléfono solo puede contener números y debe tener entre 7 y 15 dígitos")
    String telefono;

    @PastOrPresent(message="La fecha de nacimiento no puede ser futura")
    LocalDate fechaNacimiento;

    public String nombreCompleto() {
        return "%s %s".formatted(nombres, apellidos).trim();
    }
}
