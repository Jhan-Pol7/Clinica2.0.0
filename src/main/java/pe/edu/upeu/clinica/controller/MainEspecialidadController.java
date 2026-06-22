package pe.edu.upeu.clinica.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import pe.edu.upeu.clinica.components.ColumnInfo;
import pe.edu.upeu.clinica.components.FormValidator;
import pe.edu.upeu.clinica.components.TableSearchFilter;
import pe.edu.upeu.clinica.components.TableViewHelper;
import pe.edu.upeu.clinica.components.Toast;
import pe.edu.upeu.clinica.model.Especialidad;
import pe.edu.upeu.clinica.service.IEspecialidadService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// Controller del CRUD de Especialidad. Patrón estándar de todos los CRUD del
// proyecto: campo "seleccionada" para distinguir alta de edición, lista
// ObservableList enlazada al TableView, validación Jakarta antes de persistir.
public class MainEspecialidadController {

    // Servicio + Jakarta Validator (singleton, reutiliza el motor).
    private final IEspecialidadService service;
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    // null = modo "Nuevo"; != null = modo "Editando".
    private Especialidad seleccionada;

    @FXML private TextField txtNombre;
    @FXML private TextArea  txtDescripcion;
    @FXML private TableView<Especialidad> tabla;
    @FXML private TextField txtBuscar;      // búsqueda en vivo sobre la tabla
    private TableSearchFilter<Especialidad> filtro;

    public MainEspecialidadController(IEspecialidadService service) {
        this.service = service;
    }

    @FXML
    public void initialize() {
        configurarTabla();
        // Búsqueda en vivo: filtra por nombre o descripción.
        filtro = new TableSearchFilter<>(txtBuscar, tabla, esp ->
                String.join(" ",
                        esp.getNombre() == null ? "" : esp.getNombre(),
                        esp.getDescripcion() == null ? "" : esp.getDescripcion()));
        cargar();
    }

    // Define columnas + acciones (Editar/Eliminar) usando el helper genérico.
    private void configurarTabla() {
        TableViewHelper<Especialidad> helper = new TableViewHelper<>();
        LinkedHashMap<String, ColumnInfo> cols = new LinkedHashMap<>();
        cols.put("ID",          new ColumnInfo("idEspecialidad", 60.0));
        cols.put("Nombre",      new ColumnInfo("nombre",        220.0));
        cols.put("Descripción", new ColumnInfo("descripcion",   400.0));
        // Los method-refs editar/confirmarEliminar son los handlers de cada botón de fila.
        helper.addColumnsInOrderWithSize(tabla, cols, this::editar, this::confirmarEliminar);
    }

    // Lee del servicio y refresca la tabla (respetando el filtro de búsqueda actual).
    private void cargar() {
        filtro.setData(service.findAll());
    }

    // Botón "Nuevo" — resetea el modo y limpia el formulario.
    @FXML
    public void onNuevo()    { seleccionada = null; onLimpiar(); }
    // Botón "Limpiar".
    @FXML
    public void onLimpiar()  { txtNombre.clear(); txtDescripcion.clear(); }

    // Botón "Guardar". Valida con Jakarta, decide alta vs edición, persiste.
    @FXML
    public void onGuardar() {
        Especialidad e = Especialidad.builder()
                .nombre(empty(txtNombre.getText()))
                .descripcion(empty(txtDescripcion.getText()))
                .build();
        // Validación Jakarta + marcado visual del campo con error (borde rojo + tooltip).
        Map<String, Control> campos = new LinkedHashMap<>();
        campos.put("nombre", txtNombre);
        Set<ConstraintViolation<Especialidad>> v = FormValidator.validar(validator, e, campos);
        if (!v.isEmpty()) {
            mostrarError(v.stream().map(x -> "• " + x.getMessage()).collect(Collectors.joining("\n")));
            return;
        }
        try {
            if (seleccionada == null) {
                // Alta nueva.
                service.save(e);
                mostrarExito("Especialidad creada");
            } else {
                // Edición — copiar id de la entidad seleccionada para que UPDATE encuentre la fila.
                e.setIdEspecialidad(seleccionada.getIdEspecialidad());
                service.update(seleccionada.getIdEspecialidad(), e);
                mostrarExito("Especialidad actualizada");
            }
            cargar();
            onLimpiar();
            seleccionada = null;
        } catch (Exception ex) {
            mostrarError("Error: " + ex.getMessage());
        }
    }

    // Botón "Modificar" (de la barra) — equivalente a hacer click en el botón verde de una fila.
    @FXML
    public void onModificar() {
        Especialidad sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { mostrarError("Selecciona una fila"); return; }
        editar(sel);
    }

    // Botón "Eliminar" (de la barra) — pide selección y confirma.
    @FXML
    public void onEliminar() {
        Especialidad sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { mostrarError("Selecciona una fila"); return; }
        confirmarEliminar(sel);
    }

    // Pone la fila en modo edición y rellena el formulario.
    // Lo usan tanto onModificar() como el botón verde por fila del TableViewHelper.
    private void editar(Especialidad e) {
        seleccionada = e;
        txtNombre.setText(e.getNombre());
        txtDescripcion.setText(e.getDescripcion() == null ? "" : e.getDescripcion());
    }

    // Diálogo de confirmación antes de borrar. Si confirma, llama al service.
    // Si la BD tiene FKs huérfanas (ej. médico con esta especialidad), captura la excepción.
    private void confirmarEliminar(Especialidad e) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar la especialidad \"" + e.getNombre() + "\"?",
                ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText("Confirmar eliminación");
        a.showAndWait().filter(b -> b == ButtonType.OK).ifPresent(b -> {
            try {
                service.delete(e.getIdEspecialidad());
                cargar();
                mostrarExito("Especialidad eliminada");
            } catch (Exception ex) {
                mostrarError("No se pudo eliminar: " + ex.getMessage());
            }
        });
    }

    // --- Helpers de UI (Toast rojo/verde) ---
    private void mostrarError(String msg) {
        Stage st = stage();
        if (st != null) Toast.showToast(st, msg, 2800, st.getX() + 80, st.getY() + 80);
    }
    private void mostrarExito(String msg) {
        Stage st = stage();
        if (st != null) Toast.showSuccess(st, msg, 2000, st.getX() + 80, st.getY() + 80);
    }
    // Obtiene el Stage actual desde un nodo cualquiera del FXML.
    private Stage stage() {
        if (tabla == null || tabla.getScene() == null) return null;
        return (Stage) tabla.getScene().getWindow();
    }
    // Normaliza strings (null -> "", trim).
    private String empty(String s) { return s == null ? "" : s.trim(); }
}
