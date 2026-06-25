package org.Ezone.POO.TestVocabulario.action;

import org.Ezone.POO.TestVocabulario.enums.EstadoIntento;
import org.Ezone.POO.TestVocabulario.model.*;
import org.Ezone.POO.TestVocabulario.service.*;
import org.openxava.actions.*;
import org.openxava.jpa.*;

public class FinalizarYCalcularAction extends ViewBaseAction {



    @Override
    public void execute() throws Exception {
        if (getView().getEntity() == null) {
            addError("Primero debes seleccionar o guardar un intento.");
            return;
        }

        IntentoPrueba intento = (IntentoPrueba) getView().getEntity();

        if (EstadoIntento.CALIFICADO.equals(intento.getEstadoIntento())) {
            addError("Esta prueba ya fue calificada.");
            return;
        }

        IAplicacionPruebaService appService = new AplicacionPruebaService();
        ICalculoResultadoService calcService = new CalculoResultadoService();

        try {
            appService.finalizarPrueba(intento);
            calcService.calcularResultado(intento);

            XPersistence.commit();

            addMessage("Prueba finalizada y calificada con éxito.");
            getView().refresh();

        } catch (Exception e) {
            XPersistence.rollback(); // Importante: deshacer cambios si algo falló
            addError("Error al finalizar la prueba: " + e.getMessage());
        }
    }
}