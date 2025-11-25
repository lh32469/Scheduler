package org.gpc4j.web.security;

import lombok.RequiredArgsConstructor;
import org.gpc4j.web.repository.RavenUserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RavenUserDetailsService implements UserDetailsService {

  private final RavenUserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    UserAccount account = userRepository.findByUsername(username);
    if (account == null) {
      throw new UsernameNotFoundException("User not found: " + username);
    }
    List<GrantedAuthority> authorities = (account.getRoles() == null ? List.<String>of() : account.getRoles())
        .stream()
        .map(role -> role.startsWith("ROLE_") ? role : ("ROLE_" + role))
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());

    return User.withUsername(account.getUsername())
        .password(account.getPasswordHash())
        .authorities(authorities)
        .accountLocked(!account.isAccountNonLocked())
        .disabled(!account.isEnabled())
        .build();
  }
}
