import config.DatabaseInitializer;
import db.DatabaseConnection;
import dto.RoomSearchCriteria;
import model.*;
import model.enums.*;
import repository.*;
import repository.jdbc.*;
import service.*;
import util.InputUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class Main {

    // ============================================================
    // REPOSITORIES
    // ============================================================
    private static final UserRepository userRepository = new JdbcUserRepository();
    private static final RoomRepository roomRepository = new JdbcRoomRepository();
    private static final ReservationRepository reservationRepository = new JdbcReservationRepository();
    private static final PaymentRepository paymentRepository = new JdbcPaymentRepository();
    private static final InvoiceRepository invoiceRepository = new JdbcInvoiceRepository();

    // ============================================================
    // SERVICES
    // ============================================================
    private static final AuthService authService = new AuthService(userRepository);
    private static final RoomService roomService = new RoomService(roomRepository, reservationRepository);
    private static final PricingService pricingService = new PricingService();
    private static final ReservationService reservationService =
            new ReservationService(reservationRepository, roomRepository,
                    paymentRepository, invoiceRepository, pricingService);
    private static final PaymentService paymentService = new PaymentService(paymentRepository);
    private static final InvoiceService invoiceService = new InvoiceService(invoiceRepository);
    private static final ReportService reportService = new ReportService();

    // ============================================================
    // MAIN
    // ============================================================
    public static void main(String[] args) {
        try {
            seedDataIfEmpty();
            runApplication();
        } finally {
            DatabaseConnection.getInstance().closeConnection();
        }
    }

    // ============================================================
    // SEED DATA
    // ============================================================
    private static void seedDataIfEmpty() {
        if (roomRepository.findAll().isEmpty()) {
            roomRepository.save(new Room("101", RoomType.SINGLE, 1, new BigDecimal("300.00")));
            roomRepository.save(new Room("102", RoomType.DOUBLE, 2, new BigDecimal("500.00")));
            roomRepository.save(new Room("201", RoomType.DOUBLE, 2, new BigDecimal("550.00")));
            roomRepository.save(new Room("205", RoomType.SUITE, 4, new BigDecimal("900.00")));
            Room suite = new Room("301", RoomType.SUITE, 4, new BigDecimal("1200.00"));
            suite.setStatus(RoomStatus.MAINTENANCE);
            roomRepository.save(suite);
        }

        if (userRepository.findByEmail("admin@hotel.com").isEmpty()) {
            try {
                authService.register("Admin Principal", "admin@hotel.com", "+212600000001", "admin123");
                authService.logout();
                User admin = userRepository.findByEmail("admin@hotel.com").orElseThrow();
                admin.setRole(UserRole.ADMIN);
                userRepository.update(admin);
            } catch (Exception ignored) {}
        }

        if (userRepository.findByEmail("alice@example.com").isEmpty()) {
            try {
                authService.register("Alice Dupont", "alice@example.com", "+212600000002", "alice123");
                authService.logout();
            } catch (Exception ignored) {}
        }
    }

    // ============================================================
    // APP LOOP
    // ============================================================
    private static void runApplication() {
        while (true) {
            try {
                if (authService.isLoggedIn()) showLoggedInMenu();
                else showGuestMenu();
            } catch (Exception e) {
                System.out.println("\n[ERROR] " + e.getMessage());
            }
        }
    }

    private static void showGuestMenu() {
        System.out.println("\n==================== HOTEL BOOKING =====================");
        System.out.println("1. Register");
        System.out.println("2. Login");
        System.out.println("0. Exit");
        int choice = InputUtils.readInt("Choice: ");
        switch (choice) {
            case 1 -> register();
            case 2 -> login();
            case 0 -> {
                System.out.println("Goodbye!");
                System.exit(0);
            }
            default -> System.out.println("Invalid choice");
        }
    }

    private static void showLoggedInMenu() {
        User u = authService.getCurrentUser();
        System.out.println("\nLogged in as: " + u.getFullName() + " [" + u.getRole() + "]");
        System.out.println("==================== HOTEL BOOKING =====================");
        System.out.println("1. Search available rooms");
        System.out.println("2. View all rooms");
        System.out.println("3. Create reservation");
        System.out.println("4. My reservations");
        System.out.println("5. Update reservation");
        System.out.println("6. Cancel reservation");
        System.out.println("7. Update profile");
        System.out.println("8. Change password");
        if (authService.isAdmin()) {
            System.out.println("--- ADMIN ---");
            System.out.println("10. Add room");
            System.out.println("11. Set room maintenance");
            System.out.println("12. View all reservations");
            System.out.println("13. Statistics");
            System.out.println("14. Monthly revenue");
            System.out.println("15. Top rooms");
        }
        System.out.println("9. Logout");
        System.out.println("0. Exit");

        int choice = InputUtils.readInt("Choice: ");
        switch (choice) {
            case 1 -> searchAvailableRooms();
            case 2 -> viewAllRooms();
            case 3 -> createReservation();
            case 4 -> viewMyReservations();
            case 5 -> updateReservation();
            case 6 -> cancelReservation();
            case 7 -> updateProfile();
            case 8 -> changePassword();
            case 9 -> authService.logout();
            case 10 -> { if (authService.isAdmin()) addRoom(); }
            case 11 -> { if (authService.isAdmin()) setMaintenance(); }
            case 12 -> { if (authService.isAdmin()) viewAllReservations(); }
            case 13 -> { if (authService.isAdmin()) showStats(); }
            case 14 -> { if (authService.isAdmin()) showMonthlyRevenue(); }
            case 15 -> { if (authService.isAdmin()) showTopRooms(); }
            case 0 -> System.exit(0);
            default -> System.out.println("Invalid choice");
        }
    }

    // ============================================================
    // ACTIONS
    // ============================================================
    private static void register() {
        String name = InputUtils.readString("Full name: ");
        String email = InputUtils.readString("Email: ");
        String phone = InputUtils.readString("Phone: ");
        String password = InputUtils.readPassword("Password: ");
        authService.register(name, email, phone, password);
        System.out.println("Registration successful! Please login.");
    }

    private static void login() {
        String email = InputUtils.readString("Email: ");
        String password = InputUtils.readPassword("Password: ");
        authService.login(email, password);
        System.out.println("Welcome " + authService.getCurrentUser().getFullName());
    }

    private static void searchAvailableRooms() {
        LocalDate checkIn = InputUtils.readDate("Check-in (dd/MM/yyyy): ");
        LocalDate checkOut = InputUtils.readDate("Check-out (dd/MM/yyyy): ");
        int guests = InputUtils.readInt("Guests: ");

        RoomSearchCriteria criteria = new RoomSearchCriteria(checkIn, checkOut, guests);
        List<dto.AvailableRoomDTO> results = roomService.searchAvailableRooms(criteria);

        if (results.isEmpty()) {
            System.out.println("No rooms available.");
            return;
        }
        System.out.println("\nAvailable rooms:");
        results.forEach(r -> System.out.printf(
                "Room %s | %s | Cap: %d | %.2f MAD/night | Total: %.2f MAD (%d nights)%n",
                r.getRoomNumber(), r.getType(), r.getCapacity(),
                r.getPricePerNight(), r.getEstimatedTotal(), r.getNumberOfNights()));
    }

    private static void viewAllRooms() {
        System.out.println("\n--- All Rooms ---");
        roomService.getAllRooms().forEach(r -> System.out.printf(
                "Room %s | %s | Cap: %d | %.2f MAD | %s%n",
                r.getRoomNumber(), r.getType(), r.getCapacity(),
                r.getPricePerNight(), r.getStatus()));
    }

    private static void createReservation() {
        viewAllRooms();
        String roomNumber = InputUtils.readString("Room number: ");
        LocalDate checkIn = InputUtils.readDate("Check-in (dd/MM/yyyy): ");
        LocalDate checkOut = InputUtils.readDate("Check-out (dd/MM/yyyy): ");
        int guests = InputUtils.readInt("Guests: ");

        System.out.println("Payment method: 1-CASH 2-CREDIT_CARD 3-DEBIT_CARD 4-BANK_TRANSFER 5-ONLINE");
        int pm = InputUtils.readInt("Choice: ");
        PaymentMethod method = switch (pm) {
            case 1 -> PaymentMethod.CASH;
            case 2 -> PaymentMethod.CREDIT_CARD;
            case 3 -> PaymentMethod.DEBIT_CARD;
            case 4 -> PaymentMethod.BANK_TRANSFER;
            default -> PaymentMethod.ONLINE;
        };

        Reservation r = reservationService.createReservationWithPayment(
                authService.getCurrentUser().getId(), roomNumber, checkIn, checkOut, guests, method);
        System.out.println("\nReservation created!");
        displayReservation(r);
    }

    private static void viewMyReservations() {
        List<Reservation> list = reservationService.getByUser(authService.getCurrentUser().getId());
        if (list.isEmpty()) {
            System.out.println("No reservations.");
            return;
        }
        list.forEach(r -> {
            System.out.println("------------------------------");
            displayReservation(r);
        });
    }

    private static void updateReservation() {
        String code = InputUtils.readString("Reservation code: ");
        String roomNumber = InputUtils.readString("New room number: ");
        LocalDate checkIn = InputUtils.readDate("New check-in: ");
        LocalDate checkOut = InputUtils.readDate("New check-out: ");
        int guests = InputUtils.readInt("New guests: ");

        Reservation r = reservationService.updateReservation(code,
                authService.getCurrentUser().getId(), roomNumber, checkIn, checkOut, guests);
        System.out.println("Updated!");
        displayReservation(r);
    }

    private static void cancelReservation() {
        String code = InputUtils.readString("Reservation code: ");
        reservationService.cancelReservation(code, authService.getCurrentUser().getId());
        System.out.println("Cancelled. Refund processed according to policy.");
    }

    private static void updateProfile() {
        String name = InputUtils.readString("New name: ");
        String email = InputUtils.readString("New email: ");
        String phone = InputUtils.readString("New phone: ");
        authService.updateProfile(name, email, phone);
        System.out.println("Profile updated.");
    }

    private static void changePassword() {
        String oldP = InputUtils.readPassword("Current password: ");
        String newP = InputUtils.readPassword("New password: ");
        authService.changePassword(oldP, newP);
        System.out.println("Password changed.");
    }

    private static void addRoom() {
        String number = InputUtils.readString("Room number: ");
        System.out.println("Type: 1-SINGLE 2-DOUBLE 3-SUITE");
        int t = InputUtils.readInt("Choice: ");
        RoomType type = switch (t) {
            case 1 -> RoomType.SINGLE;
            case 2 -> RoomType.DOUBLE;
            default -> RoomType.SUITE;
        };
        int cap = InputUtils.readInt("Capacity: ");
        BigDecimal price = InputUtils.readBigDecimal("Price/night: ");
        roomService.addRoom(new Room(number, type, cap, price));
        System.out.println("Room added.");
    }

    private static void setMaintenance() {
        String number = InputUtils.readString("Room number: ");
        int c = InputUtils.readInt("1-Maintenance 2-Available: ");
        roomService.setMaintenance(number, c == 1);
        System.out.println("Status updated.");
    }

    private static void viewAllReservations() {
        reservationService.getAll().forEach(r -> {
            System.out.println("------------------------------");
            displayReservation(r);
        });
    }

    private static void showStats() {
        Map<String, Object> stats = reportService.getGeneralStats();
        System.out.println("\n--- STATISTICS ---");
        stats.forEach((k, v) -> System.out.println(k + ": " + v));
        System.out.println("Average Basket: " + reportService.getAverageBasket() + " MAD");
    }

    private static void showMonthlyRevenue() {
        System.out.println("\n--- MONTHLY REVENUE ---");
        reportService.getMonthlyRevenue().forEach((m, r) ->
                System.out.println(m + ": " + r + " MAD"));
    }

    private static void showTopRooms() {
        System.out.println("\n--- TOP ROOMS ---");
        reportService.getTopRooms().forEach((room, count) ->
                System.out.println("Room " + room + ": " + count + " reservations"));
    }

    private static void displayReservation(Reservation r) {
        System.out.println("Code: " + r.getReservationCode());
        System.out.println("Room: " + r.getRoomNumber());
        System.out.println("Check-in: " + r.getCheckIn());
        System.out.println("Check-out: " + r.getCheckOut());
        System.out.println("Nights: " + r.getNumberOfNights());
        System.out.println("Total: " + r.getTotalPrice() + " MAD");
        System.out.println("Status: " + r.getStatus());
    }
}