package pe.edu.upeu.clinica.config;

import pe.edu.upeu.clinica.controller.*;
import pe.edu.upeu.clinica.repository.*;
import pe.edu.upeu.clinica.service.*;
import pe.edu.upeu.clinica.service.impl.*;
import pe.edu.upeu.clinica.utils.ConsultaDNI;

import java.util.HashMap;
import java.util.Map;

/**
 * Contenedor de DI manual. Tres capas: Repositorios → Servicios → Controladores.
 * El FXMLLoader busca los controllers con setControllerFactory(context::getBean).
 *
 * Estado del registro:
 *   - Fase 1: Login + MainGui + menús dinámicos.
 *   - Fase 2: + 6 CRUDs (Especialidad, Enfermero, Paciente, Médico, Horario, Usuario).
 *   - Fase 3: + Flujo clínico (Cita, Check-in, Triaje, Consulta, Receta, Ticket).
 *   - Fase 4: pendiente (Reporte JasperReports).
 */
public class AppContext {

    private static AppContext instance;

    public static synchronized AppContext getInstance() {
        if (instance == null) instance = new AppContext();
        return instance;
    }

    private final Map<Class<?>, Object> contenedor = new HashMap<>();

    private AppContext() {
        registrarRepositorios();
        registrarServicios();
        registrarControladores();
    }

    // ── CAPA 1 — REPOSITORIOS ───────────────────────────────
    private void registrarRepositorios() {
        registrar(UsuarioRepository.class,        new UsuarioRepository());
        registrar(PerfilRepository.class,         new PerfilRepository());
        registrar(EspecialidadRepository.class,   new EspecialidadRepository());
        registrar(MedicoRepository.class,         new MedicoRepository());
        registrar(HorarioRepository.class,        new HorarioRepository());
        registrar(EnfermeroRepository.class,      new EnfermeroRepository());
        registrar(PacienteRepository.class,       new PacienteRepository());
        registrar(EmisorRepository.class,         new EmisorRepository());
        registrar(CitaRepository.class,           new CitaRepository());
        registrar(TriajeRepository.class,         new TriajeRepository());
        registrar(ConsultaRepository.class,       new ConsultaRepository());
        registrar(RecetaRepository.class,         new RecetaRepository());
        registrar(RecetaDetalleRepository.class,  new RecetaDetalleRepository());
    }

    // ── CAPA 2 — SERVICIOS ──────────────────────────────────
    private void registrarServicios() {
        registrar(ConsultaDNI.class,          new ConsultaDNI());
        registrar(IMenuMenuItemDao.class,     new MenuMenuItemDaoImp());
        registrar(IUsuarioService.class,      new UsuarioServiceImp(getBean(UsuarioRepository.class)));
        registrar(IEspecialidadService.class, new EspecialidadServiceImp(getBean(EspecialidadRepository.class)));
        registrar(IMedicoService.class,       new MedicoServiceImp(getBean(MedicoRepository.class)));
        registrar(IHorarioService.class,      new HorarioServiceImp(getBean(HorarioRepository.class)));
        registrar(IEnfermeroService.class,    new EnfermeroServiceImp(getBean(EnfermeroRepository.class)));
        registrar(IPacienteService.class,     new PacienteServiceImp(getBean(PacienteRepository.class)));
        registrar(ICitaService.class,         new CitaServiceImp(getBean(CitaRepository.class)));
        registrar(ITriajeService.class,
                new TriajeServiceImp(getBean(TriajeRepository.class), getBean(ICitaService.class)));
        registrar(IConsultaService.class,
                new ConsultaServiceImp(
                        getBean(ConsultaRepository.class),
                        getBean(RecetaRepository.class),
                        getBean(RecetaDetalleRepository.class),
                        getBean(ICitaService.class)));
        registrar(IRecetaService.class,
                new RecetaServiceImp(getBean(RecetaRepository.class), getBean(RecetaDetalleRepository.class)));
        registrar(ITicketService.class,       new TicketServiceImp(getBean(EmisorRepository.class)));
        registrar(IReporteService.class,
                new ReporteServiceImp(
                        getBean(ITicketService.class),
                        getBean(ICitaService.class),
                        getBean(IRecetaService.class),
                        getBean(EmisorRepository.class)));
    }

    // ── CAPA 3 — CONTROLADORES ──────────────────────────────
    private void registrarControladores() {
        registrar(LoginController.class,   new LoginController(getBean(IUsuarioService.class)));
        registrar(MainGuiController.class, new MainGuiController(getBean(IMenuMenuItemDao.class)));

        // CRUDs (Fase 2)
        registrar(MainEspecialidadController.class,
                new MainEspecialidadController(getBean(IEspecialidadService.class)));
        registrar(MainEnfermeroController.class,
                new MainEnfermeroController(getBean(IEnfermeroService.class)));
        registrar(MainPacienteController.class,
                new MainPacienteController(getBean(IPacienteService.class)));
        registrar(MainMedicoController.class,
                new MainMedicoController(getBean(IMedicoService.class), getBean(IEspecialidadService.class)));
        registrar(MainHorarioController.class,
                new MainHorarioController(getBean(IHorarioService.class), getBean(IMedicoService.class)));
        registrar(MainUsuarioController.class,
                new MainUsuarioController(getBean(IUsuarioService.class), getBean(PerfilRepository.class)));

        // Flujo clínico (Fase 3)
        registrar(MainCitaController.class,
                new MainCitaController(
                        getBean(ICitaService.class),
                        getBean(IPacienteService.class),
                        getBean(IMedicoService.class),
                        getBean(IEspecialidadService.class),
                        getBean(UsuarioRepository.class)));
        registrar(MainCheckinController.class,
                new MainCheckinController(getBean(ICitaService.class)));
        registrar(MainTriajeController.class,
                new MainTriajeController(getBean(ITriajeService.class),
                        getBean(ICitaService.class), getBean(EnfermeroRepository.class)));
        registrar(MainConsultaController.class,
                new MainConsultaController(getBean(IConsultaService.class),
                        getBean(ITriajeService.class), getBean(ICitaService.class)));
        registrar(MainTicketController.class,
                new MainTicketController(getBean(ITicketService.class), getBean(IRecetaService.class),
                        getBean(ICitaService.class), getBean(IConsultaService.class),
                        getBean(IReporteService.class)));
        registrar(MainRecetaController.class,
                new MainRecetaController(getBean(IRecetaService.class), getBean(IReporteService.class)));

        // Reportes (Fase 4)
        registrar(MainReporteController.class,
                new MainReporteController(
                        getBean(IReporteService.class),
                        getBean(IEspecialidadService.class),
                        getBean(IMedicoService.class)));

        registrar(PlaceholderController.class, new PlaceholderController());
    }

    private void registrar(Class<?> tipo, Object bean) { contenedor.put(tipo, bean); }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> tipo) {
        Object bean = contenedor.get(tipo);
        if (bean == null) {
            bean = contenedor.values().stream()
                    .filter(b -> tipo.isAssignableFrom(b.getClass()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException(
                            "Bean no encontrado: " + tipo.getName() +
                                    "\n→ ¿Lo registraste en AppContext?"));
        }
        return (T) bean;
    }
}
