package pe.edu.upeu.clinica.controller;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.net.URL;
import pe.edu.upeu.clinica.config.AppContext;
import pe.edu.upeu.clinica.dto.MenuMenuItenTO;
import pe.edu.upeu.clinica.dto.SessionManager;
import pe.edu.upeu.clinica.service.IMenuMenuItemDao;
import pe.edu.upeu.clinica.utils.UtilsX;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.prefs.Preferences;

// Controlador de la ventana principal. Pinta dinámicamente el MenuBar según
// el perfil del usuario logueado, expone selector de idioma y abre cada
// vista del proyecto en un Tab del TabPane.
public class MainGuiController {

    // Servicio que arma la matriz de accesos por perfil.
    private final IMenuMenuItemDao mmiDao;
    public MainGuiController(IMenuMenuItemDao mmiDao) { this.mmiDao = mmiDao; }

    // Preferencias del usuario en el SO (Windows registry / Linux dotfile).
    // Aquí persistimos el idioma elegido entre arranques de la app.
    private final Preferences userPrefs = Preferences.userRoot();
    private final UtilsX util = new UtilsX();
    private Properties myresources = new Properties();

    // Componentes FXML — los nombres deben coincidir con fx:id en maingui.fxml.
    @FXML private TabPane tabPaneFx;     // contenedor central donde se cargan las pantallas
    @FXML private BorderPane bp;          // root del FXML
    @FXML private MenuBar menuBarFx;      // se reconstruye en cada graficarMenus()

    private List<MenuMenuItenTO> lista;   // items autorizados del perfil actual
    private Parent parent;                // cache temporal usada al redireccionar
    private Stage stage;                  // referencia al Stage actual

    // Menú "Idioma" con ComboBox para es/en.
    private final Menu menuIdioma = new Menu("Idioma");
    private final ComboBox<String> comboBoxIdioma = new ComboBox<>(
            javafx.collections.FXCollections.observableArrayList("Español", "Inglés"));
    private final CustomMenuItem customItemIdioma = new CustomMenuItem(comboBoxIdioma);

    // Menú "Cambiar Estilo" con ComboBox para elegir el tema visual de la app.
    // El tema elegido se aplica a la Scene y se persiste en Preferences ("ESTILOX").
    private final Menu menuEstilo = new Menu("Cambiar Estilo");
    private final ComboBox<String> comboBoxEstilo = new ComboBox<>(
            javafx.collections.FXCollections.observableArrayList(
                    "Estilo por Defecto", "Estilo Oscuro",
                    "Estilo Azul", "Estilo Verde", "Estilo Rosado"));
    private final CustomMenuItem customItemEstilo = new CustomMenuItem(comboBoxEstilo);

    @FXML
    public void initialize() {
        // Pequeño delay para que la Scene esté lista antes de obtener el Stage.
        // (No podemos obtenerlo directamente porque initialize() corre antes del
        // primer pulso de layout.)
        PauseTransition pause = new PauseTransition(Duration.millis(300));
        pause.setOnFinished(e -> {
            if (tabPaneFx.getScene() != null && tabPaneFx.getScene().getWindow() != null) {
                stage = (Stage) tabPaneFx.getScene().getWindow();
            }
            // Reaplicar el tema persistido (si hay) ahora que la Scene ya existe.
            String estiloGuardado = userPrefs.get("ESTILOX", "Estilo por Defecto");
            comboBoxEstilo.getSelectionModel().select(estiloGuardado);
            aplicarEstilo(estiloGuardado);
        });
        pause.play();
        // Construir los menús según el perfil del usuario en sesión.
        graficarMenus();
        bp.setCenter(tabPaneFx);
    }

    // Llama al DAO con perfil + idioma activos para obtener los items autorizados.
    private List<MenuMenuItenTO> listaAccesos() {
        myresources = util.detectLanguage(userPrefs.get("IDIOMAX", "es"));
        return mmiDao.listaAccesos(SessionManager.getInstance().getUserPerfil(), myresources);
    }

    // Pre-cálculo: cuántos Menu y MenuItem hay que crear. Permite dimensionar
    // los arrays antes de iterar (mismo patrón que SysVentas).
    public int[] contarMenuMunuItem(List<MenuMenuItenTO> data) {
        int menui = 0, menuitem = 0;
        String menuN = "";
        for (MenuMenuItenTO mmiItem : data) {
            if (!mmiItem.getMenunombre().equals(menuN)) {
                menuN = mmiItem.getMenunombre();
                menui++;
            }
            if (!mmiItem.getMenuitemnombre().equals("")) menuitem++;
        }
        return new int[]{menui, menuitem};
    }

    // Construye el MenuBar entero a partir de la lista de accesos.
    // Se llama en initialize() y al cambiar de idioma para refrescar etiquetas.
    private void graficarMenus() {
        lista = listaAccesos();
        int[] mmi = contarMenuMunuItem(lista);
        Menu[] menu = new Menu[mmi[0]];
        MenuItem[] menuItem = new MenuItem[mmi[1]];
        menuBarFx = new MenuBar();
        MenuItemListener d = new MenuItemListener();
        MenuListener m = new MenuListener();
        String menuN = "";
        int menui = 0, menuitem = 0;
        char conti = 'N';                       // 'S' = continuar agregando al mismo Menu

        for (MenuMenuItenTO mmix : lista) {
            if (!mmix.getMenunombre().equals(menuN)) {
                // Cambio de Menu padre — crear un Menu nuevo en el bar.
                menu[menui] = new Menu(mmix.getMenunombre());
                menu[menui].setId("m" + mmix.getIdNombreObj());
                menu[menui].setOnShowing(m::menuSelected);
                if (!mmix.getMenuitemnombre().equals("")) {
                    // Primer item del Menu recién creado.
                    menuItem[menuitem] = new MenuItem(mmix.getMenuitemnombre());
                    menuItem[menuitem].setId("mi" + mmix.getIdNombreObj());
                    menuItem[menuitem].setOnAction(d::handle);
                    menu[menui].getItems().add(menuItem[menuitem]);
                    menuitem++;
                }
                menuBarFx.getMenus().add(menu[menui]);
                menuN = mmix.getMenunombre();
                conti = 'N';
                menui++;
            } else {
                conti = 'S';
            }
            // Item adicional dentro del mismo Menu padre.
            if (!mmix.getMenuitemnombre().equals("")
                    && mmix.getMenunombre().equals(menuN) && conti == 'S') {
                menuItem[menuitem] = new MenuItem(mmix.getMenuitemnombre());
                menuItem[menuitem].setId("mi" + mmix.getIdNombreObj());
                menuItem[menuitem].setOnAction(d::handle);
                menu[menui - 1].getItems().add(menuItem[menuitem]);
                menuitem++;
            }
        }

        // Menú extra "Cambiar Estilo".
        comboBoxEstilo.setOnAction(e -> cambiarEstilo());
        customItemEstilo.setHideOnClick(false);
        menuEstilo.getItems().clear();
        menuEstilo.getItems().add(customItemEstilo);
        menuBarFx.getMenus().addAll(menuEstilo);

        // Menú extra "Idioma".
        comboBoxIdioma.setOnAction(e -> cambiarIdioma());
        customItemIdioma.setHideOnClick(false);
        menuIdioma.getItems().clear();
        menuIdioma.getItems().add(customItemIdioma);
        menuBarFx.getMenus().addAll(menuIdioma);

        bp.setTop(menuBarFx);
    }

    // Cambia el tema visual según la opción elegida en el ComboBox y lo persiste.
    private void cambiarEstilo() {
        String estilo = comboBoxEstilo.getSelectionModel().getSelectedItem();
        if (estilo == null) return;
        userPrefs.put("ESTILOX", estilo);
        aplicarEstilo(estilo);
    }

    // Aplica el CSS correspondiente a la Scene actual. "Estilo por Defecto"
    // simplemente limpia las hojas de estilo y deja la apariencia nativa.
    private void aplicarEstilo(String estilo) {
        Scene escena = bp.getScene();
        if (escena == null) return;
        escena.getStylesheets().clear();
        String css = switch (estilo == null ? "" : estilo) {
            case "Estilo Oscuro" -> "/css/estilo-oscuro.css";
            case "Estilo Azul"   -> "/css/estilo-azul.css";
            case "Estilo Verde"  -> "/css/estilo-verde.css";
            case "Estilo Rosado" -> "/css/estilo-rosado.css";
            default -> null;   // Estilo por Defecto
        };
        if (css != null) {
            URL res = getClass().getResource(css);
            if (res != null) escena.getStylesheets().add(res.toExternalForm());
        }
    }

    // Guarda el idioma elegido y vuelve a pintar el MenuBar con las etiquetas nuevas.
    private void cambiarIdioma() {
        String idiomaSeleccionado = comboBoxIdioma.getSelectionModel().getSelectedItem();
        switch (idiomaSeleccionado == null ? "Español" : idiomaSeleccionado) {
            case "Inglés":  userPrefs.put("IDIOMAX", "en"); break;
            case "Español":
            default:        userPrefs.put("IDIOMAX", "es");
        }
        graficarMenus();
    }

    // Listener interno que decide qué hacer al hacer click en un MenuItem.
    // El mapa menuConfig viene del DAO y nos dice qué FXML cargar y cómo.
    class MenuItemListener {
        final Map<String, String[]> menuConfig;
        MenuItemListener() { menuConfig = mmiDao.accesosAutorizados(lista); }

        public void handle(ActionEvent e) {
            String id = ((MenuItem) e.getSource()).getId();
            if (menuConfig.containsKey(id)) {
                String[] cfg = menuConfig.get(id);
                // cfg = [fxmlPath, tituloTab, tipo("S"|"T")]
                if ("S".equals(cfg[2])) {
                    // Tipo S = redireccionar (típico de "Salir" -> login).
                    redireccionar(cfg[0]);
                } else {
                    // Tipo T = abrir como pestaña dentro del TabPane.
                    abrirTabConFXML(cfg[0], cfg[1]);
                }
            }
        }

        // Carga un FXML como un Tab dentro del TabPane central.
        // Reemplaza la pestaña anterior (siempre se muestra solo una a la vez).
        private void abrirTabConFXML(String fxmlPath, String tituloTab) {
            try {
                AppContext ctx = AppContext.getInstance();
                URL res = getClass().getResource(fxmlPath);
                Parent root;
                if (res == null) {
                    // Tolerante: si el FXML no existe aún (vistas futuras),
                    // mostramos un mensaje en vez de crashear.
                    javafx.scene.control.Label aviso = new javafx.scene.control.Label(
                            "Vista en construcción: " + fxmlPath);
                    aviso.setStyle("-fx-padding: 30; -fx-font-size: 14;");
                    root = new javafx.scene.layout.StackPane(aviso);
                } else {
                    FXMLLoader loader = new FXMLLoader(res);
                    // Inyectamos los controllers a través del AppContext.
                    loader.setControllerFactory(ctx::getBean);
                    root = loader.load();
                }
                ScrollPane scrollPane = new ScrollPane(root);
                scrollPane.setFitToWidth(true);
                scrollPane.setFitToHeight(true);
                Tab newTab = new Tab(tituloTab, scrollPane);
                tabPaneFx.getTabs().clear();
                tabPaneFx.getTabs().add(newTab);
            } catch (IOException e) {
                throw new RuntimeException("Error al cargar FXML: " + fxmlPath, e);
            }
        }

        // Cambia toda la Scene (no es un Tab) — se usa para "Salir" y volver al login.
        private void redireccionar(String fxmlPath) {
            tabPaneFx.getTabs().clear();
            try {
                AppContext ctx = AppContext.getInstance();
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlPath));
                fxmlLoader.setControllerFactory(ctx::getBean);
                parent = fxmlLoader.load();
                Scene scene = new Scene(parent);
                if (stage == null && bp.getScene() != null) stage = (Stage) bp.getScene().getWindow();
                stage.sizeToScene();
                stage.setScene(scene);
                stage.centerOnScreen();
                stage.setTitle("Clinica2.0 — Más Cerca de Dios");
                stage.setResizable(false);
                stage.show();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    // Hook libre para reaccionar a la apertura de un Menu (mismo patrón que SysVentas).
    class MenuListener {
        public void menuSelected(Event e) {
            // sin uso por ahora — punto de extensión para auditoría / analytics
        }
    }
}
