package com.bank.bankmanagement.admin.bootstrap;

import com.bank.bankmanagement.user.entity.BankUser;
import com.bank.bankmanagement.user.enums.UserRole;
import com.bank.bankmanagement.user.repository.BankUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrapRunner implements CommandLineRunner {
    private final BankUserRepository users;
    private final PasswordEncoder encoder;
    @Value("${APP_ADMIN_EMAIL:}") private String adminEmail;
    @Value("${APP_ADMIN_PASSWORD:}") private String adminPassword;
    @Value("${APP_ADMIN_NAME:System Administrator}") private String adminName;

    public AdminBootstrapRunner(BankUserRepository users, PasswordEncoder encoder) {
        this.users = users; this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        boolean emailSet = adminEmail != null && !adminEmail.isBlank();
        boolean passwordSet = adminPassword != null && !adminPassword.isBlank();
        if (!emailSet && !passwordSet) return; // local environments may bootstrap later
        if (!emailSet || !passwordSet || adminPassword.length() < 12)
            throw new IllegalStateException("Set both APP_ADMIN_EMAIL and APP_ADMIN_PASSWORD (minimum 12 characters)");
        String email = adminEmail.trim().toLowerCase();
        var existing = users.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            if (existing.get().getRole() != UserRole.ADMIN)
                throw new IllegalStateException("APP_ADMIN_EMAIL belongs to a non-admin account; refusing to promote it");
            return; // idempotent: never reset an existing administrator password on restart
        }
        BankUser admin = new BankUser(adminName == null || adminName.isBlank() ? "System Administrator" : adminName.trim(),
                email, encoder.encode(adminPassword), UserRole.ADMIN);
        admin.activate();
        users.save(admin);
    }
}
