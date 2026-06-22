package pe.edu.upeu.clinica.components;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Búsqueda en vivo para una {@link TableView}: filtra las filas en memoria
 * a medida que el usuario escribe en un {@link TextField}, igual que el patrón
 * de filtrado de SysVentas (01FINAL) pero encapsulado y reutilizable.
 *
 * <p>Mantiene una lista "maestra" propia (independiente de los items visibles
 * de la tabla). En cada pulsación se recalcula qué filas mostrar comparando,
 * sin distinguir mayúsculas, el filtro contra el texto que produce la función
 * {@code texto} para cada fila. Si el filtro está vacío se muestran todas.
 *
 * <p>Uso típico en un controller:
 * <pre>
 *   filtro = new TableSearchFilter&lt;&gt;(txtBuscar, tabla,
 *            m -&gt; m.getDni() + " " + m.getNombres() + " " + m.getApellidos());
 *   // y en cargar():
 *   filtro.setData(service.findAll());
 * </pre>
 *
 * @param <T> tipo de entidad que muestra la tabla
 */
public final class TableSearchFilter<T> {

    private final TableView<T> tabla;
    private final TextField campo;
    private final Function<T, String> texto;
    // Copia maestra de todos los registros; la tabla solo muestra el subconjunto filtrado.
    private final ObservableList<T> maestra = FXCollections.observableArrayList();

    public TableSearchFilter(TextField campo, TableView<T> tabla, Function<T, String> texto) {
        this.campo = campo;
        this.tabla = tabla;
        this.texto = texto;
        // Re-filtrar en cada cambio del texto de búsqueda.
        campo.textProperty().addListener((obs, anterior, actual) -> aplicar());
    }

    /** Reemplaza los datos maestros (p. ej. tras recargar de la BD) y refresca respetando el filtro actual. */
    public void setData(Collection<T> datos) {
        maestra.setAll(datos);
        aplicar();
    }

    /** Recalcula las filas visibles según el texto de búsqueda actual. */
    private void aplicar() {
        String f = campo.getText() == null ? "" : campo.getText().trim().toLowerCase();
        if (f.isEmpty()) {
            tabla.getItems().setAll(maestra);
            return;
        }
        tabla.getItems().setAll(maestra.stream()
                .filter(item -> {
                    String s = texto.apply(item);
                    return s != null && s.toLowerCase().contains(f);
                })
                .collect(Collectors.toList()));
    }
}
