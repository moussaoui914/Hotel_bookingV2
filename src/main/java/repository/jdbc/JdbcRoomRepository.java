package repository.jdbc;

import db.DatabaseConnection;
import model.Room;
import model.enums.RoomStatus;
import model.enums.RoomType;
import repository.RoomRepository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcRoomRepository implements RoomRepository {

    @Override
    public void save(Room room) {
        String sql = """
            INSERT INTO rooms (room_number, type, capacity, price_per_night, status, floor, description, created_at, updated_at)
            VALUES (?, ?::room_type, ?, ?, ?::room_status, ?, ?, ?, ?)
        """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, room.getRoomNumber());
            ps.setString(2, room.getType().name());
            ps.setInt(3, room.getCapacity());
            ps.setBigDecimal(4, room.getPricePerNight());
            ps.setString(5, room.getStatus().name());
            if (room.getFloor() != null) ps.setInt(6, room.getFloor()); else ps.setNull(6, Types.INTEGER);
            ps.setString(7, room.getDescription());
            ps.setTimestamp(8, Timestamp.valueOf(room.getCreatedAt()));
            ps.setTimestamp(9, Timestamp.valueOf(room.getUpdatedAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving room: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Room room) {
        String sql = """
            UPDATE rooms SET type = ?::room_type, capacity = ?, price_per_night = ?,
                             status = ?::room_status, floor = ?, description = ?, updated_at = ?
            WHERE room_number = ?
        """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, room.getType().name());
            ps.setInt(2, room.getCapacity());
            ps.setBigDecimal(3, room.getPricePerNight());
            ps.setString(4, room.getStatus().name());
            if (room.getFloor() != null) ps.setInt(5, room.getFloor()); else ps.setNull(5, Types.INTEGER);
            ps.setString(6, room.getDescription());
            ps.setTimestamp(7, Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setString(8, room.getRoomNumber());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating room: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Room> findByRoomNumber(String roomNumber) {
        String sql = "SELECT * FROM rooms WHERE room_number = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding room: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Room> findAll() {
        String sql = "SELECT * FROM rooms ORDER BY room_number";
        return executeQuery(sql);
    }


    @Override
    public List<Room> findByStatus(RoomStatus status) {
        String sql = "SELECT * FROM rooms WHERE status = ?::room_status ORDER BY room_number";
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rooms.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding rooms by status: " + e.getMessage(), e);
        }
        return rooms;
    }


    @Override
    public List<Room> findByType(RoomType type) {
        String sql = "SELECT * FROM rooms WHERE type = ?::room_type ORDER BY room_number";
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rooms.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding rooms by type: " + e.getMessage(), e);
        }
        return rooms;
    }

    @Override
    public List<Room> findByMaxPrice(BigDecimal maxPrice) {
        String sql = "SELECT * FROM rooms WHERE price_per_night <= ? ORDER BY price_per_night";
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, maxPrice);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rooms.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding rooms by max price: " + e.getMessage(), e);
        }
        return rooms;
    }

    @Override
    public List<Room> findByMinCapacity(int capacity) {
        String sql = "SELECT * FROM rooms WHERE capacity >= ? ORDER BY capacity";
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, capacity);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rooms.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding rooms by capacity: " + e.getMessage(), e);
        }
        return rooms;
    }

    @Override
    public List<Room> findAvailableRooms() {
        String sql = "SELECT * FROM rooms WHERE status = 'AVAILABLE' ORDER BY room_number";
        return executeQuery(sql);
    }

    @Override
    public boolean existsByRoomNumber(String roomNumber) {
        String sql = "SELECT 1 FROM rooms WHERE room_number = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking room existence: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteByRoomNumber(String roomNumber) {
        String sql = "DELETE FROM rooms WHERE room_number = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomNumber);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting room: " + e.getMessage(), e);
        }
    }

    private List<Room> executeQuery(String sql) {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) rooms.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error executing query: " + e.getMessage(), e);
        }
        return rooms;
    }

    private Room mapRow(ResultSet rs) throws SQLException {
        Room room = new Room();
        room.setRoomNumber(rs.getString("room_number"));
        room.setType(RoomType.valueOf(rs.getString("type")));
        room.setCapacity(rs.getInt("capacity"));
        room.setPricePerNight(rs.getBigDecimal("price_per_night"));
        room.setStatus(RoomStatus.valueOf(rs.getString("status")));
        int floor = rs.getInt("floor");
        room.setFloor(rs.wasNull() ? null : floor);
        room.setDescription(rs.getString("description"));
        room.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        room.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return room;
    }
}