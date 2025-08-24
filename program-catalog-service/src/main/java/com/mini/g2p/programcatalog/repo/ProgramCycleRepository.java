package com.mini.g2p.programcatalog.repo;

import com.mini.g2p.programcatalog.domain.ProgramCycle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramCycleRepository extends JpaRepository<ProgramCycle, Long> {
  List<ProgramCycle> findByProgramIdOrderByIdDesc(Long programId);
}
