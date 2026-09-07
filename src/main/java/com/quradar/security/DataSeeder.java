package com.quradar.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Dev bootstrap: creates the first ADMIN from env (ADMIN_USERNAME/ADMIN_PASSWORD)
 * when the users table is empty. Production sets real secrets via env;
 * the default password is loudly warned about at startup.
 */
@Configuration
public class DataSeeder {

    @Bean
    ApplicationRunner seedAdmin(UserRepository users, PasswordEncoder passwords,
                                @Value("${quradar.admin.username:admin}") String username,
                                @Value("${quradar.admin.password:admin123}") String password) {
        return args -> {
            if (users.count() > 0) {
                return;
            }
            users.save(new UserEntity(username, passwords.encode(password), Role.ADMIN,
                    true, null, null));
            if ("admin123".equals(password)) {
                System.out.println("WARNING: default admin password in use — set ADMIN_PASSWORD env var");
            }
        };
    }
}
