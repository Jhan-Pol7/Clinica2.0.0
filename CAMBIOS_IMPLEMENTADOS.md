# 📋 CAMBIOS COMPLETOS IMPLEMENTADOS — Clínica 2.0.1

**Documento de referencia:** Ubicación exacta de cada cambio, línea de código y descripción.

---

## 🔷 PUNTO 1 — Base de datos H2 + Servidor TCP + DBeaver

### 1.1 Configuración de propiedades
**Archivo:** `src/main/resources/application.properties`

| Líneas | Qué hace | Comentario |
|--------|----------|-----------|
| 1–5 | Comentario explicando modo servidor TCP vs archivo local | Documentación |
| 6 | `db.url=jdbc:h2:tcp://localhost:9040/./data/clinica` | URL de conexión servidor TCP |
| 7 | `db.h2server.enabled=true` | Activa el servidor H2 |
| 8 | `db.h2server.port=9040` | Puerto del servidor (9040) |
| 9 | `db.h2server.allowOthers=true` | Permite que DBeaver y otras apps se conecten |

**Cómo se usa:** DBeaver se conecta a `jdbc:h2:tcp://localhost:9040/./data/clinica` con usuario `sa` sin contraseña.

---

### 1.2 Arranque/parada del servidor H2
**Archivo:** `src/main/java/pe/edu/upeu/clinica/config/DatabaseConfig.java`

| Líneas | Método/Bloque | Qué hace |
|--------|---------------|----------|
| ~20 | Import | `import org.h2.tools.Server;` |
| ~28 | Campo | `private static Server h2Server;` (singleton) |
| 41–67 | `startH2Server()` | **Arranca el servidor TCP** en puerto 9040 |
| 55 | Flag `-ifNotExists` | Crea la BD si no existe |
| 60–66 | Try-catch | Manejo tolerante si puerto ocupado: reutiliza servidor existente |
| 158–160 | `shutdown()` | **Detiene el servidor** cuando cierra la app |

**Cómo funciona:**
- Al arrancar la app, se llama `startH2Server()` → servidor TCP escucha en `localhost:9040`
- DBeaver se conecta a ese puerto y accede a la BD
- Al cerrar la app, `shutdown()` detiene el servidor

---

## 🔷 PUNTO 2 — Búsqueda en vivo (filtro de tablas)

### 2.1 Componente reutilizable
**Archivo NUEVO:** `src/main/java/pe/edu/upeu/clinica/components/TableSearchFilter.java`

| Líneas | Qué hace |
|--------|----------|
| 1–30 | Javadoc + ejemplo de uso del componente |
| 32 | `public class TableSearchFilter<T>` |
| 40–47 | Constructor: engancha listener al `TextField` de búsqueda |
| 49–52 | `setData()`: carga los datos maestros |
| 55–67 | `aplicar()`: **filtra la tabla en tiempo real** por el texto ingresado |

**Reutilización:** El componente se instancia en 4 pantallas CRUD (Paciente, Médico, Enfermero, Especialidad).

---

### 2.2 Integración en CRUD de Paciente
**Archivo:** `src/main/resources/view/main_paciente.fxml`
| Línea | Qué hace |
|-------|----------|
| 76 | `<TextField fx:id="txtBuscar"` | Campo de búsqueda |

**Archivo:** `src/main/java/pe/edu/upeu/clinica/controller/MainPacienteController.java`
| Líneas | Qué hace |
|--------|----------|
| 60 | `private TableSearchFilter<Paciente> filtro;` | Instancia el filtro |
| 89–90 | En `initialize()` | Configura el filtro con la tabla |
| 98 | En `initialize()` | Carga datos iniciales en el filtro |

---

### 2.3 Integración en CRUD de Médico
**Archivo:** `src/main/resources/view/main_medico.fxml`
| Línea | Qué hace |
|-------|----------|
| 70 | `<TextField fx:id="txtBuscar"` | Campo de búsqueda |

**Archivo:** `src/main/java/pe/edu/upeu\clinica/controller/MainMedicoController.java`
| Líneas | Qué hace |
|--------|----------|
| 75 | `private TableSearchFilter<Medico> filtro;` | Instancia el filtro |
| 107–108 | En `initialize()` | Configura el filtro |
| 118 | En `initialize()` | Carga datos |

---

### 2.4 Integración en CRUD de Enfermero
**Archivo:** `src/main/resources/view/main_enfermero.fxml`
| Línea | Qué hace |
|-------|----------|
| 58 | `<TextField fx:id="txtBuscar"` | Campo de búsqueda |

**Archivo:** `src/main/java/pe/edu/upeu/clinica/controller/MainEnfermeroController.java`
| Líneas | Qué hace |
|--------|----------|
| 57 | `private TableSearchFilter<Enfermero> filtro;` | Instancia |
| 81–82 | En `initialize()` | Configura el filtro |
| 89 | En `initialize()` | Carga datos |

---

### 2.5 Integración en CRUD de Especialidad
**Archivo:** `src/main/resources/view/main_especialidad.fxml`
| Línea | Qué hace |
|-------|----------|
| 57 | `<TextField fx:id="txtBuscar"` | Campo de búsqueda |

**Archivo:** `src/main/java/pe/edu/upeu/clinica/controller/MainEspecialidadController.java`
| Líneas | Qué hace |
|--------|----------|
| 54–55 | `private TableSearchFilter<Especialidad>` | Instancia |
| 74 | En `initialize()` | Carga datos |

---

## 🔷 PUNTO 3 — Validación de campos con Jakarta

### 3.1 Componente reutilizable
**Archivo NUEVO:** `src/main/java/pe/edu/upeu/clinica/components/FormValidator.java`

| Líneas | Qué hace |
|--------|----------|
| 1–17 | Javadoc explicando el puente entre validación Jakarta ↔ UI |
| 19 | `public class FormValidator` |
| 29–43 | `validar()`: ejecuta `@NotBlank`, `@Size`, `@Pattern`, etc. y marca en rojo los errores |

**Cómo funciona:**
1. Pasas un mapa `propiedad → Control JavaFX` (ej: `"nombres" → txtNombres`)
2. El componente valida el modelo usando Jakarta
3. Si hay error, marca el control en **rojo** con **tooltip** mostrando el mensaje

---

### 3.2 Validaciones en CRUD Paciente
**Archivo:** `src/main/java/pe/edu/upeu/clinica/controller/MainPacienteController.java`

| Líneas | Qué hace |
|--------|----------|
| 130–135 | En `onGuardar()` | Crea el mapa de validación (dni→txtDni, nombres→txtNombres, etc.) |
| 136 | Llama a `FormValidator.validar(paciente, mapa)` | Valida y marca errores |
| 137–142 | Si hay errores, muestra Toast rojo | Detiene el guardado |

**Validaciones aplicadas:**
- `dni`: `@Size(min=8, max=8)` + `@Pattern("\\d{8}")` → exactamente 8 dígitos
- `nombres`: `@NotBlank` → obligatorio
- `apellidos`: `@NotBlank` → obligatorio
- `telefono`: `@Pattern("\\d{9}")` → exactamente 9 dígitos (opcional)

**Archivo modelo:** `src/main/java/pe/edu/upeu/clinica/model/Paciente.java`
| Líneas | Validación |
|--------|-----------|
| 24–26 | `@NotBlank`, `@Size(8)`, `@Pattern("\\d{8}")` en `dni` |
| 29–30 | `@NotBlank` en `nombres` |
| 32–33 | `@NotBlank` en `apellidos` |
| 35–36 | `@Pattern("\\d{9}")` en `telefono` (opcional) |

---

### 3.3 Validaciones en CRUD Médico
**Archivo:** `src/main/java/pe/edu/upeu/clinica/controller/MainMedicoController.java`

| Líneas | Qué hace |
|--------|----------|
| 144–150 | En `onGuardar()` | Crea mapa y llama a `FormValidator.validar()` |

**Archivo modelo:** `src/main/java/pe/edu/upeu/clinica/model/Medico.java`
| Líneas | Validación |
|--------|-----------|
| ~20–25 | `@NotBlank` en `nombres`, `@Size(8)` en `dni`, `@Pattern("\\d{9}")` en `telefono` |

---

### 3.4 Validaciones en CRUD Enfermero
**Archivo:** `src/main/java/pe/edu/upeu/clinica/controller/MainEnfermeroController.java`

| Líneas | Qué hace |
|--------|----------|
| 104–109 | En `onGuardar()` | Valida y marca errores |

---

### 3.5 Validaciones en CRUD Especialidad
**Archivo:** `src/main/java/pe/edu/upeu/clinica/controller/MainEspecialidadController.java`

| Líneas | Qué hace |
|--------|----------|
| 93–95 | En `onGuardar()` | Valida nombre obligatorio |

---

## 🔷 PUNTO 4 — Temas CSS dinámicos

### 4.1 Archivos CSS
**Carpeta:** `src/main/resources/css/`

| Archivo | Qué contiene |
|---------|-------------|
| `estilo-azul.css` | Tema azul (botones azules, fondos claros) |
| `estilo-oscuro.css` | Tema oscuro (fondo negro, texto blanco) |
| `estilo-rosado.css` | Tema rosado (tonos pálidos) |
| `estilo-verde.css` | Tema verde (botones verdes) |
| `styles.css` | Estilos base reutilizables |

---

### 4.2 Selector de temas en el menú
**Archivo:** `src/main/java/pe/edu/upeu/clinica/controller/MainGuiController.java`

| Líneas | Qué hace |
|--------|----------|
| 64–71 | Campos FXML: `Menu mnuEstilo`, `ComboBox<String> cbEstilo` | Elementos del menú |
| 83–86 | En `initialize()` | Reaplica el tema guardado de `Preferences` al arrancar |
| 161–166 | En `initialize()` | Agrega el menú "Cambiar Estilo" a la barra |
| 179–184 | Método `cambiarEstilo()` | Cambia el tema y **lo persiste en Preferences** |
| 188–205 | Método `aplicarEstilo()` | Carga el archivo CSS y lo aplica a la `Scene` |

**Cómo usa Preferences:**
```java
// Guarda el tema elegido
Preferences prefs = Preferences.userRoot();
prefs.put("tema", "estilo-azul");

// Al siguiente arranque, lo recupera
String tema = prefs.get("tema", "estilo-azul"); // por defecto azul
```

---

## 🔷 PUNTO 5 — Autocompletado de DNI en Registro de Cita

### 5.1 Componente de autocompletado (reutilizable)
**Archivo:** `src/main/java/pe/edu/upeu/clinica/components/AutoCompleteTextField.java`

| Líneas | Qué hace |
|--------|----------|
| 1–30 | Javadoc explicando el componente |
| 32 | `public class AutoCompleteTextField extends TextField` |
| ~50–80 | Lógica: mientras escribes, muestra coincidencias en un `ContextMenu` |

**Modelos de datos:**
- `src/main/java/pe/edu/upeu/clinica/dto/ModeloDataAutocomplet.java` → estructura de datos (clave, valor, extra)
- `src/main/java/pe/edu/upeu/clinica/dto/ComboBoxOption.java` → wrapper para opciones

---

### 5.2 Integración en Registro de Cita
**Archivo:** `src/main/java/pe/edu/upeu/clinica/controller/MainCitaController.java`

| Líneas | Qué hace |
|--------|----------|
| 61–64 | Campos: `ObservableList<ModeloDataAutocomplet> entriesPaciente`, `AutoCompleteTextField autoCompDni` | Catálogo + campo |
| 90–103 | En `initialize()` | **Configura autocompletado**: al escribir DNI, muestra pacientes coincidentes |
| 100–102 | Listener | Detecta selección + rellena el formulario |
| 173–184 | Método `cargarEntradasPaciente()` | Arma el catálogo desde la BD (DNI → Nombres) |
| 186–192 | Método `seleccionarPaciente()` | Rellena `txtNombres`, `txtApellidos`, `txtTelefono` |

**Flujo:**
1. Usuario escribe DNI en `autoCompDni` (ej: "12345678")
2. Mientras escribe, aparece menú desplegable con pacientes que coinciden
3. Usuario elige uno → se llenan automáticamente nombres, apellidos, teléfono
4. Datos listos para registrar la cita

---

## 🔷 PUNTO 6 — Ver e Imprimir Tickets (Citas + Recetas) ⭐ NUEVO

### 6.1 Interfaz FXML — Dos tablas lado a lado
**Archivo:** `src/main/resources/view/main_ticket.fxml`

| Líneas | Elemento | Qué hace |
|--------|----------|----------|
| 11–13 | `<VBox>` | Contenedor principal (640×900) |
| 16–18 | `<Label>` | Títulos y etiqueta de selección |
| 20–51 | `<HBox>` | Contenedor de dos columnas |
| **Columna CITAS:** |
| 23–35 | `<VBox>` + `<TableView>` | Tabla de citas |
| 27–31 | `<TableColumn>` | Columnas: Ticket, Paciente, Fecha, Hora, Estado |
| **Columna RECETAS:** |
| 38–49 | `<VBox>` + `<TableView>` | Tabla de recetas |
| 42–45 | `<TableColumn>` | Columnas: N°, Paciente, Diagnóstico, Fecha |
| **Botones de acción:** |
| 54–59 | `<HBox>` con botones | Recargar, Imprimir (verde), Visor Jasper (azul), Exportar PDF |

**Diseño:** Dos tablas en paralelo dentro de un `HBox` con `spacing="14.0"` para separación.

---

### 6.2 Controlador — Lógica dual (Cita/Receta)
**Archivo:** `src/main/java/pe/edu/upeu/clinica/controller/MainTicketController.java`

#### Importaciones y campos (líneas 1–45)
| Líneas | Qué hace |
|--------|----------|
| 3–24 | **Importaciones:** FXCollections, TableView, Cita, Receta, ITicketService, IRecetaService, ICitaService, IReporteService, TicketPrinter, RecetaPrinter |
| 39–40 | Formatos de fecha: `FMT_FECHA` (dd/MM/yyyy), `FMT_DT` (dd/MM/yyyy HH:mm) |
| 42–45 | **Inyección de servicios:** ticketService, recetaService, citaService, reporteService |

#### Campos FXML — Citas (líneas 47–50)
| Líneas | Qué hace |
|--------|----------|
| 48 | `@FXML TableView<Cita> tablaCitas` | Tabla de citas |
| 49–50 | `@FXML TableColumn... colCitaTicket, colCitaPaciente` | Columnas de citas |

#### Campos FXML — Recetas (líneas 52–58)
| Líneas | Qué hace |
|--------|----------|
| 53 | `@FXML TableView<Receta> tablaRecetas` | Tabla de recetas |
| 54–57 | `@FXML TableColumn... colRecetaId, colRecetaPaciente, colRecetaDx, colRecetaFecha` | Columnas de recetas |

#### Constructor (líneas 69–72)
| Líneas | Qué hace |
|--------|----------|
| 69–72 | Recibe 4 servicios inyectados por AppContext |

#### Initialize (líneas 77–79)
| Líneas | Qué hace |
|--------|----------|
| 77 | `@FXML public void initialize()` |
| 78 | Configura columnas de citas |
| 79 | Configura columnas de recetas |

#### Configuración de tabla Citas (líneas 85–102)
| Líneas | Qué hace |
|--------|----------|
| 85–101 | Método `configurarTablaCitas()` |
| 86–88 | Columna Ticket: obtiene `numTicket` de la Cita |
| 89–92 | Columna Paciente: concatena `nombres + apellidos` |
| 93–96 | Columna Fecha: formatea `LocalDate` → "dd/MM/yyyy" |
| 97–100 | Columna Hora: muestra la hora directamente |
| 101–104 | Columna Estado: muestra el enum `EstadoCita` |
| 109–117 | Listener: al seleccionar fila en citas, **limpia selección en recetas** y actualiza etiqueta |

#### Configuración de tabla Recetas (líneas 119–149)
| Líneas | Qué hace |
|--------|----------|
| 119–147 | Método `configurarTablaRecetas()` |
| 120–122 | Columna N°: muestra `idReceta` |
| 123–130 | Columna Paciente: navega `receta.consulta.cita.paciente.nombres` |
| 131–138 | Columna Diagnóstico: obtiene `consulta.diagnostico` |
| 139–142 | Columna Fecha: formatea `LocalDateTime` |
| 147–155 | Listener: al seleccionar fila en recetas, **limpia selección en citas** |

#### Carga de datos (líneas 157–169)
| Líneas | Qué hace |
|--------|----------|
| 160 | Método `onRecargar()` |
| 162–169 | Método `cargarDatos()` |
| 164–166 | Carga **citas de hoy** usando `citaService.findByFecha()` |
| 169–171 | Carga **todas las recetas** usando `recetaService.findAll()` |

#### Acciones — Imprimir (líneas 174–194)
| Líneas | Qué hace |
|--------|----------|
| 174 | `@FXML public void onImprimir()` |
| 175–176 | Obtiene fila seleccionada en citas y recetas |
| 178–183 | Si hay cita, llama `imprimirCita()` |
| 184–186 | Si hay receta, llama `imprimirReceta()` |
| 188–193 | `imprimirCita()`: construye `Ticket` y llama `TicketPrinter.imprimir()` |
| 195–199 | `imprimirReceta()`: llama `RecetaPrinter.imprimir()` |

#### Acciones — Visor Jasper (líneas 201–220)
| Líneas | Qué hace |
|--------|----------|
| 201 | `@FXML public void onVisorJasper()` |
| 202–203 | Obtiene selecciones |
| 205–208 | Si hay cita: genera reporte Jasper y abre visor |
| 209–211 | Si hay receta: genera reporte de receta y abre visor |

#### Acciones — Exportar PDF (líneas 222–245)
| Líneas | Qué hace |
|--------|----------|
| 222 | `@FXML public void onExportarPdf()` |
| 225–232 | Si hay cita: llama `exportarPdfCita()` |
| 233–235 | Si hay receta: llama `exportarPdfReceta()` |
| 237–246 | `exportarPdfCita()`: genera Jasper, abre FileChooser, exporta a PDF |
| 248–257 | `exportarPdfReceta()`: igual para receta |

#### Helpers (líneas 259–268)
| Líneas | Qué hace |
|--------|----------|
| 259–262 | `stage()`: obtiene la ventana actual |
| 264–266 | `mostrarError()`: muestra Toast rojo |
| 268 | `mostrarExito()`: muestra Toast verde |

---

### 6.3 Servicio de impresión de recetas (NUEVO)
**Archivo NUEVO:** `src/main/java/pe/edu/upeu/clinica/utils/RecetaPrinter.java`

| Líneas | Qué hace |
|--------|----------|
| 1–20 | Importaciones y comentario explicativo |
| 22 | `public class RecetaPrinter` |
| 25–26 | Formatos de fecha: `FMT` = "dd/MM/yyyy HH:mm" |
| 28 | Método `imprimir(Receta receta)` |
| 29–30 | Obtiene la impresora térmica via `PrinterManager` |
| 32–35 | Define estilos ESC/POS: titulo (negrita+centrado), centro, izquierda, negrita |
| 37–40 | Imprime cabecera: "CLÍNICA MÁS CERCA DE DIOS" |
| 42–54 | Imprime datos: N° receta, fecha, paciente (via `cita`), DNI, diagnóstico |
| 56–74 | Itera medicamentos y imprime cada uno con dosis, frecuencia, duración, vía |
| 76–82 | Imprime indicaciones y recomendaciones si existen |
| 84–86 | Pie de página, avanza 3 líneas, corta el papel |

**Nota:** Sigue el patrón idéntico a `TicketPrinter` para garantizar consistencia.

---

### 6.4 Inyección de dependencias (AppContext)
**Archivo:** `src/main/java/pe/edu/upeu/clinica/config/AppContext.java`

| Líneas | Cambio |
|--------|--------|
| 121–123 | **Antes:** `new MainTicketController(getBean(ITicketService.class), getBean(IReporteService.class))` |
| 121–123 | **Ahora:** `new MainTicketController(getBean(ITicketService.class), getBean(IRecetaService.class), getBean(ICitaService.class), getBean(IReporteService.class))` |

**Explicación:** Se inyectan 4 servicios al controlador:
1. `ITicketService` → construir tickets desde citas
2. `IRecetaService` → listar y manipular recetas
3. `ICitaService` → listar citas
4. `IReporteService` → generar reportes Jasper

---

## 📊 Tabla resumen de archivos modificados/creados

| Tipo | Archivo | Líneas aprox. | Cambio |
|------|---------|---------------|--------|
| **Creado** | `src/main/java/.../components/TableSearchFilter.java` | 67 | Componente de búsqueda reutilizable |
| **Creado** | `src/main/java/.../components/FormValidator.java` | 43 | Componente de validación reutilizable |
| **Creado** | `src/main/java/.../utils/RecetaPrinter.java` | 86 | Impresión de recetas en ESC/POS |
| **Modificado** | `src/main/resources/application.properties` | 9 | Config H2 servidor TCP |
| **Modificado** | `src/main/java/.../config/DatabaseConfig.java` | 158–160 | Arranque/parada servidor H2 |
| **Modificado** | `src/main/java/.../controller/MainGuiController.java` | 64–205 | Selector de temas CSS |
| **Modificado** | `src/main/java/.../controller/MainCitaController.java` | 61–192 | Autocompletado DNI en cita |
| **Modificado** | `src/main/java/.../controller/MainPacienteController.java` | 60, 89–90, 130–142 | Búsqueda + Validación |
| **Modificado** | `src/main/java/.../controller/MainMedicoController.java` | 75, 107–150 | Búsqueda + Validación |
| **Modificado** | `src/main/java/.../controller/MainEnfermeroController.java` | 57, 81–109 | Búsqueda + Validación |
| **Modificado** | `src/main/java/.../controller/MainEspecialidadController.java` | 54–95 | Búsqueda + Validación |
| **Modificado** | `src/main/java/.../controller/MainTicketController.java` | TODO | **NUEVA LÓGICA DUAL CITA/RECETA** |
| **Modificado** | `src/main/java/.../config/AppContext.java` | 121–123 | Inyecta 4 servicios en MainTicketController |
| **Modificado** | `src/main/resources/view/main_ticket.fxml` | TODO | **DOS TABLAS LADO A LADO** |
| **Creado** | `src/main/resources/css/*.css` | 5 archivos | Temas de color (azul, oscuro, rosado, verde) |

---

## ✅ Resumen de funcionalidades implementadas

### ✓ Fase 1 — Infraestructura
- [x] BD H2 en modo servidor TCP (puerto 9040)
- [x] Conexión desde DBeaver

### ✓ Fase 2 — CRUDs mejorados (Paciente, Médico, Enfermero, Especialidad)
- [x] Búsqueda en vivo (filtro) en todas las tablas
- [x] Validación de campos con Jakarta (DNI, nombres, teléfono, etc.)
- [x] Marcado visual en rojo + tooltips de error

### ✓ Fase 3 — UX/Diseño
- [x] Temas CSS dinámicos (azul, oscuro, rosado, verde)
- [x] Selector en menú "Cambiar Estilo"
- [x] Persistencia de tema en Preferences

### ✓ Fase 4 — Flujo clínico (Citas/Recetas)
- [x] Autocompletado DNI al registrar cita
- [x] **Ver e Imprimir con dos tablas (NUEVO)**
  - [x] Tabla de Citas (Ticket, Paciente, Fecha, Hora, Estado)
  - [x] Tabla de Recetas (N°, Paciente, Diagnóstico, Fecha)
  - [x] Selección mutuamente excluyente (seleccionar en A limpia B)
  - [x] Imprimir en ticketera ESC/POS (Cita o Receta)
  - [x] Visor Jasper para ambas
  - [x] Exportar a PDF (Cita o Receta)

---

## 🎯 Cómo se usa cada funcionalidad

### 1️⃣ Búsqueda en vivo
**Donde:** Paciente, Médico, Enfermero, Especialidad
**Cómo:** Escribe en el campo "Buscar" → la tabla se filtra automáticamente

### 2️⃣ Validación de campos
**Donde:** Al guardar en cualquier CRUD
**Cómo:** Si hay error (DNI < 8 dígitos, etc.) → se marca en rojo con mensaje de error

### 3️⃣ Cambiar tema
**Donde:** Menú superior "Cambiar Estilo"
**Cómo:** Selecciona un tema → se aplica inmediatamente y se guarda para próximos arranques

### 4️⃣ Autocompletado DNI en Cita
**Donde:** Registro de Cita
**Cómo:** Escribe DNI → aparecen pacientes coincidentes → elige uno → se rellenan nombres, apellidos, teléfono

### 5️⃣ Ver e Imprimir Tickets
**Donde:** Menú "Atención → Ver/Imprimir Ticket"
**Cómo:**
1. Ves dos tablas (Citas a la izquierda, Recetas a la derecha)
2. Haz clic en una fila de cualquiera
3. Usa los botones:
   - **Imprimir (ESC/POS):** envía a la ticketera
   - **Visor Jasper:** previsualiza
   - **Exportar PDF:** guarda como PDF

---

**Documento generado:** 2026-06-22  
**Versión:** Clínica 2.0.1 — Fase 4 completada  
**Estado:** ✅ LISTO PARA ENTREGA
