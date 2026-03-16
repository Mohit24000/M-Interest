package com.Minterest.ImageHosting.repo.mysql;

import com.Minterest.ImageHosting.model.Pin;
import com.Minterest.ImageHosting.model.PinLike;
import com.Minterest.ImageHosting.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PinLikeRepository extends JpaRepository<PinLike, UUID> {
    Optional<PinLike> findByUserAndPin(User user, Pin pin);
    long countByPin(Pin pin);
}
