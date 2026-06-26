# Proyecto de asignatura Programación orientada a objetos 
Grupo 1 (SOLID)
Integrantes:
Fernando Alonso Zapata Ancajima 
Erving Josué Miranda Rios
Diego Alexander Gomez Solis 

# Test de Vocabulario

Sistema desarrollado en Java para la creación, administración, aplicación y calificación de pruebas de vocabulario.

La aplicación permite que un psicólogo configure pruebas con preguntas de opción múltiple, las asigne a evaluados, registre sus respuestas y calcule automáticamente el resultado obtenido.

El proyecto utiliza OpenXava para generar la interfaz de usuario, JPA para la persistencia de datos y Lombok para reducir código repetitivo.

# El sistema administra el ciclo completo de una prueba de vocabulario:

Registro de psicólogos, evaluados e instituciones.
Creación de pruebas de vocabulario.
Registro de preguntas y opciones de respuesta.
Validación de la estructura de la prueba.
Activación de pruebas.
Creación de intentos para los evaluados.
Registro y actualización de respuestas.
Finalización de intentos.
Cálculo automático de resultados.
Interpretación del desempeño obtenido.

# Tecnologías utilizadas
Java 21
OpenXava 7.7.2
Jakarta/Java Persistence 
JPQL
Lombok
Bean Validation
Maven
Base de datos de desarrollo integrada Postgres
La versión del JDK debe coincidir con la propiedad target o release configurada en el archivo pom.xml.

Arquitectura del proyecto
El proyecto está organizado en paquetes según la responsabilidad de cada componente.
src/main/java/vocabulario/
│
├── enums/
│   ├── EstadoAcceso.java
│   ├── EstadoIntento.java
│   ├── EstadoPrueba.java
│   ├── LetraOpcion.java
│   ├── NivelAcademico.java
│   └── TipoInstitucion.java
│
├── exception/
│   ├── AplicacionPruebaException.java
│   ├── CalculoResultadoException.java
│   └── ValidacionPruebaException.java
│
├── model/
│   ├── AccesoSistema.java
│   ├── Usuario.java
│   ├── Psicologo.java
│   ├── Evaluado.java
│   ├── Institucion.java
│   ├── PruebaVocabulario.java
│   ├── PreguntaVocabulario.java
│   ├── OpcionRespuesta.java
│   ├── IntentoPrueba.java
│   ├── RespuestaEvaluado.java
│   └── ResultadoPrueba.java
│
├── service/
│   ├── IAplicacionPruebaService.java
│   ├── ICalculoResultadoService.java
│   ├── IValidacionPruebaService.java
│   ├── AplicacionPruebaService.java
│   ├── CalculoResultadoService.java
│   └── ValidacionPruebaService.java
│
└── run/
    ├── DBManager.java
    └── TestVocabulario.java
