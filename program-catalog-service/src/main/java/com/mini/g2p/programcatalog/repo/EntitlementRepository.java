package com.mini.g2p.programcatalog.repo;

import com.mini.g2p.programcatalog.domain.Entitlement;
import com.mini.g2p.programcatalog.domain.EntitlementState;

import jakarta.transaction.Transactional;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EntitlementRepository extends JpaRepository<Entitlement, Long> {
  List<Entitlement> findByCycleIdOrderByIdAsc(Long cycleId);
  List<Entitlement> findByCycleIdAndState(Long cycleId, EntitlementState state);
  long countByCycleId(Long cycleId);
  long countByCycleIdAndState(Long cycleId, EntitlementState state);
  @Transactional
  @Modifying
  @Query("update Entitlement e set e.state = :to where e.cycleId = :cycleId and e.state = :from")
  int bulkUpdateState(@Param("cycleId") Long cycleId,
                        @Param("from") EntitlementState from,
                        @Param("to") EntitlementState to);
}
