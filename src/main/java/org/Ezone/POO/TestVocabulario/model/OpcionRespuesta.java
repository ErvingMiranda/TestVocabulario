package org.Ezone.POO.TestVocabulario.model;

import javax.persistence.*;
import javax.validation.constraints.*;

import org.Ezone.POO.TestVocabulario.enums.*;
import org.openxava.annotations.*;
import org.openxava.jpa.*;
import org.openxava.model.*;

import lombok.*;

@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"pregunta_id", "letra"}))
@Getter @Setter
@View(members =
    "Pregunta { pregunta };" +
    "Respuesta { letra; texto; correcta }"
)
@Tab(properties="pregunta.numero, letra, texto, correcta")
public class OpcionRespuesta extends Identifiable {

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="pregunta_id", nullable=false)
    @DescriptionsList(descriptionProperties="numero, enunciado")
    @Required
    PreguntaVocabulario pregunta;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    @NotNull @Required
    LetraOpcion letra;

    @Column(length=250, nullable=false) @Required
    @NotBlank(message="El texto de la opción es requerido")
    @Size(max=250, message="El texto de la opción no puede exceder 250 caracteres")
    String texto;

    @Required
    boolean correcta;

    @Hidden
    @AssertTrue(message="No puede haber opciones con la misma letra en una pregunta")
    public boolean isLetraUnicaEnPregunta() {
        if (pregunta == null || letra == null) return true;

        Long total = XPersistence.getManager()
            .createQuery(
                "select count(o) from OpcionRespuesta o " +
                    "where o.pregunta = :pregunta and o.letra = :letra " +
                    "and (:id is null or o.id <> :id)",
                Long.class)
            .setParameter("pregunta", pregunta)
            .setParameter("letra", letra)
            .setParameter("id", getId())
            .getSingleResult();
        return total == 0;
    }

    @Hidden
    @AssertTrue(message="No puede haber opciones con el mismo texto en una pregunta")
    public boolean isTextoUnicoEnPregunta() {
        if (pregunta == null || texto == null || texto.isBlank()) return true;

        Long total = XPersistence.getManager()
            .createQuery(
                "select count(o) from OpcionRespuesta o " +
                    "where o.pregunta = :pregunta and lower(trim(o.texto)) = :texto " +
                    "and (:id is null or o.id <> :id)",
                Long.class)
            .setParameter("pregunta", pregunta)
            .setParameter("texto", texto.trim().toLowerCase(java.util.Locale.ROOT))
            .setParameter("id", getId())
            .getSingleResult();
        return total == 0;
    }
}
