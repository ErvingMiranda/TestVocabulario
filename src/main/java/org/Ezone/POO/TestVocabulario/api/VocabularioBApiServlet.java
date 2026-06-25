package org.Ezone.POO.TestVocabulario.api;

import java.io.*;
import java.math.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

import javax.persistence.*;
import javax.servlet.*;
import javax.servlet.http.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;

import org.Ezone.POO.TestVocabulario.enums.*;
import org.Ezone.POO.TestVocabulario.exception.*;
import org.Ezone.POO.TestVocabulario.model.*;
import org.Ezone.POO.TestVocabulario.service.*;
import org.openxava.jpa.*;

public class VocabularioBApiServlet extends HttpServlet {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) {
        addCorsHeaders(response);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        prepare(request, response);

        try {
            String path = path(request);
            if ("/pruebas/vocabulario-b".equals(path)) {
                responderPruebaActiva(response);
                return;
            }
            error(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint no encontrado");
        }
        catch (ApiException ex) {
            error(response, ex.getStatus(), ex.getMessage());
        }
        catch (ValidacionPruebaException ex) {
            error(response, HttpServletResponse.SC_CONFLICT, ex.getMessage());
        }
        catch (Exception ex) {
            logError(ex);
            error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error interno del servidor");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        prepare(request, response);

        try {
            String path = path(request);
            if ("/intentos/iniciar".equals(path)) {
                iniciarIntento(request, response);
                return;
            }

            String[] parts = parts(path);
            if (parts.length == 3 && "intentos".equals(parts[0]) && "respuestas".equals(parts[2])) {
                registrarRespuestas(parts[1], request, response);
                return;
            }
            if (parts.length == 3 && "intentos".equals(parts[0]) && "finalizar".equals(parts[2])) {
                finalizarIntento(parts[1], response);
                return;
            }

            error(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint no encontrado");
        }
        catch (ApiException ex) {
            rollbackQuietly();
            error(response, ex.getStatus(), ex.getMessage());
        }
        catch (IllegalArgumentException ex) {

            rollbackQuietly();
            error(response, HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
        }
        catch (DateTimeException ex) {
            rollbackQuietly();
            error(response, HttpServletResponse.SC_BAD_REQUEST, "Fecha invalida");
        }
        catch (AplicacionPruebaException | CalculoResultadoException | ValidacionPruebaException ex) {
            rollbackQuietly();
            error(response, HttpServletResponse.SC_CONFLICT, ex.getMessage());
        }
        catch (Exception ex) {
            rollbackQuietly();
            logError(ex);
            error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error interno del servidor");
        }
    }

    void responderPruebaActiva(HttpServletResponse response) throws IOException {
        PruebaVocabulario prueba = buscarPruebaActiva();
        if (prueba == null) {
            error(response, HttpServletResponse.SC_NOT_FOUND, "No hay una prueba de vocabulario activa");
            return;
        }

        new ValidacionPruebaService().validarParaActivar(prueba);
        sendJson(response, HttpServletResponse.SC_OK, pruebaJson(prueba));
    }

    void iniciarIntento(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonNode body = readBody(request);
        PruebaVocabulario prueba = buscarPrueba(requiredText(body, "pruebaId", "idPrueba"));
        if (!prueba.activa()) {
            throw new AplicacionPruebaException("La prueba indicada no esta activa");
        }
        new ValidacionPruebaService().validarParaActivar(prueba);

        Evaluado evaluado = crearEvaluado(body.path("evaluado").isMissingNode() ? body : body.path("evaluado"));
        IntentoPrueba intento = new IntentoPrueba();
        intento.setCodigoAplicacion(generarCodigoAplicacion());
        intento.setPrueba(prueba);
        intento.setEvaluado(evaluado);
        intento.setEstadoIntento(EstadoIntento.EN_PROGRESO);
        intento.setFechaInicio(LocalDateTime.now());
        intento.setNumeroRespuestas(0);

        EntityManager manager = XPersistence.getManager();
        manager.persist(evaluado);
        manager.persist(intento);
        manager.flush();

        ObjectNode result = intentoJson(intento);
        result.set("preguntas", preguntasJson(prueba));

        XPersistence.commit();
        sendJson(response, HttpServletResponse.SC_CREATED, result);
    }

    void registrarRespuestas(String idIntento, HttpServletRequest request, HttpServletResponse response) throws IOException {
        IntentoPrueba intento = buscarIntento(idIntento);
        validarIntentoEnProgreso(intento);

        if (tiempoAgotado(intento)) {
            ResultadoPrueba resultado = finalizarYCalcular(intento);
            XPersistence.commit();

            ObjectNode result = mapper.createObjectNode();
            result.put("estado", "TIEMPO_AGOTADO");
            result.put("mensaje", "El tiempo limite de la prueba ya fue alcanzado");
            result.set("resultado", resultadoJson(resultado));
            sendJson(response, HttpServletResponse.SC_CONFLICT, result);
            return;
        }

        JsonNode body = readBody(request);
        JsonNode respuestas = body.path("respuestas");
        if (!respuestas.isArray()) {
            throw new IllegalArgumentException("El cuerpo debe incluir un arreglo respuestas");
        }

        AplicacionPruebaService service = new AplicacionPruebaService();
        int total = 0;
        for (JsonNode item : respuestas) {
            PreguntaVocabulario pregunta = buscarPregunta(requiredText(item, "preguntaId", "idPregunta"));
            OpcionRespuesta opcion = null;
            String opcionId = optionalText(item, "opcionSeleccionadaId", "idOpcionSeleccionada", "opcionId");
            validarClasificacionFrontend(item, opcionId);
            if (opcionId != null) {
                opcion = buscarOpcion(opcionId);
            }
            validarPertenencia(intento, pregunta, opcion);
            ClasificacionRespuesta clasificacion = opcion == null ?
                clasificacionDesdeJson(item) :
                null;

            service.registrarRespuesta(intento, pregunta, opcion, clasificacion);
            total++;
        }

        XPersistence.commit();

        ObjectNode result = mapper.createObjectNode();
        result.put("idIntento", intento.getId());
        result.put("estadoIntento", intento.getEstadoIntento().name());
        result.put("respuestasRecibidas", total);
        sendJson(response, HttpServletResponse.SC_OK, result);
    }

    void finalizarIntento(String idIntento, HttpServletResponse response) throws IOException {
        IntentoPrueba intento = buscarIntento(idIntento);
        if (EstadoIntento.CALIFICADO.equals(intento.getEstadoIntento())) {
            ResultadoPrueba resultado = buscarResultado(intento);
            if (resultado == null) {
                throw new CalculoResultadoException("El intento esta calificado, pero no tiene resultado guardado");
            }
            sendJson(response, HttpServletResponse.SC_OK, resultadoJson(resultado));
            return;
        }

        validarIntentoEnProgreso(intento);
        ResultadoPrueba resultado = finalizarYCalcular(intento);
        XPersistence.commit();
        sendJson(response, HttpServletResponse.SC_OK, resultadoJson(resultado));
    }

    ResultadoPrueba finalizarYCalcular(IntentoPrueba intento) {
        IAplicacionPruebaService aplicacionService = new AplicacionPruebaService();
        ICalculoResultadoService calculoService = new CalculoResultadoService();
        aplicacionService.finalizarPrueba(intento);
        return calculoService.calcularResultado(intento);
    }

    Evaluado crearEvaluado(JsonNode node) {
        Evaluado evaluado = new Evaluado();
        evaluado.setNombres(requiredText(node, "nombres"));
        evaluado.setApellidos(requiredText(node, "apellidos"));
        evaluado.setCorreo(optionalText(node, "correo", "correoElectronico"));
        evaluado.setTelefono(optionalText(node, "telefono"));
        evaluado.setCodigoEvaluado(optionalText(node, "codigoEvaluado"));

        String fechaNacimiento = optionalText(node, "fechaNacimiento");
        if (fechaNacimiento != null) {
            evaluado.setFechaNacimiento(LocalDate.parse(fechaNacimiento, DATE_FORMAT));
        }

        String nivelAcademico = optionalText(node, "nivelAcademico");
        if (nivelAcademico != null) {
            evaluado.setNivelAcademico(NivelAcademico.valueOf(nivelAcademico));
        }

        String institucionId = optionalText(node, "institucionId", "idInstitucion");
        if (institucionId != null) {
            evaluado.setInstitucion(buscarInstitucion(institucionId));
        }

        return evaluado;
    }

    PruebaVocabulario buscarPruebaActiva() {
        return XPersistence.getManager()
            .createQuery(
                "from PruebaVocabulario p where p.estadoPrueba = :estado order by p.fechaCreacion desc",
                PruebaVocabulario.class)
            .setParameter("estado", EstadoPrueba.ACTIVA)
            .setMaxResults(1)
            .getResultList()
            .stream()
            .findFirst()
            .orElse(null);
    }

    PruebaVocabulario buscarPrueba(String id) {
        PruebaVocabulario prueba = XPersistence.getManager().find(PruebaVocabulario.class, id);
        if (prueba == null) {
            throw new ApiException(HttpServletResponse.SC_NOT_FOUND, "No existe la prueba indicada");
        }
        return prueba;
    }

    IntentoPrueba buscarIntento(String id) {
        IntentoPrueba intento = XPersistence.getManager().find(IntentoPrueba.class, id);
        if (intento == null) {
            throw new ApiException(HttpServletResponse.SC_NOT_FOUND, "No existe el intento indicado");
        }
        return intento;
    }

    PreguntaVocabulario buscarPregunta(String id) {
        PreguntaVocabulario pregunta = XPersistence.getManager().find(PreguntaVocabulario.class, id);
        if (pregunta == null) {
            throw new ApiException(HttpServletResponse.SC_NOT_FOUND, "No existe la pregunta indicada");
        }
        return pregunta;
    }

    OpcionRespuesta buscarOpcion(String id) {
        OpcionRespuesta opcion = XPersistence.getManager().find(OpcionRespuesta.class, id);
        if (opcion == null) {
            throw new ApiException(HttpServletResponse.SC_NOT_FOUND, "No existe la opcion indicada");
        }
        return opcion;
    }

    Institucion buscarInstitucion(String id) {
        Institucion institucion = XPersistence.getManager().find(Institucion.class, id);
        if (institucion == null) {
            throw new ApiException(HttpServletResponse.SC_NOT_FOUND, "No existe la institucion indicada");
        }
        return institucion;
    }

    ResultadoPrueba buscarResultado(IntentoPrueba intento) {
        return XPersistence.getManager()
            .createQuery("from ResultadoPrueba r where r.intento = :intento", ResultadoPrueba.class)
            .setParameter("intento", intento)
            .setMaxResults(1)
            .getResultList()
            .stream()
            .findFirst()
            .orElse(null);
    }

    void validarIntentoEnProgreso(IntentoPrueba intento) {
        if (!EstadoIntento.EN_PROGRESO.equals(intento.getEstadoIntento())) {
            throw new ApiException(HttpServletResponse.SC_CONFLICT, "El intento no esta en progreso");
        }
    }

    void validarPertenencia(IntentoPrueba intento, PreguntaVocabulario pregunta, OpcionRespuesta opcion) {
        if (!pregunta.getPrueba().equals(intento.getPrueba())) {
            throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "La pregunta no pertenece a la prueba del intento");
        }
        if (opcion != null && !opcion.getPregunta().equals(pregunta)) {
            throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "La opcion no pertenece a la pregunta indicada");
        }
    }

    boolean tiempoAgotado(IntentoPrueba intento) {
        if (intento.getFechaInicio() == null || intento.getPrueba() == null) return false;
        LocalDateTime limite = intento.getFechaInicio().plusMinutes(intento.getPrueba().getTiempoLimiteMinutos());
        return LocalDateTime.now().isAfter(limite);
    }

    ObjectNode pruebaJson(PruebaVocabulario prueba) {
        ObjectNode json = mapper.createObjectNode();
        json.put("idPrueba", prueba.getId());
        json.put("codigo", prueba.getCodigo());
        json.put("nombre", prueba.getNombre());
        json.put("descripcion", prueba.getDescripcion());
        json.put("instrucciones", prueba.getDescripcion());
        json.put("tiempoLimiteMinutos", prueba.getTiempoLimiteMinutos());
        json.set("preguntas", preguntasJson(prueba));
        return json;
    }

    ObjectNode intentoJson(IntentoPrueba intento) {
        ObjectNode json = mapper.createObjectNode();
        json.put("idIntento", intento.getId());
        json.put("codigoAplicacion", intento.getCodigoAplicacion());
        json.put("fechaInicio", format(intento.getFechaInicio()));
        json.put("tiempoLimiteMinutos", intento.getPrueba().getTiempoLimiteMinutos());
        return json;
    }

    ArrayNode preguntasJson(PruebaVocabulario prueba) {
        ArrayNode preguntasJson = mapper.createArrayNode();
        for (PreguntaVocabulario pregunta : preguntas(prueba)) {
            ObjectNode preguntaJson = mapper.createObjectNode();
            preguntaJson.put("idPregunta", pregunta.getId());
            preguntaJson.put("numero", pregunta.getNumero());
            preguntaJson.put("enunciado", pregunta.getEnunciado());
            preguntaJson.put("ejemplo", pregunta.isEjemplo());
            preguntaJson.set("opciones", opcionesJson(pregunta));
            preguntasJson.add(preguntaJson);
        }
        return preguntasJson;
    }

    List<PreguntaVocabulario> preguntas(PruebaVocabulario prueba) {
        return XPersistence.getManager()
            .createQuery(
                "from PreguntaVocabulario p where p.prueba = :prueba and p.activa = true order by p.numero",
                PreguntaVocabulario.class)
            .setParameter("prueba", prueba)
            .getResultList();
    }

    ArrayNode opcionesJson(PreguntaVocabulario pregunta) {
        ArrayNode opcionesJson = mapper.createArrayNode();
        for (OpcionRespuesta opcion : opciones(pregunta)) {
            ObjectNode opcionJson = mapper.createObjectNode();
            opcionJson.put("idOpcion", opcion.getId());
            opcionJson.put("letra", opcion.getLetra().name());
            opcionJson.put("texto", opcion.getTexto());
            opcionesJson.add(opcionJson);
        }
        return opcionesJson;
    }

    List<OpcionRespuesta> opciones(PreguntaVocabulario pregunta) {
        return XPersistence.getManager()
            .createQuery("from OpcionRespuesta o where o.pregunta = :pregunta order by o.letra", OpcionRespuesta.class)
            .setParameter("pregunta", pregunta)
            .getResultList();
    }

    ObjectNode resultadoJson(ResultadoPrueba resultado) {
        ObjectNode json = mapper.createObjectNode();
        json.put("idIntento", resultado.getIntento().getId());
        json.put("puntajeDirecto", resultado.getPuntajeDirecto());
        putBigDecimal(json, "notaFinal", resultado.getNotaFinal());
        json.put("correctas", resultado.getCantidadCorrectas());
        json.put("incorrectas", resultado.getCantidadIncorrectas());
        json.put("noSe", resultado.getCantidadNoSe());
        json.put("omitidas", resultado.getCantidadOmitidas());
        json.put("fechaCalculo", format(resultado.getFechaCalculo()));
        return json;
    }

    ClasificacionRespuesta clasificacionDesdeJson(JsonNode item) {
        String value = optionalText(item, "clasificacionRespuesta", "clasificacion");
        if (value == null) {
            return ClasificacionRespuesta.OMITIDA;
        }
        return parseClasificacion(value);
    }

    void validarClasificacionFrontend(JsonNode item, String opcionId) {
        String value = optionalText(item, "clasificacionRespuesta", "clasificacion");
        if (value == null) return;

        ClasificacionRespuesta clasificacion = parseClasificacion(value);
        if (ClasificacionRespuesta.CORRECTA.equals(clasificacion) ||
            ClasificacionRespuesta.INCORRECTA.equals(clasificacion)) {

            throw new ApiException(
                HttpServletResponse.SC_BAD_REQUEST,
                "La clasificacion CORRECTA/INCORRECTA se determina en servidor");
        }
        if (opcionId != null && (ClasificacionRespuesta.NO_SE.equals(clasificacion) ||
            ClasificacionRespuesta.OMITIDA.equals(clasificacion))) {

            throw new ApiException(
                HttpServletResponse.SC_BAD_REQUEST,
                "Las respuestas NO_SE u OMITIDA no deben incluir opcion seleccionada");
        }
    }

    ClasificacionRespuesta parseClasificacion(String value) {
        try {
            return ClasificacionRespuesta.valueOf(value.toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException ex) {
            throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "Clasificacion de respuesta invalida");
        }
    }

    JsonNode readBody(HttpServletRequest request) throws IOException {
        try (Reader reader = request.getReader()) {
            JsonNode body = mapper.readTree(reader);
            if (body == null || body.isNull()) {
                throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "El cuerpo JSON es requerido");
            }
            return body;
        }
        catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "JSON invalido");
        }
    }

    void prepare(HttpServletRequest request, HttpServletResponse response) throws UnsupportedEncodingException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        addCorsHeaders(response);
    }

    void addCorsHeaders(HttpServletResponse response) {
        String allowedOrigin = "*"; // Desarrollo local. En despliegue, reemplazar por el origen real del frontend.
        response.setHeader("Access-Control-Allow-Origin", allowedOrigin);
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    }

    void sendJson(HttpServletResponse response, int status, JsonNode body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        mapper.writeValue(response.getWriter(), body);
    }

    void error(HttpServletResponse response, int status, String message) throws IOException {
        ObjectNode json = mapper.createObjectNode();
        json.put("error", message == null || message.isBlank() ? "Error inesperado" : message);
        sendJson(response, status, json);
    }

    void rollbackQuietly() {
        try {
            XPersistence.rollback();
        }
        catch (Exception ignored) {
        }
    }

    void logError(Exception ex) {
        ServletContext context = getServletContext();
        if (context != null) {
            context.log("Error no controlado en API Vocabulario B", ex);
        }
    }

    String path(HttpServletRequest request) {
        String path = request.getPathInfo();
        return path == null || path.isBlank() ? "/" : path;
    }

    String[] parts(String path) {
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        return normalized.isBlank() ? new String[0] : normalized.split("/");
    }

    String requiredText(JsonNode node, String... names) {
        String value = optionalText(node, names);
        if (value == null) {
            throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "Campo requerido: " + String.join("/", names));
        }
        return value;
    }

    String optionalText(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.path(name);
            if (!value.isMissingNode() && !value.isNull() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }

    String generarCodigoAplicacion() {
        return "VOC2-" + UUID.randomUUID().toString().replace("-", "");
    }

    String format(LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMAT.format(value);
    }

    void putBigDecimal(ObjectNode json, String name, BigDecimal value) {
        if (value == null) {
            json.putNull(name);
        }
        else {
            json.put(name, value);
        }
    }

    static class ApiException extends RuntimeException {

        private final int status;

        ApiException(int status, String message) {
            super(message);
            this.status = status;
        }

        int getStatus() {
            return status;
        }
    }
}
