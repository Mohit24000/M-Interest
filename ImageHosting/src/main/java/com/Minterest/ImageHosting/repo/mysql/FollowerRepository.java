package com.Minterest.ImageHosting.repo.mysql;

import com.Minterest.ImageHosting.model.Follower;
import com.Minterest.ImageHosting.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface FollowerRepository extends JpaRepository<Follower, UUID> {
    Optional<Follower> findByFollowerAndFollowing(User follower, User following);
}
