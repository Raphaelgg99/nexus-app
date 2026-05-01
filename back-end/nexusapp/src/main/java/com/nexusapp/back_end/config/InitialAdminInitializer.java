package com.nexusapp.back_end.config;

import com.nexusapp.back_end.user.model.User;
import com.nexusapp.back_end.user.model.UserRole;
import com.nexusapp.back_end.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class InitialAdminInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(InitialAdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String initialAdminUsername;
    private final String initialAdminPassword;

    public InitialAdminInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${INITIAL_ADMIN_USERNAME:}") String initialAdminUsername,
            @Value("${INITIAL_ADMIN_PASSWORD:}") String initialAdminPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.initialAdminUsername = initialAdminUsername;
        this.initialAdminPassword = initialAdminPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        if (!StringUtils.hasText(initialAdminUsername) || !StringUtils.hasText(initialAdminPassword)) {
            logger.warn("No users found, but INITIAL_ADMIN_USERNAME/INITIAL_ADMIN_PASSWORD are missing. Initial admin was not created.");
            return;
        }

        User admin = new User(
                initialAdminUsername.trim(),
                UserRole.ADMIN,
                passwordEncoder.encode(initialAdminPassword)
        );

        userRepository.save(admin);
        logger.info("Initial administrator user created successfully.");
    }
}
