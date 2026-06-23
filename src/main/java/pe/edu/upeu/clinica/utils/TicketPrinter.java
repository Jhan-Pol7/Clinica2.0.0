package pe.edu.upeu.clinica.utils;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.escpos.barcode.QRCode;
import com.github.anastaciocintra.output.PrinterOutputStream;
import pe.edu.upeu.clinica.model.Ticket;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

// Impresor ESC/POS para tickets en papel térmico (típicamente 80 mm).
// Es opcional: si no hay impresora física conectada, lanza IOException y el
// usuario puede usar el visor Jasper o exportar a PDF como alternativa.
public class TicketPrinter {

    // Formatos de fecha y hora usados en el ticket.
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    // Envía el ticket a la impresora térmica usando el protocolo ESC/POS.
    // qrContent: texto que se codifica en el QR (datos de la cita). Si es null
    // o vacío, simplemente no se imprime el QR.
    public void imprimir(Ticket t, String qrContent) throws IOException {
        PrinterManager pm = PrinterManager.getInstance();
        try (EscPos escpos = new EscPos(new PrinterOutputStream(pm.getPrintService()))) {
            // Estilos: negrita+centrado para títulos, centrado normal y alineado a la izquierda.
            Style titulo = new Style()
                    .setBold(true)
                    .setJustification(EscPosConst.Justification.Center);
            Style centro = new Style().setJustification(EscPosConst.Justification.Center);
            Style izq = new Style().setJustification(EscPosConst.Justification.Left_Default);

            // Cabecera con el nombre de la clínica (del Emisor o por defecto).
            escpos.writeLF(titulo, t.getEmisor() == null ? "CLÍNICA MÁS CERCA DE DIOS"
                    : t.getEmisor().getNombreComercial().toUpperCase());
            if (t.getEmisor() != null) {
                escpos.writeLF(centro, "RUC: " + t.getEmisor().getRuc());
                escpos.writeLF(centro, t.getEmisor().getDomicilioFiscal() + " — " + t.getEmisor().getDistrito());
            }
            escpos.writeLF(centro, "----------------------------------------");
            escpos.writeLF(titulo, "TICKET DE CITA MÉDICA");
            escpos.writeLF(centro, "----------------------------------------");
            escpos.writeLF(izq, "N° Ticket   : " + t.getNumTicket());
            escpos.writeLF(izq, "Paciente    : " + t.getPacienteNombre());
            escpos.writeLF(izq, "DNI         : " + t.getPacienteDni());
            escpos.writeLF(izq, "Especialidad: " + t.getEspecialidad());
            escpos.writeLF(izq, "Médico      : " + t.getMedico());
            escpos.writeLF(izq, "Fecha       : " + (t.getFecha() == null ? "" : t.getFecha().format(DATE)));
            escpos.writeLF(izq, "Hora        : " + (t.getHora() == null ? "" : t.getHora().format(TIME)));
            escpos.writeLF(izq, "Turno       : N° " + t.getTurno());
            escpos.writeLF(izq, "Tipo Atenc. : " + t.getTipoAtencion());
            escpos.writeLF(centro, "----------------------------------------");

            // Código QR centrado con los datos de la cita (escaneable).
            if (qrContent != null && !qrContent.isBlank()) {
                QRCode qrcode = new QRCode();
                qrcode.setSize(5);                                   // módulo 5 px (tamaño legible en 80mm)
                qrcode.setJustification(EscPosConst.Justification.Center);
                escpos.write(qrcode, qrContent);
                escpos.writeLF(centro, "Escanea para ver tu cita");
                escpos.writeLF(centro, "----------------------------------------");
            }

            escpos.writeLF(centro, "Gracias por su preferencia");
            escpos.writeLF(centro, "Conserve este ticket");
            escpos.writeLF(centro, "========================================");
            // Avanza 3 líneas y corta el papel automáticamente.
            escpos.feed(3);
            escpos.cut(EscPos.CutMode.FULL);
        }
    }
}
