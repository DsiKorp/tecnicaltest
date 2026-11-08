# Demo de entrevista Java Senior — las 10 preguntas de EPAM

Proyecto de práctica para una entrevista de **Java Senior**, construido sobre el artículo
*"10 senior Java developer interview questions and answers"* de
[Gaetano Piazzolla (EPAM)](https://www.epam.com/).

Cada una de las 10 preguntas tiene:

- una **implementación real** (no un maqueta): las colecciones desde cero, un endpoint SOAP
  publishing un WSDL de verdad, una jerarquía de excepciones sellada, un profiler de muestreo
  propio, AOP con los tres casos de uso del artículo…
- sus **tests**, repartidos en las capas de la pirámide de testing;
- un **endpoint HTTP** para poder hacer la demo en vivo desde el navegador.

La idea es poder abrir un terminal, arrancar la app y responder *"déjame enseñarte"* con código
funcionando delante del entrevistador.

---

## 1. Requisitos

| Herramienta | Versión | Nota |
|---|---|---|
| JDK | 25 (Temurin) | `<java.version>25</java.version>` en el POM |
| Maven | 3.9+ | el wrapper `./mvnw` descarga la 3.10.0 en el primer uso |
| Docker | 20+ | solo para Redis y para el despliegue |

> **Sin conexión a internet** el wrapper `./mvnw` falla al descargarse. Si tienes Maven instalado
> en el sistema, `mvn` funciona igual. El `Dockerfile` usa la imagen `maven:`, nunca el wrapper.

---

## 2. Cómo arrancarlo

### Opción A — con Maven (la habitual)

```bash
./mvnw clean verify          # compila y pasa todos los tests
./mvnw spring-boot:run       # arranca en http://localhost:8080
```

Abre <http://localhost:8080>: la landing page tiene las 10 tarjetas, cada una con su botón.

### Opción B — con Docker (y Redis)

```bash
docker compose up -d --build
curl -s localhost:8080/q08/store     # debe decir "redis"
```

### Opción C — sin Redis (el escenario de AWS)

```bash
EPAM_Q08_MODE=memory ./mvnw spring-boot:run
curl -s localhost:8080/q08/store     # dice "in-memory"
```

La app **nunca se cae por falta de Redis**: hace un único `PING` con timeout de 1 s al arrancar
y, si no responde, cae al almacén en memoria con un `WARN` en el log.

### Endpoints útiles

| URL | Qué es |
|---|---|
| <http://localhost:8080> | landing page con las 10 demos |
| <http://localhost:8080/q02-api.html> | WSDL y OpenAPI lado a lado |
| <http://localhost:8080/actuator/health> | salud, incluido qué almacén de la P8 está activo |
| <http://localhost:8080/q02/wsdl> | el WSDL servido por la app |
| <http://localhost:8080/q02/openapi.yaml> | el documento OpenAPI |

---

## 3. Mapa de endpoints

| # | Pregunta | Endpoints |
|---|---|---|
| 1 | ArrayList vs LinkedList | `/q01/benchmark`, `/q01/internals` |
| 2 | REST vs SOAP | `/q02/rest/greeting`, `/q02/soap-roundtrip`, `/q02/compare`, `/q02/wsdl`, `/q02/openapi.yaml` |
| 3 | Merge vs rebase | `/q03/summary`, `/q03/transcript` (+ `scripts/git-merge-vs-rebase.sh`) |
| 4 | Pirámide de testing | `/q04/pyramid`, `/q04/load` |
| 5 | Ordenar un HashSet | `/q05/order`, `/q05/explain`, `/q05/collisions`, `/q05/treeification` |
| 6 | Jerarquía de excepciones | `/q06/hierarchy`, `/q06/throw`, `/q06/fault-barrier`, `/q06/result` |
| 7 | Problemas de rendimiento | `/q07/measure`, `/q07/sampling`, `/q07/jfr` |
| 8 | Datos enormes en Redis | `/q08/store`, `/q08/blob/{id}`, `/q08/benchmark` |
| 9 | AOP y su trampa | `/q09/order`, `/q09/overhead`, `/q09/self-invocation`, `/q09/proxy-info` |
| 10 | Limitaciones de GET | `/q10/measure`, `/q10/body-probe`, `/q10/unsafe-increment`, `POST /q10/safe-increment` |

---

## 4. Qué responder en la entrevista, pregunta por pregunta

### 1. ArrayList vs LinkedList

**Qué responder.** Ambos implementan `List`; `LinkedList` además implementa `Deque`, así que
también sirve como cola FIFO. La diferencia es de interna: `ArrayList` es un array redimensionable
(`get(i)` es O(1), insertar en medio es O(n) porque hay que desplazar), `LinkedList` es una cadena
de nodos con punteros a siguiente y anterior (insertar o eliminar en los extremos es O(1), pero
`get(i)` es O(n) porque hay que recorrer la cadena). En la práctica `ArrayList` es la respuesta
casi siempre: `LinkedList` solo gana cuando el acceso es secuencial o se manipulan los extremos.

**Punteros de código.**

- `q01collections/SimpleArrayList.java` — array de backing, crecimiento ×1,5, iterador *fail-fast*
  con `ConcurrentModificationException` (como el JDK).
- `q01collections/SimpleLinkedList.java` — nodos `next`/`prev`, punteros `head`/`tail`, y las
  operaciones de `Deque` (`push`, `pop`, `offer`, `poll`).
- `q01collections/IntArrayList.java` — `int[]` puro, sin boxing. Es la respuesta literal a
  *"implementa un ArrayList usando solo primitivas"*.
- `GET /q01/benchmark` compara nuestras tres implementaciones con las del JDK en cuatro
  escenarios (append, acceso aleatorio, remove por delante, remove por detrás).

**Extra que suma puntos.** El coste del boxing: un `Integer` es un objeto de ~16 bytes, un `int`
en un array son 4. Un `ArrayList<Integer>` con 1M de elementos usa unas 6 veces más memoria que un
`int[]`, y añade un indirección en cada lectura.

---

### 2. REST vs SOAP

**Qué responder.** La comparación teórica ("SOAP es un protocolo y REST no") no dice mucho. Lo que
distingue a alguien con experiencia real son los **contratos**: el WSDL genera los stubs de Java
con `wsimport`, y el **OpenAPI es su equivalente en el mundo REST** (genera clientes con
`openapi-generator`, alimenta Swagger UI y Postman). Los dos son "contrato primero": se escribe el
contrato y la implementación se genera o se valida contra él. En la práctica SOAP es XML verboso y
arrastra un binding por operación con especificaciones que se solapan; REST usa el propio HTTP y
un único documento que lo describe todo. SOAP se sigue eligiendo cuando hay WS-Security o
interoperabilidad con stacks legacy.

**Punteros de código.**

- `q02webservices/Q02WebServicesConfig.java` — `@EnableWs` + el `MessageDispatcherServlet` de
  Spring-WS.
- `q02webservices/soap/GreetingSoapEndpoint.java` — el endpoint SOAP real (`@PayloadRoot`,
  `@RequestPayload`, `@ResponsePayload`, `@Endpoint`).
- `q02webservices/SoapGreetingClient.java` — cliente que habla el protocolo a pelo, para poder
  **enseñar el sobre SOAP** al lado del JSON de REST.
- `src/main/resources/wsdl/q02-greeting.wsdl` — el WSDL 1.1 que genera los stubs.
- `src/main/resources/openapi/q02-openapi.yaml` — el OpenAPI 3.0, fuente única de verdad.
- `GET /q02/compare` — la comparación en JSON, legible por máquina.

**Extra que suma puntos.** En Spring-WS 5 (y en Spring Boot 4) el estilo moderno es
`@PayloadRoot` en vez del `@WebService` clásico de JAX-WS: el mapping se hace por el **elemento
raíz del sobre**, no por la URL, y el mismo endpoint atiende cualquier ruta bajo `/soap/*`.

---

### 3. Merge vs rebase

**Qué responder.** Cuando hay conflictos hay dos salidas. `merge` **conserva** el historial: crea
un commit de merge y el historial queda como un DAG con intersecciones. `rebase` **reescribe** el
historial: mueve la base de la rama sobre la punta de otra, dejándolo lineal, pero **los SHAs de
los commits cambian**. Regla de oro: **nunca rebases una rama pública**, porque cualquiera que ya
se haya descargado los commits antiguos acaba con historia duplicada o conflicts extra. Y el
`stash` no es una rama: es un estante de cambios del working directory, no se puede ramificar desde
él ni pushearlo, y conviene no acumular varios.

**Punteros de código.**

- `scripts/git-merge-vs-rebase.sh` — crea un repo de prueba, provoca un conflicto y muestra el
  DAG de un merge frente al historial lineal de un rebase, el cambio de SHAs y el stash.
  La transcripción queda en `target/git-demo/transcript.txt`.
- `q03gitops/GitDemoController.java` — `/q03/summary` (la respuesta) y `/q03/transcript`.
- `docs/q03-merge-vs-rebase.md` — la respuesta por escrito.

**Cómo demoarlo.** En una terminal: `./scripts/git-merge-vs-rebase.sh`. En el navegador:
`/q03/transcript`.

---

### 4. Pirámide de testing

**Qué responder.** La pirámide son las **capas**: muchos tests unitarios abajo (rápidos, sin
Spring), integration en medio (la capa web y la de persistencia, con el contexto real), y UI/E2E
arriba (pocos, lentos, frágiles). Para un senior hay que añadir el **segundo bloque**: tests de
componente, de **carga** y de **performance**. Lo importante no es cuántas capas hay, sino que cada
capa pruebe lo que le corresponde y que el tiempo total de la suite siga siendo bajo.

**Punteros de código.**

- `q04testing/TestLayer.java` — enum con las capas y **el inventario real de tests** de cada una.
- `q04testing/LoadTestRunner.java` — test de carga con `HttpClient` y virtual threads; devuelve
  p50/p95/p99 y peticiones/segundo.
- `src/test/java/.../` — la pirámide entera:
  - **unit** (sin Spring): `SimpleArrayListTest`, `SimpleLinkedListTest`,
    `HashSetOrderingServiceTest`, `OrderServiceTest`, `BlobCodecTest`,
    `InMemoryChunkedBlobStoreTest`, `GetLimitsServiceTest`
  - **component**: `WebLayerComponentTest`, `AopAspectIntegrationTest`
  - **integration**: `Q02WebServicesIntegrationTest` (el round-trip SOAP real)
  - **E2E**: `DemosEndToEndTest` (recorre las 10 demos)
  - **load**: `LoadTest` (fuera del run por defecto: `mvn test -Dtest=LoadTest -Dsurefire.excludes=`)

**Extra que suma puntos.** En los tests **nunca se asserta sobre tiempo**: los asserts de timing
son flaky y envenenan el build. El timing va en los endpoints de demo, que son la "demo", no el
test.

---

### 5. Ordenar un HashSet de enteros

**Qué responder.** La respuesta es *"no se puede"* sin una estructura adicional: `TreeSet` (orden
natural), `LinkedHashSet` (orden de inserción) o pasarlo a `List`. Y el porqué de que sea un
**hash** set: es una tabla de cubos indexada por `hashCode()`, y se itera **en orden de cubo**, no
de inserción ni ordenado. De ahí vienen las dos demos: la colisión clásica (`"Ea"` y `"FB"` tienen
el mismo `hashCode()`, 2236) y la **treeificación** (cuando un cubo llega a 8 entradas, el JDK lo
convierte en un árbol rojo-negro en vez de una lista; si la tabla aún es pequeña, primero la
redimensiona).

**Punteros de código.**

- `q05hashset/HashSetOrderingService.java` — `explain()` calcula el índice de cubo real
  (`(h ^ h>>>16) & (tableSize-1)`), `collisions()` y `treeification()`.
- `q05hashset/HashSetController.java` — `/q05/order`, `/q05/explain`, `/q05/collisions`,
  `/q05/treeification`.

**Extra que suma puntos.** El índice de cubo se calcula contra 16 (la capacidad por defecto)
porque **no se puede leer la tabla interna del JDK** desde fuera: es el mismo truco que usan las
herramientas de análisis de JDKs.

---

### 6. Jerarquía de excepciones

**Qué responder.** La jerarquía debe separar dos familias: los **fallos de negocio** (esperados, el
cliente puede actuar: "no hay stock", "el pedido no existe") y los **fallos técnicos** (de
infraestructura: la BD no responde). Con esa separación se puede construir una **fault barrier**:
la capa de servicio atrapa los técnicos y los traduce a errores de negocio, de modo que el SQLSTATE
o el timeout **nunca cruzan la frontera HTTP** hacia el cliente. Además, con tipos **sellados**
(`sealed`) el compilador conoce el conjunto completo de subtipos: el `switch` del handler global
es exhaustivo **sin `default`**, así que si se añade una excepción concreta nueva el build falla
hasta que se le asigne un status HTTP. Y para los fallos **esperados** hay una alternativa a la
excepción: un `Result<T>` sellado, donde el fallo es un valor de retorno y no un stack trace en el
camino normal.

**Punteros de código.**

- `q06exceptions/AppException.java` — raíz sellada; `BusinessException` y `TechnicalException`
  como familias `non-sealed`.
- `q06exceptions/OrderService.java` — **la fault barrier**, en un solo `catch`.
- `q06exceptions/Result.java` — la variante sin excepciones para el flujo esperado.
- `support/web/GlobalExceptionHandler.java` — el tercer nivel de captura y el `switch` exhaustivo
  que mapea a `ProblemDetail` (RFC 9457).
- `GET /q06/fault-barrier?fail=true` — lo hace fallar en vivo y muestra que al cliente le llega un
  409 de negocio, no un 500 con el SQLSTATE.

---

### 7. Identificar problemas de rendimiento

**Qué responder.** El primer paso es **medir**: sin medición, cualquier "optimización" es una
suposición. Se mide con `System.nanoTime()` por paso (con *warmup*, que si no lo que se mide es el
coste de compilación de la JIT, no el del algoritmo). El segundo paso es **localizar** la pieza
exacta: a mano con logs, o automáticamente con un profiler. En producción, **JFR** (Java Flight
Recorder), que viene en el JDK: `-XX:StartFlightRecording=filename=app.jfr,settings=profile`. El
tercer paso es **optimizar y volver a medir**, para comprobar que la mejora es real.

**Punteros de código.**

- `q07performance/MeasurementService.java` — el "medir" con `nanoTime` por paso.
- `q07performance/SamplingProfiler.java` — profiler de muestreo propio: un hilo observa la pila
  del hilo de trabajo cada milisegundo y agrega las hojas en un histograma.
- `q07performance/JfrProfiler.java` — **JFR real** con `jdk.jfr.Recording` y
  `jdk.jfr.consumer.RecordingFile`. Cero dependencias.

**Extra que suma puntos.** El sesgo conocido del muestreo: solo se muestrea en hilos `RUNNABLE`,
no en los bloqueados. Y la alternativa a los proxies de runtime: **AspectJ en tiempo de
compilación** (weaving), que elimina el sobrecoste de la reflexión.

---

### 8. Datos enormes en una caché Redis

**Qué responder.** La primera pregunta es *"¿tiene sentido tener datos enormes en Redis?"*: hay
un **límite de 512 MB por item** y la RAM del host es el recurso escaso. Se puede **trocear**
(un HASH por blob, o varios items) y **comprimir** (gzip), pero eso no arregla el fondo: la
respuesta correcta es **no usar Redis para eso**, sino un almacén de documentos o una base NoSQL.
Redis es una **caché**: lo que se guarda son datos pequeños y calientes; lo grande va a otro sitio.
Y si aun así se usa, hay que ser consciente de que leer un valor enorme entero está entre los
peores prácticas de Redis.

**Punteros de código.**

- `q08redis/BlobCodec.java` — la lógica pura: gzip + troceado en chunks de 256 KiB. Es
  testeable **sin Redis**, y por eso la lógica interesante está separada del almacén.
- `q08redis/RedisChunkedBlobStore.java` — un HASH por blob (`__meta` + `chunk:000000`…), lectura
  con **`HGETALL`**.
- `q08redis/FileSystemBlobStore.java` — **la respuesta correcta**: el almacén de documentos.
- `q08redis/InMemoryChunkedBlobStore.java` — el fallback.
- `q08redis/ChunkedBlobStoreConfiguration.java` — la autodetección: un `PING` con timeout de 1 s
  al arrancar; si no responde, fallback en memoria con `WARN`.
- `GET /q08/store` — informa de qué almacén está vivo y por qué.

**Extra que suma puntos.** Gzip solo comprime si hay redundancia: con datos aleatorios, comprimir
es un peaje (CPU + cabecera) sin beneficio. El codec compara el tamaño comprimido con el
original y **solo se queda con el comprimido si realmente ayuda**.

---

### 9. AOP y su mayor trampa

**Qué responder.** AOP (programación orientada a aspectos) permite definir **capas transversales**
a la lógica de negocio: logging, autorización, caché. Es la forma de meter una preocupación
horizontal sin ensuciar cada método. Su mayor trampa es el **sobrecoste**: Spring AOP funciona con
**proxies y reflexión**, así que una llamada directa es mucho más rápida que una que pasa por el
proxy. El coste es proporcional al del método, de modo que el peligro real no es "AOP es lento"
sino **aplicar aspectos a métodos hot y triviales**. La segunda trampa, más sutil, es que **AOP
por proxy no intercepta las self-invocations**: si un método llama a `this.otroMetodo()`, la
llamada **no pasa por el proxy** y el aspect no se dispara. Y la tercera es de legibilidad: la
lógica de negocio se vuelve más difícil de leer para quien no vive en ese estilo.

**Punteros de código.**

- `q09aop/AuditAspect.java`, `AuthorizationAspect.java`, `CachingAspect.java` — los tres casos de
  uso del artículo.
- `q09aop/AopOverheadService.java` — la misma lógica, con y sin proxy, para medir la diferencia.
- `GET /q09/self-invocation` — la demo más valiosa: el aspect se dispara por el proxy y **no** en la
  llamada interna.
- `GET /q09/proxy-info` — devuelve la clase en runtime, con el `$$SpringCGLIB$$` dentro: el
  llamante ya no habla con el objeto que cree.

**Extra que suma puntos.** Los arreglos posibles: **AspectJ en tiempo de compilación** (weaving, sin
proxies), o aplicar los aspectos solo en fronteras gruesas (controladores, casos de uso), nunca en
el interior de un método caliente.

---

### 10. Limitaciones de GET

**Qué responder.** `GET` debería usarse solo para **recuperar** recursos, sin alterar el estado de
la aplicación (la excepción es el *caching*, donde una modificación leve del estado es tolerable).
Su limitación principal es que **no tiene body**: todos los parámetros van en la URL, que tiene un
largo limitado — **~2K caracteres** de forma práctica, y el límite **duro** lo impone el servidor
(el contenedor web). En la práctica sí se puede mandar un body con `GET`, pero es mala práctica:
no forma parte de la **clave de caché**, los proxies y caches lo pueden descartar, y viola la
semántica del método. La otra limitación es semántica: un `GET` que muta estado rompe las
garantías del protocolo (reenvío, prefetch, crawlers) y por eso el propio artículo lo marca como
el error clásico.

**Punteros de código.**

- `q10httpget/GetLimitsService.java` — compara el presupuesto de 2K con el límite real del
  contenedor.
- `GET /q10/measure?length=3000` — genera una URL de ese tamaño y la clasifica.
- `GET /q10/body-probe` — comprueba si llegó body y explica por qué no sirve de nada.
- `GET /q10/unsafe-increment` vs `POST /q10/safe-increment` — el contador hace visible la
  mutación de estado con `GET`.

---

## 5. Estructura del proyecto

```
src/main/java/com/dsklabs/epam/tecnicaltest/
├── TecnicaltestApplication.java
├── support/
│   ├── benchmark/     BenchmarkHarness (micro-benchmarks in-process), BenchmarkResult
│   └── web/           ApiResponse (sobre común), GlobalExceptionHandler, ForbiddenOperationException
├── q01collections/    SimpleArrayList, SimpleLinkedList, IntArrayList + servicio/controlador
├── q02webservices/    endpoint SOAP (Spring-WS), cliente SOAP, WSDL, OpenAPI, controladores
├── q03gitops/         GitDemoController (+ scripts/git-merge-vs-rebase.sh)
├── q04testing/        TestLayer, LoadTestRunner, TestingPyramidController
├── q05hashset/        HashSetOrderingService, HashSetController
├── q06exceptions/     jerarquía sellada, OrderService (fault barrier), Result, controlador
├── q07performance/    MeasurementService, SamplingProfiler, JfrProfiler, PerformanceController
├── q08redis/          BlobCodec, ChunkedBlobStore + 3 implementaciones, autodetección
├── q09aop/            3 aspects, OrderQueryService, AopOverheadService, AopController
└── q10httpget/        GetLimitsService, GetLimitsController

src/main/resources/
├── application.yaml, application-aws.yml
├── openapi/q02-openapi.yaml
├── wsdl/q02-greeting.wsdl
└── static/            index.html (landing), q02-api.html, assets/
```

---

## 6. Despliegue en AWS

La topología replica la de `spring-boot-medassistant`: **EC2 + Docker + ECR**, con GitHub Actions
como pipeline.

```
Internet
   │ http://<elastic-ip>:8080
   ▼
┌────────────────────────────┐
│ EC2 t3.small (AL2023)      │
│  docker run --restart      │
│  always -p 8080:8080      │
│  ┌──────────────────────┐  │
│  │  app (JRE 25, non-root)│ │
│  └──────────────────────┘  │
└────────────────────────────┘
       ▲ image
┌──────┴───────┐
│    ECR       │
└──────────────┘

GitHub Actions ──(OIDC, sin access keys)──► ECR push ──SSM Run Command──► EC2 pull + restart
```

### Recursos

| Recurso | Nombre | Notas |
|---|---|---|
| ECR | `tecnicaltest` | tags `latest` + `sha-<corto>` |
| IAM role (EC2) | `tecnicaltest-ec2-ecr-role` | `AmazonEC2ContainerRegistryReadOnly` + `AmazonSSMManagedInstanceCore` |
| IAM role (CI) | `tecnicaltest-gha-deploy` | **OIDC**: sin access keys en GitHub Secrets |
| Security group | `tecnicaltest-ec2-sg` | 8080 abierto, 22 restringido a tu IP |
| EC2 | `t3.small` AL2023 | subred pública, IP elástica propia (URL estable) |
| Elastic IP | — | URL estable para la entrevista |

> **Aviso de seguridad.** Las credenciales de `~/.aws/credentials` de esta cuenta son de la
> **cuenta root** con access keys de largo plazo. El pipeline **no** las usa: GitHub Actions se
> autentica con **OIDC** (`token.actions.githubusercontent.com`) contra un rol con la condición
> `repo:DsiKorp/tecnicaltest:ref:refs/heads/main`. Aun así, conviene rotar esas claves y activar
> MFA en la root.

### Scripts

```bash
./scripts/ec2-userdata.sh     # cloud-init: instala Docker y configura la rotación de logs
./scripts/teardown-aws.sh     # borra todos los recursos (¡no dejar la t3.small cobrando!)
```

### Despliegue manual (sin CI)

```bash
aws ecr create-repository --repository-name tecnicaltest --region us-east-1
docker build -t tecnicaltest .
docker tag tecnicaltest 626159997716.dkr.ecr.us-east-1.amazonaws.com/tecnicaltest:latest
aws ecr get-login-password --region us-east-1 | docker login --username AWS \
    --password-stdin 626159997716.dkr.ecr.us-east-1.amazonaws.com
docker push 626159997716.dkr.ecr.us-east-1.amazonaws.com/tecnicaltest:latest
```

Y en la instancia:

```bash
docker pull 626159997716.dkr.ecr.us-east-1.amazonaws.com/tecnicaltest:latest
docker rm -f tecnicaltest || true
docker run -d --name tecnicaltest --restart unless-stopped -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=aws -e EPAM_Q08_MODE=auto \
  626159997716.dkr.ecr.us-east-1.amazonaws.com/tecnicaltest:latest
```

---

## 7. Decisiones técnicas que conviene saber explicar

| Decisión | Por qué |
|---|---|
| `spring-boot-starter-aspectj` (¡no `-aop`!) | En Spring Boot 4 el starter de AOP se renombró. `spring-boot-starter-aop` **no existe** en 4.1.2: el build falla al resolver. |
| Sin JMH | El annotation processor de JMH y `maven-shade-plugin` pelean con `spring-boot-maven-plugin:repackage` por el mismo artifact. El harness propio hace lo esencial (warmup, varias iteraciones, min/mediana/p95) y además se sirve por HTTP. En producción usaría JMH. |
| OpenAPI escrito a mano | `springdoc` es un third-party: si su autoconfiguración se rompe, se rompe el arranque. Un contrato a mano es determinista y además permite poner WSDL y OpenAPI lado a lado. |
| Jackson nunca importado | Spring Boot 4 usa **Jackson 3** (`tools.jackson.*`). Acoplar el código al serializador es justo lo que hay que evitar: se devuelven records y Maps. |
| SOAP con Spring-WS, no JAX-WS clásico | `@PayloadRoot` es el estilo idiomático desde Spring-WS 4. `@WebService` + `Endpoint.publish` es el camino heredado. |
| Cero imports de Jackson en el código | Ver arriba. El formato de los metadatos de la P8 es un string delimitado, no JSON. |
| `RestTestClient` en los tests | Es la API de test HTTP de Spring Framework 7 / Boot 4; sustituye a `TestRestTemplate` y `MockMvc`. |
| Sin `assert` sobre tiempos | Los asserts de timing son flaky. El timing vive en los endpoints de demo. |

---

## 8. Licencia

Material de práctica. El artículo original es de EPAM
(*"10 senior Java developer interview questions and answers"*, Gaetano Piazzolla).
