package com.mini.g2p.auth.security;

import com.mini.g2p.auth.repo.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
  private final UserRepository users;
  public UserDetailsServiceImpl(UserRepository users) { this.users = users; }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    var u = users.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
    var auths = u.getRoles().stream()
        .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toSet());
    return new org.springframework.security.core.userdetails.User(u.getUsername(), u.getPasswordHash(), auths);
  }
}
