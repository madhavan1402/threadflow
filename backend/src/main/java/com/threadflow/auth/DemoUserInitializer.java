package com.threadflow.auth;

import com.threadflow.user.Role;
import com.threadflow.user.RoleRepository;
import com.threadflow.user.User;
import com.threadflow.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
@Order(1)
public class DemoUserInitializer implements ApplicationRunner {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String merchandiserEmail;
    private final String merchandiserPassword;
    private final String supervisorEmail;
    private final String supervisorPassword;

    public DemoUserInitializer(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${threadflow.demo.merchandiser-email:}") String merchandiserEmail,
            @Value("${threadflow.demo.merchandiser-password:}") String merchandiserPassword,
            @Value("${threadflow.demo.supervisor-email:}") String supervisorEmail,
            @Value("${threadflow.demo.supervisor-password:}") String supervisorPassword
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.merchandiserEmail = merchandiserEmail;
        this.merchandiserPassword = merchandiserPassword;
        this.supervisorEmail = supervisorEmail;
        this.supervisorPassword = supervisorPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        provision("MERCHANDISER", merchandiserEmail, merchandiserPassword);
        provision("SUPERVISOR", supervisorEmail, supervisorPassword);
    }

    private void provision(String roleName, String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }
        if (password.length() < 12) {
            throw new IllegalStateException("Demo account passwords must be at least 12 characters");
        }
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Missing required role: " + roleName));
        User user = userRepository.findByEmailIgnoreCase(email.trim()).orElseGet(
                () -> new User(email.trim().toLowerCase(), passwordEncoder.encode(password), true)
        );
        user.replacePasswordHash(passwordEncoder.encode(password));
        if (!user.hasRole(roleName)) {
            user.addRole(role);
        }
        userRepository.save(user);
    }
}
