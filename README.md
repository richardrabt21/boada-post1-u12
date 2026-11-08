# Sistema de Solicitudes de Reembolso de Gastos Corporativos

## Estructura del Proyecto y Arquitectura Resultante

El proyecto sigue una arquitectura en capas simples con separaciÃ³n estricta de responsabilidades entre paquetes, verificada mediante pruebas de arquitectura automatizadas (ArchUnit):

```
reembolsos-integrado/
â”œâ”€â”€ pom.xml
â”œâ”€â”€ .github/
â”‚   â””â”€â”€ workflows/
â”‚       â””â”€â”€ arquitectura.yml              â† Pipeline de validaciÃ³n arquitectÃ³nica
â”œâ”€â”€ docs/
â”‚   â””â”€â”€ adr/
â”‚       â””â”€â”€ ADR-001.md                    â† Registro de decisiÃ³n arquitectÃ³nica (ADR)
â””â”€â”€ src/
    â”œâ”€â”€ main/
    â”‚   â”œâ”€â”€ java/com/empresa/reembolsos/
    â”‚   â”‚   â”œâ”€â”€ ReembolsosApplication.java
    â”‚   â”‚   â”œâ”€â”€ dominio/                  â† Reglas de negocio puras (sin dependencias Spring/JPA)
    â”‚   â”‚   â”‚   â”œâ”€â”€ SolicitudReembolso.java
    â”‚   â”‚   â”‚   â”œâ”€â”€ CategoriaGasto.java
    â”‚   â”‚   â”‚   â””â”€â”€ EstadoSolicitud.java
    â”‚   â”‚   â”œâ”€â”€ procesamiento/            â† ImplementaciÃ³n de patrones de comportamiento GoF
    â”‚   â”‚   â”‚   â”œâ”€â”€ validacion/           â† Chain of Responsibility (RF1)
    â”‚   â”‚   â”‚   â”‚   â”œâ”€â”€ ResultadoValidacion.java
    â”‚   â”‚   â”‚   â”‚   â”œâ”€â”€ ValidadorSolicitud.java
    â”‚   â”‚   â”‚   â”‚   â”œâ”€â”€ ValidadorDocumentosSoporte.java
    â”‚   â”‚   â”‚   â”‚   â”œâ”€â”€ ValidadorMontoMaximo.java
    â”‚   â”‚   â”‚   â”‚   â”œâ”€â”€ ValidadorDuplicados.java
    â”‚   â”‚   â”‚   â”‚   â””â”€â”€ CadenaValidadores.java
    â”‚   â”‚   â”‚   â”œâ”€â”€ calculo/              â† Strategy + Registry (RF2)
    â”‚   â”‚   â”‚   â”‚   â”œâ”€â”€ CalculadoraReembolso.java
    â”‚   â”‚   â”‚   â”‚   â”œâ”€â”€ CalculadoraViaje.java
    â”‚   â”‚   â”‚   â”‚   â”œâ”€â”€ CalculadoraRepresentacion.java
    â”‚   â”‚   â”‚   â”‚   â”œâ”€â”€ CalculadoraCapacitacion.java
    â”‚   â”‚   â”‚   â”‚   â””â”€â”€ SelectorCalculadora.java
    â”‚   â”‚   â”‚   â””â”€â”€ notificacion/         â† Observer (RF3)
    â”‚   â”‚   â”‚       â”œâ”€â”€ ObservadorCambioEstado.java
    â”‚   â”‚   â”‚       â”œâ”€â”€ NotificadorEmpleado.java
    â”‚   â”‚   â”‚       â”œâ”€â”€ NotificadorContabilidad.java
    â”‚   â”‚   â”‚       â””â”€â”€ NotificadorAuditoria.java
    â”‚   â”‚   â”œâ”€â”€ aplicacion/               â† OrquestaciÃ³n de casos de uso y coordinaciÃ³n
    â”‚   â”‚   â”‚   â”œâ”€â”€ SolicitudReembolsoService.java
    â”‚   â”‚   â”‚   â””â”€â”€ SolicitudInvalidaException.java
    â”‚   â”‚   â”œâ”€â”€ infraestructura/          â† Acceso a datos relacionales (Spring Data JPA)
    â”‚   â”‚   â”‚   â””â”€â”€ RepositorioSolicitudesJpa.java
    â”‚   â”‚   â””â”€â”€ adaptadores/rest/         â† Controladores HTTP REST
    â”‚   â”‚       â””â”€â”€ SolicitudReembolsoController.java
    â”‚   â””â”€â”€ resources/
    â”‚       â”œâ”€â”€ application.properties
    â”‚       â””â”€â”€ META-INF/
    â”‚           â””â”€â”€ orm.xml               â† Mapeo JPA declarativo (mantiene dominio libre de JPA)
    â””â”€â”€ test/
        â””â”€â”€ java/com/empresa/reembolsos/
            â”œâ”€â”€ ReglasArquitectura.java   â† 4 reglas de arquitectura con ArchUnit
            â”œâ”€â”€ integracion/
            â”‚   â””â”€â”€ SolicitudReembolsoIntegrationTest.java
            â””â”€â”€ procesamiento/
                â”œâ”€â”€ validacion/
                â”‚   â”œâ”€â”€ ValidadorDocumentosSoporteTest.java
                â”‚   â”œâ”€â”€ ValidadorMontoMaximoTest.java
                â”‚   â””â”€â”€ ValidadorDuplicadosTest.java
                â””â”€â”€ calculo/
                    â”œâ”€â”€ CalculadoraViajeTest.java
                    â”œâ”€â”€ CalculadoraRepresentacionTest.java
                    â””â”€â”€ CalculadoraCapacitacionTest.java
```

---

## DiseÃ±o Propuesto (AnÃ¡lisis Previo a la ImplementaciÃ³n)

### RF1 â€” ValidaciÃ³n en varias etapas
- **Punto de variabilidad:** El conjunto y la secuencia de verificaciones de negocio independientes (documentos soporte, tope de monto por categorÃ­a, detecciÃ³n de duplicados) que deben ejecutarse antes de que una solicitud sea admitida para trÃ¡mite, con previsiÃ³n de aÃ±adir mÃ¡s reglas a futuro.
- **PatrÃ³n propuesto:** **Chain of Responsibility (Cadena de Responsabilidad)**.
- **JustificaciÃ³n:** Desacopla el servicio emisor de la ejecuciÃ³n de cada validaciÃ³n individual, organizando cada verificaciÃ³n en un eslabÃ³n autÃ³nomo que evalÃºa y decide si aprueba o rechaza sin conocer la identidad ni la cantidad de los eslabones subsiguientes (Open/Closed Principle).

### RF2 â€” CÃ¡lculo distinto por categorÃ­a de gasto
- **Punto de variabilidad:** El algoritmo matemÃ¡tico especÃ­fico utilizado para liquidar el monto aprobado segÃºn la categorÃ­a de gasto (viÃ¡ticos diarios acumulados, reembolso completo con tope mensual, o 80% con tope anual), con el requerimiento de incorporar nuevas categorÃ­as sin modificar el cÃ³digo existente.
- **PatrÃ³n propuesto:** **Strategy (Estrategia) con Registro DinÃ¡mico / Map Inyectado**.
- **JustificaciÃ³n:** Encapsula cada fÃ³rmula de liquidaciÃ³n en una clase dedicada intercambiable bajo la interfaz comÃºn `CalculadoraReembolso`, permitiendo que el contenedor de Spring inyecte automÃ¡ticamente todas las estrategias en un mapa indexado por categorÃ­a, eliminando condicionales `switch` y habilitando extensiÃ³n sin modificaciÃ³n.

### RF3 â€” NotificaciÃ³n simultÃ¡nea a varios interesados
- **Punto de variabilidad:** La cantidad y naturaleza de los actores y mÃ³dulos del sistema (empleado, contabilidad, auditorÃ­a interna, panel de mÃ©tricas) que deben reaccionar con lÃ³gicas dispares e independientes cada vez que una solicitud cambia de estado.
- **PatrÃ³n propuesto:** **Observer (Observador)**.
- **JustificaciÃ³n:** Establece una relaciÃ³n de suscripciÃ³n uno-a-muchos mediante la interfaz `ObservadorCambioEstado`, permitiendo que el servicio despache eventos de cambio de estado a una lista de suscriptores sin acoplarse a sus canales de salida ni a sus condiciones particulares de negocio.

---

## Decisiones de DiseÃ±o

### DecisiÃ³n de DiseÃ±o 1: Orden de encadenamiento de validadores
- **Pregunta:** Â¿En quÃ© orden encadenar los validadores (`ValidadorDocumentosSoporte`, `ValidadorMontoMaximo`, `ValidadorDuplicados`) en `CadenaValidadores` segÃºn criterios de eficiencia?
- **DecisiÃ³n:** `ValidadorDocumentosSoporte` $\rightarrow$ `ValidadorMontoMaximo` $\rightarrow$ `ValidadorDuplicados`.
- **JustificaciÃ³n:** Las validaciones de presencia de documentos y de comparaciÃ³n contra el tope de categorÃ­a son operaciones inmediatas en memoria de complejidad temporal $O(1)$ sobre los atributos intrÃ­nsecos de la solicitud. En contraste, `ValidadorDuplicados` requiere iterar y filtrar sobre la colecciÃ³n de solicitudes pendientes del empleado ($O(N)$). Colocar las verificaciones simples primero aplica un patrÃ³n de *fail-fast*: si faltan documentos o el monto excede el tope, el flujo se interrumpe de inmediato sin ejecutar la bÃºsqueda de duplicados, optimizando el consumo de CPU y memoria. Ubicar duplicados al inicio penalizarÃ­a el rendimiento en solicitudes que de cualquier forma serÃ­an descartadas por datos incompletos.

### DecisiÃ³n de DiseÃ±o 2: SelecciÃ³n de calculadora (Mapa dinÃ¡mico vs. Switch)
- **Pregunta:** Â¿CÃ³mo seleccionar la calculadora correspondiente a cada categorÃ­a: mediante un mapa construido a partir de `List<CalculadoraReembolso>` inyectado por Spring o un `switch` manual sobre `CategoriaGasto`?
- **DecisiÃ³n:** Mapa dinÃ¡mico indexado en `SelectorCalculadora` a partir de `List<CalculadoraReembolso>`.
- **JustificaciÃ³n:** El requisito funcional RF2 exige explÃ­citamente agregar nuevas categorÃ­as de gasto (ej. bienestar) sin modificar el cÃ³digo existente. Con el mapa dinÃ¡mico, incorporar una nueva categorÃ­a Ãºnicamente requiere crear una nueva clase anotada con `@Component` que implemente `CalculadoraReembolso` y registrarla en el enum; `SelectorCalculadora` la descubre e indexa en tiempo de arranque sin requerir alterar ninguna lÃ­nea ya escrita. Un `switch` manual requerirÃ­a editar la clase del selector cada vez que surja una categorÃ­a, violando el principio Open/Closed.

### DecisiÃ³n de DiseÃ±o 3: Persistencia y NotificaciÃ³n (Capas simples vs. Hexagonal)
- **Pregunta:** Â¿Debe el proyecto adoptar arquitectura Hexagonal completa para persistencia y notificaciÃ³n o mantenerse en capas simples?
- **DecisiÃ³n:** Se mantiene arquitectura en capas simples para persistencia (`SolicitudReembolsoService` depende directamente de `RepositorioSolicitudesJpa`) y se mantiene el *seam* existente para notificaciÃ³n (`ObservadorCambioEstado`).
- **JustificaciÃ³n:** Conforme al principio YAGNI y a los lineamientos de la GuÃ­a de la unidad (secciÃ³n 6.2), una arquitectura Hexagonal con puertos y adaptadores de persistencia solo se justifica si existe un plan concreto de cambiar el motor de base de datos a mediano plazo, lo cual no aplica a este sistema. Crear interfaces de puerto y mappers especulativos hoy aÃ±ade costo de mantenimiento sin beneficio real. Para notificaciÃ³n, el contrato `ObservadorCambioEstado` ya proporciona el seam necesario para que `NotificadorContabilidad` delegue a una API externa en el prÃ³ximo trimestre sin modificar el servicio de aplicaciÃ³n. Documentado formalmente en `docs/adr/ADR-001.md`.

### DecisiÃ³n de DiseÃ±o 4: Alcance de dependencias en Regla 1 de ArchUnit
- **Pregunta:** Â¿Por quÃ© la Regla 1 prohÃ­be `org.springframework.data..`, `jakarta.persistence..`, adaptadores e infraestructura pero permite `@Component` en el paquete `procesamiento`?
- **DecisiÃ³n:** Distinguir entre anotaciones declarativas de estereotipo (`@Component`) y dependencias tecnolÃ³gicas de infraestructura/acceso a datos/web.
- **JustificaciÃ³n:** Las anotaciones de estereotipo (`@Component`) son mecanismos del contenedor IoC que facilitan la inyecciÃ³n de dependencias y el ensamblado de las estrategias y observadores sin acoplar la lÃ³gica de procesamiento a drivers, bases de datos o protocolos de transporte. En cambio, permitir `spring-data` o `jakarta.persistence` en `procesamiento` contaminarÃ­a la lÃ³gica de negocio con detalles de infraestructura relacional, y permitir `adaptadores.rest` acoplarÃ­a el procesamiento al protocolo HTTP. Prohibir la infraestructura y permitir el estereotipo equilibra pureza arquitectÃ³nica y ergonomÃ­a del framework.

---

## Resumen de DecisiÃ³n ArquitectÃ³nica (ADR-001)

- **TÃ­tulo:** Capas simples para persistencia; el seam de notificaciÃ³n ya cubre el prÃ³ximo cambio conocido.
- **Estado:** Aceptado (2026-11-08).
- **Consecuencias Positivas:**
  - Cero cÃ³digo boilerplate de puertos y adaptadores para persistencia innecesarios hoy (YAGNI).
  - El punto de extensiÃ³n para la futura integraciÃ³n bancaria/externa de Contabilidad ya existe gracias al patrÃ³n Observer.
- **Consecuencias Negativas:**
  - Si en el futuro se requiriera reemplazar JPA por una base documental o no-relacional, la interfaz de persistencia deberÃ¡ introducirse de forma retroactiva.
  - La asimetrÃ­a deliberada (puerto en notificaciÃ³n pero no en persistencia) requiere consulta obligatoria del ADR.

---

## QuÃ© se DescartÃ³ por SobreingenierÃ­a y Por QuÃ©

### 1. Spring ApplicationEventPublisher en lugar de lista de observadores inyectada
- **QuÃ© se considerÃ³:** Publicar un evento de dominio (`SolicitudCambioEstadoEvent`) a travÃ©s del bus interno `ApplicationEventPublisher` de Spring y capturarlo con anotaciones `@EventListener`.
- **Por quÃ© se descartÃ³:** Con exactamente tres observadores conocidos en tiempo de compilaciÃ³n que ejecutan en el mismo hilo, la indirecciÃ³n de un bus de eventos genÃ©rico aÃ±ade dificultad para depurar y rastrear el flujo de ejecuciÃ³n sin aportar desacoplamiento adicional al que ya entrega la interfaz `ObservadorCambioEstado`.
- **SeÃ±al futura para reconsiderar:** Si se requiriera procesar las notificaciones de manera asÃ­ncrona (`@Async`) en hilos separados o publicar los eventos hacia un broker de mensajerÃ­a externo (RabbitMQ / Kafka).

### 2. Visitor o Abstract Factory para exportaciÃ³n de reportes en mÃºltiples formatos
- **QuÃ© se considerÃ³:** Anticipar que Contabilidad solicitarÃ­a reportes de solicitudes en PDF y Excel ademÃ¡s del reporte en texto plano/CSV, creando una jerarquÃ­a de exportadores mediante Abstract Factory o Visitor.
- **Por quÃ© se descartÃ³:** Hoy en dÃ­a Ãºnicamente existe la necesidad de reportes simples; anticipar formatos adicionales viola la *Rule of Three* (no abstraer hasta que no existan al menos tres casos concretos reales). Construir la abstracciÃ³n representarÃ­a cÃ³digo muerto especulativo.
- **SeÃ±al futura para reconsiderar:** Cuando Finanzas formalice el requerimiento de al menos dos formatos binarios adicionales con layouts complejos.

### 3. JerarquÃ­a de excepciones tipadas por validador vs. `SolicitudInvalidaException` unificada *(Aporte propio del estudiante)*
- **QuÃ© se considerÃ³:** Crear una clase de excepciÃ³n independiente para cada eslabÃ³n de la cadena de validaciÃ³n (`DocumentosFaltantesException`, `MontoExcedidoException`, `SolicitudDuplicadaException`) con sus respectivos `@ResponseStatus` y manejadores `@ExceptionHandler` dedicados.
- **Por quÃ© se descartÃ³:** Todos los rechazos de validaciÃ³n se traducen idÃ©nticamente en la capa REST en una respuesta HTTP `400 Bad Request` acompaÃ±ada del mensaje descriptivo generado por el validador. Crear tres clases adicionales de excepciÃ³n para un solo tipo de respuesta genera sobrecarga de mantenimiento sin beneficio alguno para el cliente API (principio KISS y YAGNI).
- **SeÃ±al futura para reconsiderar:** Si la API evoluciona hacia una integraciÃ³n mÃ¡quina-a-mÃ¡quina que requiera cÃ³digos de error numÃ©ricos estructurados (ej. error 4001 vs 4002) o cÃ³digos HTTP diferenciados (`422 Unprocessable Entity` para documentos vs `409 Conflict` para duplicados).

---

## Evidencia de Pruebas y Pipeline de IntegraciÃ³n Continua

### Pruebas Unitarias e IntegraciÃ³n
La suite de pruebas automatizadas garantiza la cobertura de cada componente y del flujo de extremo a extremo:
- `ValidadorDocumentosSoporteTest`: Valida rechazo por falta de soportes y aprobaciÃ³n cuando existen.
- `ValidadorMontoMaximoTest`: Valida cumplimiento y superaciÃ³n del tope de categorÃ­a.
- `ValidadorDuplicadosTest`: Valida detecciÃ³n de solicitudes idÃ©nticas en el mismo perÃ­odo/categorÃ­a.
- `CalculadoraViajeTest`: Valida cÃ¡lculo con tope diario multiplicado por dÃ­as de viaje.
- `CalculadoraRepresentacionTest`: Valida liquidaciÃ³n hasta el tope mensual.
- `CalculadoraCapacitacionTest`: Valida 80% y restricciÃ³n por tope anual.
- `SolicitudReembolsoIntegrationTest`: Prueba integral `@SpringBootTest` con base de datos H2 para creaciÃ³n, validaciÃ³n, liquidaciÃ³n y cambio de estado con notificaciÃ³n.

### VerificaciÃ³n de Reglas de Arquitectura con ArchUnit
`ReglasArquitectura` ejecuta 4 pruebas estÃ¡ticas que verifican los lÃ­mites de paquete:
1. `procesamientoSinPersistenciaNiWeb`: Asegura que `procesamiento` no importe infraestructura, adaptadores, `spring-data` ni `jakarta.persistence`.
2. `controladorSoloUsaServicio`: Asegura que los controladores REST Ãºnicamente accedan a `aplicacion`, `dominio` y librerÃ­as web.
3. `calculadorasImplementanInterfaz`: Garantiza que toda clase en `procesamiento.calculo` implemente `CalculadoraReembolso`.
4. `notificadoresImplementanInterfaz`: Garantiza que toda clase en `procesamiento.notificacion` implemente `ObservadorCambioEstado`.

### Pipeline de IntegraciÃ³n Continua (GitHub Actions)
El workflow `.github/workflows/arquitectura.yml` automatiza la verificaciÃ³n en cada push a `main`:
1. ConfiguraciÃ³n de JDK 17 (Temurin).
2. Cache de dependencias Maven.
3. EjecuciÃ³n aislada de reglas ArchUnit: `mvn test -Dtest=ReglasArquitectura`.
4. EjecuciÃ³n completa del ciclo de construcciÃ³n: `mvn verify`.

