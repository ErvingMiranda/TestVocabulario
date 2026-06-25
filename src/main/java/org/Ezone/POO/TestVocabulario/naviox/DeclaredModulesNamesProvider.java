package org.Ezone.POO.TestVocabulario.naviox;

import java.util.*;

import org.openxava.application.meta.*;

import com.openxava.naviox.impl.*;

public class DeclaredModulesNamesProvider implements IAllModulesNamesProvider {

    private static final Collection<String> MODULES = Collections.unmodifiableList(Arrays.asList(
        "PruebaVocabulario",
        "PreguntaVocabulario",
        "IntentoPrueba",
        "ResultadoPrueba",
        "Evaluado",
        "Psicologo"
    ));

    @Override
    public Collection<String> getAllModulesNames(MetaApplication application) {
        return MODULES;
    }
}
