package pe.edu.upeu.clinica.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import pe.edu.upeu.clinica.components.ColumnInfo;
import pe.edu.upeu.clinica.components.TableViewHelper;
import pe.edu.upeu.clinica.components.Toast;
import pe.edu.upeu.clinica.dto.SessionManager;
import pe.edu.upeu.clinica.enums.EstadoCita;
import pe.edu.upeu.clinica.model.Cita;
import pe.edu.upeu.clinica.model.Consulta;
import pe.edu.upeu.clinica.model.Receta;
import pe.edu.upeu.clinica.model.RecetaDetalle;
import pe.edu.upeu.clinica.model.Triaje;
import pe.edu.upeu.clinica.service.ICitaService;
import pe.edu.upeu.clinica.service.IConsultaService;
import pe.edu.upeu.clinica.service.ITriajeService;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

/**
 * Consultorio (médico): citas EN_CONSULTA del médico logueado.
 * - Selección de fila muestra paciente + signos vitales del triaje (read-only).
 * - Formulario consulta: síntomas, diagnóstico, observaciones, exámenes.
 * - Tabla de RecetaDetalle (medicamento, dosis, frecuencia, duración, vía) con +/− dinámicos.
 * - Botón "Finalizar Consulta" → cierra todo y deja la cita ATENDIDA.
 */
public class MainConsultaController {

    private final IConsultaService consultaService;
    private final ITriajeService   triajeService;
    private final ICitaService     citaService;

    private final ObservableList<Cita> citasData = FXCollections.observableArrayList();
    private final ObservableList<RecetaDetalle> detallesData = FXCollections.observableArrayList();
    private Cita citaSeleccionada;

    @FXML private TableView<Cita> tabla;
    @FXML private TableView<RecetaDetalle> tablaDetalles;
    @FXML private Label lblPaciente, lblSignosVitales;
    @FXML private TextArea txtSintomas, txtDiagnostico, txtObservaciones, txtExamenes,
                            txtIndicaciones, txtRecomendaciones;
    @FXML private TextField txtMed, txtDosis, txtFrec, txtDur, txtVia;
    @FXML private Button btnAgregar, btnQuitar, btnFinalizar;   // se habilitan al seleccionar una cita

    public MainConsultaController(IConsultaService consultaService,
                                  ITriajeService triajeService,
                                  ICitaService citaService) {
        this.consultaService = consultaService;
        this.triajeService = triajeService;
        this.citaService = citaService;
    }

    @FXML
    public void initialize() {
        TableViewHelper<Cita> h = new TableViewHelper<>();
        LinkedHashMap<String, ColumnInfo> cols = new LinkedHashMap<>();
        cols.put("Ticket",       new ColumnInfo("numTicket",           120.0));
        cols.put("Paciente",     new ColumnInfo("paciente.nombres",    140.0));
        cols.put("DNI",          new ColumnInfo("paciente.dni",        100.0));
        cols.put("Especialidad", new ColumnInfo("especialidad.nombre", 130.0));
        cols.put("Hora",         new ColumnInfo("hora",                 80.0));
        h.addColumnsInOrderWithSize(tabla, cols, this::seleccionar, c -> { /* no eliminar */ });

        // Tabla detalles receta (sin columna acciones)
        TableColumn<RecetaDetalle, String> cMed   = new TableColumn<>("Medicamento");
        TableColumn<RecetaDetalle, String> cDosis = new TableColumn<>("Dosis");
        TableColumn<RecetaDetalle, String> cFrec  = new TableColumn<>("Frecuencia");
        TableColumn<RecetaDetalle, String> cDur   = new TableColumn<>("Duración");
        TableColumn<RecetaDetalle, String> cVia   = new TableColumn<>("Vía");
        cMed.setCellValueFactory(new PropertyValueFactory<>("medicamento")); cMed.setPrefWidth(180);
        cDosis.setCellValueFactory(new PropertyValueFactory<>("dosis"));     cDosis.setPrefWidth(80);
        cFrec.setCellValueFactory(new PropertyValueFactory<>("frecuencia")); cFrec.setPrefWidth(110);
        cDur.setCellValueFactory(new PropertyValueFactory<>("duracion"));    cDur.setPrefWidth(90);
        cVia.setCellValueFactory(new PropertyValueFactory<>("via"));         cVia.setPrefWidth(80);
        tablaDetalles.getColumns().setAll(cMed, cDosis, cFrec, cDur, cVia);
        tablaDetalles.setItems(detallesData);

        recargar();
    }

    @FXML
    public void onRecargar() { recargar(); }

    private void recargar() {
        Long idMedico = SessionManager.getInstance().getIdReferencia();
        if (idMedico == null) {
            // Root/Admin: mostrar todas las citas EN_CONSULTA
            citasData.setAll(citaService.findByEstado(EstadoCita.EN_CONSULTA));
        } else {
            citasData.setAll(citaService.findByMedicoYEstado(idMedico, EstadoCita.EN_CONSULTA));
        }
        tabla.setItems(citasData);
        limpiarFormulario();
        citaSeleccionada = null;
        // Formulario bloqueado hasta que el médico pulse el botón verde (✎) de una cita.
        setFormularioHabilitado(false);
        if (lblPaciente != null)      lblPaciente.setText("Selecciona una cita con el botón verde ✎ →");
        if (lblSignosVitales != null) lblSignosVitales.setText("");
    }

    // Habilita/deshabilita todos los campos y botones del formulario de consulta.
    // Solo se permite escribir tras seleccionar una cita (botón verde de la tabla).
    private void setFormularioHabilitado(boolean activo) {
        Control[] campos = { txtSintomas, txtDiagnostico, txtObservaciones, txtExamenes,
                txtIndicaciones, txtRecomendaciones, txtMed, txtDosis, txtFrec, txtDur, txtVia,
                tablaDetalles, btnAgregar, btnQuitar, btnFinalizar };
        for (Control c : campos) if (c != null) c.setDisable(!activo);
    }

    private void seleccionar(Cita c) {
        citaSeleccionada = c;
        // Al elegir la cita con el botón verde se habilita el formulario para escribir.
        setFormularioHabilitado(true);
        if (lblPaciente != null) {
            lblPaciente.setText("Paciente: " + c.getPaciente().getNombres() + " " + c.getPaciente().getApellidos()
                    + " (DNI " + c.getPaciente().getDni() + ")  —  Ticket: " + c.getNumTicket());
        }
        // Cargar signos vitales del triaje
        triajeService.findByCita(c.getIdCita()).ifPresentOrElse(t -> {
            if (lblSignosVitales != null) lblSignosVitales.setText(formatoTriaje(t));
        }, () -> {
            if (lblSignosVitales != null) lblSignosVitales.setText("(Sin triaje registrado)");
        });
        detallesData.clear();
    }

    private String formatoTriaje(Triaje t) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM HH:mm");
        return String.format("Triaje %s — Presión: %s/%s | Temp: %s°C | FC: %s | Peso: %s kg | Talla: %s m | Motivo: %s",
                t.getFechaReg() == null ? "—" : t.getFechaReg().format(f),
                t.getPresionSistolica() == null ? "—" : t.getPresionSistolica(),
                t.getPresionDiastolica() == null ? "—" : t.getPresionDiastolica(),
                t.getTemperatura()     == null ? "—" : t.getTemperatura(),
                t.getFrecCardiaca()    == null ? "—" : t.getFrecCardiaca(),
                t.getPeso()            == null ? "—" : t.getPeso(),
                t.getTalla()           == null ? "—" : t.getTalla(),
                t.getMotivoConsulta()  == null ? "—" : t.getMotivoConsulta());
    }

    // ── Receta dinámica ──────────────────────────────────────
    @FXML
    public void onAgregarMedicamento() {
        String med = empty(txtMed.getText());
        if (med.isEmpty()) { mostrarError("Ingresa el medicamento"); return; }
        detallesData.add(RecetaDetalle.builder()
                .medicamento(med)
                .dosis(empty(txtDosis.getText()))
                .frecuencia(empty(txtFrec.getText()))
                .duracion(empty(txtDur.getText()))
                .via(empty(txtVia.getText()))
                .build());
        txtMed.clear(); txtDosis.clear(); txtFrec.clear(); txtDur.clear(); txtVia.clear();
    }

    @FXML
    public void onQuitarMedicamento() {
        RecetaDetalle sel = tablaDetalles.getSelectionModel().getSelectedItem();
        if (sel != null) detallesData.remove(sel);
    }

    @FXML
    public void onFinalizarConsulta() {
        if (citaSeleccionada == null) { mostrarError("Selecciona una cita"); return; }
        if (empty(txtDiagnostico.getText()).isEmpty()) { mostrarError("El diagnóstico es obligatorio"); return; }

        Consulta c = Consulta.builder()
                .cita(citaSeleccionada)
                .sintomas(empty(txtSintomas.getText()))
                .diagnostico(empty(txtDiagnostico.getText()))
                .observaciones(empty(txtObservaciones.getText()))
                .examenesSolicitados(empty(txtExamenes.getText()))
                .build();
        Receta r = null;
        if (!detallesData.isEmpty() || !empty(txtIndicaciones.getText()).isEmpty()
                || !empty(txtRecomendaciones.getText()).isEmpty()) {
            r = Receta.builder()
                    .indicacionesGenerales(empty(txtIndicaciones.getText()))
                    .recomendaciones(empty(txtRecomendaciones.getText()))
                    .detalles(new java.util.ArrayList<>(detallesData))
                    .build();
        }
        try {
            Consulta saved = consultaService.guardarConsulta(c, r);
            SessionManager.getInstance().setLastConsulta(saved);
            mostrarExito("Consulta finalizada — cita " + citaSeleccionada.getNumTicket() + " marcada ATENDIDA");
            // Mostrar la receta recién generada para que el médico la vea sin salir de la pantalla.
            mostrarRecetaPopup(saved, c.getDiagnostico(), r);
            recargar();
        } catch (Exception ex) { mostrarError(ex.getMessage()); }
    }

    // Muestra en un cuadro de diálogo el resumen de la receta recién emitida
    // (diagnóstico, medicamentos, indicaciones). Si no hubo receta, lo informa.
    private void mostrarRecetaPopup(Consulta consulta, String diagnostico, Receta receta) {
        StringBuilder sb = new StringBuilder();
        sb.append("Diagnóstico: ").append(diagnostico == null || diagnostico.isEmpty() ? "—" : diagnostico).append("\n\n");
        if (receta == null) {
            sb.append("La consulta se cerró SIN receta (no se prescribió medicación).");
        } else {
            sb.append("MEDICAMENTOS:\n");
            if (detallesData.isEmpty()) {
                sb.append("  (sin medicamentos)\n");
            } else {
                for (RecetaDetalle d : detallesData) {
                    sb.append("  • ").append(d.getMedicamento())
                            .append("  ").append(d.getDosis())
                            .append(" | ").append(d.getFrecuencia())
                            .append(" | ").append(d.getDuracion())
                            .append(" | ").append(d.getVia()).append("\n");
                }
            }
            if (receta.getIndicacionesGenerales() != null && !receta.getIndicacionesGenerales().isEmpty())
                sb.append("\nIndicaciones: ").append(receta.getIndicacionesGenerales());
            if (receta.getRecomendaciones() != null && !receta.getRecomendaciones().isEmpty())
                sb.append("\nRecomendaciones: ").append(receta.getRecomendaciones());
        }
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Receta médica");
        a.setHeaderText("Receta de la cita " + consulta.getCita().getNumTicket());
        a.setContentText(sb.toString());
        a.getDialogPane().setMinWidth(480);
        a.showAndWait();
    }

    @FXML
    public void onLimpiar() { limpiarFormulario(); }

    private void limpiarFormulario() {
        if (txtSintomas != null) txtSintomas.clear();
        if (txtDiagnostico != null) txtDiagnostico.clear();
        if (txtObservaciones != null) txtObservaciones.clear();
        if (txtExamenes != null) txtExamenes.clear();
        if (txtIndicaciones != null) txtIndicaciones.clear();
        if (txtRecomendaciones != null) txtRecomendaciones.clear();
        if (txtMed != null) txtMed.clear();
        if (txtDosis != null) txtDosis.clear();
        if (txtFrec != null) txtFrec.clear();
        if (txtDur != null) txtDur.clear();
        if (txtVia != null) txtVia.clear();
        detallesData.clear();
    }

    private Stage stage() { return tabla == null || tabla.getScene() == null ? null : (Stage) tabla.getScene().getWindow(); }
    private void mostrarError(String m) { Stage s = stage(); if (s != null) Toast.showToast(s, m, 3000, s.getX() + 80, s.getY() + 80); }
    private void mostrarExito(String m) { Stage s = stage(); if (s != null) Toast.showSuccess(s, m, 2400, s.getX() + 80, s.getY() + 80); }
    private String empty(String s) { return s == null ? "" : s.trim(); }
}
