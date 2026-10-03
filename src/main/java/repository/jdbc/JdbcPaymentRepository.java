package repository.jdbc;

import db.DatabaseConnection;
import model.Payment;
import model.enums.PaymentMethod;
import model.enums.PaymentStatus;
import repository.PaymentRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JdbcPaymentRepository implements PaymentRepository {

    @Override
    public void save(Payment payment) {
        String sql = """
            INSERT INTO payments (id, reservation_id, amount, payment_method, status,
                                  transaction_ref, payment_date, refund_amount, refund_date,
                                  notes, created_at, updated_at)
            VALUES (?, ?, ?, ?::payment_method, ?::payment_status, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, payment.getId());
            ps.setObject(2, payment.getReservationId());
            ps.setBigDecimal(3, payment.getAmount());
            ps.setString(4, payment.getPaymentMethod().name());
            ps.setString(5, payment.getStatus().name());
            ps.setString(6, payment.getTransactionRef());
            ps.setTimestamp(7, Timestamp.valueOf(payment.getPaymentDate()));
            ps.setBigDecimal(8, payment.getRefundAmount());
            if (payment.getRefundDate() != null) ps.setTimestamp(9, Timestamp.valueOf(payment.getRefundDate()));
            else ps.setNull(9, Types.TIMESTAMP);
            ps.setString(10, payment.getNotes());
            ps.setTimestamp(11, Timestamp.valueOf(payment.getCreatedAt()));
            ps.setTimestamp(12, Timestamp.valueOf(payment.getUpdatedAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving payment: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Payment payment) {
        String sql = """
            UPDATE payments SET amount = ?, payment_method = ?::payment_method,
                                status = ?::payment_status, transaction_ref = ?,
                                refund_amount = ?, refund_date = ?, notes = ?, updated_at = ?
            WHERE id = ?
        """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, payment.getAmount());
            ps.setString(2, payment.getPaymentMethod().name());
            ps.setString(3, payment.getStatus().name());
            ps.setString(4, payment.getTransactionRef());
            ps.setBigDecimal(5, payment.getRefundAmount());
            if (payment.getRefundDate() != null) ps.setTimestamp(6, Timestamp.valueOf(payment.getRefundDate()));
            else ps.setNull(6, Types.TIMESTAMP);
            ps.setString(7, payment.getNotes());
            ps.setTimestamp(8, Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setObject(9, payment.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating payment: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        String sql = "SELECT * FROM payments WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding payment: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Payment> findByReservationId(UUID reservationId) {
        String sql = "SELECT * FROM payments WHERE reservation_id = ? ORDER BY payment_date DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding payment by reservation: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Payment> findAll() {
        String sql = "SELECT * FROM payments ORDER BY payment_date DESC";
        List<Payment> payments = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) payments.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all payments: " + e.getMessage(), e);
        }
        return payments;
    }

    @Override
    public void deleteById(UUID id) {
        String sql = "DELETE FROM payments WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting payment: " + e.getMessage(), e);
        }
    }

    private Payment mapRow(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setId(rs.getObject("id", UUID.class));
        p.setReservationId(rs.getObject("reservation_id", UUID.class));
        p.setAmount(rs.getBigDecimal("amount"));
        p.setPaymentMethod(PaymentMethod.valueOf(rs.getString("payment_method")));
        p.setStatus(PaymentStatus.valueOf(rs.getString("status")));
        p.setTransactionRef(rs.getString("transaction_ref"));
        p.setPaymentDate(rs.getTimestamp("payment_date").toLocalDateTime());
        p.setRefundAmount(rs.getBigDecimal("refund_amount"));
        Timestamp refundDate = rs.getTimestamp("refund_date");
        if (refundDate != null) p.setRefundDate(refundDate.toLocalDateTime());
        p.setNotes(rs.getString("notes"));
        p.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        p.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return p;
    }
}