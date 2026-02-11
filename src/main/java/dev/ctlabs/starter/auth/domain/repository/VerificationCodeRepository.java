package dev.ctlabs.starter.auth.domain.repository;

import dev.ctlabs.starter.auth.domain.model.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, UUID> {
    Optional<VerificationCode> findByUser_IdAndTypeAndCode(UUID userId, String type, String code);

    void deleteByUser_IdAndType(UUID userId, String type);
}