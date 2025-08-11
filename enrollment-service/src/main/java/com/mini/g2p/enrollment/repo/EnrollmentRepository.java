package com.mini.g2p.enrollment.repo;

import com.mini.g2p.enrollment.domain.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
  List<Enrollment> findByCitizenUsername(String username);
}
