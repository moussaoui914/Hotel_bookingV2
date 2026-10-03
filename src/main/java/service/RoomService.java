package service;

import dto.AvailableRoomDTO;
import dto.RoomSearchCriteria;
import exception.RoomNotFoundException;
import model.Room;
import model.enums.RoomStatus;
import model.enums.RoomType;
import repository.ReservationRepository;
import repository.RoomRepository;
import util.DateUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class RoomService {
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    public RoomService(RoomRepository roomRepository, ReservationRepository reservationRepository) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public List<Room> getAvailableRooms() {
        return roomRepository.findAvailableRooms();
    }

    public Room getRoomByNumber(String roomNumber) {
        return roomRepository.findByRoomNumber(roomNumber)
                .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomNumber));
    }

    public List<Room> filterByType(RoomType type) {
        return roomRepository.findByType(type);
    }

    public List<Room> filterByMaxPrice(BigDecimal maxPrice) {
        return roomRepository.findByMaxPrice(maxPrice);
    }

    public List<Room> filterByMinCapacity(int capacity) {
        return roomRepository.findByMinCapacity(capacity);
    }

    public List<AvailableRoomDTO> searchAvailableRooms(RoomSearchCriteria criteria) {
        if (!DateUtils.isValidDateRange(criteria.getCheckIn(), criteria.getCheckOut())) {
            throw new IllegalArgumentException("Invalid date range");
        }

        long nights = DateUtils.calculateNights(criteria.getCheckIn(), criteria.getCheckOut());

        return roomRepository.findAvailableRooms().stream()
                .filter(r -> r.getCapacity() >= criteria.getNumberOfGuests())
                .filter(r -> !criteria.hasTypeFilter() || r.getType() == criteria.getType())
                .filter(r -> !criteria.hasMaxPriceFilter() || r.getPricePerNight().compareTo(criteria.getMaxPrice()) <= 0)
                .filter(r -> !criteria.hasMinCapacityFilter() || r.getCapacity() >= criteria.getMinCapacity())
                .filter(r -> reservationRepository.isRoomAvailableForDates(
                        r.getRoomNumber(), criteria.getCheckIn(), criteria.getCheckOut()))
                .map(r -> new AvailableRoomDTO(
                        r.getRoomNumber(), r.getType(), r.getCapacity(),
                        r.getPricePerNight(),
                        r.getPricePerNight().multiply(BigDecimal.valueOf(nights)),
                        (int) nights))
                .collect(Collectors.toList());
    }

    public void addRoom(Room room) {
        if (roomRepository.existsByRoomNumber(room.getRoomNumber())) {
            throw new IllegalArgumentException("Room already exists: " + room.getRoomNumber());
        }
        roomRepository.save(room);
    }

    public void updateRoom(Room room) {
        getRoomByNumber(room.getRoomNumber());
        roomRepository.update(room);
    }

    public void setMaintenance(String roomNumber, boolean maintenance) {
        Room room = getRoomByNumber(roomNumber);
        room.setStatus(maintenance ? RoomStatus.MAINTENANCE : RoomStatus.AVAILABLE);
        roomRepository.update(room);
    }
}