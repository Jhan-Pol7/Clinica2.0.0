package pe.edu.upeu.clinica.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;
import pe.edu.upeu.clinica.components.JasperViewerFX;
import pe.edu.upeu.clinica.components.Toast;
import pe.edu.upeu.clinica.model.Cita;
import pe.edu.upeu.clinica.model.Consulta;
import pe.edu.upeu.clinica.model.Receta;
import pe.edu.upeu.clinica.model.RecetaDetalle;
import pe.edu.upeu.clinica.model.Ticket;
import pe.edu.upeu.clinica.dto.SessionManager;
import pe.edu.upeu.clinica.service.ICitaService;
import pe.edu.upeu.clinica.service.IConsultaService;
import pe.edu.upeu.clinica.service.IRecetaService;
import pe.edu.upeu.clinica.service.IReporteService;
import pe.edu.upeu.clinica.service.ITicketService;
import pe.edu.upeu.clinica.utils.TicketPrinter;
import pe.edu.upeu.clinica.utils.RecetaPrinter;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Lista todas las citas (30 días) y recetas (30 días) en dos tablas.
 * El usuario selecciona una fila de cualquiera de las dos tablas y usa
 * los botones compartidos para imprimir, previsualizar o exportar PDF.
 */
public class MainTicketController {

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_DT    = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ITicketService   ticketService;
    private final IRecetaService   recetaService;
    private final ICitaService     citaService;
    private final IConsultaService consultaService;
    private final IReporteService  reporteService;

    // ── FXML — Citas ──────────────────────────────────────────
    @FXML private TableView<Cita>   tablaCitas;
    @FXML private TableColumn<Cita, String> colCitaTicket;
    @FXML private TableColumn<Cita, String> colCitaPaciente;
    @FXML private TableColumn<Cita, String> colCitaFecha;
    @FXML private TableColumn<Cita, String> colCitaHora;
    @FXML private TableColumn<Cita, String> colCitaEstado;

    // ── FXML — Recetas ────────────────────────────────────────
    @FXML private TableView<Receta>   tablaRecetas;
    @FXML private TableColumn<Receta, String> colRecetaId;
    @FXML private TableColumn<Receta, String> colRecetaPaciente;
    @FXML private TableColumn<Receta, String> colRecetaDx;
    @FXML private TableColumn<Receta, String> colRecetaFecha;

    @FXML private Label lblSeleccion;

    private final ObservableList<Cita>   citas   = FXCollections.observableArrayList();
    private final ObservableList<Receta> recetas = FXCollections.observableArrayList();

    public MainTicketController(ITicketService ticketService, IRecetaService recetaService,
                                ICitaService citaService, IConsultaService consultaService,
                                IReporteService reporteService) {
        this.ticketService   = ticketService;
        this.recetaService   = recetaService;
        this.citaService     = citaService;
        this.consultaService = consultaService;
        this.reporteService  = reporteService;
    }

    @FXML
    public void initialize() {
        configurarTablaCitas();
        configurarTablaRecetas();
        cargarDatos();
    }

    // ── Configuración de columnas ─────────────────────────────

    private void configurarTablaCitas() {
        colCitaTicket.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getNumTicket()));
        colCitaPaciente.setCellValueFactory(c -> {
            Cita ci = c.getValue();
            String nom = ci.getPaciente() == null ? "—"
                    : ci.getPaciente().getNombres() + " " + ci.getPaciente().getApellidos();
            return new javafx.beans.property.SimpleStringProperty(nom);
        });
        colCitaFecha.setCellValueFactory(c -> {
            LocalDate f = c.getValue().getFecha();
            return new javafx.beans.property.SimpleStringProperty(f == null ? "—" : f.format(FMT_FECHA));
        });
        colCitaHora.setCellValueFactory(c -> {
            var h = c.getValue().getHora();
            return new javafx.beans.property.SimpleStringProperty(h == null ? "—" : h.toString());
        });
        colCitaEstado.setCellValueFactory(c -> {
            var e = c.getValue().getEstado();
            return new javafx.beans.property.SimpleStringProperty(e == null ? "—" : e.name());
        });
        tablaCitas.setItems(citas);

        // Al seleccionar una cita, limpiar selección de recetas
        tablaCitas.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                tablaRecetas.getSelectionModel().clearSelection();
                lblSeleccion.setText("Cita seleccionada: " + sel.getNumTicket()
                        + " — " + (sel.getPaciente() == null ? "" : sel.getPaciente().getNombres()
                        + " " + sel.getPaciente().getApellidos()));
            }
        });
    }

    private void configurarTablaRecetas() {
        colRecetaId.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getIdReceta() == null ? "—" : "#" + c.getValue().getIdReceta()));
        colRecetaPaciente.setCellValueFactory(c -> {
            Receta r = c.getValue();
            String nom = "—";
            if (r.getConsulta() != null && r.getConsulta().getCita() != null
                    && r.getConsulta().getCita().getPaciente() != null) {
                var p = r.getConsulta().getCita().getPaciente();
                nom = p.getNombres() + " " + p.getApellidos();
            }
            return new javafx.beans.property.SimpleStringProperty(nom);
        });
        colRecetaDx.setCellValueFactory(c -> {
            Receta r = c.getValue();
            String dx = "—";
            if (r.getConsulta() != null && r.getConsulta().getDiagnostico() != null) {
                dx = r.getConsulta().getDiagnostico();
            }
            return new javafx.beans.property.SimpleStringProperty(dx);
        });
        colRecetaFecha.setCellValueFactory(c -> {
            var f = c.getValue().getFechaReg();
            return new javafx.beans.property.SimpleStringProperty(f == null ? "—" : f.format(FMT_DT));
        });
        tablaRecetas.setItems(recetas);

        // Al seleccionar una receta, limpiar selección de citas
        tablaRecetas.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                tablaCitas.getSelectionModel().clearSelection();
                String id = sel.getIdReceta() == null ? "—" : "#" + sel.getIdReceta();
                lblSeleccion.setText("Receta seleccionada: " + id
                        + (sel.getConsulta() != null && sel.getConsulta().getDiagnostico() != null
                        ? " — Dx: " + sel.getConsulta().getDiagnostico() : ""));
            }
        });
    }

    // ── Carga de datos ────────────────────────────────────────

    @FXML
    public void onRecargar() { cargarDatos(); }

    private void cargarDatos() {
        // Citas del día (ya vienen con paciente/médico hidratados por findByFecha).
        List<Cita> todasCitas = citaService.findByFecha(LocalDate.now());
        citas.setAll(todasCitas);

        // Recetas: el findAll() solo trae el idConsulta. Enriquecemos cada receta
        // con su consulta completa (idCita + diagnóstico) y la cita completa
        // (con paciente) para que las columnas y el reporte Jasper funcionen.
        List<Receta> todasRecetas = recetaService.findAll();
        for (Receta r : todasRecetas) {
            enriquecerReceta(r);
        }
        recetas.setAll(todasRecetas);

        lblSeleccion.setText("Selecciona una fila de la tabla Citas o Recetas y usa los botones de abajo.");
    }

    // Hidrata la receta: carga su consulta (con diagnóstico + idCita) y la cita
    // completa (con paciente). Tolerante a datos huérfanos: si algo no existe,
    // deja la receta con lo que tenga sin lanzar excepción.
    private void enriquecerReceta(Receta r) {
        try {
            if (r.getConsulta() == null || r.getConsulta().getIdConsulta() == null) return;
            Consulta consulta = consultaService.findById(r.getConsulta().getIdConsulta());
            if (consulta != null && consulta.getCita() != null
                    && consulta.getCita().getIdCita() != null) {
                Cita citaFull = citaService.findById(consulta.getCita().getIdCita());
                consulta.setCita(citaFull);
            }
            r.setConsulta(consulta);
        } catch (Exception ignore) {
            // Receta huérfana (sin consulta/cita válida): se muestra con "—".
        }
    }

    // ── Acciones ──────────────────────────────────────────────

    @FXML
    public void onImprimir() {
        Cita cita = tablaCitas.getSelectionModel().getSelectedItem();
        Receta receta = tablaRecetas.getSelectionModel().getSelectedItem();

        if (cita != null) {
            imprimirCita(cita);
        } else if (receta != null) {
            imprimirReceta(receta);
        } else {
            mostrarError("Selecciona una Cita o una Receta para imprimir");
        }
    }

    private void imprimirCita(Cita cita) {
        try {
            Ticket t = ticketService.buildTicket(cita);
            new TicketPrinter().imprimir(t);
            mostrarExito("Ticket de cita enviado a la impresora térmica");
        } catch (IOException ex) {
            mostrarError("Sin impresora ESC/POS — usa 'Visor Jasper' o 'PDF' como alternativa");
        }
    }

    private void imprimirReceta(Receta receta) {
        // RESTRICCIÓN DE ROL: la impresión térmica (ESC/POS) de recetas está
        // permitida al personal médico y a los administradores (Admin/Root).
        // Otros perfiles (Recepción, Enfermero) solo pueden ver/exportar vía Jasper/PDF.
        if (!puedeImprimirReceta()) {
            mostrarError("Solo médicos y administradores pueden imprimir recetas en ticketera. Usa 'Visor Jasper' o 'PDF'.");
            return;
        }
        if (receta.getConsulta() == null) { mostrarError("Receta sin consulta asociada"); return; }
        try {
            // RecetaPrinter recorre los detalles: los cargamos antes de imprimir.
            List<RecetaDetalle> detalles = recetaService.findDetalles(receta.getIdReceta());
            receta.setDetalles(detalles);
            new RecetaPrinter().imprimir(receta);
            mostrarExito("Receta enviada a la impresora térmica");
        } catch (IOException ex) {
            mostrarError("Sin impresora ESC/POS — usa 'Visor Jasper' o 'PDF' como alternativa");
        }
    }

    // True si el usuario puede imprimir recetas en ticketera: médicos y
    // administradores (Admin/Root). El resto de perfiles queda excluido.
    private boolean puedeImprimirReceta() {
        String perfil = SessionManager.getInstance().getUserPerfil();
        if (perfil == null) return false;
        return perfil.equalsIgnoreCase("Medico")
                || perfil.equalsIgnoreCase("Administrador")
                || perfil.equalsIgnoreCase("Root");
    }

    @FXML
    public void onVisorJasper() {
        Cita cita = tablaCitas.getSelectionModel().getSelectedItem();
        Receta receta = tablaRecetas.getSelectionModel().getSelectedItem();

        try {
            if (cita != null) {
                JasperPrint jp = reporteService.generarTicket(cita);
                new JasperViewerFX().viewReport("Ticket " + cita.getNumTicket(), jp);
            } else if (receta != null && receta.getConsulta() != null) {
                JasperPrint jp = reporteService.generarReceta(receta.getConsulta());
                new JasperViewerFX().viewReport("Receta #" + receta.getIdReceta(), jp);
            } else {
                mostrarError("Selecciona una Cita o una Receta");
            }
        } catch (Exception ex) {
            mostrarError("Error en Jasper: " + ex.getMessage());
        }
    }

    @FXML
    public void onExportarPdf() {
        Cita cita = tablaCitas.getSelectionModel().getSelectedItem();
        Receta receta = tablaRecetas.getSelectionModel().getSelectedItem();

        try {
            if (cita != null) {
                exportarPdfCita(cita);
            } else if (receta != null && receta.getConsulta() != null) {
                exportarPdfReceta(receta);
            } else {
                mostrarError("Selecciona una Cita o una Receta para exportar");
            }
        } catch (Exception ex) {
            mostrarError("Error PDF: " + ex.getMessage());
        }
    }

    private void exportarPdfCita(Cita cita) throws Exception {
        JasperPrint jp = reporteService.generarTicket(cita);
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar ticket en PDF");
        fc.setInitialFileName(cita.getNumTicket() + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File f = fc.showSaveDialog(stage());
        if (f == null) return;
        JasperExportManager.exportReportToPdfFile(jp, f.getAbsolutePath());
        mostrarExito("PDF guardado: " + f.getName());
    }

    private void exportarPdfReceta(Receta receta) throws Exception {
        JasperPrint jp = reporteService.generarReceta(receta.getConsulta());
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar receta en PDF");
        fc.setInitialFileName("receta_" + receta.getIdReceta() + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File f = fc.showSaveDialog(stage());
        if (f == null) return;
        JasperExportManager.exportReportToPdfFile(jp, f.getAbsolutePath());
        mostrarExito("PDF guardado: " + f.getName());
    }

    // ── Helpers ───────────────────────────────────────────────

    private Stage stage() {
        if (tablaCitas != null && tablaCitas.getScene() != null)
            return (Stage) tablaCitas.getScene().getWindow();
        return null;
    }

    private void mostrarError(String m) {
        Stage s = stage();
        if (s != null) Toast.showToast(s, m, 3000, s.getX() + 80, s.getY() + 80);
    }

    private void mostrarExito(String m) {
        Stage s = stage();
        if (s != null) Toast.showSuccess(s, m, 2200, s.getX() + 80, s.getY() + 80);
    }
}
