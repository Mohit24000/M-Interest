package com.Minterest.ImageHosting.service;


import com.Minterest.ImageHosting.model.AppFeatures.RedisPubSubNotification;
import com.Minterest.ImageHosting.model.Comments;
import com.Minterest.ImageHosting.model.Pin;
import com.Minterest.ImageHosting.repo.mysql.CommentRepository;
import com.Minterest.ImageHosting.repo.mysql.PinRepository;
import com.Minterest.ImageHosting.repo.mysql.UserRepository;
import com.Minterest.ImageHosting.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PinRepository pinRepository;
    private final RedisFeedService redisFeedService;
    private final RedisPublisherService redisPublisherService;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional
    @CacheEvict(value = {"pin", "pinComments", "replies"}, allEntries = true)
    public Comments addCommentToPin(UUID pinId, String content, UUID userId) {
        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Pin not found with ID: " + pinId));

        Comments comment = new Comments();
        comment.setContent(content);
        comment.setPin(pin);
        comment.setUser(userRepository.findById(userId).orElse(null));

        Comments savedComment = commentRepository.save(comment);
        log.info("Comment added to pin: {} with content: {}", pinId, content);

        // Update trending score (Comments weigh +3)
        redisFeedService.updatePinScore(pinId, 3.0);
        
        // dispatch to redis Pub/Sub for
        RedisPubSubNotification notification = new RedisPubSubNotification(
            "comment", 
            "ADD_COMMENT", 
            userId, 
            pinId, 
            java.time.LocalDateTime.now()
        );
        redisPublisherService.publishCommentEvent(notification);

        // Send Email Notification to Pin Owner
        try {
            String pinOwnerEmail = pin.getUser().getEmail();
            String commenterName = userRepository.findById(userId)
                    .map(u -> u.getUsername())
                    .orElse("Someone");
            
            emailService.sendCommentEmail(pinOwnerEmail, commenterName + " commented: " + content);
            log.info("Sent comment notification email to {}", pinOwnerEmail);
        } catch (Exception e) {
            log.error("NON-BLOCKING: Failed to send comment notification email: {}", e.getMessage());
        }

        return savedComment;
    }

    @Transactional
    @CacheEvict(value = {"pin", "pinComments", "replies"}, allEntries = true)
    public Comments addReplyToComment(Long parentCommentId, String content, UUID pinId, UUID userId) {
        // Verify pin exists
        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Pin not found with ID: " + pinId));

        Comments parentComment = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found with ID: " + parentCommentId));

        Comments reply = new Comments();
        reply.setContent(content);
        reply.setParentComment(parentComment);
        reply.setPin(pin); // Important: link reply to pin
        reply.setUser(userRepository.findById(userId).orElse(null));

        Comments savedReply = commentRepository.save(reply);
        log.info("Reply added to comment: {} on pin: {}", parentCommentId, pinId);

        // Update trending score for reply as well
        redisFeedService.updatePinScore(pinId, 2.0);
        
        // Dispatch to Redis Pub/Sub
        RedisPubSubNotification notification = new RedisPubSubNotification(
            "comment", 
            "REPLY_COMMENT", 
            null, // Anonymous user
            pinId, 
            java.time.LocalDateTime.now()
        );
        redisPublisherService.publishCommentEvent(notification);

        // Send Email Notification to Pin Owner
        try {
            String pinOwnerEmail = pin.getUser().getEmail();
            emailService.sendCommentEmail(pinOwnerEmail, "Someone replied: " + content);
            log.info("Sent reply notification email to {}", pinOwnerEmail);
        } catch (Exception e) {
            log.error("NON-BLOCKING: Failed to send reply notification email: {}", e.getMessage());
        }

        return savedReply;
    }
     @Cacheable(value = "pinComments")
    @Transactional(readOnly = true)
    public List<Comments> getCommentsForPin(UUID pinId) {
        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Pin not found with ID: " + pinId));

        return commentRepository.findByPinAndParentCommentIsNull(pin);
    }
    @Cacheable(value = "replies")
    @Transactional(readOnly = true)
    public Comments getCommentWithReplies(Long commentId) {
        return commentRepository.findByIdWithReplies(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with ID: " + commentId));
    }

    @Transactional
    @CacheEvict(value = {"pin", "pinComments", "replies"}, allEntries = true)
    public void deleteComment(Long commentId) {
        Comments comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with ID: " + commentId));

        if (comment.getParentComment() != null) {
            comment.getParentComment().getReplies().remove(comment);
        }
        if (comment.getPin() != null) {
            comment.getPin().getCommentsList().remove(comment);
        }

        commentRepository.delete(comment);
        log.info("Comment deleted: {}", commentId);
    }

    @Transactional
    @CacheEvict(value = {"pin", "pinComments", "replies"}, allEntries = true)
    public Comments updateComment(Long commentId, String newContent) {
        Comments comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with ID: " + commentId));

        comment.setContent(newContent);
        Comments updatedComment = commentRepository.save(comment);
        log.info("Comment updated: {}", commentId);

        return updatedComment;
    }

    @Transactional(readOnly = true)
    public long getCommentCountForPin(UUID pinId) {
        return commentRepository.countByPinId(pinId);
    }
}