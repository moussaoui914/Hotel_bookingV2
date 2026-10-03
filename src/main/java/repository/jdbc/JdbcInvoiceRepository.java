package repository.jdbc;

import db.DatabaseConnection;
import model.Invoice;
import repository.InvoiceRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JdbcInvoiceRepository implements InvoiceRepository {

    @Override
    public void save(Invoice invoice) {
        String sql = """
            INSERT INTO invoices (id, invoice_number, reservation_id, payment_id,
                                  subtotal_ht, tva_rate, tva_amount, total_ttc,
                                  issue_date, due_date, is_paid, notes, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, invoice.getId());
            ps.setString(2, invoice.getInvoiceNumber());
            ps.setObject(3, invoice.getReservationId());
            ps.setObject(4, invoice.getPaymentId());
            ps.setBigDecimal(5, invoice.getSubtotalHt());
            ps.setBigDecimal(6, invoice.getTvaRate());
            ps.setBigDecimal(7, invoice.getTvaAmount());
            ps.setBigDecimal(8, invoice.getTotalTtc());
            ps.setDate(9, Date.valueOf(invoice.getIssueDate()));
            ps.setDate(10, Date.valueOf(invoice.getDueDate()));
            ps.setBoolean(11, invoice.isPaid());
            ps.setString(12, invoice.getNotes());
            ps.setTimestamp(13, Timestamp.valueOf(invoice.getCreatedAt()));
            ps.setTimestamp(14, Timestamp.valueOf(invoice.getUpdatedAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving invoice: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Invoice invoice) {
        String sql = """
            UPDATE invoices SET subtotal_ht = ?, tva_rate = ?, tva_amount = ?,
                                total_ttc = ?, due_date = ?, is_paid = ?, notes = ?, updated_at = ?
            WHERE id = ?
        """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, invoice.getSubtotalHt());
            ps.setBigDecimal(2, invoice.getTvaRate());
            ps.setBigDecimal(3, invoice.getTvaAmount());
            ps.setBigDecimal(4, invoice.getTotalTtc());
            ps.setDate(5, Date.valueOf(invoice.getDueDate()));
            ps.setBoolean(6, invoice.isPaid());
            ps.setString(7, invoice.getNotes());
            ps.setTimestamp(8, Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setObject(9, invoice.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating invoice: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Invoice> findById(UUID id) {
        String sql = "SELECT * FROM invoices WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding invoice: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Invoice> findByInvoiceNumber(String invoiceNumber) {
        String sql = "SELECT * FROM invoices WHERE invoice_number = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, invoiceNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding invoice by number: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Invoice> findByReservationId(UUID reservationId) {
        String sql = "SELECT * FROM invoices WHERE reservation_id = ? ORDER BY issue_date DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding invoice by reservation: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Invoice> findAll() {
        String sql = "SELECT * FROM invoices ORDER BY issue_date DESC";
        List<Invoice> invoices = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) invoices.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all invoices: " + e.getMessage(), e);
        }
        return invoices;
    }

    @Override
    public List<Invoice> findUnpaidInvoices() {
        String sql = "SELECT * FROM invoices WHERE is_paid = FALSE ORDER BY due_date";
        List<Invoice> invoices = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) invoices.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error finding unpaid invoices: " + e.getMessage(), e);
        }
        return invoices;
    }

    @Override
    public void deleteById(UUID id) {
        String sql = "DELETE FROM invoices WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting invoice: " + e.getMessage(), e);
        }
    }

    private Invoice mapRow(ResultSet rs) throws SQLException {
        Invoice inv = new Invoice();
        inv.setId(rs.getObject("id", UUID.class));
        inv.setInvoiceNumber(rs.getString("invoice_number"));
        inv.setReservationId(rs.getObject("reservation_id", UUID.class));
        inv.setPaymentId(rs.getObject("payment_id", UUID.class));
        inv.setSubtotalHt(rs.getBigDecimal("subtotal_ht"));
        inv.setTvaRate(rs.getBigDecimal("tva_rate"));
        inv.setTvaAmount(rs.getBigDecimal("tva_amount"));
        inv.setTotalTtc(rs.getBigDecimal("total_ttc"));
        inv.setIssueDate(rs.getDate("issue_date").toLocalDate());
        inv.setDueDate(rs.getDate("due_date").toLocalDate());
        inv.setPaid(rs.getBoolean("is_paid"));
        inv.setNotes(rs.getString("notes"));
        inv.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        inv.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return inv;
    }
}