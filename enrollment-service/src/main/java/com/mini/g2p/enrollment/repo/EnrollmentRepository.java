package com.mini.g2p.enrollment.repo;

import com.mini.g2p.enrollment.domain.Enrollment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

  List<Enrollment> findByCitizenUsernameOrderByCreatedAtDesc(String citizenUsername);

  List<Enrollment> findByProgramIdOrderByCreatedAtDesc(Long programId);

  List<Enrollment> findByProgramIdAndStatusOrderByCreatedAtDesc(Long programId, Enrollment.Status status);
}
