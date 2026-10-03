package service;

import exception.AuthenticationException;
import exception.EmailAlreadyExistsException;
import exception.InvalidCredentialsException;
import model.User;
import model.enums.UserRole;
import repository.UserRepository;
import util.PasswordUtils;
import util.ValidationUtils;

import java.util.Optional;

public class AuthService {
    private final UserRepository userRepository;
    private User currentUser;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void register(String fullName, String email, String phone, String password) {
        validateRegistration(fullName, email, phone, password);

        String salt = PasswordUtils.generateSalt();
        String hash = PasswordUtils.hashPassword(password, salt);

        User user = new User(fullName, email, phone, hash, salt);
        user.setRole(UserRole.CLIENT);
        userRepository.save(user);
    }

    public void login(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new InvalidCredentialsException("User not found");
        }

        User user = userOpt.get();
        if (!PasswordUtils.verifyPassword(password, user.getSalt(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid password");
        }

        if (!user.isActive()) {
            throw new AuthenticationException("Account is disabled");
        }

        this.currentUser = user;
    }

    public void logout() {
        this.currentUser = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public User getCurrentUser() {
        if (currentUser == null) throw new AuthenticationException("No user logged in");
        return currentUser;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    public void requireAdmin() {
        if (!isAdmin()) {
            throw new AuthenticationException("Admin access required");
        }
    }

    public void updateProfile(String fullName, String email, String phone) {
        requireLogin();
        validateProfile(fullName, email, phone);

        if (!currentUser.getEmail().equalsIgnoreCase(email) && userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        currentUser.setFullName(fullName);
        currentUser.setEmail(email);
        currentUser.setPhone(phone);
        userRepository.update(currentUser);
    }

    public void changePassword(String oldPassword, String newPassword) {
        requireLogin();

        if (!PasswordUtils.verifyPassword(oldPassword, currentUser.getSalt(), currentUser.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }
        if (!ValidationUtils.isValidPassword(newPassword)) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }

        String newSalt = PasswordUtils.generateSalt();
        String newHash = PasswordUtils.hashPassword(newPassword, newSalt);
        currentUser.setSalt(newSalt);
        currentUser.setPasswordHash(newHash);
        userRepository.update(currentUser);
    }

    private void requireLogin() {
        if (currentUser == null) throw new AuthenticationException("No user logged in");
    }

    private void validateRegistration(String fullName, String email, String phone, String password) {
        if (!ValidationUtils.isValidName(fullName)) throw new IllegalArgumentException("Full name is required");
        if (!ValidationUtils.isValidEmail(email)) throw new IllegalArgumentException("Invalid email");
        if (!ValidationUtils.isValidPhone(phone)) throw new IllegalArgumentException("Invalid phone");
        if (!ValidationUtils.isValidPassword(password)) throw new IllegalArgumentException("Password must be at least 6 characters");
        if (userRepository.existsByEmail(email)) throw new EmailAlreadyExistsException("Email already exists");
    }

    private void validateProfile(String fullName, String email, String phone) {
        if (!ValidationUtils.isValidName(fullName)) throw new IllegalArgumentException("Full name is required");
        if (!ValidationUtils.isValidEmail(email)) throw new IllegalArgumentException("Invalid email");
        if (!ValidationUtils.isValidPhone(phone)) throw new IllegalArgumentException("Invalid phone");
    }
}