/* ==========================================================================
   Demo entrevista Java Senior — lógica de la landing page.
   Vanilla JS: sin build, sin framework, sin CDN.
   ========================================================================== */

const PREGUNTAS = [
    {
        numero: 1,
        titulo: "ArrayList vs LinkedList",
        queDemuestra: "Implementamos ArrayList y LinkedList desde cero (solo primitivas) y comparamos " +
            "con las del JDK: acceso por índice, inserción, eliminación en los extremos y el coste del boxing.",
        queResponder: "Ambos implementan List; LinkedList además es un Deque. ArrayList es un array " +
            "redimensionable: get(i) es O(1) pero insertar en medio es O(n). LinkedList es una cadena de " +
            "nodos: insertar/eliminar en los extremos es O(1) pero get(i) es O(n) porque hay que recorrerla. " +
            "En la práctica, ArrayList es la respuesta correcta casi siempre.",
        acciones: [
            { etiqueta: "Benchmark", url: "/q01/benchmark?size=5000&iterations=20", principal: true },
            { etiqueta: "Internals", url: "/q01/internals?elements=5" }
        ]
    },
    {
        numero: 2,
        titulo: "REST vs SOAP",
        queDemuestra: "El mismo servicio en los dos transportes: endpoint REST con contrato OpenAPI y " +
            "endpoint SOAP real (Spring-WS) con su WSDL, más el round-trip del sobre SOAP crudo.",
        queResponder: "La comparación teórica importa poco; lo que delata la experiencia real son los " +
            "contratos: el WSDL genera los stubs con wsimport y el OpenAPI es su equivalente en REST " +
            "(genera clientes con openapi-generator). Ambos son 'contrato primero'. SOAP añade XML " +
            "verboso y especificaciones que se solapan; REST usa el propio HTTP y OpenAPI, que además " +
            "alimenta Swagger UI y Postman.",
        acciones: [
            { etiqueta: "REST", url: "/q02/rest/greeting?name=Ana", principal: true },
            { etiqueta: "SOAP", url: "/q02/soap-roundtrip?name=Ana" },
            { etiqueta: "Comparar", url: "/q02/compare" },
            { etiqueta: "OpenAPI ↔ WSDL", url: "/q02-api.html", link: true }
        ]
    },
    {
        numero: 3,
        titulo: "Merge vs rebase",
        queDemuestra: "Un script construye un repositorio de prueba, provoca un conflicto y muestra el DAG " +
            "de un merge frente al historial lineal de un rebase, incluido el cambio de SHAs y el stash.",
        queResponder: "Si hay conflictos, merge conserva el historial (crea un commit de merge y el " +
            "historial queda como un DAG con intersecciones) y rebase mueve la base de la rama sobre la " +
            "punta de otra, dejando el historial lineal pero reescribiendo los commits (cambian los SHA). " +
            "Regla de oro: nunca rebases una rama pública. El stash es un estante de cambios del working " +
            "directory, no una rama: no se puede ramificar desde él ni pushearlo.",
        acciones: [
            { etiqueta: "Resumen", url: "/q03/summary", principal: true },
            { etiqueta: "Transcripción", url: "/q03/transcript" },
            { etiqueta: "Ejecutar el script", url: "scripts/git-merge-vs-rebase.sh", nota: "Ejecuta ./scripts/git-merge-vs-rebase.sh en la terminal" }
        ]
    },
    {
        numero: 4,
        titulo: "Pirámide de testing",
        queDemuestra: "El inventario real de tests del proyecto por capa (unit, component, integration, E2E) " +
            "más el bloque lateral de carga y performance, con un test de carga que se ejecuta de verdad.",
        queResponder: "La pirámide son las capas: muchos tests unitarios abajo, integration en medio, " +
            "UI/E2E arriba (pocos y lentos). Para un senior hay que añadir el segundo bloque: tests de " +
            "componente, de carga y de performance. La clave no es la cantidad de capas sino que cada " +
            "capa pruebe lo que le corresponde, y que el tiempo total siga siendo bajo.",
        acciones: [
            { etiqueta: "Pirámide", url: "/q04/pyramid", principal: true },
            { etiqueta: "Test de carga", url: "/q04/load?requests=200&concurrency=20" }
        ]
    },
    {
        numero: 5,
        titulo: "Ordenar un HashSet",
        queDemuestra: "El orden de iteración de un HashSet NO es el de inserción ni el ordenado: lo " +
            "calculamos (hash, cubo, tabla), y vemos colisiones y treeificación en acción.",
        queResponder: "La respuesta es 'no se puede' sin una estructura extra: TreeSet (orden natural), " +
            "LinkedHashSet (orden de inserción) o pasarlo a List. Y el porqué: un HashSet es una tabla " +
            "de cubos indexada por hashCode; se itera en orden de cubo, no de inserción. Con 8 elementos " +
            "en el mismo cubo el JDK lo convierte en árbol rojo-negro.",
        acciones: [
            { etiqueta: "Orden", url: "/q05/order?strategy=tree&size=20", principal: true },
            { etiqueta: "Explicar el hash", url: "/q05/explain?size=20" },
            { etiqueta: "Colisiones", url: "/q05/collisions" },
            { etiqueta: "Treeificación", url: "/q05/treeification" }
        ]
    },
    {
        numero: 6,
        titulo: "Jerarquía de excepciones",
        queDemuestra: "Una jerarquía sellada (AppException → Business/Technical) que el compilador " +
            "conoce entera, con la fault barrier que evita que un fallo de la BD llegue al cliente.",
        queResponder: "La jerarquía debe separar fallos de negocio de fallos técnicos: los primeros son " +
            "esperados y el cliente puede actuar; los segundos son de infraestructura. La fault barrier " +
            "es la capa que traduce los técnicos en errores de negocio, de modo que el SQLSTATE no cruza " +
            "la frontera HTTP. Con tipos sellados, el switch del handler es exhaustivo sin default: " +
            "añadir una excepción nueva rompe el build hasta que se le asigne un status.",
        acciones: [
            { etiqueta: "Jerarquía", url: "/q06/hierarchy", principal: true },
            { etiqueta: "Fault barrier", url: "/q06/fault-barrier?fail=true" },
            { etiqueta: "Result en vez de excepción", url: "/q06/result?sku=SKU-2&cantidad=5" },
            { etiqueta: "404 de negocio", url: "/q06/throw?case=not-found" }
        ]
    },
    {
        numero: 7,
        titulo: "Identificar problemas de rendimiento",
        queDemuestra: "El proceso completo: medir cada paso con nanoTime, localizar con un profiler de " +
            "muestreo propio y con JFR (el profiler que trae el JDK, sin instalar nada).",
        queResponder: "Primero se mide; sin medición cualquier optimización es una suposición. Después " +
            "se localiza: con logs y tiempo por paso, o con un profiler que muestrea automáticamente. En " +
            "producción, JFR: -XX:StartFlightRecording=filename=app.jfr,settings=profile. Y el siguiente " +
            "paso es medir de nuevo para comprobar que la mejora es real.",
        acciones: [
            { etiqueta: "Medir", url: "/q07/measure?iterations=100", principal: true },
            { etiqueta: "Profiler de muestreo", url: "/q07/sampling?durationMs=2000" },
            { etiqueta: "JFR (el del JDK)", url: "/q07/jfr?durationMs=2000" }
        ]
    },
    {
        numero: 8,
        titulo: "Datos enormes en Redis",
        queDemuestra: "Compresión gzip + troceado en chunks de 256 KiB sobre un HASH de Redis (HGETALL), " +
            "con tres almacenes: Redis, memoria y disco. Si no hay Redis, la app NO se cae.",
        queResponder: "La primera pregunta es si tiene sentido meter datos enormes en Redis: hay un " +
            "límite de 512 MB por item y la RAM del host es el recurso escaso. Se puede trocear y " +
            "comprimir, pero eso no arregla el fondo: la respuesta correcta es un almacén de documentos " +
            "o una base NoSQL. En una caché, lo que se guarda son datos pequeños y hot; lo grande va " +
            "a otro sitio.",
        acciones: [
            { etiqueta: "Estado del almacén", url: "/q08/store", principal: true },
            { etiqueta: "Benchmark", url: "/q08/benchmark?sizeMb=4" }
        ]
    },
    {
        numero: 9,
        titulo: "AOP y su mayor trampa",
        queDemuestra: "Tres aspects reales (log, autorización, caché) y la trampa DEMOSTRADA: el " +
            "sobrecoste por proxy, el proxy que se filtra y la self-invocation donde el aspect no se dispara.",
        queResponder: "AOP permite capas transversales (logging, autorización, caché). Su mayor trampa " +
            "es el sobrecoste: Spring AOP usa proxies y reflexión, así que una llamada directa es más " +
            "rápida; el coste es proporcional al del método, así que el peligro es aplicarlo a métodos " +
            "hot y triviales. Y la trampa de verdad: es AOP por proxy, así que una self-invocation " +
            "(this.metodo()) NO pasa por el proxy y el aspect no se dispara. El arreglo es AspectJ en " +
            "tiempo de compilación.",
        acciones: [
            { etiqueta: "Sobrecoste", url: "/q09/overhead?iterations=200000", principal: true },
            // El aspect de autorización exige la cabecera X-Role: sin ella responde 403,
            // que es justo lo que hay que enseñar en la demo.
            { etiqueta: "Self-invocation", url: "/q09/self-invocation", cabeceras: { "X-Role": "ADMIN" } },
            { etiqueta: "El proxy se filtra", url: "/q09/proxy-info" },
            { etiqueta: "Pedido con aspecto", url: "/q09/order?id=42", cabeceras: { "X-Role": "USER" } }
        ]
    },
    {
        numero: 10,
        titulo: "Limitaciones de GET",
        queDemuestra: "El presupuesto de ~2K caracteres frente al límite real del contenedor, el body en " +
            "un GET y la mutación de estado con GET (el contador lo hace visible).",
        queResponder: "GET solo recupera recursos y no debe alterar el estado (salvo caching). Su " +
            "limitación principal es que no tiene body: todo va en la query string, y la URL tiene un " +
            "largo limitado (~2K de forma práctica; el límite duro lo impone el servidor). En la " +
            "práctica se puede mandar un body con GET, pero no está en la clave de caché, los proxies " +
            "lo descartan y viola la semántica del método.",
        acciones: [
            { etiqueta: "Medir la URL", url: "/q10/measure?length=3000", principal: true },
            { etiqueta: "Body en un GET", url: "/q10/body-probe" },
            { etiqueta: "GET que muta (anti-patrón)", url: "/q10/unsafe-increment" },
            { etiqueta: "POST correcto", url: "/q10/safe-increment", method: "POST" }
        ]
    }
];

/* ---------------------------------------------------------------- render */

function crearTarjeta(p) {
    const card = document.createElement("article");
    card.className = "card";

    const head = document.createElement("div");
    head.className = "card__head";
    head.innerHTML = `<span class="card__number">${p.numero}</span><h2 class="card__title"></h2>`;
    head.querySelector(".card__title").textContent = p.titulo;
    card.appendChild(head);

    const what = document.createElement("p");
    what.className = "card__what";
    what.textContent = p.queDemuestra;
    card.appendChild(what);

    const answer = document.createElement("p");
    answer.className = "card__answer";
    answer.innerHTML = "<strong>Qué responder</strong>";
    answer.appendChild(document.createTextNode(p.queResponder));
    card.appendChild(answer);

    const actions = document.createElement("div");
    actions.className = "card__actions";
    p.acciones.forEach(a => actions.appendChild(crearBoton(a)));
    card.appendChild(actions);

    return card;
}

function crearBoton(a) {
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = "btn" + (a.principal ? " btn--primary" : "");
    btn.textContent = a.etiqueta;

    btn.addEventListener("click", () => {
        if (a.link) {
            window.open(a.url, "_blank");
            return;
        }
        if (a.nota) {
            mostrar(`${a.etiqueta}`, a.nota);
            return;
        }
        const metodo = a.method || "GET";
        ejecutar(a.url, metodo, a.etiqueta, a.cabeceras);
    });

    return btn;
}

/* ---------------------------------------------------------------- fetch */

async function ejecutar(url, metodo, etiqueta, cabeceras) {
    mostrar(`${etiqueta} — ${metodo} ${url}`, "Cargando…");
    try {
        const opciones = { method: metodo };
        if (cabeceras) {
            opciones.headers = cabeceras;
        }
        const response = await fetch(url, opciones);
        const texto = await response.text();
        let cuerpo = texto;
        try {
            cuerpo = JSON.stringify(JSON.parse(texto), null, 2);
        } catch (e) {
            // No es JSON (por ejemplo el WSDL): se muestra tal cual.
        }
        mostrar(`${etiqueta} — HTTP ${response.status}`, cuerpo);
    } catch (error) {
        mostrar(`${etiqueta} — error`, String(error));
    }
}

function mostrar(titulo, cuerpo) {
    const output = document.getElementById("output");
    document.getElementById("output-title").textContent = titulo;
    document.getElementById("output-body").textContent = cuerpo;
    output.hidden = false;
    output.scrollIntoView({ behavior: "smooth", block: "nearest" });
}

/* ---------------------------------------------------------------- status */

async function cargarEstado() {
    try {
        const health = await fetch("/actuator/health").then(r => r.json());
        setStatus("status-health", health.status || "—", health.status === "UP" ? "ok" : "warn");
    } catch (e) {
        setStatus("status-health", "sin conexión", "error");
    }

    try {
        const store = await fetch("/q08/store").then(r => r.json());
        const activo = store?.data?.almacenActivo || "?";
        setStatus("status-store", activo, activo === "redis" ? "ok" : "warn");
    } catch (e) {
        setStatus("status-store", "?", "warn");
    }

    try {
        const response = await fetch("/q02/wsdl");
        setStatus("status-wsdl", response.ok ? "disponible" : "error", response.ok ? "ok" : "error");
    } catch (e) {
        setStatus("status-wsdl", "error", "error");
    }
}

function setStatus(id, valor, clase) {
    const element = document.getElementById(id);
    element.textContent = valor;
    element.className = "status__value" + (clase ? " status__value--" + clase : "");
}

/* ---------------------------------------------------------------- init */

document.addEventListener("DOMContentLoaded", () => {
    const grid = document.getElementById("grid");
    PREGUNTAS.forEach(p => grid.appendChild(crearTarjeta(p)));

    document.getElementById("output-close").addEventListener("click", () => {
        document.getElementById("output").hidden = true;
    });

    cargarEstado();
});
