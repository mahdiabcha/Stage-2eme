package com.mini.g2p.programcatalog.repo;

import com.mini.g2p.programcatalog.domain.Program;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramRepository extends JpaRepository<Program, Long> {}
