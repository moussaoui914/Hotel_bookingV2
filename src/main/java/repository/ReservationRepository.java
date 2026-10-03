package repository;

import model.Reservation;
import model.enums.PaymentMethod;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository {
    void save(Reservation reservation);
    void update(Reservation reservation);
    Optional<Reservation> findById(UUID id);
    Optional<Reservation> findByCode(String code);
    List<Reservation> findByUserId(UUID userId);
    List<Reservation> findByRoomNumber(String roomNumber);
    List<Reservation> findAll();
    List<Reservation> findActiveReservationsByRoom(String roomNumber);
    boolean isRoomAvailableForDates(String roomNumber, LocalDate checkIn, LocalDate checkOut);
    boolean isRoomAvailableForDatesExcluding(String roomNumber, LocalDate checkIn, LocalDate checkOut, UUID excludeId);
    List<Reservation> findByDateRange(LocalDate start, LocalDate end);
    void deleteById(UUID id);


}