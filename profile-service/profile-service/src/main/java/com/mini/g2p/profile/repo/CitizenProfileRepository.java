package com.mini.g2p.profile.repo;

import com.mini.g2p.profile.domain.CitizenProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CitizenProfileRepository extends JpaRepository<CitizenProfile, Long> {
  Optional<CitizenProfile> findByUsername(String username);
}
