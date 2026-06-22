package pe.edu.upeu.clinica.components;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import javafx.scene.control.Control;

import java.util.Map;
import java.util.Set;

/**
 * Puente entre Jakarta Bean Validation y la UI: valida un objeto y marca
 * visualmente (borde rojo + tooltip, vía {@link ToltipCustom}) cada campo del
 * formulario cuya propiedad tenga una violación.
 *
 * <p>El {@code Map} asocia el nombre de la propiedad del modelo (p. ej. "dni",
 * "nombres", "especialidad") con el control de la UI que la captura. Las claves
 * deben coincidir con los nombres de campo anotados en la entidad.
 */
public final class FormValidator {

    private static final ToltipCustom TTC = new ToltipCustom();

    private FormValidator() {}

    /**
     * Limpia las marcas previas, valida {@code obj} y marca en rojo los campos
     * con error. Devuelve el conjunto de violaciones (vacío si todo es válido).
     */
    public static <T> Set<ConstraintViolation<T>> validar(Validator validator, T obj, Map<String, Control> campos) {
        // Restablecer el aspecto normal de todos los campos antes de revalidar.
        campos.values().forEach(TTC::limpiarCampo);

        Set<ConstraintViolation<T>> violaciones = validator.validate(obj);
        for (ConstraintViolation<T> v : violaciones) {
            String propiedad = v.getPropertyPath().toString();
            Control control = campos.get(propiedad);
            if (control != null) {
                TTC.marcarError(control, v.getMessage());
            }
        }
        return violaciones;
    }
}
