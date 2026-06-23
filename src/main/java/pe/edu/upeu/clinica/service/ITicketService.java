package pe.edu.upeu.clinica.service;

import pe.edu.upeu.clinica.model.Cita;
import pe.edu.upeu.clinica.model.Ticket;

// Servicio de Ticket (DTO de impresión). No persiste tabla propia — proyecta
// los datos de una Cita + el Emisor de la clínica.
public interface ITicketService {
    // Construye el Ticket a partir de una Cita ya persistida, anexando
    // los datos del Emisor (clínica) y calculando el turno desde el numTicket.
    Ticket buildTicket(Cita cita);

    // Renderiza el ticket como texto ASCII monoespaciado (80mm).
    // Es el fallback que se muestra en pantalla si Jasper no está disponible.
    String renderText(Ticket ticket);

    // Arma el contenido legible que se codifica en el QR del ticket.
    // Al escanearlo se muestran los datos de la cita (ticket, paciente, DNI,
    // fecha/hora, médico, especialidad) en texto plano.
    String buildQrContent(Ticket ticket);
}
