package service;

import db.DatabaseConnection;
import exception.*;
import model.*;
import model.enums.PaymentMethod;
import model.enums.PaymentStatus;
import model.enums.ReservationStatus;
import policy.RefundPolicy;
import policy.StandardRefundPolicy;
import repository.InvoiceRepository;
import repository.PaymentRepository;
import repository.ReservationRepository;
import repository.RoomRepository;
import util.DateUtils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PricingService pricingService;
    private RefundPolicy refundPolicy;

    public ReservationService(ReservationRepository reservationRepository,
                              RoomRepository roomRepository,
                              PaymentRepository paymentRepository,
                              InvoiceRepository invoiceRepository,
                              PricingService pricingService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.pricingService = pricingService;
        this.refundPolicy = new StandardRefundPolicy();
    }

    public void setRefundPolicy(RefundPolicy refundPolicy) {
        this.refundPolicy = refundPolicy;
    }

    public Reservation createReservationWithPayment(UUID userId, String roomNumber,
                                                    LocalDate checkIn, LocalDate checkOut,
                                                    int guests, PaymentMethod paymentMethod) {
        validateReservation(roomNumber, checkIn, checkOut, guests);

        Room room = roomRepository.findByRoomNumber(roomNumber)
                .orElseThrow(() -> new RoomNotFoundException("Room not found"));

        if (!room.isAvailable()) {
            throw new RoomNotAvailableException("Room is under maintenance");
        }
        if (guests > room.getCapacity()) {
            throw new RoomCapacityExceededException("Capacity exceeded. Max: " + room.getCapacity());
        }
        if (!reservationRepository.isRoomAvailableForDates(roomNumber, checkIn, checkOut)) {
            throw new RoomNotAvailableException("Room unavailable for selected dates");
        }

        long nights = DateUtils.calculateNights(checkIn, checkOut);
        BigDecimal totalPrice = pricingService.calculateTotalPrice(room, checkIn, checkOut);

        Reservation reservation = new Reservation(userId, roomNumber, checkIn, checkOut,
                guests, (int) nights, totalPrice);
        reservation.setBasePrice(room.getPricePerNight().multiply(BigDecimal.valueOf(nights)));

        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            reservationRepository.save(reservation);

            Reservation saved = reservationRepository.findById(reservation.getId())
                    .orElseThrow(() -> new ReservationNotFoundException("Failed to save reservation"));

            Payment payment = new Payment(saved.getId(), totalPrice, paymentMethod);
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setTransactionRef("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            paymentRepository.save(payment);

            Invoice invoice = new Invoice(saved.getId(), payment.getId(), totalPrice);
            invoice.setPaid(true);
            invoiceRepository.save(invoice);

            conn.commit();
            return saved;
        } catch (SQLException e) {
            rollback(conn);
            throw new PaymentException("Transaction failed: " + e.getMessage(), e);
        } catch (Exception e) {
            rollback(conn);
            throw e;
        } finally {
            resetAutoCommit(conn);
        }
    }

    public Double nbrReservation(User user){
        return reservationRepository.findAll().stream().filter(rs->rs.getUserId().equals(user.getId())).count();
    }

    public Reservation createReservation(UUID userId, String roomNumber,
                                         LocalDate checkIn, LocalDate checkOut, int guests) {
        validateReservation(roomNumber, checkIn, checkOut, guests);

        Room room = roomRepository.findByRoomNumber(roomNumber)
                .orElseThrow(() -> new RoomNotFoundException("Room not found"));

        if (!room.isAvailable()) {
            throw new RoomNotAvailableException("Room is under maintenance");
        }
        if (guests > room.getCapacity()) {
            throw new RoomCapacityExceededException("Capacity exceeded. Max: " + room.getCapacity());
        }
        if (!reservationRepository.isRoomAvailableForDates(roomNumber, checkIn, checkOut)) {
            throw new RoomNotAvailableException("Room unavailable for selected dates");
        }

        long nights = DateUtils.calculateNights(checkIn, checkOut);
        BigDecimal totalPrice = pricingService.calculateTotalPrice(room, checkIn, checkOut);

        Reservation reservation = new Reservation(userId, roomNumber, checkIn, checkOut,
                guests, (int) nights, totalPrice);
        reservation.setBasePrice(room.getPricePerNight().multiply(BigDecimal.valueOf(nights)));

        reservationRepository.save(reservation);

        return reservationRepository.findById(reservation.getId())
                .orElseThrow(() -> new ReservationNotFoundException("Failed to save reservation"));
    }

    public Reservation getByCode(String code) {
        return reservationRepository.findByCode(code)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found: " + code));
    }

    public List<Reservation> getByUser(UUID userId) {
        return reservationRepository.findByUserId(userId);
    }

    public List<Reservation> getAll() {
        return reservationRepository.findAll();
    }

    public void cancelReservation(String code, UUID userId) {
        Reservation reservation = getByCode(code);

        if (!reservation.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Not your reservation");
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ReservationAlreadyCancelledException("Already cancelled");
        }
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new InvalidReservationException("Cannot cancel this reservation");
        }

        BigDecimal refund = refundPolicy.calculateRefund(reservation);

        paymentRepository.findByReservationId(reservation.getId()).ifPresent(payment -> {
            payment.setRefundAmount(refund);
            payment.setRefundDate(java.time.LocalDateTime.now());
            payment.setStatus(refund.compareTo(BigDecimal.ZERO) > 0
                    ? PaymentStatus.PARTIALLY_REFUNDED
                    : PaymentStatus.REFUNDED);
            paymentRepository.update(payment);
        });

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.update(reservation);
    }


    public Reservation updateReservation(String code, UUID userId, String newRoomNumber,
                                         LocalDate newCheckIn, LocalDate newCheckOut, int newGuests) {
        Reservation reservation = getByCode(code);

        if (!reservation.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Not your reservation");
        }
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new InvalidReservationException("Only CONFIRMED reservations can be modified");
        }

        validateReservation(newRoomNumber, newCheckIn, newCheckOut, newGuests);

        Room room = roomRepository.findByRoomNumber(newRoomNumber)
                .orElseThrow(() -> new RoomNotFoundException("Room not found"));

        if (!room.isAvailable()) throw new RoomNotAvailableException("Room under maintenance");
        if (newGuests > room.getCapacity()) throw new RoomCapacityExceededException("Capacity exceeded");
        if (!reservationRepository.isRoomAvailableForDatesExcluding(
                newRoomNumber, newCheckIn, newCheckOut, reservation.getId())) {
            throw new RoomNotAvailableException("Room unavailable for selected dates");
        }

        long nights = DateUtils.calculateNights(newCheckIn, newCheckOut);
        BigDecimal totalPrice = pricingService.calculateTotalPrice(room, newCheckIn, newCheckOut);

        reservation.setRoomNumber(newRoomNumber);
        reservation.setCheckIn(newCheckIn);
        reservation.setCheckOut(newCheckOut);
        reservation.setNumberOfGuests(newGuests);
        reservation.setNumberOfNights((int) nights);
        reservation.setBasePrice(room.getPricePerNight().multiply(BigDecimal.valueOf(nights)));
        reservation.setTotalPrice(totalPrice);

        reservationRepository.update(reservation);
        return reservation;
    }

    private void validateReservation(String roomNumber, LocalDate checkIn, LocalDate checkOut, int guests) {
        if (!DateUtils.isTodayOrFuture(checkIn)) {
            throw new InvalidReservationException("Check-in must be today or future");
        }
        if (!DateUtils.isValidDateRange(checkIn, checkOut)) {
            throw new InvalidReservationException("Check-out must be after check-in");
        }
        if (guests <= 0) {
            throw new InvalidReservationException("Guests must be positive");
        }
    }

    private void rollback(Connection conn) {
        if (conn != null) {
            try { conn.rollback(); } catch (SQLException ignored) {}
        }
    }

    private void resetAutoCommit(Connection conn) {
        if (conn != null) {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }
}