package org.gpc4j.web.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gpc4j.web.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityDataInitializer implements ApplicationRunner {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${app.security.seed.username:admin}")
  private String seedUsername;

  @Value("${app.security.seed.password:admin}")
  private String seedPassword;

  @Value("${app.security.seed.roles:ADMIN}")
  private List<String> seedRoles;

  @Override
  public void run(ApplicationArguments args) {
    try {
      if (userRepository.count() == 0) {
        log.info("Seeding initial admin user: {}", seedUsername);
        UserAccount admin = new UserAccount();
        admin.setUsername(seedUsername);
        admin.setPasswordHash(passwordEncoder.encode(seedPassword));
        admin.setRoles(seedRoles);
        admin.setEnabled(true);
        admin.setAccountNonLocked(true);
        userRepository.save(admin);
      }
    } catch (Exception e) {
      log.warn("Skipping security data seed due to error: {}", e.toString());
    }
  }
}
