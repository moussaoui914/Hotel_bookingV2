package config;

import db.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initialize() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                DO $$ BEGIN
                    CREATE TYPE user_role AS ENUM ('CLIENT', 'ADMIN');
                EXCEPTION WHEN duplicate_object THEN null;
                END $$;
            """);

            stmt.execute("""
                DO $$ BEGIN
                    CREATE TYPE room_type AS ENUM ('SINGLE', 'DOUBLE', 'SUITE');
                EXCEPTION WHEN duplicate_object THEN null;
                END $$;
            """);

            stmt.execute("""
                DO $$ BEGIN
                    CREATE TYPE room_status AS ENUM ('AVAILABLE', 'MAINTENANCE');
                EXCEPTION WHEN duplicate_object THEN null;
                END $$;
            """);

            stmt.execute("""
                DO $$ BEGIN
                    CREATE TYPE reservation_status AS ENUM ('CONFIRMED', 'CANCELLED', 'COMPLETED');
                EXCEPTION WHEN duplicate_object THEN null;
                END $$;
            """);

            stmt.execute("""
                DO $$ BEGIN
                    CREATE TYPE payment_method AS ENUM ('CASH', 'CREDIT_CARD', 'DEBIT_CARD', 'BANK_TRANSFER', 'ONLINE');
                EXCEPTION WHEN duplicate_object THEN null;
                END $$;
            """);

            stmt.execute("""
                DO $$ BEGIN
                    CREATE TYPE payment_status AS ENUM ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED', 'PARTIALLY_REFUNDED');
                EXCEPTION WHEN duplicate_object THEN null;
                END $$;
            """);

            System.out.println("Database types initialized successfully.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database: " + e.getMessage(), e);
        }
    }
}