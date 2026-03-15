package com.Minterest.ImageHosting.repo.mysql;

import com.Minterest.ImageHosting.model.Follower;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowerRepository extends JpaRepository<Follower, Long> {
}
