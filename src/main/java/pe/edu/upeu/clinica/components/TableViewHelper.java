package pe.edu.upeu.clinica.components;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

// Helper genérico que arma columnas de una TableView de forma rápida.
// Soporta campos planos (ej: "nombres") y campos anidados (ej: "especialidad.nombre").
// También agrega automáticamente una columna "Acciones" con botones Editar/Eliminar.
public class TableViewHelper<T> {

    // Crea las columnas en el orden dado y agrega al final la columna de acciones.
    // - tableView: la tabla a la que se le agregarán columnas
    // - columns: mapa con el nombre visible -> info de la columna (campo + ancho)
    // - updateAction: función que se llama al hacer click en "Editar"
    // - deleteAction: función que se llama al hacer click en "Eliminar"
    public void addColumnsInOrderWithSize(TableView<T> tableView,
                                          LinkedHashMap<String, ColumnInfo> columns,
                                          Consumer<T> updateAction,
                                          Consumer<T> deleteAction) {
        for (Map.Entry<String, ColumnInfo> entry : columns.entrySet()) {
            TableColumn<T, Object> column = new TableColumn<>(entry.getKey());
            String field = entry.getValue().getField();

            // Si el campo contiene un punto, es anidado (ej. "medico.nombres").
            // Se accede al objeto anidado por reflexión usando los getters.
            if (field.contains(".")) {
                column.setCellValueFactory(cellData -> {
                    T item = cellData.getValue();
                    String[] fieldPath = field.split("\\.");
                    try {
                        Object value = item.getClass().getMethod("get" + capitalize(fieldPath[0])).invoke(item);
                        if (value != null) {
                            Object nestedValue = value.getClass().getMethod("get" + capitalize(fieldPath[1])).invoke(value);
                            return new SimpleObjectProperty<>(nestedValue != null ? nestedValue : "N/A");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return new SimpleObjectProperty<>("N/A");
                });
            } else {
                // Campo plano: JavaFX lo resuelve automáticamente con PropertyValueFactory.
                column.setCellValueFactory(new PropertyValueFactory<>(field));
            }

            // Si se definió un ancho preferido, se aplica.
            if (entry.getValue().getWidth() != null) {
                column.setPrefWidth(entry.getValue().getWidth());
            }
            tableView.getColumns().add(column);
        }
        addActionColumn(tableView, updateAction, deleteAction);
        tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    }

    // Crea la columna "Acciones" con los botones "Editar" y "Eliminar" en cada fila.
    private void addActionColumn(TableView<T> tableView, Consumer<T> updateAction, Consumer<T> deleteAction) {
        TableColumn<T, Void> actionColumn = new TableColumn<>("Acciones");

        Callback<TableColumn<T, Void>, TableCell<T, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<T, Void> call(final TableColumn<T, Void> param) {
                return new TableCell<>() {
                    // Botón de editar: solo verde con ícono de lápiz (sin la palabra "Editar").
                    private final Button btnUpdate = new Button("✎");
                    private final Button btnDelete = new Button("Eliminar");
                    {
                        btnUpdate.setStyle("-fx-background-color: #27AE60; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 3 10 3 10;");
                        btnUpdate.setTooltip(new Tooltip("Editar"));
                        btnDelete.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 3 8 3 8;");
                        btnUpdate.setOnAction(event -> {
                            T data = getTableView().getItems().get(getIndex());
                            updateAction.accept(data);
                        });
                        btnDelete.setOnAction(event -> {
                            T data = getTableView().getItems().get(getIndex());
                            deleteAction.accept(data);
                        });
                    }
                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            HBox buttons = new HBox(btnUpdate, btnDelete);
                            buttons.setSpacing(8);
                            setGraphic(buttons);
                        }
                    }
                };
            }
        };
        actionColumn.setCellFactory(cellFactory);
        actionColumn.setPrefWidth(160);
        tableView.getColumns().add(actionColumn);
    }

    // Convierte la primera letra a mayúscula (para construir nombres de getters por reflexión).
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
