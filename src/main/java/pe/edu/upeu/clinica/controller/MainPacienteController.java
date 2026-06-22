package pe.edu.upeu.clinica.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableView;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;
import pe.edu.upeu.clinica.components.ColumnInfo;
import pe.edu.upeu.clinica.components.FormValidator;
import pe.edu.upeu.clinica.components.TableSearchFilter;
import pe.edu.upeu.clinica.components.TableViewHelper;
import pe.edu.upeu.clinica.components.Toast;
import pe.edu.upeu.clinica.enums.Sexo;
import pe.edu.upeu.clinica.model.Paciente;
import pe.edu.upeu.clinica.service.IPacienteService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// CRUD de Paciente. Extiende el patrón estándar añadiendo búsqueda por DNI
// (botón "Buscar") que precarga el formulario si el paciente ya existe.
// También incluye DatePicker para la fecha de nacimiento y ComboBox para Sexo.
public class MainPacienteController {

    private final IPacienteService service;
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private Paciente seleccionado;          // null = modo Nuevo

    @FXML private TextField  txtDni, txtNombres, txtApellidos, txtTelefono, txtDireccion, txtEmail;
    @FXML private DatePicker dpFechaNac;
    @FXML private ComboBox<Sexo> cmbSexo;   // alimentado con Sexo.values() en initialize
    @FXML private TableView<Paciente> tabla;
    @FXML private TextField txtBuscar;      // búsqueda en vivo sobre la tabla
    private TableSearchFilter<Paciente> filtro;

    public MainPacienteController(IPacienteService service) { this.service = service; }

    @FXML
    public void initialize() {
        cmbSexo.setItems(FXCollections.observableArrayList(Sexo.values()));

        // Filtros de entrada: DNI solo acepta hasta 8 dígitos numéricos.
        txtDni.setTextFormatter(new TextFormatter<>(cambio -> {
            String nuevoTexto = cambio.getControlNewText();
            return nuevoTexto.matches("\\d{0,8}") ? cambio : null;
        }));
        // Teléfono solo acepta hasta 9 dígitos numéricos.
        txtTelefono.setTextFormatter(new TextFormatter<>(cambio -> {
            String nuevoTexto = cambio.getControlNewText();
            return nuevoTexto.matches("\\d{0,9}") ? cambio : null;
        }));

        TableViewHelper<Paciente> h = new TableViewHelper<>();
        LinkedHashMap<String, ColumnInfo> cols = new LinkedHashMap<>();
        cols.put("DNI",         new ColumnInfo("dni",             100.0));
        cols.put("Nombres",     new ColumnInfo("nombres",         140.0));
        cols.put("Apellidos",   new ColumnInfo("apellidos",       160.0));
        cols.put("Sexo",        new ColumnInfo("sexo",             80.0));
        cols.put("F. Nac.",     new ColumnInfo("fechaNacimiento", 100.0));
        cols.put("Teléfono",    new ColumnInfo("telefono",        100.0));
        cols.put("Email",       new ColumnInfo("email",           180.0));
        h.addColumnsInOrderWithSize(tabla, cols, this::editar, this::confirmarEliminar);

        // Columna "N°" con numeración secuencial (1, 2, 3...) insertada al inicio.
        javafx.scene.control.TableColumn<Paciente, Void> colNum = new javafx.scene.control.TableColumn<>("N°");
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

        // Búsqueda en vivo: filtra por DNI, nombres, apellidos, teléfono o email.
        filtro = new TableSearchFilter<>(txtBuscar, tabla, p ->
                String.join(" ",
                        p.getDni(), p.getNombres(), p.getApellidos(),
                        p.getTelefono() == null ? "" : p.getTelefono(),
                        p.getEmail() == null ? "" : p.getEmail()));
        cargar();
    }

    private void cargar() { filtro.setData(service.findAll()); }

    @FXML public void onNuevo()   { seleccionado = null; onLimpiar(); }
    @FXML public void onLimpiar() {
        txtDni.clear(); txtNombres.clear(); txtApellidos.clear();
        txtTelefono.clear(); txtDireccion.clear(); txtEmail.clear();
        dpFechaNac.setValue(null); cmbSexo.getSelectionModel().clearSelection();
    }

    // Botón "Buscar DNI" — si encuentra al paciente, precarga el formulario en modo edición.
    @FXML
    public void onBuscarDni() {
        String dni = empty(txtDni.getText());
        if (dni.isEmpty()) { mostrarError("Ingresa el DNI"); return; }
        service.findByDni(dni).ifPresentOrElse(
                this::editar,                                            // existe -> rellenar formulario
                () -> mostrarError("No existe paciente con ese DNI"));    // no existe -> avisar
    }

    @FXML
    public void onGuardar() {
        Paciente p = Paciente.builder()
                .dni(empty(txtDni.getText()))
                .nombres(empty(txtNombres.getText()))
                .apellidos(empty(txtApellidos.getText()))
                .telefono(empty(txtTelefono.getText()))
                .fechaNacimiento(dpFechaNac.getValue())
                .sexo(cmbSexo.getValue())
                .direccion(empty(txtDireccion.getText()))
                .email(empty(txtEmail.getText()))
                .build();
        // Validación Jakarta + marcado visual de cada campo con error (borde rojo + tooltip).
        Map<String, Control> campos = new LinkedHashMap<>();
        campos.put("dni", txtDni);
        campos.put("nombres", txtNombres);
        campos.put("apellidos", txtApellidos);
        campos.put("telefono", txtTelefono);
        Set<ConstraintViolation<Paciente>> v = FormValidator.validar(validator, p, campos);
        if (!v.isEmpty()) {
            mostrarError(v.stream().map(x -> "• " + x.getMessage()).collect(Collectors.joining("\n")));
            return;
        }
        try {
            if (seleccionado == null) { service.save(p); mostrarExito("Paciente registrado"); }
            else { p.setIdPaciente(seleccionado.getIdPaciente()); service.update(seleccionado.getIdPaciente(), p); mostrarExito("Paciente actualizado"); }
            cargar(); onLimpiar(); seleccionado = null;
        } catch (Exception ex) { mostrarError("Error: " + ex.getMessage()); }
    }

    @FXML
    public void onModificar() {
        Paciente s = tabla.getSelectionModel().getSelectedItem();
        if (s == null) { mostrarError("Selecciona una fila"); return; }
        editar(s);
    }

    @FXML
    public void onEliminar() {
        Paciente s = tabla.getSelectionModel().getSelectedItem();
        if (s == null) { mostrarError("Selecciona una fila"); return; }
        confirmarEliminar(s);
    }

    private void editar(Paciente p) {
        seleccionado = p;
        txtDni.setText(p.getDni()); txtNombres.setText(p.getNombres());
        txtApellidos.setText(p.getApellidos()); txtTelefono.setText(p.getTelefono());
        txtDireccion.setText(p.getDireccion() == null ? "" : p.getDireccion());
        txtEmail.setText(p.getEmail() == null ? "" : p.getEmail());
        dpFechaNac.setValue(p.getFechaNacimiento());
        cmbSexo.setValue(p.getSexo());
    }

    private void confirmarEliminar(Paciente p) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar al paciente " + p.getNombres() + " " + p.getApellidos() + "?",
                ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText("Confirmar eliminación");
        a.showAndWait().filter(b -> b == ButtonType.OK).ifPresent(b -> {
            try { service.delete(p.getIdPaciente()); cargar(); mostrarExito("Paciente eliminado"); }
            catch (Exception ex) { mostrarError("No se pudo eliminar: " + ex.getMessage()); }
        });
    }

    private Stage stage() { return tabla == null || tabla.getScene() == null ? null : (Stage) tabla.getScene().getWindow(); }
    private void mostrarError(String m) { Stage s = stage(); if (s != null) Toast.showToast(s, m, 2800, s.getX() + 80, s.getY() + 80); }
    private void mostrarExito(String m) { Stage s = stage(); if (s != null) Toast.showSuccess(s, m, 2000, s.getX() + 80, s.getY() + 80); }
    private String empty(String s) { return s == null ? "" : s.trim(); }
}
