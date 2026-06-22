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
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;
import pe.edu.upeu.clinica.components.ColumnInfo;
import pe.edu.upeu.clinica.components.FormValidator;
import pe.edu.upeu.clinica.components.TableSearchFilter;
import pe.edu.upeu.clinica.components.TableViewHelper;
import pe.edu.upeu.clinica.components.Toast;
import pe.edu.upeu.clinica.model.Enfermero;
import pe.edu.upeu.clinica.service.IEnfermeroService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// Controller del CRUD de Enfermero. Sigue el mismo patrón que MainEspecialidadController:
// formulario + tabla + acciones Nuevo/Guardar/Modificar/Eliminar/Limpiar
// con validaciones Jakarta y Toast como feedback.
public class MainEnfermeroController {

    private final IEnfermeroService service;
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private Enfermero seleccionado;       // null = modo Nuevo; != null = modo Editar

    // Campos FXML — todos los TextField se declaran en una línea para compacidad.
    @FXML private TextField txtDni, txtNombres, txtApellidos, txtTelefono;
    @FXML private TableView<Enfermero> tabla;
    @FXML private TextField txtBuscar;      // búsqueda en vivo sobre la tabla
    private TableSearchFilter<Enfermero> filtro;

    public MainEnfermeroController(IEnfermeroService service) { this.service = service; }

    @FXML
    public void initialize() {
        // Filtro de entrada: DNI solo acepta hasta 8 dígitos numéricos.
        txtDni.setTextFormatter(new TextFormatter<>(cambio -> {
            String nuevoTexto = cambio.getControlNewText();
            return nuevoTexto.matches("\\d{0,8}") ? cambio : null;
        }));
        // Teléfono solo acepta hasta 9 dígitos numéricos.
        txtTelefono.setTextFormatter(new TextFormatter<>(cambio -> {
            String nuevoTexto = cambio.getControlNewText();
            return nuevoTexto.matches("\\d{0,9}") ? cambio : null;
        }));

        // Configura las columnas de la tabla con sus tamaños y acciones por fila.
        TableViewHelper<Enfermero> h = new TableViewHelper<>();
        LinkedHashMap<String, ColumnInfo> cols = new LinkedHashMap<>();
        cols.put("DNI",       new ColumnInfo("dni",        100.0));
        cols.put("Nombres",   new ColumnInfo("nombres",    180.0));
        cols.put("Apellidos", new ColumnInfo("apellidos",  180.0));
        cols.put("Teléfono",  new ColumnInfo("telefono",   120.0));
        h.addColumnsInOrderWithSize(tabla, cols, this::editar, this::confirmarEliminar);

        // Columna "N°" con numeración secuencial (1, 2, 3...) al inicio de la tabla.
        javafx.scene.control.TableColumn<Enfermero, Void> colNum = new javafx.scene.control.TableColumn<>("N°");
        colNum.setPrefWidth(50.0);
        colNum.setSortable(false);
        colNum.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : String.valueOf(getIndex() + 1));
            }
        });
        tabla.getColumns().add(0, colNum);

        // Búsqueda en vivo: filtra por DNI, nombres, apellidos o teléfono.
        filtro = new TableSearchFilter<>(txtBuscar, tabla, en ->
                String.join(" ",
                        en.getDni(), en.getNombres(), en.getApellidos(),
                        en.getTelefono() == null ? "" : en.getTelefono()));
        cargar();
    }

    private void cargar() { filtro.setData(service.findAll()); }

    @FXML public void onNuevo()   { seleccionado = null; onLimpiar(); }
    @FXML public void onLimpiar() { txtDni.clear(); txtNombres.clear(); txtApellidos.clear(); txtTelefono.clear(); }

    // Guarda (alta o edición). Valida con Jakarta; si falla -> Toast rojo y abortar.
    @FXML
    public void onGuardar() {
        Enfermero e = Enfermero.builder()
                .dni(empty(txtDni.getText()))
                .nombres(empty(txtNombres.getText()))
                .apellidos(empty(txtApellidos.getText()))
                .telefono(empty(txtTelefono.getText()))
                .build();
        // Validación Jakarta + marcado visual de cada campo con error (borde rojo + tooltip).
        Map<String, Control> campos = new LinkedHashMap<>();
        campos.put("dni", txtDni);
        campos.put("nombres", txtNombres);
        campos.put("apellidos", txtApellidos);
        campos.put("telefono", txtTelefono);
        Set<ConstraintViolation<Enfermero>> v = FormValidator.validar(validator, e, campos);
        if (!v.isEmpty()) {
            mostrarError(v.stream().map(x -> "• " + x.getMessage()).collect(Collectors.joining("\n")));
            return;
        }
        try {
            if (seleccionado == null) { service.save(e); mostrarExito("Enfermero creado"); }
            else { e.setIdEnfermero(seleccionado.getIdEnfermero()); service.update(seleccionado.getIdEnfermero(), e); mostrarExito("Enfermero actualizado"); }
            cargar(); onLimpiar(); seleccionado = null;
        } catch (Exception ex) { mostrarError("Error: " + ex.getMessage()); }
    }

    @FXML
    public void onModificar() {
        Enfermero s = tabla.getSelectionModel().getSelectedItem();
        if (s == null) { mostrarError("Selecciona una fila"); return; }
        editar(s);
    }

    @FXML
    public void onEliminar() {
        Enfermero s = tabla.getSelectionModel().getSelectedItem();
        if (s == null) { mostrarError("Selecciona una fila"); return; }
        confirmarEliminar(s);
    }

    private void editar(Enfermero e) {
        seleccionado = e;
        txtDni.setText(e.getDni()); txtNombres.setText(e.getNombres());
        txtApellidos.setText(e.getApellidos()); txtTelefono.setText(e.getTelefono());
    }

    private void confirmarEliminar(Enfermero e) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar al enfermero " + e.getNombres() + " " + e.getApellidos() + "?",
                ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText("Confirmar eliminación");
        a.showAndWait().filter(b -> b == ButtonType.OK).ifPresent(b -> {
            try { service.delete(e.getIdEnfermero()); cargar(); mostrarExito("Eliminado"); }
            catch (Exception ex) { mostrarError("No se pudo eliminar: " + ex.getMessage()); }
        });
    }

    private Stage stage() { return tabla == null || tabla.getScene() == null ? null : (Stage) tabla.getScene().getWindow(); }
    private void mostrarError(String m) { Stage s = stage(); if (s != null) Toast.showToast(s, m, 2800, s.getX() + 80, s.getY() + 80); }
    private void mostrarExito(String m) { Stage s = stage(); if (s != null) Toast.showSuccess(s, m, 2000, s.getX() + 80, s.getY() + 80); }
    private String empty(String s) { return s == null ? "" : s.trim(); }
}
