package com.Minterest.ImageHosting.repo.mysql;


import com.Minterest.ImageHosting.model.Comments;
import com.Minterest.ImageHosting.model.Pin;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comments, Long> {

    @EntityGraph(attributePaths = {"replies"})
    List<Comments> findByParentCommentIsNull();

    @EntityGraph(attributePaths = {"replies"})
    List<Comments> findByPinAndParentCommentIsNull(Pin pin);

    @EntityGraph(attributePaths = {"replies"})
    List<Comments> findByPin(Pin pin);

    @Query("SELECT c FROM Comments c LEFT JOIN FETCH c.replies WHERE c.id = :commentId")
    Optional<Comments> findByIdWithReplies(@Param("commentId") Long commentId);

    @Query("SELECT COUNT(c) FROM Comments c WHERE c.pin.pinId = :pinId")
    long countByPinId(@Param("pinId") UUID pinId);

    @EntityGraph(attributePaths = {"replies", "pin"})
    @Query("SELECT c FROM Comments c WHERE c.pin.pinId = :pinId")
    List<Comments> findAllByPinIdWithReplies(@Param("pinId") UUID pinId);
}