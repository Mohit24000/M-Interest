package com.Minterest.ImageHosting.repo.mysql;

import com.Minterest.ImageHosting.model.Pin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PinRepository extends JpaRepository<Pin, UUID> {
}
