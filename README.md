# Sistema de Solicitudes de Reembolso de Gastos Corporativos

## Estructura del Proyecto y Arquitectura Resultante

El proyecto sigue una arquitectura en capas simples con separación estricta de responsabilidades entre paquetes, verificada mediante pruebas de arquitectura automatizadas (ArchUnit):

```
reembolsos-integrado/
├── pom.xml
├── .github/
│   └── workflows/
│       └── arquitectura.yml              ← Pipeline de validación arquitectónica
├── docs/
│   └── adr/
│       └── ADR-001.md                    ← Registro de decisión arquitectónica (ADR)
└── src/
    ├── main/
    │   ├── java/com/empresa/reembolsos/
    │   │   ├── ReembolsosApplication.java
    │   │   ├── dominio/                  ← Reglas de negocio puras (sin dependencias Spring/JPA)
    │   │   │   ├── SolicitudReembolso.java
    │   │   │   ├── CategoriaGasto.java
    │   │   │   └── EstadoSolicitud.java
    │   │   ├── procesamiento/            ← Implementación de patrones de comportamiento GoF
    │   │   │   ├── validacion/           ← Chain of Responsibility (RF1)
    │   │   │   │   ├── ResultadoValidacion.java
    │   │   │   │   ├── ValidadorSolicitud.java
    │   │   │   │   ├── ValidadorDocumentosSoporte.java
    │   │   │   │   ├── ValidadorMontoMaximo.java
    │   │   │   │   ├── ValidadorDuplicados.java
    │   │   │   │   └── CadenaValidadores.java
    │   │   │   ├── calculo/              ← Strategy + Registry (RF2)
    │   │   │   │   ├── CalculadoraReembolso.java
    │   │   │   │   ├── CalculadoraViaje.java
    │   │   │   │   ├── CalculadoraRepresentacion.java
    │   │   │   │   ├── CalculadoraCapacitacion.java
    │   │   │   │   └── SelectorCalculadora.java
    │   │   │   └── notificacion/         ← Observer (RF3)
    │   │   │       ├── ObservadorCambioEstado.java
    │   │   │       ├── NotificadorEmpleado.java
    │   │   │       ├── NotificadorContabilidad.java
    │   │   │       └── NotificadorAuditoria.java
    │   │   ├── aplicacion/               ← Orquestación de casos de uso y coordinación
    │   │   │   ├── SolicitudReembolsoService.java
    │   │   │   └── SolicitudInvalidaException.java
    │   │   ├── infraestructura/          ← Acceso a datos relacionales (Spring Data JPA)
    │   │   │   └── RepositorioSolicitudesJpa.java
    │   │   └── adaptadores/rest/         ← Controladores HTTP REST
    │   │       └── SolicitudReembolsoController.java
    │   └── resources/
    │       ├── application.properties
    │       └── META-INF/
    │           └── orm.xml               ← Mapeo JPA declarativo (mantiene dominio libre de JPA)
    └── test/
        └── java/com/empresa/reembolsos/
            ├── ReglasArquitectura.java   ← 4 reglas de arquitectura con ArchUnit
            ├── integracion/
            │   └── SolicitudReembolsoIntegrationTest.java
            └── procesamiento/
                ├── validacion/
                │   ├── ValidadorDocumentosSoporteTest.java
                │   ├── ValidadorMontoMaximoTest.java
                │   └── ValidadorDuplicadosTest.java
                └── calculo/
                    ├── CalculadoraViajeTest.java
                    ├── CalculadoraRepresentacionTest.java
                    └── CalculadoraCapacitacionTest.java
```

---

## Diseño Propuesto (Análisis Previo a la Implementación)

### RF1 — Validación en varias etapas
- **Punto de variabilidad:** El conjunto y la secuencia de verificaciones de negocio independientes (documentos soporte, tope de monto por categoría, detección de duplicados) que deben ejecutarse antes de que una solicitud sea admitida para trámite, con previsión de añadir más reglas a futuro.
- **Patrón propuesto:** **Chain of Responsibility (Cadena de Responsabilidad)**.
- **Justificación:** Desacopla el servicio emisor de la ejecución de cada validación individual, organizando cada verificación en un eslabón autónomo que evalúa y decide si aprueba o rechaza sin conocer la identidad ni la cantidad de los eslabones subsiguientes (Open/Closed Principle).

### RF2 — Cálculo distinto por categoría de gasto
- **Punto de variabilidad:** El algoritmo matemático específico utilizado para liquidar el monto aprobado según la categoría de gasto (viáticos diarios acumulados, reembolso completo con tope mensual, o 80% con tope anual), con el requerimiento de incorporar nuevas categorías sin modificar el código existente.
- **Patrón propuesto:** **Strategy (Estrategia) con Registro Dinámico / Map Inyectado**.
- **Justificación:** Encapsula cada fórmula de liquidación en una clase dedicada intercambiable bajo la interfaz común `CalculadoraReembolso`, permitiendo que el contenedor de Spring inyecte automáticamente todas las estrategias en un mapa indexado por categoría, eliminando condicionales `switch` y habilitando extensión sin modificación.

### RF3 — Notificación simultánea a varios interesados
- **Punto de variabilidad:** La cantidad y naturaleza de los actores y módulos del sistema (empleado, contabilidad, auditoría interna, panel de métricas) que deben reaccionar con lógicas dispares e independientes cada vez que una solicitud cambia de estado.
- **Patrón propuesto:** **Observer (Observador)**.
- **Justificación:** Establece una relación de suscripción uno-a-muchos mediante la interfaz `ObservadorCambioEstado`, permitiendo que el servicio despache eventos de cambio de estado a una lista de suscriptores sin acoplarse a sus canales de salida ni a sus condiciones particulares de negocio.

---

## Decisiones de Diseño

### Decisión de Diseño 1: Orden de encadenamiento de validadores
- **Pregunta:** ¿En qué orden encadenar los validadores (`ValidadorDocumentosSoporte`, `ValidadorMontoMaximo`, `ValidadorDuplicados`) en `CadenaValidadores` según criterios de eficiencia?
- **Decisión:** `ValidadorDocumentosSoporte` $\rightarrow$ `ValidadorMontoMaximo` $\rightarrow$ `ValidadorDuplicados`.
- **Justificación:** Las validaciones de presencia de documentos y de comparación contra el tope de categoría son operaciones inmediatas en memoria de complejidad temporal $O(1)$ sobre los atributos intrínsecos de la solicitud. En contraste, `ValidadorDuplicados` requiere iterar y filtrar sobre la colección de solicitudes pendientes del empleado ($O(N)$). Colocar las verificaciones simples primero aplica un patrón de *fail-fast*: si faltan documentos o el monto excede el tope, el flujo se interrumpe de inmediato sin ejecutar la búsqueda de duplicados, optimizando el consumo de CPU y memoria. Ubicar duplicados al inicio penalizaría el rendimiento en solicitudes que de cualquier forma serían descartadas por datos incompletos.

### Decisión de Diseño 2: Selección de calculadora (Mapa dinámico vs. Switch)
- **Pregunta:** ¿Cómo seleccionar la calculadora correspondiente a cada categoría: mediante un mapa construido a partir de `List<CalculadoraReembolso>` inyectado por Spring o un `switch` manual sobre `CategoriaGasto`?
- **Decisión:** Mapa dinámico indexado en `SelectorCalculadora` a partir de `List<CalculadoraReembolso>`.
- **Justificación:** El requisito funcional RF2 exige explícitamente agregar nuevas categorías de gasto (ej. bienestar) sin modificar el código existente. Con el mapa dinámico, incorporar una nueva categoría únicamente requiere crear una nueva clase anotada con `@Component` que implemente `CalculadoraReembolso` y registrarla en el enum; `SelectorCalculadora` la descubre e indexa en tiempo de arranque sin requerir alterar ninguna línea ya escrita. Un `switch` manual requeriría editar la clase del selector cada vez que surja una categoría, violando el principio Open/Closed.

### Decisión de Diseño 3: Persistencia y Notificación (Capas simples vs. Hexagonal)
- **Pregunta:** ¿Debe el proyecto adoptar arquitectura Hexagonal completa para persistencia y notificación o mantenerse en capas simples?
- **Decisión:** Se mantiene arquitectura en capas simples para persistencia (`SolicitudReembolsoService` depende directamente de `RepositorioSolicitudesJpa`) y se mantiene el *seam* existente para notificación (`ObservadorCambioEstado`).
- **Justificación:** Conforme al principio YAGNI y a los lineamientos de la Guía de la unidad (sección 6.2), una arquitectura Hexagonal con puertos y adaptadores de persistencia solo se justifica si existe un plan concreto de cambiar el motor de base de datos a mediano plazo, lo cual no aplica a este sistema. Crear interfaces de puerto y mappers especulativos hoy añade costo de mantenimiento sin beneficio real. Para notificación, el contrato `ObservadorCambioEstado` ya proporciona el seam necesario para que `NotificadorContabilidad` delegue a una API externa en el próximo trimestre sin modificar el servicio de aplicación. Documentado formalmente en `docs/adr/ADR-001.md`.

### Decisión de Diseño 4: Alcance de dependencias en Regla 1 de ArchUnit
- **Pregunta:** ¿Por qué la Regla 1 prohíbe `org.springframework.data..`, `jakarta.persistence..`, adaptadores e infraestructura pero permite `@Component` en el paquete `procesamiento`?
- **Decisión:** Distinguir entre anotaciones declarativas de estereotipo (`@Component`) y dependencias tecnológicas de infraestructura/acceso a datos/web.
- **Justificación:** Las anotaciones de estereotipo (`@Component`) son mecanismos del contenedor IoC que facilitan la inyección de dependencias y el ensamblado de las estrategias y observadores sin acoplar la lógica de procesamiento a drivers, bases de datos o protocolos de transporte. En cambio, permitir `spring-data` o `jakarta.persistence` en `procesamiento` contaminaría la lógica de negocio con detalles de infraestructura relacional, y permitir `adaptadores.rest` acoplaría el procesamiento al protocolo HTTP. Prohibir la infraestructura y permitir el estereotipo equilibra pureza arquitectónica y ergonomía del framework.

---

## Resumen de Decisión Arquitectónica (ADR-001)

- **Título:** Capas simples para persistencia; el seam de notificación ya cubre el próximo cambio conocido.
- **Estado:** Aceptado (2026-11-08).
- **Consecuencias Positivas:**
  - Cero código boilerplate de puertos y adaptadores para persistencia innecesarios hoy (YAGNI).
  - El punto de extensión para la futura integración bancaria/externa de Contabilidad ya existe gracias al patrón Observer.
- **Consecuencias Negativas:**
  - Si en el futuro se requiriera reemplazar JPA por una base documental o no-relacional, la interfaz de persistencia deberá introducirse de forma retroactiva.
  - La asimetría deliberada (puerto en notificación pero no en persistencia) requiere consulta obligatoria del ADR.

---

## Qué se Descartó por Sobreingeniería y Por Qué

### 1. Spring ApplicationEventPublisher en lugar de lista de observadores inyectada
- **Qué se consideró:** Publicar un evento de dominio (`SolicitudCambioEstadoEvent`) a través del bus interno `ApplicationEventPublisher` de Spring y capturarlo con anotaciones `@EventListener`.
- **Por qué se descartó:** Con exactamente tres observadores conocidos en tiempo de compilación que ejecutan en el mismo hilo, la indirección de un bus de eventos genérico añade dificultad para depurar y rastrear el flujo de ejecución sin aportar desacoplamiento adicional al que ya entrega la interfaz `ObservadorCambioEstado`.
- **Señal futura para reconsiderar:** Si se requiriera procesar las notificaciones de manera asíncrona (`@Async`) en hilos separados o publicar los eventos hacia un broker de mensajería externo (RabbitMQ / Kafka).

### 2. Visitor o Abstract Factory para exportación de reportes en múltiples formatos
- **Qué se consideró:** Anticipar que Contabilidad solicitaría reportes de solicitudes en PDF y Excel además del reporte en texto plano/CSV, creando una jerarquía de exportadores mediante Abstract Factory o Visitor.
- **Por qué se descartó:** Hoy en día únicamente existe la necesidad de reportes simples; anticipar formatos adicionales viola la *Rule of Three* (no abstraer hasta que no existan al menos tres casos concretos reales). Construir la abstracción representaría código muerto especulativo.
- **Señal futura para reconsiderar:** Cuando Finanzas formalice el requerimiento de al menos dos formatos binarios adicionales con layouts complejos.

### 3. Jerarquía de excepciones tipadas por validador vs. `SolicitudInvalidaException` unificada *(Aporte propio del estudiante)*
- **Qué se consideró:** Crear una clase de excepción independiente para cada eslabón de la cadena de validación (`DocumentosFaltantesException`, `MontoExcedidoException`, `SolicitudDuplicadaException`) con sus respectivos `@ResponseStatus` y manejadores `@ExceptionHandler` dedicados.
- **Por qué se descartó:** Todos los rechazos de validación se traducen idénticamente en la capa REST en una respuesta HTTP `400 Bad Request` acompañada del mensaje descriptivo generado por el validador. Crear tres clases adicionales de excepción para un solo tipo de respuesta genera sobrecarga de mantenimiento sin beneficio alguno para el cliente API (principio KISS y YAGNI).
- **Señal futura para reconsiderar:** Si la API evoluciona hacia una integración máquina-a-máquina que requiera códigos de error numéricos estructurados (ej. error 4001 vs 4002) o códigos HTTP diferenciados (`422 Unprocessable Entity` para documentos vs `409 Conflict` para duplicados).

---

## Evidencia de Pruebas y Pipeline de Integración Continua

### Pruebas Unitarias e Integración
La suite de pruebas automatizadas garantiza la cobertura de cada componente y del flujo de extremo a extremo:
- `ValidadorDocumentosSoporteTest`: Valida rechazo por falta de soportes y aprobación cuando existen.
- `ValidadorMontoMaximoTest`: Valida cumplimiento y superación del tope de categoría.
- `ValidadorDuplicadosTest`: Valida detección de solicitudes idénticas en el mismo período/categoría.
- `CalculadoraViajeTest`: Valida cálculo con tope diario multiplicado por días de viaje.
- `CalculadoraRepresentacionTest`: Valida liquidación hasta el tope mensual.
- `CalculadoraCapacitacionTest`: Valida 80% y restricción por tope anual.
- `SolicitudReembolsoIntegrationTest`: Prueba integral `@SpringBootTest` con base de datos H2 para creación, validación, liquidación y cambio de estado con notificación.

### Verificación de Reglas de Arquitectura con ArchUnit
`ReglasArquitectura` ejecuta 4 pruebas estáticas que verifican los límites de paquete:
1. `procesamientoSinPersistenciaNiWeb`: Asegura que `procesamiento` no importe infraestructura, adaptadores, `spring-data` ni `jakarta.persistence`.
2. `controladorSoloUsaServicio`: Asegura que los controladores REST únicamente accedan a `aplicacion`, `dominio` y librerías web.
3. `calculadorasImplementanInterfaz`: Garantiza que toda clase en `procesamiento.calculo` implemente `CalculadoraReembolso`.
4. `notificadoresImplementanInterfaz`: Garantiza que toda clase en `procesamiento.notificacion` implemente `ObservadorCambioEstado`.

### Pipeline de Integración Continua (GitHub Actions)
El workflow `.github/workflows/arquitectura.yml` automatiza la verificación en cada push a `main`:
1. Configuración de JDK 17 (Temurin).
2. Cache de dependencias Maven.
3. Ejecución aislada de reglas ArchUnit: `mvn test -Dtest=ReglasArquitectura`.
4. Ejecución completa del ciclo de construcción: `mvn verify`.
