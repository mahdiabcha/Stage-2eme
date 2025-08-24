package com.mini.g2p.programcatalog.repo;

import com.mini.g2p.programcatalog.domain.Entitlement;
import com.mini.g2p.programcatalog.domain.EntitlementState;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntitlementRepository extends JpaRepository<Entitlement, Long> {
  List<Entitlement> findByCycleIdOrderByIdAsc(Long cycleId);
  List<Entitlement> findByCycleIdAndState(Long cycleId, EntitlementState state);
}
