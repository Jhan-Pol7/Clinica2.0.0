# 🏥 ESTRUCTURA DEL PROYECTO — Clínica 2.0 "Más Cerca de Dios"

> **Tecnologías:** Java 17 · JavaFX 21 · H2 Database · HikariCP · JasperReports · Lombok · Jakarta Validation · ESC/POS · Maven

---

## 📁 Árbol general del proyecto

```
Clinica2.0.1/
├── pom.xml                         ← Gestión de dependencias y build Maven
├── src/main/
│   ├── java/pe/edu/upeu/clinica/
│   │   ├── App.java                ← Punto de entrada JavaFX
│   │   ├── ClinicaApplication.java ← Lanzador sin módulos JavaFX
│   │   ├── components/             ← Componentes UI reutilizables
│   │   ├── config/                 ← Configuración BD y contenedor DI
│   │   ├── controller/             ← Controladores FXML (MVC)
│   │   ├── dto/                    ← Objetos de transferencia y sesión
│   │   ├── enums/                  ← Enumeraciones del dominio
│   │   ├── exception/              ← Excepciones personalizadas
│   │   ├── model/                  ← Entidades del dominio
│   │   ├── repository/             ← Acceso a datos (JDBC manual)
│   │   ├── service/                ← Interfaces de servicios
│   │   │   └── impl/              ← Implementaciones de servicios
│   │   └── utils/                  ← Utilidades (impresión, HTTP, etc.)
│   └── resources/
│       ├── application.properties  ← Config de BD y pool de conexiones
│       ├── schema_clinica.sql      ← DDL + datos semilla (idempotente)
│       ├── logback.xml             ← Configuración de logging
│       ├── css/                    ← Temas visuales (azul, oscuro, rosado, verde)
│       ├── img/                    ← Logo de la clínica
│       ├── jasper/                 ← Reportes JasperReports (.jrxml)
│       ├── language/               ← Archivos de internacionalización (ES/EN)
│       └── view/                   ← Vistas FXML (una por pantalla)
```

---

## 🔧 pom.xml — Configuración Maven

El archivo `pom.xml` define todo lo que el proyecto necesita para compilar, empaquetar y ejecutarse.

### Coordenadas del proyecto
```xml
groupId:    pe.edu.upeu.clinica
artifactId: Clinica2.0
version:    2.0.0-SNAPSHOT
```

### Dependencias principales

| Dependencia | Versión | Para qué sirve |
|------------|---------|----------------|
| **HikariCP** | 5.1.0 | Pool de conexiones JDBC de alta performance. Gestiona un conjunto de conexiones reutilizables a H2 (máximo 5 simultáneas), evitando el overhead de abrir/cerrar conexiones en cada operación. |
| **H2 Database** | 2.2.224 | Base de datos embebida que funciona en modo archivo + servidor TCP. Se inicia junto con la app y almacena todos los datos en `data/clinica.mv.db`. |
| **Hibernate Validator** | 8.0.1 | Implementación de Jakarta Validation. Ejecuta las anotaciones `@NotBlank`, `@Size`, `@Pattern` definidas en los modelos para validar formularios antes de persistir. |
| **jakarta.validation-api** | 3.0.2 | API estándar de validación de beans (Jakarta EE). Define las anotaciones de validación usadas en los modelos. |
| **Lombok** | 1.18.34 | Genera automáticamente `getters`, `setters`, `constructores`, `@Builder`, `@Data` en tiempo de compilación. Reduce código boilerplate en todos los modelos. |
| **JavaFX Controls** | 21.0.2 | Componentes de interfaz gráfica: `TableView`, `Button`, `TextField`, `ComboBox`, `Label`, etc. |
| **JavaFX FXML** | 21.0.2 | Motor para cargar archivos `.fxml` y conectarlos con sus controladores Java. |
| **JavaFX Swing** | 21.0.2 | Permite integrar componentes Swing dentro de JavaFX (usado por el visor JasperReports). |
| **JasperReports** | 6.21.5 | Motor de reportes. Compila archivos `.jrxml` y genera PDFs con tickets de cita y recetas médicas. |
| **jrviewer-fx** | 0.1.1 | Visor de JasperReports nativo para JavaFX. Muestra los reportes en una ventana JavaFX sin depender de Swing puro. |
| **Jackson Databind** | 2.17.1 | Serialización/deserialización JSON. Usado en `ConsultaDNI` para parsear la respuesta de la API del RENIEC. |
| **jackson-datatype-jsr310** | 2.17.1 | Soporte para tipos de fecha Java 8+ (`LocalDate`, `LocalDateTime`) en Jackson. |
| **Apache POI OOXML** | 5.2.3 | Lectura y escritura de archivos Excel (.xlsx). Disponible para exportaciones de reportes. |
| **jsoup** | 1.18.1 | Parser HTML. Usado para hacer scraping/consulta de servicios web que devuelven HTML (consulta RENIEC). |
| **Gson** | 2.11.0 | Librería Google para parsear JSON. Alternativa ligera a Jackson para respuestas simples. |
| **escpos-coffee** | 4.1.0 | Librería para comunicación con impresoras térmicas ESC/POS. Permite enviar comandos de impresión (negrita, centrado, corte de papel) a ticketeras POS-80. |
| **Logback Classic** | 1.4.14 | Framework de logging principal. Escribe logs con niveles INFO/WARN/ERROR configurables. |
| **Log4j API/Core** | 2.23.1 | Framework de logging secundario (requerido por JasperReports internamente). |
| **JUnit Jupiter** | 5.10.2 | Framework de pruebas unitarias (scope test). |
| **MigLayout** | 3.7.4 | Layout manager avanzado para paneles complejos (requerido transitoriamente por JasperReports). |

### Plugins de build

| Plugin | Para qué sirve |
|--------|---------------|
| **javafx-maven-plugin** | Ejecuta la app con `mvn javafx:run`. Configura el `mainClass` y los argumentos JVM de encoding UTF-8. |
| **maven-compiler-plugin** | Compila con Java 17, habilita `-parameters` (necesario para Jakarta Validation) y configura el procesador de anotaciones de Lombok. |
| **maven-shade-plugin** | Empaqueta todas las dependencias en un único JAR ejecutable (fat-jar/uber-jar). Excluye firmas digitales que causarían conflictos (`*.SF`, `*.DSA`, `*.RSA`). |

---

## 📄 resources/application.properties

Archivo de configuración externo que permite cambiar parámetros sin recompilar.

```properties
# BD en modo servidor TCP (accesible desde DBeaver simultáneamente)
db.url=jdbc:h2:tcp://localhost:9040/./data/clinica;DB_CLOSE_ON_EXIT=FALSE
db.h2server.enabled=true       # ¿Levantar servidor TCP al iniciar la app?
db.h2server.port=9040          # Puerto TCP de H2 (DBeaver se conecta aquí)
db.h2server.allowOthers=true   # Permite conexiones desde otros procesos

db.driver=org.h2.Driver
db.username=sa
db.password=                   # Sin contraseña (entorno de desarrollo)

# HikariCP — pool de conexiones
db.pool.maximumPoolSize=5      # Máximo 5 conexiones simultáneas
db.pool.minimumIdle=1          # Al menos 1 conexión siempre lista
db.pool.connectionTimeout=30000
db.pool.idleTimeout=600000
db.pool.maxLifetime=1800000

# DDL automático al arrancar
db.ddl.auto=true
db.ddl.script=schema_clinica.sql  # Script SQL a ejecutar
```

**Por qué TCP y no embebido:** El modo servidor TCP permite que DBeaver se conecte en paralelo mientras la app está corriendo, lo que facilita la administración y depuración de la base de datos sin cerrar la aplicación.

---

## 🗄️ resources/schema_clinica.sql

Script SQL **idempotente** que crea la estructura de la base de datos y carga datos iniciales. Se ejecuta automáticamente cada vez que inicia la app.

### Tablas creadas

| Tabla | Descripción |
|-------|-------------|
| `upeu_emisor` | Datos de la clínica (nombre, RUC, dirección) usados en cabeceras de reportes |
| `upeu_perfil` | Roles del sistema: Root, Administrador, Recepcionista, Medico, Enfermero |
| `upeu_usuario` | Cuentas de acceso con clave y referencia al perfil |
| `upeu_paciente` | Datos personales del paciente (DNI, nombres, teléfono, dirección) |
| `upeu_especialidad` | Catálogo médico (Medicina General, Pediatría, Cardiología, etc.) |
| `upeu_medico` | Médicos con número de colegiatura y especialidad |
| `upeu_horario` | Horarios de atención por médico y día de la semana |
| `upeu_enfermero` | Datos del personal de enfermería |
| `upeu_cita` | Cita médica con estado, ticket, fecha, hora y tipo de atención |
| `upeu_triaje` | Signos vitales (presión, temperatura, peso, talla) registrados por enfermero |
| `upeu_consulta` | Consulta médica: síntomas, diagnóstico, exámenes solicitados |
| `upeu_receta` | Receta emitida por el médico: indicaciones y recomendaciones |
| `upeu_receta_detalle` | Líneas de la receta: medicamento, dosis, frecuencia, duración, vía |

### Claves de diseño
- `IF NOT EXISTS` en cada `CREATE TABLE` garantiza que el script se puede ejecutar múltiples veces sin error.
- `MERGE INTO ... KEY(id)` en los datos semilla actualiza si ya existe o inserta si es nuevo, evitando duplicados.
- Las secuencias IDENTITY se reinician en 100 tras cada arranque (en `DatabaseConfig.syncIdentitySequences()`) para que los registros nuevos no colisionen con los seeds que usan IDs 1–8.

### Estado máquina de la Cita
```
PROGRAMADA → EN_ESPERA → TRIAJE → EN_CONSULTA → ATENDIDA
     *      →                                  → CANCELADA
```
Restringido por constraint: `CHECK (estado IN ('PROGRAMADA','EN_ESPERA','TRIAJE','EN_CONSULTA','ATENDIDA','CANCELADA'))`

---

## ⚙️ config/DatabaseConfig.java

Clase utilitaria estática que gestiona toda la infraestructura de base de datos.

**Lo más valioso:**
- **`init()`**: punto de entrada único. Levanta el servidor H2 TCP, configura HikariCP, ejecuta el DDL y sincroniza secuencias. Es `synchronized` para garantizar que solo una instancia del pool exista aunque múltiples threads llamen a `init()` simultáneamente.
- **Servidor H2 TCP tolerante**: si el puerto ya está ocupado (app reiniciada en caliente), continúa sin fallar asumiendo que el servidor existente es válido.
- **`runDdlScript()`**: lee el SQL, lo divide por `;`, ignora comentarios `--` y ejecuta cada sentencia individualmente. Tolera errores de "ya existe" pero registra los demás para depuración.
- **`syncIdentitySequences()`**: recorre todas las tablas y hace `ALTER TABLE ... RESTART WITH MAX(pk)+1` para evitar colisiones entre los IDs semilla (1–8) y los IDs generados en runtime.
- **`shutdown()`**: llamado al cerrar la app. Cierra el pool HikariCP y detiene el servidor H2 TCP limpiamente.

---

## ⚙️ config/AppContext.java

**Contenedor de inyección de dependencias (DI) manual** en tres capas: Repositorios → Servicios → Controladores.

**Lo más valioso:**
- Reemplaza frameworks como Spring sin la complejidad de anotaciones y configuración XML.
- El `FXMLLoader` usa `setControllerFactory(context::getBean)` para que JavaFX instancie los controladores con sus dependencias ya inyectadas.
- `getBean()` primero busca por tipo exacto; si no encuentra, busca por asignabilidad (subclase/interfaz), lo que permite registrar implementaciones concretas y recuperarlas por su interfaz.
- El orden de registro importa: Repositorios primero (sin dependencias), luego Servicios (dependen de Repositorios), luego Controladores (dependen de Servicios).

---

## 🚀 App.java / ClinicaApplication.java

### App.java
Clase principal de JavaFX. Extiende `Application` y orquesta el arranque:
1. Llama `DatabaseConfig.init()` para levantar la BD antes de mostrar cualquier ventana.
2. Configura `AppContext` (DI).
3. Carga `login.fxml` como ventana inicial con `StageManager`.
4. Registra `DatabaseConfig.shutdown()` en el hook de cierre de la app.

### ClinicaApplication.java
Clase puente que lanza `App.main()` desde un contexto sin módulos JavaFX. Necesaria cuando se ejecuta el fat-JAR generado por `maven-shade-plugin`, ya que el JAR no tiene module-info y Java requiere este patrón para acceder a los módulos JavaFX.

---

## 📦 model/ — Entidades del dominio

Todas las entidades usan Lombok (`@Data @Builder @NoArgsConstructor @AllArgsConstructor`) para generar automáticamente getters, setters, equals, hashCode y el patrón Builder.

| Clase | Tabla | Descripción |
|-------|-------|-------------|
| `Cita` | `upeu_cita` | Entidad central del flujo clínico. Contiene el `numTicket` ("T-yyyyMMdd-####"), el estado de la state-machine, referencias a Paciente, Medico y Especialidad. |
| `Consulta` | `upeu_consulta` | Consulta médica: síntomas, diagnóstico, observaciones. Referencia a su Cita. Al cerrarse, la cita pasa a ATENDIDA. |
| `Receta` | `upeu_receta` | Receta emitida opcionalmente al cierre de una Consulta. Tiene indicaciones generales y recomendaciones. La lista de `detalles` se carga por separado. |
| `RecetaDetalle` | `upeu_receta_detalle` | Una línea de medicamento: medicamento, dosis, frecuencia, duración, vía. |
| `Ticket` | *(no persiste)* | DTO de impresión proyectado desde una Cita. Lo consume el `.jrxml` y el `TicketPrinter`. |
| `Paciente` | `upeu_paciente` | Datos del paciente con validaciones Jakarta: DNI exactamente 8 dígitos, nombres obligatorios, teléfono 9 dígitos. |
| `Medico` | `upeu_medico` | Médico con número de colegiatura y especialidad anidada. |
| `Especialidad` | `upeu_especialidad` | Catálogo de especialidades médicas. |
| `Horario` | `upeu_horario` | Horario de un médico: día de semana (LUN–DOM), hora inicio y fin. |
| `Enfermero` | `upeu_enfermero` | Personal de enfermería que realiza el triaje. |
| `Triaje` | `upeu_triaje` | Signos vitales registrados antes de la consulta: presión sistólica/diastólica, temperatura, frecuencia cardíaca, peso, talla. |
| `Usuario` | `upeu_usuario` | Cuenta de sistema con perfil y referencia al médico o enfermero asociado. |
| `Perfil` | `upeu_perfil` | Rol del usuario: Root, Administrador, Recepcionista, Medico, Enfermero. |
| `Emisor` | `upeu_emisor` | Datos de la clínica para cabeceras de reportes: nombre comercial, RUC, dirección. |

---

## 🗃️ repository/ — Acceso a datos

### ICrudGenericoRepository.java
Interfaz con las 5 operaciones CRUD básicas: `save`, `update`, `findById`, `findAll`, `deleteById`, `existsById`. Base común de todos los repositorios.

### helper/SqlHelper.java
Clase base que encapsula la comunicación con JDBC:
- `executeQuery()`: ejecuta un SELECT y mapea resultados con `mapRow()`.
- `executeQueryOne()`: SELECT que devuelve `Optional<T>`.
- `executeInsertGetKey()`: INSERT que devuelve la PK autogenerada.
- `executeUpdateStandalone()`: UPDATE/DELETE con su propia conexión del pool.
- `executeExists()`: SELECT 1 para verificar existencia sin mapear.
- `openConnection()`: obtiene conexión de `DatabaseConfig.getDataSource()`.
- El método abstracto `mapRow(ResultSet)` lo implementa cada repositorio concreto.

### AbstractJpaRepository.java
Implementa `ICrudGenericoRepository` usando `SqlHelper`. Aporta:
- Transacciones manuales en `save()` y `update()`: `autoCommit=false` + `commit`/`rollback`.
- `findAll()` y `deleteById()` genéricos usando `getTableName()` y `getPkColumn()`.
- Los repositorios que necesitan JOINs (Cita, Medico, Usuario) sobreescriben `findById()` con un SELECT propio.

### Repositorios concretos destacados

**CitaRepository:**
- `findByFecha()`, `findByEstado()`, `findByEstadoYFecha()`, `findByMedicoYFecha()`, `findByMedicoYEstado()`: múltiples filtros para las pantallas del flujo.
- `getSiguienteTurno()`: calcula el próximo número correlativo del día para el `numTicket`.
- `mapRow()`: hidrata la Cita con JOINs internos cargando Paciente, Medico (con Especialidad) y Especialidad completos.

**RecetaRepository:**
- `findByConsulta()`: busca la receta más reciente de una consulta. Usado por `MainRecetaController`.
- `mapRow()`: solo carga `idConsulta` (sin JOIN). El caller debe enriquecer llamando a `ConsultaRepository` y `CitaRepository`.

**ConsultaRepository:**
- `findByCita()`: última consulta de una cita.
- `mapRow()`: solo carga `idCita`. Diseño deliberado: el caller decide cuándo necesitar la Cita completa.

**UsuarioRepository:**
- `findByUsuarioYClave()`: autenticación. Busca por credenciales y carga el Perfil asociado.

---

## 🔌 service/ — Lógica de negocio

### ICrudGenericoService.java
Interfaz con `save`, `update`, `findAll`, `findById` (lanza `ModelNotFoundException` si no existe), `delete`. El `findById` aquí lanza excepción en vez de devolver `Optional`, garantizando que el caller no tenga que manejar ausencia.

### CrudGenericoServiceImp.java
Implementación base que delega al repositorio. El método `findById` convierte el `Optional` del repositorio en excepción si está vacío. Todas las implementaciones de servicio heredan de esta clase.

### ICitaService / CitaServiceImp
**El servicio más complejo del proyecto.** Implementa la state-machine completa:
- `registrarCita()`: valida disponibilidad del médico en ese slot, genera el `numTicket` correlativo (`T-yyyyMMdd-####`) y persiste la cita en estado PROGRAMADA.
- `checkIn()`: PROGRAMADA → EN_ESPERA. Valida que la cita esté en PROGRAMADA antes de mover.
- `marcarEnTriaje()`, `marcarEnConsulta()`, `marcarAtendida()`: transiciones internas con guardas de estado.
- `cancelar()`: rechaza si ya está ATENDIDA o CANCELADA.

### IConsultaService / ConsultaServiceImp
- `guardarConsulta()`: operación transaccional compleja. Inserta la Consulta, inserta la Receta si no es null, itera y guarda cada `RecetaDetalle`, y finalmente marca la Cita como ATENDIDA. Todo en una sola transacción lógica.

### IRecetaService / RecetaServiceImp
- `findByConsulta()`: delega al repositorio.
- `findDetalles()`: busca todos los `RecetaDetalle` de una receta. Usado antes de imprimir para tener la lista de medicamentos.

### IReporteService / ReporteServiceImp
- **Cache de reportes compilados:** usa `ConcurrentHashMap<String, JasperReport>` para compilar cada `.jrxml` solo la primera vez y reutilizar en las siguientes llamadas.
- `generarTicket()`: recarga la cita completa con JOINs, proyecta a `Ticket`, construye el mapa de parámetros y llena el reporte con `JREmptyDataSource` (1 fila vacía, suficiente para el banner del ticket).
- `generarReceta()`: valida que la consulta tenga cita, busca la receta por consulta, carga los detalles, construye los parámetros y usa `JRBeanCollectionDataSource` con la lista de medicamentos como fuente de datos del detail band.
- `generarReporteCitas()`: pasa una `Connection` JDBC directamente a Jasper para que ejecute la query SQL embebida en el `.jrxml`.

### ITicketService / TicketServiceImp
- `buildTicket()`: proyecta una `Cita` a un `Ticket` (DTO de impresión). Extrae el turno desde el sufijo numérico del `numTicket`, obtiene los datos del `Emisor` de la BD y completa todos los campos.
- `renderText()`: renderiza el ticket como texto ASCII monoespaciado de 42 caracteres. Sirve como fallback visual cuando Jasper no está disponible.

---

## 🎮 controller/ — Controladores FXML

Cada controlador corresponde a una pantalla de la app. JavaFX los instancia a través del `ControllerFactory` de `AppContext`, con todas sus dependencias ya inyectadas.

### LoginController
Gestiona el formulario de login. Valida usuario/contraseña contra la BD, puebla `SessionManager` con el perfil y el ID del usuario, y navega a `maingui.fxml`.

### MainGuiController
Controlador de la ventana principal con la barra de menús. Carga el menú dinámicamente desde la BD según el perfil del usuario. Gestiona el selector de temas CSS (azul, oscuro, rosado, verde) con persistencia en `java.util.prefs.Preferences`.

### MainCitaController
Registro de nuevas citas médicas. Incorpora autocompletado de DNI: mientras se escribe el DNI del paciente, un `AutoCompleteTextField` muestra coincidencias y rellena automáticamente nombre, apellidos y teléfono al seleccionar.

### MainCheckinController
Confirma la llegada del paciente. Lista las citas en estado PROGRAMADA y permite hacer check-in (PROGRAMADA → EN_ESPERA) con un botón.

### MainTriajeController
Registro de signos vitales por el enfermero. Carga citas EN_ESPERA, permite seleccionar una y registrar presión, temperatura, peso, talla, frecuencia cardíaca y motivo de consulta.

### MainConsultaController
Consulta médica. Lista citas EN_CONSULTA del médico logueado. Permite registrar síntomas, diagnóstico, exámenes solicitados, receta con medicamentos y finalizar la consulta (que guarda todo y cierra la cita como ATENDIDA).

### MainTicketController ⭐ (recién mejorado)
Pantalla "Ver e Imprimir". Muestra **dos tablas en paralelo**: Citas del día a la izquierda, Recetas a la derecha. Al seleccionar una fila, los botones actúan sobre el ítem seleccionado. Enriquece cada receta en `cargarDatos()` para cargar la consulta completa (diagnóstico + paciente). Control de rol para impresión ESC/POS de recetas: solo Médico, Administrador y Root.

### MainRecetaController
Visualización de la última receta cerrada. Muestra cabecera (diagnóstico, fecha), tabla de medicamentos y campos de indicaciones/recomendaciones. Permite copiar al portapapeles, ver en Jasper o exportar PDF.

### MainReporteController
Generación de reportes de citas. Filtros por rango de fechas, especialidad, médico y estado. Genera el reporte vía `ReporteServiceImp.generarReporteCitas()` que pasa una conexión JDBC directa al motor Jasper.

### CRUDs (Especialidad, Médico, Enfermero, Paciente, Horario, Usuario)
Patrón uniforme en los 6 controladores CRUD:
1. Tabla con búsqueda en vivo (`TableSearchFilter`).
2. Formulario a la derecha con campos vinculados.
3. Botones: Nuevo, Guardar, Actualizar, Eliminar.
4. `onGuardar()` valida con `FormValidator` (Jakarta) antes de persistir.
5. Al seleccionar fila, rellena el formulario automáticamente.

---

## 🧩 components/ — Componentes UI Reutilizables

### AutoCompleteTextField.java
Extiende `TextField`. Mientras el usuario escribe, filtra una lista de `ModeloDataAutocomplet` y muestra coincidencias en un `ContextMenu` desplegable. Al elegir una opción, dispara un callback configurable. Usado en Registro de Cita para autocompletar DNI → datos del paciente.

### TableSearchFilter.java
Filtro de búsqueda en tiempo real para `TableView<T>`. El constructor recibe un `TextField` y un `TableView` y registra un listener en el texto. Al escribir, filtra los ítems de la tabla mostrando solo los que coincidan con el texto en cualquier columna. Reutilizado en los 4 CRUDs principales.

### FormValidator.java
Puente entre Jakarta Validation y la UI JavaFX. Recibe un objeto anotado con Jakarta (`@NotBlank`, `@Size`, `@Pattern`) y un mapa `propiedad → Control`. Ejecuta `Validator.validate()`, y por cada violación encontrada, marca el control correspondiente en rojo con un tooltip que muestra el mensaje de error.

### Toast.java
Notificaciones flotantes no bloqueantes. `showToast()` muestra error en rojo, `showSuccess()` en verde. Se posicionan sobre la ventana, se muestran con una animación de fade-in y desaparecen automáticamente tras el tiempo especificado.

### ToltipCustom.java
Aplica estilos visuales a controles con error: borde rojo (`-fx-border-color: red`) y tooltip con el mensaje de validación. Usado por `FormValidator` para marcar campos inválidos.

### JasperViewerFX.java
Abre una nueva ventana JavaFX mostrando un `JasperPrint` pre-generado usando el visor `jrviewer-fx`. Recibe el título y el objeto `JasperPrint`, crea un `Stage` y muestra el reporte en él.

### StageManager.java
Gestor centralizado de ventanas JavaFX. Carga archivos `.fxml` con el `ControllerFactory` de `AppContext`, aplica el tema CSS guardado en `Preferences` y abre la ventana como nueva `Scene`. Garantiza que todos los formularios usen el tema visual actual.

### TableViewHelper.java
Utilidad para configurar columnas de `TableView` de forma declarativa usando `ColumnInfo`. Reduce el código repetitivo de definir columnas en cada controlador.

### ReportAlert.java / ReportDialog.java
Diálogos de alerta y dialogs modales para presentar información de reportes al usuario con estilos consistentes con el tema actual de la app.

---

## 📋 dto/ — Objetos de Transferencia y Sesión

### SessionManager.java
**Singleton thread-safe de sesión.** Almacena en memoria el estado de la sesión activa:
- `userId`, `userName`, `userPerfil` ("Root", "Administrador", "Recepcionista", "Medico", "Enfermero"), `idReferencia` (id_medico o id_enfermero).
- `lastCita`: última cita registrada → la consume `MainTicketController`.
- `lastConsulta`: última consulta cerrada → la consume `MainRecetaController`.

No se persiste; vive solo mientras la app está abierta. El patrón singleton con `synchronized` garantiza que no haya dos instancias aunque múltiples threads accedan simultáneamente.

### TicketDto.java
DTO auxiliar para representar datos del ticket en formatos intermedios de exportación.

### ColaPacienteDto.java
DTO de proyección para la cola de atención. Agrupa paciente + cita + estado para la vista de cola.

### ModeloDataAutocomplet.java
Modelo de datos para el autocompletado. Tiene tres campos: `clave` (DNI), `valor` (nombre a mostrar) y `extra` (datos adicionales a recuperar al seleccionar).

### ComboBoxOption.java
Envuelve un ID (Long) y una descripción (String) para llenar ComboBox en los formularios. Sobreescribe `toString()` para mostrar la descripción.

### MenuMenuItenTO.java
Transfer Object que representa un ítem del menú cargado dinámicamente desde la BD (nombre del menú, nombre del ítem, ruta del FXML, icono).

### PersonaDto.java
DTO base con campos comunes de persona (dni, nombres, apellidos) usado para transferir datos entre capas sin exponer la entidad completa.

---

## 🔢 enums/ — Enumeraciones del dominio

| Enum | Valores | Uso |
|------|---------|-----|
| `EstadoCita` | PROGRAMADA, EN_ESPERA, TRIAJE, EN_CONSULTA, ATENDIDA, CANCELADA | State-machine de la cita. El repositorio guarda y lee el `.name()`. |
| `TipoAtencion` | PROGRAMADA, ORDEN_LLEGADA, EMERGENCIA | Tipo de atención de la cita, influye en la prioridad. |
| `DiaSemana` | LUN, MAR, MIE, JUE, VIE, SAB, DOM | Días de horario del médico. Validado con CHECK constraint en BD. |
| `Sexo` | MASCULINO, FEMENINO, OTRO | Sexo del paciente en el formulario. |
| `TipoDocumento` | DNI, CARNET_EXTRANJERIA, PASAPORTE | Tipo de documento de identidad (para extensiones futuras). |

---

## ⚠️ exception/

### ModelNotFoundException.java
Extiende `RuntimeException`. Se lanza desde `CrudGenericoServiceImp.findById()` cuando el registro no existe en la BD. Recibe el tipo de entidad y el ID buscado para construir un mensaje descriptivo.

---

## 🖨️ utils/ — Utilidades

### TicketPrinter.java
Envía un `Ticket` a la impresora térmica ESC/POS usando la librería `escpos-coffee`. Aplica estilos de formato (negrita centrada para el nombre de la clínica, alineado izquierda para los datos). Al final avanza 3 líneas y corta el papel automáticamente (`CutMode.FULL`). Lanza `IOException` si no hay impresora disponible.

### RecetaPrinter.java
Imprime una receta médica en la ticketera térmica. Navega la cadena `receta → consulta → cita → paciente` para extraer el nombre del paciente y el diagnóstico. Itera los detalles e imprime cada medicamento con su dosis, frecuencia, duración y vía. Al final imprime indicaciones y recomendaciones.

### PrinterManager.java
Singleton que busca la impresora térmica "POS-80-Series" en las impresoras del sistema. Recorre `PrinterOutputStream.getListPrintServicesNames()` y selecciona la primera que contenga el texto buscado. Lanza `IOException` si no encuentra ninguna, permitiendo que la app continúe sin impresora física.

### ConsultaDNI.java
Consulta la API del RENIEC (o un servicio web equivalente) para autocompletar datos de persona por DNI. Usa `jsoup` para hacer la petición HTTP y `Jackson`/`Gson` para parsear la respuesta JSON. Devuelve `PersonaDto` con nombres y apellidos.

### UtilsX.java
Métodos de utilidad general: formateo de fechas, validaciones adicionales de DNI, conversión de tipos, etc.

---

## 🎨 resources/css/ — Temas visuales

Cuatro temas CSS que se aplican dinámicamente a la `Scene` de JavaFX desde `MainGuiController`.

| Archivo | Tema | Características |
|---------|------|-----------------|
| `estilo-azul.css` | Azul profesional | Botones azules, fondos gris oscuro, texto blanco |
| `estilo-oscuro.css` | Oscuro (dark mode) | Fondo negro/gris muy oscuro, texto blanco, bordes sutiles |
| `estilo-rosado.css` | Rosado suave | Tonos pastel rosas, ideal para entorno clínico femenino |
| `estilo-verde.css` | Verde salud | Botones verdes, evoca ambiente médico/hospitalario |
| `styles.css` | Base | Variables de color y estilos compartidos entre temas |

El tema elegido se persiste en `java.util.prefs.Preferences` con la clave `"tema"` y se reaaplica al siguiente arranque de la app.

---

## 📊 resources/jasper/ — Reportes JasperReports

Los `.jrxml` son archivos XML que JasperReports compila a `JasperReport` en la primera ejecución (se cachean en `ReporteServiceImp`).

### ticket_cita.jrxml
Ticket de cita médica. Formato de papel estrecho (80mm). Recibe parámetros (no datasource con registros): nombre del emisor, RUC, dirección, datos del paciente, médico, especialidad, fecha, hora, turno y tipo de atención. Imprime en formato de ticketera.

### receta_medica.jrxml
Receta médica. Usa `JRBeanCollectionDataSource` con la lista de `RecetaDetalle` como datasource del detail band (una línea por medicamento). Parámetros para cabecera: paciente, médico, diagnóstico, indicaciones, recomendaciones y fecha.

### reporte_citas.jrxml
Reporte gerencial de citas. Tiene la query SQL embebida en el XML, con parámetros opcionales para filtrar por fecha, especialidad, médico y estado. Recibe una `Connection` JDBC directa de Jasper para ejecutarla.

---

## 🌐 resources/language/ — Internacionalización

### idiomas-es.properties
Textos de la interfaz en español. Cargado por defecto.

### idiomas-en.properties
Textos de la interfaz en inglés. Activado desde el menú "Idioma". Permite cambiar el idioma de la app en runtime.

---

## 🖼️ resources/view/ — Vistas FXML

Cada archivo `.fxml` define la estructura visual de una pantalla. Están vinculados a su controlador mediante el atributo `fx:controller`.

| Archivo | Pantalla | Descripción |
|---------|----------|-------------|
| `login.fxml` | Login | Formulario de usuario y contraseña con logo de la clínica |
| `maingui.fxml` | Ventana principal | Barra de menú dinámica + área de contenido central (AnchorPane) |
| `main_cita.fxml` | Registrar Cita | Formulario con autocompletado DNI, selección de médico/especialidad/fecha/hora |
| `main_checkin.fxml` | Check-in | Lista de citas PROGRAMADA con botón de confirmación de llegada |
| `main_triaje.fxml` | Triaje | Formulario de signos vitales: presión, temperatura, peso, talla, FC |
| `main_consulta.fxml` | Consulta Médica | Anamnesis + diagnóstico + receta con tabla de medicamentos |
| `main_ticket.fxml` | Ver e Imprimir | **Dos tablas**: Citas (izquierda) y Recetas (derecha) + botones Imprimir/Jasper/PDF |
| `main_receta.fxml` | Ver Receta | Tabla de medicamentos + indicaciones + recomendaciones + visor |
| `main_paciente.fxml` | Pacientes | CRUD con búsqueda en vivo, validación de DNI (8 dígitos) y teléfono (9 dígitos) |
| `main_medico.fxml` | Médicos | CRUD con selector de especialidad y número de colegiatura |
| `main_enfermero.fxml` | Enfermeros | CRUD básico |
| `main_especialidad.fxml` | Especialidades | CRUD de catálogo médico |
| `main_horario.fxml` | Horarios | Asignación de horarios por médico y día de semana |
| `main_usuario.fxml` | Usuarios | CRUD con selector de perfil y contraseña |
| `main_reporte.fxml` | Reportes | Filtros de fecha/especialidad/médico/estado + visor de reporte |

---

## 🔐 Seguridad y control de roles

El sistema aplica control de acceso basado en roles (`SessionManager.getUserPerfil()`) en puntos clave:

| Acción | Root | Admin | Recepción | Médico | Enfermero |
|--------|:----:|:-----:|:---------:|:------:|:---------:|
| Imprimir Cita (ESC/POS) | ✅ | ✅ | ✅ | ✅ | ✅ |
| Imprimir Receta (ESC/POS) | ✅ | ✅ | ❌ | ✅ | ❌ |
| Ver Receta (Jasper/PDF) | ✅ | ✅ | ✅ | ✅ | ✅ |
| Gestión CRUDs | ✅ | ✅ | ❌ | ❌ | ❌ |
| Registrar Cita | ✅ | ✅ | ✅ | ❌ | ❌ |
| Consulta Médica | ❌ | ❌ | ❌ | ✅ | ❌ |
| Triaje | ❌ | ❌ | ❌ | ❌ | ✅ |

El menú dinámico cargado desde la BD filtra automáticamente las opciones según el perfil, de modo que cada usuario solo ve las pantallas a las que tiene acceso.

---

## 🔄 Flujo clínico completo

```
Recepcionista           Enfermero              Médico
     │                      │                     │
     ▼                      │                     │
Registrar Cita              │                     │
(PROGRAMADA)                │                     │
     │                      │                     │
     ▼                      │                     │
  Check-in                  │                     │
(EN_ESPERA)                 │                     │
     │                      ▼                     │
     │              Registrar Triaje               │
     │              (TRIAJE + signos               │
     │               vitales)                      │
     │                      │                     │
     │                      ▼                     │
     │                      │            Consulta Médica
     │                      │            (EN_CONSULTA)
     │                      │                     │
     │                      │            Diagnóstico +
     │                      │            Receta opcional
     │                      │                     │
     │                      │                     ▼
     │                      │              Finalizar
     │                      │             (ATENDIDA)
     │                      │                     │
     ▼                      ▼                     ▼
                   Ver e Imprimir: Ticket de Cita / Receta Médica
```

---

## 📐 Patrón arquitectónico

El proyecto implementa **MVC en capas** sin frameworks de Spring ni JPA:

```
Vista (FXML)
    ↕ FXMLLoader + ControllerFactory
Controlador (controller/)
    ↕ inyección manual (AppContext)
Servicio (service/impl/)
    ↕ delegación directa
Repositorio (repository/)
    ↕ JDBC + HikariCP
Base de datos H2
```

**Ventajas de este diseño:**
- Sin reflexión en runtime (más rápido que Spring).
- Sin XML de configuración complejo.
- Las dependencias son explícitas y trazables en `AppContext`.
- Las transacciones son manuales y predecibles (`autoCommit=false + commit/rollback`).
- El pool HikariCP garantiza eficiencia incluso con múltiples pantallas abiertas.

---

**Documento generado:** 2026-06-22 | **Versión:** Clínica 2.0.1
