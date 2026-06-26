(function () {
    "use strict";

    var API_BASE_URL = new URL("../api/", window.location.href);
    var ACTIVE_SESSION_KEY = "test-vocabulario-b:active-session";
    var state = {
        prueba: null,
        intento: null,
        preguntas: [],
        respuestas: {},
        indiceActual: 0,
        pantalla: "inicio",
        enviando: false,
        comenzando: false,
        sincronizando: false,
        timerId: null,
        autosaveId: null,
        pendientes: {}
    };

    var screens = {
        loading: document.getElementById("loading-screen"),
        start: document.getElementById("start-screen"),
        instructions: document.getElementById("instructions-screen"),
        test: document.getElementById("test-screen"),
        final: document.getElementById("final-screen"),
        error: document.getElementById("error-screen")
    };

    var elements = {
        testSummary: document.getElementById("test-summary"),
        timer: document.getElementById("timer"),
        form: document.getElementById("evaluado-form"),
        instructionsContent: document.getElementById("instructions-content"),
        noSeRule: document.getElementById("no-se-rule"),
        beginTest: document.getElementById("begin-test"),
        questionPosition: document.getElementById("question-position"),
        questionTitle: document.getElementById("question-title"),
        questionContainer: document.getElementById("question-container"),
        answeredCount: document.getElementById("answered-count"),
        progressBar: document.getElementById("progress-bar"),
        syncStatus: document.getElementById("sync-status"),
        previousQuestion: document.getElementById("previous-question"),
        clearAnswer: document.getElementById("clear-answer"),
        nextQuestion: document.getElementById("next-question"),
        finishTest: document.getElementById("finish-test"),
        finalTitle: document.getElementById("final-title"),
        finalMessage: document.getElementById("final-message"),
        errorMessage: document.getElementById("error-message"),
        retry: document.getElementById("retry")
    };

    document.addEventListener("DOMContentLoaded", iniciarAplicacion);
    elements.form.addEventListener("submit", iniciarIntento);
    elements.beginTest.addEventListener("click", mostrarPrueba);
    elements.previousQuestion.addEventListener("click", preguntaAnterior);
    elements.nextQuestion.addEventListener("click", preguntaSiguiente);
    elements.clearAnswer.addEventListener("click", limpiarRespuestaActual);
    elements.finishTest.addEventListener("click", function () {
        if (window.confirm("¿Seguro que deseas finalizar la prueba? Aún puedes revisar tus respuestas si tienes tiempo disponible.")) {
            finalizarPrueba("manual");
        }
    });
    elements.retry.addEventListener("click", iniciarAplicacion);

    async function iniciarAplicacion() {
        detenerTimer();
        mostrarPantalla("loading");
        limpiarMensajeSincronizacion();

        var sesion = cargarSesionActiva();
        if (sesion) {
            restaurarSesion(sesion);
            normalizarSesionRestaurada();
            actualizarResumenPrueba();
            mostrarPantalla(state.pantalla === "prueba" ? "test" : "instructions");
            if (state.pantalla === "prueba") {
                iniciarTimer();
                if (tiempoRestanteMs() <= 0) {
                    finalizarPrueba("tiempo");
                    return;
                }
                renderizarPregunta();
            }
            else {
                elements.timer.hidden = true;
                renderizarInstrucciones();
            }
            if (Object.keys(state.pendientes).length) {
                programarAutosave();
            }
            return;
        }

        try {
            state.prueba = await apiGet("pruebas/vocabulario-b");
            state.preguntas = Array.isArray(state.prueba.preguntas) ? state.prueba.preguntas : [];
            actualizarResumenPrueba();
            mostrarPantalla("start");
        }
        catch (error) {
            mostrarError(error);
        }
    }

    async function iniciarIntento(event) {
        event.preventDefault();

        if (!elements.form.reportValidity()) {
            return;
        }
        if (!state.prueba || !state.prueba.idPrueba) {
            mostrarError(new Error("No hay una prueba activa disponible."));
            return;
        }

        bloquearFormulario(true);
        limpiarMensajeSincronizacion();

        try {
            var datos = leerDatosEvaluado();
            var respuesta = await apiPost("intentos/iniciar", {
                pruebaId: state.prueba.idPrueba,
                evaluado: datos
            });

            state.intento = {
                idIntento: respuesta.idIntento,
                codigoAplicacion: respuesta.codigoAplicacion,
                fechaInicio: respuesta.fechaInicio || null,
                tiempoLimiteMinutos: respuesta.tiempoLimiteMinutos || state.prueba.tiempoLimiteMinutos
            };
            state.preguntas = Array.isArray(respuesta.preguntas) ? respuesta.preguntas : state.preguntas;
            state.respuestas = {};
            state.indiceActual = 0;
            state.pantalla = "instrucciones";
            state.pendientes = {};

            guardarSesionActiva();
            actualizarResumenPrueba();
            renderizarInstrucciones();
            mostrarPantalla("instructions");
            elements.timer.hidden = true;
        }
        catch (error) {
            mostrarError(error);
        }
        finally {
            bloquearFormulario(false);
        }
    }

    function leerDatosEvaluado() {
        var datos = {
            nombres: valor("nombres"),
            apellidos: valor("apellidos"),
            correo: valor("correo"),
            nivelAcademico: valor("nivelAcademico")
        };

        agregarSiTieneValor(datos, "fechaNacimiento", valor("fechaNacimiento"));
        agregarSiTieneValor(datos, "telefono", valor("telefono"));

        return datos;
    }

    function renderizarInstrucciones() {
        var instrucciones = state.prueba && state.prueba.instrucciones ?
            state.prueba.instrucciones :
            "Lee cada pregunta y selecciona la opción que consideres correcta.";

        elements.instructionsContent.textContent = instrucciones;
        if (elements.noSeRule) {
            elements.noSeRule.hidden = !permiteNoSe();
        }
    }

    async function mostrarPrueba() {
        if (state.comenzando || state.pantalla === "prueba") {
            return;
        }
        if (!state.intento || !state.intento.idIntento) {
            mostrarError(new Error("No hay un intento activo para comenzar la prueba."));
            return;
        }

        state.comenzando = true;
        elements.beginTest.disabled = true;
        limpiarMensajeSincronizacion();

        try {
            if (!state.intento.fechaInicio) {
                var respuesta = await apiPost("intentos/" + encodeURIComponent(state.intento.idIntento) + "/comenzar", null);
                state.intento.fechaInicio = respuesta.fechaInicio;
                state.intento.tiempoLimiteMinutos = respuesta.tiempoLimiteMinutos || state.intento.tiempoLimiteMinutos;
            }

            state.pantalla = "prueba";
            guardarSesionActiva();
            mostrarPantalla("test");
            renderizarPregunta();
            iniciarTimer();
        }
        catch (error) {
            if (error.status === 409) {
                descartarSesionActiva();
            }
            mostrarError(error);
        }
        finally {
            state.comenzando = false;
            elements.beginTest.disabled = false;
        }
    }

    function renderizarPregunta() {
        if (!state.preguntas.length) {
            mostrarError(new Error("La prueba activa no tiene preguntas disponibles."));
            return;
        }

        var pregunta = preguntaActual();
        var total = state.preguntas.length;
        var numeroVisible = state.indiceActual + 1;
        var respuesta = state.respuestas[pregunta.idPregunta];

        elements.questionPosition.textContent = "Pregunta " + numeroVisible + " de " + total;
        elements.questionTitle.textContent = pregunta.ejemplo ? "Pregunta de ejemplo" : "Pregunta " + pregunta.numero;
        elements.questionContainer.innerHTML = "";

        var texto = document.createElement("p");
        texto.className = "question-text";
        texto.textContent = pregunta.enunciado || "Sin enunciado";
        elements.questionContainer.appendChild(texto);

        var opciones = document.createElement("div");
        opciones.className = "options";

        (pregunta.opciones || []).forEach(function (opcion) {
            opciones.appendChild(crearOpcion(pregunta, opcion, respuesta));
        });
        if (permiteNoSe()) {
            opciones.appendChild(crearOpcionNoSe(pregunta, respuesta));
        }

        elements.questionContainer.appendChild(opciones);
        actualizarNavegacion();
        actualizarProgreso();
    }

    function crearOpcion(pregunta, opcion, respuesta) {
        var label = document.createElement("label");
        label.className = "option";

        var input = document.createElement("input");
        input.type = "radio";
        input.name = "respuesta";
        input.value = opcion.idOpcion;
        input.checked = Boolean(respuesta && respuesta.tipo === "OPCION" && respuesta.opcionSeleccionadaId === opcion.idOpcion);
        input.addEventListener("change", function () {
            registrarRespuesta(pregunta.idPregunta, {
                tipo: "OPCION",
                opcionSeleccionadaId: opcion.idOpcion
            });
        });

        var contenido = document.createElement("span");
        contenido.innerHTML = '<span class="option-letter"></span> <span class="option-text"></span>';
        contenido.querySelector(".option-letter").textContent = (opcion.letra || "") + ".";
        contenido.querySelector(".option-text").textContent = opcion.texto || "";

        label.appendChild(input);
        label.appendChild(contenido);
        return label;
    }

    function crearOpcionNoSe(pregunta, respuesta) {
        var label = document.createElement("label");
        label.className = "option";

        var input = document.createElement("input");
        input.type = "radio";
        input.name = "respuesta";
        input.value = "NO_SE";
        input.checked = Boolean(respuesta && respuesta.tipo === "NO_SE");
        input.addEventListener("change", function () {
            registrarRespuesta(pregunta.idPregunta, {
                tipo: "NO_SE"
            });
        });

        var contenido = document.createElement("span");
        contenido.innerHTML = '<span class="option-letter">No sé.</span> <span class="option-text">Prefiero no responder esta pregunta.</span>';

        label.appendChild(input);
        label.appendChild(contenido);
        return label;
    }

    function registrarRespuesta(idPregunta, respuesta) {
        state.respuestas[idPregunta] = respuesta;
        marcarPendiente(idPregunta);
        guardarSesionActiva();
        actualizarProgreso();
        programarAutosave();
    }

    function limpiarRespuestaActual() {
        var pregunta = preguntaActual();
        if (!pregunta) return;

        delete state.respuestas[pregunta.idPregunta];
        marcarPendiente(pregunta.idPregunta);
        guardarSesionActiva();
        renderizarPregunta();
        programarAutosave();
    }

    function preguntaAnterior() {
        if (state.indiceActual <= 0) return;
        state.indiceActual--;
        guardarSesionActiva();
        renderizarPregunta();
    }

    function preguntaSiguiente() {
        if (state.indiceActual >= state.preguntas.length - 1) return;
        state.indiceActual++;
        guardarSesionActiva();
        renderizarPregunta();
    }

    function actualizarNavegacion() {
        elements.previousQuestion.disabled = state.indiceActual === 0 || state.enviando;
        elements.nextQuestion.disabled = state.indiceActual >= state.preguntas.length - 1 || state.enviando;
        elements.finishTest.disabled = state.enviando;
        elements.clearAnswer.disabled = state.enviando;
    }

    function actualizarProgreso() {
        var total = state.preguntas.length || 1;
        var respondidas = Object.keys(state.respuestas).length;
        var avance = ((state.indiceActual + 1) / total) * 100;

        elements.answeredCount.textContent = respondidas + " respondida" + (respondidas === 1 ? "" : "s");
        elements.progressBar.style.width = avance + "%";
    }

    async function finalizarPrueba(motivo) {
        if (state.enviando || !state.intento || !state.intento.idIntento) {
            return;
        }

        state.enviando = true;
        actualizarNavegacion();
        mostrarMensajeSincronizacion(motivo === "tiempo" ? "Tiempo agotado. Enviando respuestas..." : "Enviando respuestas...");

        try {
            detenerTimer();
            await enviarTodasLasRespuestas();
            await apiPost("intentos/" + encodeURIComponent(state.intento.idIntento) + "/finalizar", null);
            finalizarEnPantalla("Gracias", "Tus respuestas fueron registradas correctamente.");
        }
        catch (error) {
            if (error.tiempoAgotado) {
                finalizarEnPantalla("Tiempo agotado", "El tiempo límite fue alcanzado. Se registró el intento con las respuestas recibidas por el servidor.");
                return;
            }

            state.enviando = false;
            iniciarTimer();
            actualizarNavegacion();
            mostrarMensajeSincronizacion("");
            mostrarError(error);
        }
    }

    async function enviarTodasLasRespuestas() {
        var respuestas = state.preguntas.map(function (pregunta) {
            return respuestaParaApi(pregunta.idPregunta);
        });

        await apiPost("intentos/" + encodeURIComponent(state.intento.idIntento) + "/respuestas", {
            respuestas: respuestas
        });
    }

    function programarAutosave() {
        if (!state.intento || !state.intento.fechaInicio || state.pantalla !== "prueba" || state.enviando) return;

        mostrarMensajeSincronizacion("Guardando cambios localmente...");
        window.clearTimeout(state.autosaveId);
        state.autosaveId = window.setTimeout(sincronizarPendientes, 450);
    }

    async function sincronizarPendientes() {
        if (state.sincronizando || state.enviando || !state.intento) {
            return;
        }

        var ids = Object.keys(state.pendientes);
        if (!ids.length) {
            return;
        }

        state.sincronizando = true;
        mostrarMensajeSincronizacion("Sincronizando respuestas...");

        try {
            var respuestas = ids.map(respuestaParaApi);
            await apiPost("intentos/" + encodeURIComponent(state.intento.idIntento) + "/respuestas", {
                respuestas: respuestas
            });

            ids.forEach(function (id) {
                delete state.pendientes[id];
            });
            guardarSesionActiva();
            mostrarMensajeSincronizacion("Respuestas guardadas.");
        }
        catch (error) {
            if (error.tiempoAgotado) {
                finalizarEnPantalla("Tiempo agotado", "El tiempo límite fue alcanzado. Se registró el intento con las respuestas recibidas por el servidor.");
                return;
            }

            mostrarMensajeSincronizacion("No se pudo sincronizar ahora. Se reintentará al finalizar.");
        }
        finally {
            state.sincronizando = false;
        }
    }

    function respuestaParaApi(idPregunta) {
        var respuesta = state.respuestas[idPregunta];
        if (!respuesta) {
            return {
                preguntaId: idPregunta,
                clasificacionRespuesta: "OMITIDA"
            };
        }

        if (respuesta.tipo === "NO_SE") {
            return {
                preguntaId: idPregunta,
                clasificacionRespuesta: "NO_SE"
            };
        }

        return {
            preguntaId: idPregunta,
            opcionSeleccionadaId: respuesta.opcionSeleccionadaId
        };
    }

    function marcarPendiente(idPregunta) {
        state.pendientes[idPregunta] = true;
    }

    function iniciarTimer() {
        if (!state.intento || !state.intento.fechaInicio) {
            elements.timer.hidden = true;
            return;
        }

        detenerTimer();
        elements.timer.hidden = false;
        actualizarTimer();
        state.timerId = window.setInterval(actualizarTimer, 1000);
    }

    function detenerTimer() {
        if (state.timerId) {
            window.clearInterval(state.timerId);
            state.timerId = null;
        }
    }

    function actualizarTimer() {
        var restante = tiempoRestanteMs();
        if (restante <= 0) {
            elements.timer.textContent = "00:00";
            elements.timer.classList.add("warning");
            detenerTimer();
            finalizarPrueba("tiempo");
            return;
        }

        var totalSegundos = Math.ceil(restante / 1000);
        var minutos = Math.floor(totalSegundos / 60);
        var segundos = totalSegundos % 60;

        elements.timer.textContent = rellenar(minutos) + ":" + rellenar(segundos);
        elements.timer.classList.toggle("warning", restante <= 60000);
    }

    function tiempoRestanteMs() {
        if (!state.intento || !state.intento.fechaInicio) return 0;

        var inicio = Date.parse(state.intento.fechaInicio);
        if (Number.isNaN(inicio)) {
            inicio = Number(state.intento.inicioCliente) || Date.now();
            state.intento.inicioCliente = inicio;
        }

        var minutos = Number(state.intento.tiempoLimiteMinutos || (state.prueba && state.prueba.tiempoLimiteMinutos) || 0);
        return inicio + minutos * 60000 - Date.now();
    }

    function finalizarEnPantalla(titulo, mensaje) {
        detenerTimer();
        window.clearTimeout(state.autosaveId);
        state.enviando = false;
        state.pantalla = "final";
        localStorage.removeItem(ACTIVE_SESSION_KEY);
        elements.timer.hidden = true;
        elements.finalTitle.textContent = titulo;
        elements.finalMessage.textContent = mensaje;
        mostrarPantalla("final");
    }

    function actualizarResumenPrueba() {
        if (!state.prueba) {
            elements.testSummary.textContent = "Cargando prueba activa...";
            return;
        }

        var total = state.preguntas.length;
        var minutos = state.prueba.tiempoLimiteMinutos || (state.intento && state.intento.tiempoLimiteMinutos);
        elements.testSummary.textContent = (state.prueba.nombre || "Prueba activa") +
            " · " + total + " pregunta" + (total === 1 ? "" : "s") +
            (minutos ? " · " + minutos + " minutos" : "");
    }

    function mostrarPantalla(nombre) {
        Object.keys(screens).forEach(function (key) {
            screens[key].hidden = key !== nombre;
        });
    }

    function mostrarError(error) {
        detenerTimer();
        elements.timer.hidden = true;
        elements.errorMessage.textContent = mensajeAmigable(error);
        mostrarPantalla("error");
    }

    function mensajeAmigable(error) {
        if (!error) return "Ocurrió un error inesperado.";
        if (error.status === 400) return "La información enviada no es válida. " + (error.message || "Revisa los datos e intenta de nuevo.");
        if (error.status === 404) return "No se encontró la prueba o el recurso solicitado. " + (error.message || "");
        if (error.status === 409) return "La prueba no pudo continuar. " + (error.message || "Verifica si el tiempo se agotó o si el intento ya finalizó.");
        if (error.status >= 500) return "Ocurrió un error interno del servidor. Intenta de nuevo más tarde.";
        if (error.message) return error.message;
        return "No se pudo comunicar con la API. Revisa tu conexión e intenta de nuevo.";
    }

    function mostrarMensajeSincronizacion(mensaje) {
        elements.syncStatus.textContent = mensaje || "";
    }

    function limpiarMensajeSincronizacion() {
        mostrarMensajeSincronizacion("");
    }

    function bloquearFormulario(bloquear) {
        Array.prototype.forEach.call(elements.form.elements, function (elemento) {
            elemento.disabled = bloquear;
        });
    }

    function guardarSesionActiva() {
        if (!state.intento) return;

        var sesion = {
            prueba: state.prueba,
            intento: state.intento,
            preguntas: state.preguntas,
            respuestas: state.respuestas,
            indiceActual: state.indiceActual,
            pantalla: state.pantalla,
            pendientes: state.pendientes
        };
        localStorage.setItem(ACTIVE_SESSION_KEY, JSON.stringify(sesion));
    }

    function descartarSesionActiva() {
        localStorage.removeItem(ACTIVE_SESSION_KEY);
        state.intento = null;
        state.respuestas = {};
        state.pendientes = {};
        state.indiceActual = 0;
        state.pantalla = "inicio";
    }

    function cargarSesionActiva() {
        try {
            var raw = localStorage.getItem(ACTIVE_SESSION_KEY);
            return raw ? JSON.parse(raw) : null;
        }
        catch (error) {
            localStorage.removeItem(ACTIVE_SESSION_KEY);
            return null;
        }
    }

    function restaurarSesion(sesion) {
        state.prueba = sesion.prueba;
        state.intento = sesion.intento;
        state.preguntas = Array.isArray(sesion.preguntas) ? sesion.preguntas : [];
        state.respuestas = sesion.respuestas || {};
        state.indiceActual = Math.min(Number(sesion.indiceActual) || 0, Math.max(state.preguntas.length - 1, 0));
        state.pantalla = sesion.pantalla || "instrucciones";
        state.pendientes = sesion.pendientes || {};
    }

    function normalizarSesionRestaurada() {
        if (state.pantalla === "prueba" && (!state.intento || !state.intento.fechaInicio)) {
            state.pantalla = "instrucciones";
            state.indiceActual = 0;
            guardarSesionActiva();
        }
    }

    async function apiGet(path) {
        return apiRequest(path, {
            method: "GET"
        });
    }

    async function apiPost(path, body) {
        var options = {
            method: "POST"
        };

        if (body !== null) {
            options.body = JSON.stringify(body);
        }

        return apiRequest(path, options);
    }

    async function apiRequest(path, options) {
        var headers = {
            Accept: "application/json"
        };

        if (options.body) {
            headers["Content-Type"] = "application/json";
        }

        var response;
        try {
            response = await fetch(apiUrl(path), {
                method: options.method,
                headers: headers,
                body: options.body
            });
        }
        catch (error) {
            throw new Error("No se pudo comunicar con la API.");
        }

        var data = await leerJson(response);
        if (!response.ok) {
            var apiError = new Error((data && (data.error || data.mensaje)) || "Error de API");
            apiError.status = response.status;
            apiError.data = data;
            apiError.tiempoAgotado = response.status === 409 && data && data.estado === "TIEMPO_AGOTADO";
            throw apiError;
        }

        return data;
    }

    async function leerJson(response) {
        var texto = await response.text();
        if (!texto) return null;

        try {
            return JSON.parse(texto);
        }
        catch (error) {
            return null;
        }
    }

    function apiUrl(path) {
        return new URL(path.replace(/^\/+/, ""), API_BASE_URL).toString();
    }

    function preguntaActual() {
        return state.preguntas[state.indiceActual];
    }

    function permiteNoSe() {
        return true;
    }

    function valor(id) {
        return document.getElementById(id).value.trim();
    }

    function agregarSiTieneValor(objeto, nombre, valorCampo) {
        if (valorCampo) {
            objeto[nombre] = valorCampo;
        }
    }

    function rellenar(numero) {
        return String(numero).padStart(2, "0");
    }
})();
