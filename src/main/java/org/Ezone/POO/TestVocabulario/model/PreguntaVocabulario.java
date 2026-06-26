package org.Ezone.POO.TestVocabulario.model;

import java.util.*;

import javax.persistence.*;
import javax.validation.constraints.*;

import org.openxava.annotations.*;
import org.openxava.jpa.*;
import org.openxava.model.*;

import lombok.*;

@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"prueba_id", "numero"}))
@Getter @Setter
@View(members =
    "Contexto { prueba; numero, puntaje; activa, ejemplo, puntuable };" +
    "Contenido { enunciado };" +
    "Opciones { opciones }"
)
@Tab(properties="numero, prueba.nombre, enunciado, puntaje, activa, ejemplo, puntuable")
public class PreguntaVocabulario extends Identifiable {

    @OneToMany(mappedBy="pregunta", cascade=CascadeType.ALL, orphanRemoval=true)
    @OrderBy("letra")
    @ListProperties("letra, texto, correcta")
    Collection<OpcionRespuesta> opciones;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="prueba_id", nullable=false)
    @DescriptionsList(descriptionProperties="nombre")
    @Required
    PruebaVocabulario prueba;

    @Min(value=1, message="El número de pregunta debe ser mayor que cero") @Required
    int numero;

    @Stereotype("MEMO")
    @Required
    @NotBlank(message="El enunciado de la pregunta es requerido")
    String enunciado;

    @Min(value=1, message="El puntaje de pregunta debe ser mayor que cero") @Required
    int puntaje = 1;

    @Required
    boolean ejemplo = false;

    @Required
    boolean puntuable = true;

    @Required
    boolean activa = true;

    @Hidden
    @AssertTrue(message="No puede haber preguntas con el mismo número en una prueba")
    public boolean isNumeroUnicoEnPrueba() {
        if (prueba == null || numero <= 0) return true;

        Long total = XPersistence.getManager()
            .createQuery(
                "select count(p) from PreguntaVocabulario p " +
                    "where p.prueba = :prueba and p.numero = :numero " +
                    "and (:id is null or p.id <> :id)",
                Long.class)
            .setParameter("prueba", prueba)
            .setParameter("numero", numero)
            .setParameter("id", getId())
            .getSingleResult();
        return total == 0;
    }

    @Hidden
    @AssertTrue(message="No puede haber preguntas con el mismo enunciado en una prueba")
    public boolean isEnunciadoUnicoEnPrueba() {
        if (prueba == null || enunciado == null || enunciado.isBlank()) return true;

        Long total = XPersistence.getManager()
            .createQuery(
                "select count(p) from PreguntaVocabulario p " +
                    "where p.prueba = :prueba and lower(trim(p.enunciado)) = :enunciado " +
                    "and (:id is null or p.id <> :id)",
                Long.class)
            .setParameter("prueba", prueba)
            .setParameter("enunciado", enunciado.trim().toLowerCase(Locale.ROOT))
            .setParameter("id", getId())
            .getSingleResult();
        return total == 0;
    }
}
