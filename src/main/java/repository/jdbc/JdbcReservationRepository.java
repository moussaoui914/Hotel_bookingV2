package repository.jdbc;

import db.DatabaseConnection;
import model.Reservation;
import model.enums.PaymentMethod;
import model.enums.ReservationStatus;
import repository.ReservationRepository;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JdbcReservationRepository implements ReservationRepository {

    @Override
    public void save(Reservation reservation) {
        String sql = """
            INSERT INTO reservations (id, reservation_code, user_id, room_number, check_in, check_out,
                                      number_of_guests, number_of_nights, base_price, total_price,
                                      status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::reservation_status, ?, ?)
        """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, reservation.getId());
            ps.setString(2, reservation.getReservationCode());
            ps.setObject(3, reservation.getUserId());
            ps.setString(4, reservation.getRoomNumber());
            ps.setDate(5, Date.valueOf(reservation.getCheckIn()));
            ps.setDate(6, Date.valueOf(reservation.getCheckOut()));
            ps.setInt(7, reservation.getNumberOfGuests());
            ps.setInt(8, reservation.getNumberOfNights());
            ps.setBigDecimal(9, reservation.getBasePrice());
            ps.setBigDecimal(10, reservation.getTotalPrice());
            ps.setString(11, reservation.getStatus().name());
            ps.setTimestamp(12, Timestamp.valueOf(reservation.getCreatedAt()));
            ps.setTimestamp(13, Timestamp.valueOf(reservation.getUpdatedAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving reservation: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Reservation reservation) {
        String sql = """
            UPDATE reservations SET room_number = ?, check_in = ?, check_out = ?,
                                    number_of_guests = ?, number_of_nights = ?,
                                    base_price = ?, total_price = ?, status = ?::reservation_status,
                                    updated_at = ?
            WHERE id = ?
        """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reservation.getRoomNumber());
            ps.setDate(2, Date.valueOf(reservation.getCheckIn()));
            ps.setDate(3, Date.valueOf(reservation.getCheckOut()));
            ps.setInt(4, reservation.getNumberOfGuests());
            ps.setInt(5, reservation.getNumberOfNights());
            ps.setBigDecimal(6, reservation.getBasePrice());
            ps.setBigDecimal(7, reservation.getTotalPrice());
            ps.setString(8, reservation.getStatus().name());
            ps.setTimestamp(9, Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setObject(10, reservation.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating reservation: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Reservation> findById(UUID id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding reservation by id: " + e.getMessage(), e);
        }
        return Optional.empty();
    }




    @Override
    public Optional<Reservation> findByCode(String code) {
        String sql = "SELECT * FROM reservations WHERE reservation_code = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding reservation by code: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Reservation> findByUserId(UUID userId) {
        String sql = "SELECT * FROM reservations WHERE user_id = ? ORDER BY created_at DESC";
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) reservations.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding reservations by user: " + e.getMessage(), e);
        }
        return reservations;
    }

    @Override
    public List<Reservation> findByRoomNumber(String roomNumber) {
        String sql = "SELECT * FROM reservations WHERE room_number = ? ORDER BY check_in";
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomNumber);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) reservations.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding reservations by room: " + e.getMessage(), e);
        }
        return reservations;
    }

    @Override
    public List<Reservation> findAll() {
        String sql = "SELECT * FROM reservations ORDER BY created_at DESC";
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) reservations.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all reservations: " + e.getMessage(), e);
        }
        return reservations;
    }

    @Override
    public List<Reservation> findActiveReservationsByRoom(String roomNumber) {
        String sql = """
            SELECT * FROM reservations 
            WHERE room_number = ? AND status = 'CONFIRMED'
            ORDER BY check_in
        """;
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomNumber);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) reservations.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding active reservations: " + e.getMessage(), e);
        }
        return reservations;
    }

    @Override
    public boolean isRoomAvailableForDates(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return isRoomAvailableForDatesExcluding(roomNumber, checkIn, checkOut, null);
    }

    @Override
    public boolean isRoomAvailableForDatesExcluding(String roomNumber, LocalDate checkIn, LocalDate checkOut, UUID excludeId) {
        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(*) FROM reservations
            WHERE room_number = ?
              AND status = 'CONFIRMED'
              AND check_in < ?
              AND check_out > ?
        """);

        if (excludeId != null) {
            sql.append(" AND id != ?");
        }

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setString(1, roomNumber);
            ps.setDate(2, Date.valueOf(checkOut));
            ps.setDate(3, Date.valueOf(checkIn));
            if (excludeId != null) ps.setObject(4, excludeId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) == 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking room availability: " + e.getMessage(), e);
        }
        return false;
    }

    @Override
    public List<Reservation> findByDateRange(LocalDate start, LocalDate end) {
        String sql = """
            SELECT * FROM reservations 
            WHERE check_in >= ? AND check_out <= ?
            ORDER BY check_in
        """;
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(start));
            ps.setDate(2, Date.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) reservations.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding reservations by date range: " + e.getMessage(), e);
        }
        return reservations;
    }

    @Override
    public void deleteById(UUID id) {
        String sql = "DELETE FROM reservations WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting reservation: " + e.getMessage(), e);
        }
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getObject("id", UUID.class));
        r.setReservationCode(rs.getString("reservation_code"));
        r.setUserId(rs.getObject("user_id", UUID.class));
        r.setRoomNumber(rs.getString("room_number"));
        r.setCheckIn(rs.getDate("check_in").toLocalDate());
        r.setCheckOut(rs.getDate("check_out").toLocalDate());
        r.setNumberOfGuests(rs.getInt("number_of_guests"));
        r.setNumberOfNights(rs.getInt("number_of_nights"));
        r.setBasePrice(rs.getBigDecimal("base_price"));
        r.setTotalPrice(rs.getBigDecimal("total_price"));
        r.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        r.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return r;
    }
}