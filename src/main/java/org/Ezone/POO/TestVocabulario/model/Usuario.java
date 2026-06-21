package org.Ezone.POO.TestVocabulario.model;

import java.time.*;

import javax.persistence.*;

import org.openxava.annotations.*;
import org.openxava.model.*;

import lombok.*;

@MappedSuperclass
@Getter @Setter
public abstract class Usuario extends Identifiable {

    @Column(length=80) @Required
    String nombres;

    @Column(length=80) @Required
    String apellidos;

    @Column(length=120)
    String correo;

    @Column(length=20)
    String telefono;

    LocalDate fechaNacimiento;

    public String nombreCompleto() {
        return "%s %s".formatted(nombres, apellidos).trim();
    }
}
