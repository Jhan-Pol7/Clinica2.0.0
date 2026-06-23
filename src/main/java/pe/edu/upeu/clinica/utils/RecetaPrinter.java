package pe.edu.upeu.clinica.utils;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.output.PrinterOutputStream;
import pe.edu.upeu.clinica.model.Receta;
import pe.edu.upeu.clinica.model.RecetaDetalle;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

// Imprime una receta médica en la ticketera térmica ESC/POS (80 mm).
// Sigue el mismo patrón que TicketPrinter: lanza IOException si no hay impresora.
public class RecetaPrinter {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void imprimir(Receta receta) throws IOException {
        PrinterManager pm = PrinterManager.getInstance();
        try (EscPos escpos = new EscPos(new PrinterOutputStream(pm.getPrintService()))) {
            Style titulo = new Style()
                    .setBold(true)
                    .setJustification(EscPosConst.Justification.Center);
            Style centro = new Style().setJustification(EscPosConst.Justification.Center);
            Style izq    = new Style().setJustification(EscPosConst.Justification.Left_Default);
            Style negrita = new Style().setBold(true)
                    .setJustification(EscPosConst.Justification.Left_Default);

            // Cabecera
            escpos.writeLF(titulo, "CLINICA MAS CERCA DE DIOS");
            escpos.writeLF(centro, "----------------------------------------");
            escpos.writeLF(titulo, "RECETA MÉDICA");
            escpos.writeLF(centro, "----------------------------------------");

            // Datos de la receta
            escpos.writeLF(izq, "Receta N°   : " + receta.getIdReceta());
            escpos.writeLF(izq, "Fecha       : " + (receta.getFechaReg() == null ? "—"
                    : receta.getFechaReg().format(FMT)));

            // Datos del paciente y diagnóstico via la consulta
            if (receta.getConsulta() != null) {
                var cita = receta.getConsulta().getCita();
                if (cita != null && cita.getPaciente() != null) {
                    var p = cita.getPaciente();
                    escpos.writeLF(izq, "Paciente    : " + p.getNombres() + " " + p.getApellidos());
                    escpos.writeLF(izq, "DNI         : " + (p.getDni() == null ? "—" : p.getDni()));
                }
                if (receta.getConsulta().getDiagnostico() != null) {
                    escpos.writeLF(izq, "Diagnóstico : " + receta.getConsulta().getDiagnostico());
                }
            }

            // Medicamentos
            escpos.writeLF(centro, "----------------------------------------");
            escpos.writeLF(negrita, "MEDICAMENTOS PRESCRITOS:");
            escpos.writeLF(centro, "----------------------------------------");

            List<RecetaDetalle> detalles = receta.getDetalles();
            if (detalles != null && !detalles.isEmpty()) {
                for (RecetaDetalle d : detalles) {
                    escpos.writeLF(negrita, d.getMedicamento());
                    escpos.writeLF(izq, "  Dosis    : " + d.getDosis());
                    escpos.writeLF(izq, "  Frecuenc.: " + d.getFrecuencia());
                    escpos.writeLF(izq, "  Duración : " + d.getDuracion());
                    escpos.writeLF(izq, "  Vía      : " + d.getVia());
                    escpos.writeLF(izq, "");
                }
            } else {
                escpos.writeLF(centro, "(Sin medicamentos)");
            }

            // Indicaciones y recomendaciones
            escpos.writeLF(centro, "----------------------------------------");
            if (receta.getIndicacionesGenerales() != null && !receta.getIndicacionesGenerales().isBlank()) {
                escpos.writeLF(negrita, "Indicaciones:");
                escpos.writeLF(izq, receta.getIndicacionesGenerales());
            }
            if (receta.getRecomendaciones() != null && !receta.getRecomendaciones().isBlank()) {
                escpos.writeLF(negrita, "Recomendaciones:");
                escpos.writeLF(izq, receta.getRecomendaciones());
            }

            escpos.writeLF(centro, "========================================");
            escpos.writeLF(centro, "Conserve esta receta");
            escpos.feed(3);
            escpos.cut(EscPos.CutMode.FULL);
        }
    }
}
