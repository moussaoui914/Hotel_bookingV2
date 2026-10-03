package repository;

import model.Room;
import model.enums.RoomStatus;
import model.enums.RoomType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RoomRepository {
    void save(Room room);
    void update(Room room);
    Optional<Room> findByRoomNumber(String roomNumber);
    List<Room> findAll();
    List<Room> findByStatus(RoomStatus status);
    List<Room> findByType(RoomType type);
    List<Room> findByMaxPrice(BigDecimal maxPrice);
    List<Room> findByMinCapacity(int capacity);
    List<Room> findAvailableRooms();
    boolean existsByRoomNumber(String roomNumber);
    void deleteByRoomNumber(String roomNumber);
}