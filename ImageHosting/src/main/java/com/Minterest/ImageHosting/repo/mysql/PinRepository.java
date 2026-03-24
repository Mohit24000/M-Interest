package com.Minterest.ImageHosting.repo.mysql;

import com.Minterest.ImageHosting.model.Pin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import com.Minterest.ImageHosting.model.User;

public interface PinRepository extends JpaRepository<Pin, UUID> {
    Page<Pin> findByUser(User user, Pageable pageable);
    // Eager-load User to avoid LazyInitializationException during ES sync
    @Query("SELECT p FROM Pin p JOIN FETCH p.user")
    List<Pin> findAllWithUser();
    // DB fallback search when Elasticsearch is unavailable
    Page<Pin> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description, Pageable pageable);
}
