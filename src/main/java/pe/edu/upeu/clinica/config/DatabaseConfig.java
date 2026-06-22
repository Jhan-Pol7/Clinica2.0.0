package pe.edu.upeu.clinica.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Properties;

// Configuración de la base de datos con HikariCP (pool de conexiones) y
// ejecución automática del script DDL de H2 al arrancar.
// Toda la configuración (URL, usuario, tamaño del pool) se lee desde el
// archivo application.properties que está en el classpath.
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);
    private static HikariDataSource dataSource;
    private static Server h2Server;   // servidor TCP de H2 (null si está deshabilitado)

    // Constructor privado: esta clase es de tipo utilidad estática.
    private DatabaseConfig() {}

    // Inicializa el pool de conexiones a la BD una sola vez (singleton).
    // Si ya está inicializado, no hace nada.
    public static synchronized void init() {
        if (dataSource != null && !dataSource.isClosed()) {
            return;
        }
        Properties props = loadProperties("application.properties");

        // Si está habilitado, levanta un servidor H2 TCP en un puerto fijo ANTES
        // de crear el pool. Esto permite que clientes externos (DBeaver) se
        // conecten a la misma base de datos mientras la aplicación está en uso.
        boolean h2ServerEnabled = Boolean.parseBoolean(props.getProperty("db.h2server.enabled", "false"));
        if (h2ServerEnabled) {
            String port = props.getProperty("db.h2server.port", "9092");
            boolean allowOthers = Boolean.parseBoolean(props.getProperty("db.h2server.allowOthers", "true"));
            try {
                java.util.List<String> args = new java.util.ArrayList<>();
                args.add("-tcp");
                args.add("-tcpPort");
                args.add(port);
                if (allowOthers) args.add("-tcpAllowOthers");
                // Permite crear la BD si aún no existe (H2 lo bloquea por defecto en
                // conexiones por servidor desde la versión 1.4.198 por seguridad).
                args.add("-ifNotExists");
                h2Server = Server.createTcpServer(args.toArray(new String[0])).start();
                log.info("Servidor H2 TCP iniciado en el puerto {} (DBeaver puede conectarse)", port);
            } catch (SQLException e) {
                // Si el puerto ya está ocupado, asumimos que otra instancia de la app
                // (o un servidor previo) ya tiene H2 escuchando en ese puerto: en vez de
                // abortar el arranque, continuamos y nos conectamos a ese servidor.
                String msg = e.getMessage();
                if (msg != null && (msg.contains("port may be in use") || msg.contains("Address already in use"))) {
                    log.warn("El puerto {} ya está en uso; se reutilizará el servidor H2 existente.", port);
                } else {
                    throw new RuntimeException("No se pudo iniciar el servidor H2 TCP en el puerto " + port, e);
                }
            }
        }

        // Configura HikariCP con los valores leídos de application.properties.
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(props.getProperty("db.url"));
        config.setDriverClassName(props.getProperty("db.driver", "org.h2.Driver"));
        config.setUsername(props.getProperty("db.username", "sa"));
        config.setPassword(props.getProperty("db.password", ""));
        config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.maximumPoolSize", "5")));
        config.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.minimumIdle", "1")));
        config.setConnectionTimeout(Long.parseLong(props.getProperty("db.pool.connectionTimeout", "30000")));
        config.setIdleTimeout(Long.parseLong(props.getProperty("db.pool.idleTimeout", "600000")));
        config.setMaxLifetime(Long.parseLong(props.getProperty("db.pool.maxLifetime", "1800000")));
        config.setPoolName("ClinicaPool");

        dataSource = new HikariDataSource(config);
        log.info("HikariCP pool '{}' iniciado — url: {}", config.getPoolName(), props.getProperty("db.url"));

        // Si db.ddl.auto=true, ejecuta el script de creación de tablas y carga datos iniciales.
        boolean ddlAuto = Boolean.parseBoolean(props.getProperty("db.ddl.auto", "true"));
        if (ddlAuto) {
            String script = props.getProperty("db.ddl.script", "schema_clinica.sql");
            runDdlScript(script);
            syncIdentitySequences();
        }
    }

    /**
     * Recalibra todas las secuencias IDENTITY a MAX(pk)+1 para evitar
     * colisiones cuando los seeds usan ids explícitos (MERGE INTO) que
     * NO avanzan la secuencia automática.
     */
    private static void syncIdentitySequences() {
        String[][] tablas = {
                {"upeu_emisor",          "id_emisor"},
                {"upeu_perfil",          "id_perfil"},
                {"upeu_usuario",         "id_usuario"},
                {"upeu_paciente",        "id_paciente"},
                {"upeu_especialidad",    "id_especialidad"},
                {"upeu_medico",          "id_medico"},
                {"upeu_horario",         "id_horario"},
                {"upeu_enfermero",       "id_enfermero"},
                {"upeu_cita",            "id_cita"},
                {"upeu_triaje",          "id_triaje"},
                {"upeu_consulta",        "id_consulta"},
                {"upeu_receta",          "id_receta"},
                {"upeu_receta_detalle",  "id_receta_detalle"}
        };
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            for (String[] t : tablas) {
                try {
                    long next;
                    try (java.sql.ResultSet rs = stmt.executeQuery(
                            "SELECT COALESCE(MAX(" + t[1] + "), 0) + 1 FROM " + t[0])) {
                        rs.next();
                        next = rs.getLong(1);
                    }
                    // Mínimo 100 para dejar margen sobre seeds 1-5.
                    if (next < 100) next = 100;
                    stmt.execute("ALTER TABLE " + t[0]
                            + " ALTER COLUMN " + t[1]
                            + " RESTART WITH " + next);
                } catch (SQLException ex) {
                    System.err.println("[DDL] No se pudo recalibrar " + t[0] + ": " + ex.getMessage());
                }
            }
            System.out.println("[DDL] Secuencias IDENTITY sincronizadas");
        } catch (SQLException ex) {
            throw new RuntimeException("Error sincronizando secuencias", ex);
        }
    }

    public static DataSource getDataSource() {
        if (dataSource == null || dataSource.isClosed()) {
            init();
        }
        return dataSource;
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    public static synchronized void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            log.info("Cerrando HikariCP pool...");
            dataSource.close();
        }
        if (h2Server != null && h2Server.isRunning(false)) {
            log.info("Deteniendo servidor H2 TCP...");
            h2Server.stop();
        }
    }

    private static Properties loadProperties(String filename) {
        Properties props = new Properties();
        try (InputStream is = DatabaseConfig.class.getClassLoader().getResourceAsStream(filename)) {
            if (is == null) {
                throw new RuntimeException("No se encontró " + filename + " en el classpath");
            }
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Error leyendo " + filename, e);
        }
        return props;
    }

    private static void runDdlScript(String scriptName) {
        System.out.println("[DDL] Ejecutando script desde classpath: " + scriptName);
        try (InputStream is = DatabaseConfig.class.getClassLoader().getResourceAsStream(scriptName)) {
            if (is == null) {
                System.err.println("[DDL] Script '" + scriptName + "' NO ENCONTRADO en classpath — se omite");
                return;
            }
            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            String[] statements = sql.split(";");
            System.out.println("[DDL] Sentencias detectadas: " + statements.length);

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                int executed = 0, skipped = 0, errors = 0;
                for (String raw : statements) {
                    String s = raw.strip();
                    if (s.isEmpty()) continue;
                    // Quitar SOLO las líneas que son comentarios; el SQL real puede estar
                    // precedido de comentarios en el mismo bloque (no descartar el bloque).
                    String noComments = Arrays.stream(s.split("\n"))
                            .filter(line -> !line.strip().startsWith("--"))
                            .reduce("", (a, b) -> a + "\n" + b).strip();
                    if (noComments.isEmpty()) continue;
                    try {
                        stmt.execute(noComments);
                        executed++;
                    } catch (SQLException e) {
                        String msg = e.getMessage();
                        // Tolerar SOLO duplicados reales (ya existe / constraint duplicado)
                        boolean alreadyExists = msg != null && (
                                msg.contains("already exists")
                                || msg.contains("ya existe")
                                || msg.startsWith("Duplicate"));
                        if (alreadyExists) {
                            skipped++;
                        } else {
                            errors++;
                            System.err.println("[DDL] ERROR (state=" + e.getSQLState() + ", code=" + e.getErrorCode() + "): " +
                                    noComments.substring(0, Math.min(160, noComments.length())).replaceAll("\\s+", " ") +
                                    "\n       msg=" + msg);
                        }
                    }
                }
                System.out.println("[DDL] Completado — ejecutadas=" + executed
                        + ", omitidas=" + skipped + ", errores=" + errors);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error ejecutando script DDL: " + scriptName, e);
        }
    }
}
