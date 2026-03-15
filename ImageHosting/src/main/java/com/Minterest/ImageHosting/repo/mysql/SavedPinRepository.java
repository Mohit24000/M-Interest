package com.Minterest.ImageHosting.repo.mysql;

import com.Minterest.ImageHosting.model.SavedPin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedPinRepository extends JpaRepository<SavedPin, Long> {
}
