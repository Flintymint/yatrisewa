package com.example.travel_yatra.travel_yatra.controller;

import com.example.travel_yatra.travel_yatra.model.User;
import com.example.travel_yatra.travel_yatra.model.Trip;
import com.example.travel_yatra.travel_yatra.repository.UserRepository;
import com.example.travel_yatra.travel_yatra.repository.TripRepository;
import com.example.travel_yatra.travel_yatra.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class UserController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TripRepository tripRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtUtil jwtUtil;

    public UserController() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String jwtSecret = dotenv.get("JWT_SECRET", "defaultsecretdefaultsecretdefaultse");
        this.jwtUtil = new JwtUtil(jwtSecret);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            // Role must be one of: admin, bus_driver, traveller
            if (user.getRole() == null || user.getRole().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("Role is required and must be one of: admin, bus_driver, traveller");
            }
            if (userRepository.findByEmail(user.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body("Email already exists");
            }
            if (!user.getRole().equals("admin") && !user.getRole().equals("bus_driver")
                    && !user.getRole().equals("traveller")) {
                return ResponseEntity.badRequest().body("Invalid role. Must be one of: admin, bus_driver, traveller");
            }
            // Hash the password before saving
            String hashed = passwordEncoder.encode(user.getPassword());
            user.setPassword(hashed);
            User saved = userRepository.save(user);
            // Do not return the password at all
            return ResponseEntity.ok(new UserResponse(saved));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body("Data integrity violation");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            var userOpt = userRepository.findByEmail(loginRequest.email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401).body("Invalid credentials");
            }
            User user = userOpt.get();
            if (!passwordEncoder.matches(loginRequest.password, user.getPassword())) {
                return ResponseEntity.status(401).body("Invalid credentials");
            }
            // Generate JWT token
            String token = jwtUtil.generateToken(user.getId().toString(), user.getEmail(), user.getRole());
            return ResponseEntity.ok(new JwtResponse(token));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body("Missing or invalid Authorization header");
            }
            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity.status(401).body("Invalid or expired token");
            }
            Claims claims = jwtUtil.extractAllClaims(token);
            String email = claims.get("email", String.class);
            var userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body("User not found");
            }
            return ResponseEntity.ok(new UserResponse(userOpt.get()));
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid or expired token");
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/drivers")
    public ResponseEntity<?> getAllDrivers() {
        var drivers = userRepository.fetchAllByRole("bus_driver");
        var safeDrivers = drivers.stream().map(user -> new UserResponse(user)).toList();
        return ResponseEntity.ok(safeDrivers);
    }

    @GetMapping("/drivers/available")
    public ResponseEntity<?> getAvailableDrivers(
            @RequestParam String fromId,
            @RequestParam String toId,
            @RequestParam String departureDate // ISO format yyyy-MM-dd
    ) {
        List<User> allDrivers = userRepository.fetchAllByRole("bus_driver");
        List<Trip> conflictingTrips = tripRepository.findAllByFrom_IdAndTo_IdAndDepartureDate(
                UUID.fromString(fromId),
                UUID.fromString(toId),
                LocalDate.parse(departureDate));
        Set<UUID> busyDriverIds = conflictingTrips.stream()
                .map(trip -> trip.getBusDriver().getId())
                .collect(Collectors.toSet());
        List<User> availableDrivers = allDrivers.stream()
                .filter(driver -> !busyDriverIds.contains(driver.getId()))
                .toList();
        return ResponseEntity.ok(availableDrivers);
    }

    // Endpoint: Upload profile picture
    @PostMapping("/upload-profile-picture")
    public ResponseEntity<?> uploadProfilePicture(@RequestHeader("Authorization") String authHeader,
            @RequestParam("file") MultipartFile file) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body("Missing or invalid Authorization header");
            }
            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity.status(401).body("Invalid or expired token");
            }
            Claims claims = jwtUtil.extractAllClaims(token);
            String email = claims.get("email", String.class);
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body("User not found");
            }
            User user = userOpt.get();
            // Save file to /uploads
            String uploadsDir = "uploads";
            java.io.File uploadsFolder = new java.io.File(uploadsDir);
            if (!uploadsFolder.exists())
                uploadsFolder.mkdirs();
            String originalFilename = file.getOriginalFilename();
            String ext = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                    : "";
            String filename = "profile_" + user.getId() + ext;
            java.nio.file.Path path = java.nio.file.Paths.get(uploadsDir, filename);
            file.transferTo(path);
            // Update user profileImageUrl
            user.setProfileImageUrl("/uploads/" + filename);
            userRepository.save(user);
            return ResponseEntity.ok("Profile picture uploaded successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error uploading profile picture: " + e.getMessage());
        }
    }

    // Endpoint: Update password
    @PostMapping("/update-password")
    public ResponseEntity<?> updatePassword(@RequestHeader("Authorization") String authHeader,
            @RequestBody UpdatePasswordRequest req) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body("Missing or invalid Authorization header");
            }
            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity.status(401).body("Invalid or expired token");
            }
            Claims claims = jwtUtil.extractAllClaims(token);
            String email = claims.get("email", String.class);
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body("User not found");
            }
            User user = userOpt.get();
            if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
                return ResponseEntity.status(400).body("Old password is incorrect");
            }
            user.setPassword(passwordEncoder.encode(req.getNewPassword()));
            userRepository.save(user);
            return ResponseEntity.ok("Password updated successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error updating password: " + e.getMessage());
        }
    }

    // Profile image upload endpoint
    @PostMapping("/profile/upload")
    public ResponseEntity<?> uploadProfileImage(@RequestHeader("Authorization") String authHeader,
            @RequestParam("file") MultipartFile file) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body("Missing or invalid Authorization header");
            }
            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity.status(401).body("Invalid or expired token");
            }
            Claims claims = jwtUtil.extractAllClaims(token);
            String email = claims.get("email", String.class);
            if (email == null) {
                return ResponseEntity.status(400).body("Email claim missing in token");
            }
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body("User not found");
            }
            User user = userOpt.get();
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("No file uploaded");
            }
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()) {
                return ResponseEntity.badRequest().body("File name missing");
            }
            originalFilename = StringUtils.cleanPath(originalFilename);
            String fileExtension = originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                    : "";
            String newFilename = user.getId().toString() + fileExtension;
            String uploadDir = "uploads/profile-images";
            Files.createDirectories(Paths.get(uploadDir));
            Path filePath = Paths.get(uploadDir, newFilename);
            Files.write(filePath, file.getBytes());
            // Update user profileImageUrl
            user.setProfileImageUrl("/" + uploadDir + "/" + newFilename);
            userRepository.save(user);
            return ResponseEntity.ok().body("Profile image uploaded successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error uploading profile image: " + e.getMessage());
        }
    }

    // DTO for user response
    public static class UserResponse {
        public String id;
        public String fullName;
        public String email;
        public String role;
        public String profileImageUrl;

        public UserResponse(User user) {
            this.id = user.getId().toString();
            this.fullName = user.getFullName();
            this.email = user.getEmail();
            this.role = user.getRole();
            this.profileImageUrl = user.getProfileImageUrl();
        }
    }

    static class LoginRequest {
        public String email;
        public String password;
    }

    static class JwtResponse {
        public String token;

        public JwtResponse(String token) {
            this.token = token;
        }
    }

    // DTO for password update
    public static class UpdatePasswordRequest {
        private String oldPassword;
        private String newPassword;

        public String getOldPassword() {
            return oldPassword;
        }

        public void setOldPassword(String oldPassword) {
            this.oldPassword = oldPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}
