package com.Minterest.ImageHosting.repo.mysql;

import com.Minterest.ImageHosting.model.Pin;
import com.Minterest.ImageHosting.model.SavedPin;
import com.Minterest.ImageHosting.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SavedPinRepository extends JpaRepository<SavedPin, UUID> {
    Optional<SavedPin> findByUserAndPin(User user, Pin pin);


    long countByPin(Pin pin);
}
