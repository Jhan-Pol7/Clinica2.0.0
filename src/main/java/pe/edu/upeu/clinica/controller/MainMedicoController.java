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
import pe.edu.upeu.clinica.model.Especialidad;
import pe.edu.upeu.clinica.model.Medico;
import pe.edu.upeu.clinica.service.IEspecialidadService;
import pe.edu.upeu.clinica.service.IMedicoService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// CRUD de Médico. A diferencia de los CRUDs sin FKs, este Controller
// recibe DOS servicios: el suyo y el de Especialidad (para poblar el ComboBox).
public class MainMedicoController {

    private final IMedicoService service;
    private final IEspecialidadService espService;  // necesario para alimentar cmbEspecialidad
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private Medico seleccionado;

    @FXML private TextField txtDni, txtNombres, txtApellidos, txtColeg, txtTelefono, txtEmail;
    @FXML private ComboBox<Especialidad> cmbEspecialidad;
    @FXML private TableView<Medico> tabla;
    @FXML private TextField txtBuscar;      // búsqueda en vivo sobre la tabla
    private TableSearchFilter<Medico> filtro;

    public MainMedicoController(IMedicoService service, IEspecialidadService espService) {
        this.service = service;
        this.espService = espService;
    }

    @FXML
    public void initialize() {
        // Cargar todas las especialidades en el ComboBox. Especialidad.toString()
        // devuelve el nombre, así que no se necesita cellFactory.
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

        // Llena el ComboBox con todas las especialidades disponibles.
        // Especialidad.toString() ya devuelve el nombre, por eso no se necesita cellFactory.
        recargarEspecialidades();
        // Cada vez que el usuario abra el ComboBox se recargan las especialidades.
        // Así, si recién se acaba de agregar una especialidad nueva, aparece sin
        // necesidad de cerrar y volver a abrir la pestaña.
        cmbEspecialidad.setOnShowing(e -> {
            Especialidad actual = cmbEspecialidad.getValue();
            recargarEspecialidades();
            if (actual != null) {
                cmbEspecialidad.getItems().stream()
                        .filter(x -> x.getIdEspecialidad().equals(actual.getIdEspecialidad()))
                        .findFirst().ifPresent(cmbEspecialidad::setValue);
            }
        });

        // Construye las columnas de la tabla. La columna "N°" se agrega aparte (abajo).
        TableViewHelper<Medico> h = new TableViewHelper<>();
        LinkedHashMap<String, ColumnInfo> cols = new LinkedHashMap<>();
        cols.put("DNI",           new ColumnInfo("dni",              100.0));
        cols.put("Nombres",       new ColumnInfo("nombres",          140.0));
        cols.put("Apellidos",     new ColumnInfo("apellidos",        160.0));
        cols.put("Colegiatura",   new ColumnInfo("numColegiatura",   110.0));
        cols.put("Especialidad",  new ColumnInfo("especialidad.nombre", 140.0));
        cols.put("Teléfono",      new ColumnInfo("telefono",         100.0));
        h.addColumnsInOrderWithSize(tabla, cols, this::editar, this::confirmarEliminar);

        // Columna "N°" con numeración secuencial (1, 2, 3...) al inicio de la tabla.
        javafx.scene.control.TableColumn<Medico, Void> colNum = new javafx.scene.control.TableColumn<>("N°");
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

        // Búsqueda en vivo: filtra por DNI, nombres, apellidos, colegiatura o especialidad.
        filtro = new TableSearchFilter<>(txtBuscar, tabla, m ->
                String.join(" ",
                        m.getDni(), m.getNombres(), m.getApellidos(),
                        m.getNumColegiatura() == null ? "" : m.getNumColegiatura(),
                        m.getEspecialidad() == null ? "" : m.getEspecialidad().getNombre(),
                        m.getTelefono() == null ? "" : m.getTelefono()));
        cargar();
    }

    // Refresca la tabla de médicos con la información más reciente de la BD.
    private void cargar() { filtro.setData(service.findAll()); }

    // Vuelve a leer las especialidades de la BD y las pone en el ComboBox.
    private void recargarEspecialidades() {
        cmbEspecialidad.setItems(FXCollections.observableArrayList(espService.findAll()));
    }

    @FXML public void onNuevo()   { seleccionado = null; onLimpiar(); }
    @FXML public void onLimpiar() {
        txtDni.clear(); txtNombres.clear(); txtApellidos.clear();
        txtColeg.clear(); txtTelefono.clear(); txtEmail.clear();
        cmbEspecialidad.getSelectionModel().clearSelection();
    }

    @FXML
    public void onGuardar() {
        Medico m = Medico.builder()
                .dni(empty(txtDni.getText()))
                .nombres(empty(txtNombres.getText()))
                .apellidos(empty(txtApellidos.getText()))
                .numColegiatura(empty(txtColeg.getText()))
                .telefono(empty(txtTelefono.getText()))
                .email(empty(txtEmail.getText()))
                .especialidad(cmbEspecialidad.getValue())
                .build();
        // Validación Jakarta + marcado visual de cada campo con error (borde rojo + tooltip).
        Map<String, Control> campos = new LinkedHashMap<>();
        campos.put("dni", txtDni);
        campos.put("nombres", txtNombres);
        campos.put("apellidos", txtApellidos);
        campos.put("telefono", txtTelefono);
        campos.put("especialidad", cmbEspecialidad);
        Set<ConstraintViolation<Medico>> v = FormValidator.validar(validator, m, campos);
        if (!v.isEmpty()) {
            mostrarError(v.stream().map(x -> "• " + x.getMessage()).collect(Collectors.joining("\n")));
            return;
        }
        try {
            if (seleccionado == null) { service.save(m); mostrarExito("Médico registrado"); }
            else { m.setIdMedico(seleccionado.getIdMedico()); service.update(seleccionado.getIdMedico(), m); mostrarExito("Médico actualizado"); }
            cargar(); onLimpiar(); seleccionado = null;
        } catch (Exception ex) { mostrarError("Error: " + ex.getMessage()); }
    }

    @FXML
    public void onModificar() {
        Medico s = tabla.getSelectionModel().getSelectedItem();
        if (s == null) { mostrarError("Selecciona una fila"); return; }
        editar(s);
    }

    @FXML
    public void onEliminar() {
        Medico s = tabla.getSelectionModel().getSelectedItem();
        if (s == null) { mostrarError("Selecciona una fila"); return; }
        confirmarEliminar(s);
    }

    // Pone la fila en modo edición y rellena el formulario.
    // El ComboBox necesita un truco: buscar la instancia exacta de su items
    // que tiene el mismo id (no la que viene en el médico) — si no, el combo
    // no marca ningún item como seleccionado.
    private void editar(Medico m) {
        seleccionado = m;
        txtDni.setText(m.getDni()); txtNombres.setText(m.getNombres());
        txtApellidos.setText(m.getApellidos()); txtColeg.setText(m.getNumColegiatura());
        txtTelefono.setText(m.getTelefono()); txtEmail.setText(m.getEmail() == null ? "" : m.getEmail());
        if (m.getEspecialidad() != null) {
            cmbEspecialidad.getItems().stream()
                    .filter(e -> e.getIdEspecialidad().equals(m.getEspecialidad().getIdEspecialidad()))
                    .findFirst()
                    .ifPresent(cmbEspecialidad::setValue);
        }
    }

    private void confirmarEliminar(Medico m) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar al médico " + m.getNombres() + " " + m.getApellidos() + "?",
                ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText("Confirmar eliminación");
        a.showAndWait().filter(b -> b == ButtonType.OK).ifPresent(b -> {
            try { service.delete(m.getIdMedico()); cargar(); mostrarExito("Médico eliminado"); }
            catch (Exception ex) { mostrarError("No se pudo eliminar: " + ex.getMessage()); }
        });
    }

    private Stage stage() { return tabla == null || tabla.getScene() == null ? null : (Stage) tabla.getScene().getWindow(); }
    private void mostrarError(String m) { Stage s = stage(); if (s != null) Toast.showToast(s, m, 2800, s.getX() + 80, s.getY() + 80); }
    private void mostrarExito(String m) { Stage s = stage(); if (s != null) Toast.showSuccess(s, m, 2000, s.getX() + 80, s.getY() + 80); }
    private String empty(String s) { return s == null ? "" : s.trim(); }
}
