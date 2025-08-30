package com.mini.g2p.enrollment.repo;

import com.mini.g2p.enrollment.domain.Enrollment;
import com.mini.g2p.enrollment.domain.Enrollment.Status;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByCitizenUsernameOrderByCreatedAtDesc(String citizenUsername);

    List<Enrollment> findByProgramIdOrderByCreatedAtDesc(Long programId);

    List<Enrollment> findByProgramIdAndStatusOrderByCreatedAtDesc(Long programId, Status status);

    boolean existsByProgramIdAndCitizenUsernameAndStatusIn(Long programId, String citizenUsername, List<Status> statuses);
}
