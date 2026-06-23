package pe.edu.upeu.clinica.service.impl;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import pe.edu.upeu.clinica.config.DatabaseConfig;
import pe.edu.upeu.clinica.enums.EstadoCita;
import pe.edu.upeu.clinica.model.Cita;
import pe.edu.upeu.clinica.model.Consulta;
import pe.edu.upeu.clinica.model.Emisor;
import pe.edu.upeu.clinica.model.Receta;
import pe.edu.upeu.clinica.model.RecetaDetalle;
import pe.edu.upeu.clinica.model.Ticket;
import pe.edu.upeu.clinica.repository.EmisorRepository;
import pe.edu.upeu.clinica.service.ICitaService;
import pe.edu.upeu.clinica.service.IRecetaService;
import pe.edu.upeu.clinica.service.IReporteService;
import pe.edu.upeu.clinica.service.ITicketService;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// Implementación de los 3 reportes JasperReports del proyecto.
// Optimización: los .jrxml se compilan al primer uso y se guardan en un
// cache (ConcurrentHashMap), evitando recompilar en cada generación.
public class ReporteServiceImp implements IReporteService {

    // Formatos para los parámetros String pasados a los .jrxml.
    private static final DateTimeFormatter DATE_F = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_F = DateTimeFormatter.ofPattern("hh:mm a");

    // Dependencias inyectadas por AppContext.
    private final ITicketService ticketService;
    private final ICitaService   citaService;
    private final IRecetaService recetaService;
    private final EmisorRepository emisorRepo;

    // Cache: path del jrxml -> JasperReport compilado. ConcurrentHashMap
    // porque el visor podría llamarse desde threads de JavaFX simultáneos.
    private final Map<String, JasperReport> cache = new ConcurrentHashMap<>();

    public ReporteServiceImp(ITicketService ticketService,
                             ICitaService citaService,
                             IRecetaService recetaService,
                             EmisorRepository emisorRepo) {
        this.ticketService = ticketService;
        this.citaService = citaService;
        this.recetaService = recetaService;
        this.emisorRepo = emisorRepo;
    }

    @Override
    public JasperPrint generarTicket(Cita cita) {
        // Re-cargamos la cita completa (con JOINs) si nos pasaron solo un id.
        Cita full = (cita.getIdCita() != null) ? citaService.findById(cita.getIdCita()) : cita;
        // Proyectamos la cita a Ticket (incluye datos del Emisor).
        Ticket t = ticketService.buildTicket(full);
        Emisor e = t.getEmisor() == null ? emisorRepo.findFirst().orElse(null) : t.getEmisor();

        // El .jrxml de ticket usa parámetros (no field), así que rellenamos el Map.
        Map<String, Object> params = new HashMap<>();
        params.put("emisorNombre",    e == null ? null : e.getNombreComercial());
        params.put("emisorRuc",       e == null ? null : e.getRuc());
        params.put("emisorDireccion", e == null ? null :
                (e.getDomicilioFiscal() + " — " + e.getDistrito() + ", " + e.getDepartamento()));
        params.put("numTicket",       t.getNumTicket());
        params.put("paciente",        t.getPacienteNombre());
        params.put("dni",             t.getPacienteDni());
        params.put("especialidad",    t.getEspecialidad());
        params.put("medico",          t.getMedico());
        params.put("fecha",           t.getFecha() == null ? "" : t.getFecha().format(DATE_F));
        params.put("hora",            t.getHora()  == null ? "" : t.getHora().format(TIME_F));
        params.put("turno",           t.getTurno() == null ? null : String.valueOf(t.getTurno()));
        params.put("tipoAtencion",    t.getTipoAtencion());
        // Imagen QR con los datos de la cita (escaneable desde el ticket impreso/PDF).
        params.put("qr", pe.edu.upeu.clinica.utils.QrGenerator.generar(
                ticketService.buildQrContent(t), 150));

        try {
            // JREmptyDataSource = 1 fila vacía, suficiente para que el banner se renderice una vez.
            return JasperFillManager.fillReport(
                    compile("/jasper/ticket_cita.jrxml"),
                    params,
                    new JREmptyDataSource());
        } catch (JRException ex) {
            throw new RuntimeException("Error generando ticket: " + ex.getMessage(), ex);
        }
    }

    @Override
    public JasperPrint generarReceta(Consulta consulta) {
        if (consulta == null || consulta.getCita() == null) {
            throw new IllegalArgumentException("Consulta sin cita asociada");
        }
        // La receta vive en otra tabla — la buscamos por la consulta.
        Optional<Receta> r = recetaService.findByConsulta(consulta.getIdConsulta());
        if (r.isEmpty()) throw new IllegalStateException("La consulta no tiene receta");

        Cita cita = citaService.findById(consulta.getCita().getIdCita());
        Emisor e = emisorRepo.findFirst().orElse(null);
        List<RecetaDetalle> detalles = recetaService.findDetalles(r.get().getIdReceta());

        // Parámetros de la cabecera + paciente + médico + diagnóstico.
        Map<String, Object> params = new HashMap<>();
        params.put("emisorNombre",    e == null ? null : e.getNombreComercial());
        params.put("emisorRuc",       e == null ? null : e.getRuc());
        params.put("emisorDireccion", e == null ? null :
                (e.getDomicilioFiscal() + " — " + e.getDistrito() + ", " + e.getDepartamento()));
        params.put("paciente",        cita.getPaciente().getNombres() + " " + cita.getPaciente().getApellidos());
        params.put("dni",             cita.getPaciente().getDni());
        params.put("medico",          "Dr. " + cita.getMedico().getNombres() + " " + cita.getMedico().getApellidos());
        params.put("numColegiatura",  cita.getMedico().getNumColegiatura());
        params.put("especialidad",    cita.getEspecialidad().getNombre());
        params.put("diagnostico",     consulta.getDiagnostico());
        params.put("indicaciones",    r.get().getIndicacionesGenerales());
        params.put("recomendaciones", r.get().getRecomendaciones());
        params.put("fecha",           r.get().getFechaReg() == null ? ""
                : r.get().getFechaReg().toLocalDate().format(DATE_F));

        try {
            // El .jrxml usa la colección de detalles como datasource del detail band.
            // Si la receta está vacía, mostramos un placeholder para que el reporte no quede en blanco.
            List<RecetaDetalle> data = (detalles == null || detalles.isEmpty())
                    ? Collections.singletonList(RecetaDetalle.builder().medicamento("(sin medicamentos)").build())
                    : new ArrayList<>(detalles);
            return JasperFillManager.fillReport(
                    compile("/jasper/receta_medica.jrxml"),
                    params,
                    new JRBeanCollectionDataSource(data));
        } catch (JRException ex) {
            throw new RuntimeException("Error generando receta: " + ex.getMessage(), ex);
        }
    }

    @Override
    public JasperPrint generarReporteCitas(LocalDate fechaIni, LocalDate fechaFin,
                                           Long idEspecialidad, Long idMedico, EstadoCita estado) {
        // El reporte_citas.jrxml tiene la query SQL embebida; aquí solo pasamos
        // los 5 parámetros opcionales (null = no filtrar por ese campo).
        Map<String, Object> params = new HashMap<>();
        params.put("fechaIni",       fechaIni == null ? null : Date.valueOf(fechaIni));
        params.put("fechaFin",       fechaFin == null ? null : Date.valueOf(fechaFin));
        params.put("idEspecialidad", idEspecialidad);
        params.put("idMedico",       idMedico);
        params.put("estado",         estado == null ? null : estado.name());

        // Le pasamos directamente una Connection del pool — Jasper ejecutará
        // el queryString del jrxml contra ella.
        try (Connection conn = DatabaseConfig.getConnection()) {
            return JasperFillManager.fillReport(
                    compile("/jasper/reporte_citas.jrxml"),
                    params,
                    conn);
        } catch (JRException | SQLException ex) {
            throw new RuntimeException("Error generando reporte de citas: " + ex.getMessage(), ex);
        }
    }

    // Devuelve el JasperReport ya compilado, usando el cache.
    // computeIfAbsent garantiza que solo se compila una vez aunque haya
    // múltiples llamadas concurrentes.
    private JasperReport compile(String classpathPath) {
        return cache.computeIfAbsent(classpathPath, this::compileFromClasspath);
    }

    // Lee el .jrxml del classpath y lo compila a JasperReport (memoria).
    private JasperReport compileFromClasspath(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) throw new RuntimeException("Recurso no encontrado: " + path);
            return JasperCompileManager.compileReport(is);
        } catch (Exception e) {
            throw new RuntimeException("Error compilando " + path + ": " + e.getMessage(), e);
        }
    }
}
