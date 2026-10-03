package db;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class DatabaseConnection {

    private static volatile DatabaseConnection instance;
    private static String url;
    private static String user;
    private static String password;

    private DatabaseConnection() {
        try {
            // 1) Charger explicitement le driver PostgreSQL
            Class.forName("org.postgresql.Driver");

            // 2) Priorité aux variables d'environnement (Docker)
            String urlEnv  = System.getenv("DB_URL");
            String userEnv = System.getenv("DB_USER");
            String pwdEnv  = System.getenv("DB_PASSWORD");

            // 3) Fallback sur db.properties (exécution locale)
            if (urlEnv == null || userEnv == null || pwdEnv == null) {
                Properties props = new Properties();
                try (InputStream input = getClass().getClassLoader()
                        .getResourceAsStream("db.properties")) {
                    if (input == null) {
                        throw new IllegalStateException(
                                "db.properties introuvable et variables d'environnement absentes");
                    }
                    props.load(input);
                }
                if (urlEnv  == null) urlEnv  = props.getProperty("db.url");
                if (userEnv == null) userEnv = props.getProperty("db.user");
                if (pwdEnv  == null) pwdEnv  = props.getProperty("db.password");
            }

            if (urlEnv == null || userEnv == null || pwdEnv == null) {
                throw new IllegalStateException(
                        "db.url / db.user / db.password manquant(s)");
            }

            // Stocker dans les champs statiques
            url = urlEnv;
            user = userEnv;
            password = pwdEnv;

            // Debug
            System.out.println("========================================");
            System.out.println("=== DEBUG CONNEXION DB ===");
            System.out.println("URL:      " + url);
            System.out.println("USER:     " + user);
            System.out.println("PASSWORD: ***" + password.substring(Math.max(0, password.length() - 3)));
            System.out.println("========================================");

        } catch (Exception e) {
            throw new RuntimeException(
                    "Erreur critique de configuration JDBC : " + e.getMessage(), e);
        }
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Retourne une NOUVELLE connexion à chaque appel.
     * C'est la solution au problème "This connection has been closed".
     */
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (Exception e) {
            throw new RuntimeException("Erreur de connexion JDBC : " + e.getMessage(), e);
        }
    }

    /**
     * Ne fait rien : chaque appel à getConnection() crée une nouvelle connexion
     * qui sera fermée automatiquement par le try-with-resources de l'appelant.
     */
    public void closeConnection() {
        // Rien à faire
    }
}