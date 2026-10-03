package service;

import db.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReportService {

    public Map<String, Object> getGeneralStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        String sql = """
            SELECT 
                (SELECT COUNT(*) FROM reservations) AS total_reservations,
                (SELECT COUNT(*) FROM reservations WHERE status = 'CANCELLED') AS cancelled_reservations,
                (SELECT COUNT(*) FROM reservations WHERE status = 'CONFIRMED') AS active_reservations,
                (SELECT COALESCE(SUM(total_price), 0) FROM reservations WHERE status IN ('CONFIRMED','COMPLETED')) AS total_revenue,
                (SELECT COUNT(*) FROM users WHERE role = 'CLIENT') AS total_clients,
                (SELECT COUNT(*) FROM rooms) AS total_rooms
        """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                stats.put("Total Reservations", rs.getInt("total_reservations"));
                stats.put("Cancelled", rs.getInt("cancelled_reservations"));
                stats.put("Active", rs.getInt("active_reservations"));
                stats.put("Total Revenue (MAD)", rs.getBigDecimal("total_revenue"));
                stats.put("Total Clients", rs.getInt("total_clients"));
                stats.put("Total Rooms", rs.getInt("total_rooms"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error generating stats: " + e.getMessage(), e);
        }
        return stats;
    }

    public Map<String, BigDecimal> getMonthlyRevenue() {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        String sql = """
            SELECT TO_CHAR(created_at, 'YYYY-MM') AS month,
                   COALESCE(SUM(total_price), 0) AS revenue
            FROM reservations
            WHERE status IN ('CONFIRMED', 'COMPLETED')
            GROUP BY TO_CHAR(created_at, 'YYYY-MM')
            ORDER BY month DESC
        """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.put(rs.getString("month"), rs.getBigDecimal("revenue"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error generating monthly revenue: " + e.getMessage(), e);
        }
        return result;
    }

    public Map<String, Integer> getTopRooms() {
        Map<String, Integer> result = new LinkedHashMap<>();
        String sql = """
            SELECT room_number, COUNT(*) AS count
            FROM reservations
            WHERE status IN ('CONFIRMED', 'COMPLETED')
            GROUP BY room_number
            ORDER BY count DESC
            LIMIT 10
        """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.put(rs.getString("room_number"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error generating top rooms: " + e.getMessage(), e);
        }
        return result;
    }

    public BigDecimal getAverageBasket() {
        String sql = """
            SELECT COALESCE(AVG(total_price), 0) FROM reservations
            WHERE status IN ('CONFIRMED', 'COMPLETED')
        """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getBigDecimal(1);
        } catch (SQLException e) {
            throw new RuntimeException("Error calculating average basket: " + e.getMessage(), e);
        }
        return BigDecimal.ZERO;
    }
}