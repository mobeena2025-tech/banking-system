package com.bank.management_system.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.bank.management_system.user.Role;
import com.bank.management_system.user.User;
import com.bank.management_system.user.UserRepository;

/** Creates the first admin from ADMIN_EMAIL / ADMIN_PASSWORD, if set and not already present. */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public AdminSeeder(UserRepository users, PasswordEncoder encoder,
            @Value("${app.admin.email}") String email, @Value("${app.admin.password}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank() || users.existsByEmailIgnoreCase(email)) {
            return;
        }
        users.save(new User("Administrator", email.toLowerCase(), encoder.encode(password), null, Role.ADMIN));
        log.info("Created admin user {}", email);
    }
}
