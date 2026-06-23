package pe.edu.upeu.clinica.service.impl;

import pe.edu.upeu.clinica.model.Cita;
import pe.edu.upeu.clinica.model.Emisor;
import pe.edu.upeu.clinica.model.Ticket;
import pe.edu.upeu.clinica.repository.EmisorRepository;
import pe.edu.upeu.clinica.service.ITicketService;

import java.time.format.DateTimeFormatter;

// Servicio de Ticket. Convierte una Cita en un DTO de impresión Ticket
// y produce su representación textual (fallback sin Jasper).
public class TicketServiceImp implements ITicketService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a");

    private final EmisorRepository emisorRepo;

    public TicketServiceImp(EmisorRepository emisorRepo) { this.emisorRepo = emisorRepo; }

    @Override
    public Ticket buildTicket(Cita c) {
        if (c == null) throw new IllegalArgumentException("Cita null");
        // Cargamos al Emisor (datos de la clínica) para la cabecera del ticket.
        Emisor emisor = emisorRepo.findFirst().orElse(null);

        // Extraer el turno: último segmento de "T-yyyyMMdd-####".
        Integer turno = null;
        if (c.getNumTicket() != null) {
            String[] parts = c.getNumTicket().split("-");
            if (parts.length >= 3) {
                try { turno = Integer.parseInt(parts[parts.length - 1]); }
                catch (NumberFormatException ignored) { /* numTicket no estándar: dejamos turno=null */ }
            }
        }

        // Proyectar Cita -> Ticket (formato plano, listo para Jasper o impresión).
        return Ticket.builder()
                .numTicket(c.getNumTicket())
                .pacienteNombre(c.getPaciente() == null ? "" :
                        (c.getPaciente().getNombres() + " " + c.getPaciente().getApellidos()).trim())
                .pacienteDni(c.getPaciente() == null ? "" : c.getPaciente().getDni())
                .especialidad(c.getEspecialidad() == null ? "" : c.getEspecialidad().getNombre())
                .medico(c.getMedico() == null ? "" :
                        ("Dr. " + c.getMedico().getNombres() + " " + c.getMedico().getApellidos()).trim())
                .fecha(c.getFecha())
                .hora(c.getHora())
                .turno(turno)
                .tipoAtencion(c.getTipoAtencion() == null ? "" : c.getTipoAtencion().name())
                .emisor(emisor)
                .build();
    }

    @Override
    public String renderText(Ticket t) {
        // Layout ASCII estilo papel térmico 80mm (40 columnas).
        // Coincide exactamente con el formato que pide la sección 9.8 del prompt.
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("   ").append(t.getEmisor() == null ? "CLÍNICA MÁS CERCA DE DIOS"
                : t.getEmisor().getNombreComercial().toUpperCase()).append("\n");
        if (t.getEmisor() != null) {
            sb.append("   RUC: ").append(t.getEmisor().getRuc()).append("\n");
            sb.append("   ").append(t.getEmisor().getDomicilioFiscal())
              .append(" — ").append(t.getEmisor().getDistrito())
              .append(", ").append(t.getEmisor().getDepartamento()).append("\n");
        }
        sb.append("----------------------------------------\n");
        sb.append("        TICKET DE CITA MÉDICA\n");
        sb.append("----------------------------------------\n");
        sb.append(String.format("N° Ticket   : %s%n", n(t.getNumTicket())));
        sb.append(String.format("Paciente    : %s%n", n(t.getPacienteNombre())));
        sb.append(String.format("DNI         : %s%n", n(t.getPacienteDni())));
        sb.append(String.format("Especialidad: %s%n", n(t.getEspecialidad())));
        sb.append(String.format("Médico      : %s%n", n(t.getMedico())));
        sb.append(String.format("Fecha       : %s%n", t.getFecha() == null ? "" : t.getFecha().format(DATE)));
        sb.append(String.format("Hora        : %s%n", t.getHora() == null ? "" : t.getHora().format(TIME)));
        sb.append(String.format("Turno       : N° %s%n", t.getTurno() == null ? "?" : t.getTurno()));
        sb.append(String.format("Tipo Atenc. : %s%n", n(t.getTipoAtencion())));
        sb.append("----------------------------------------\n");
        sb.append("  Gracias por su preferencia\n");
        sb.append("  Conserve este ticket\n");
        sb.append("========================================\n");
        return sb.toString();
    }

    @Override
    public String buildQrContent(Ticket t) {
        // Texto plano y compacto: al escanear el QR se ven los datos de la cita.
        StringBuilder sb = new StringBuilder();
        sb.append(t.getEmisor() == null ? "CLINICA MAS CERCA DE DIOS"
                : t.getEmisor().getNombreComercial()).append("\n");
        sb.append("Ticket: ").append(n(t.getNumTicket())).append("\n");
        sb.append("Paciente: ").append(n(t.getPacienteNombre())).append("\n");
        sb.append("DNI: ").append(n(t.getPacienteDni())).append("\n");
        sb.append("Fecha: ").append(t.getFecha() == null ? "" : t.getFecha().format(DATE));
        sb.append("  Hora: ").append(t.getHora() == null ? "" : t.getHora().format(TIME)).append("\n");
        sb.append("Medico: ").append(n(t.getMedico())).append("\n");
        sb.append("Especialidad: ").append(n(t.getEspecialidad()));
        return sb.toString();
    }

    // Null-safe para evitar "null" literal en el output.
    private String n(String s) { return s == null ? "" : s; }
}
