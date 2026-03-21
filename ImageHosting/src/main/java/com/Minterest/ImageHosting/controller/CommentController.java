package com.Minterest.ImageHosting.controller;

import com.Minterest.ImageHosting.model.Comments;
import com.Minterest.ImageHosting.service.CommentService;
import com.Minterest.ImageHosting.service.ImageService.PinService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("comments")
public class CommentController {
    CommentService commentService;
    PinService pinService ;

    public CommentController(CommentService commentService, PinService pinService) {
        this.commentService = commentService;
        this.pinService = pinService;
    }
    // Comment endpoints
    @PostMapping("/{pinId}/comments")
    public ResponseEntity<Comments> addComment(
            @PathVariable UUID pinId,
            @RequestParam String content,
            @RequestParam UUID userId) {

        Comments comment = commentService.addCommentToPin(pinId, content, userId);
        return new ResponseEntity<>(comment, HttpStatus.CREATED);
    }

    @PostMapping("/{pinId}/comments/{commentId}/replies")
    public ResponseEntity<Comments> addReply(
            @PathVariable UUID pinId,
            @PathVariable Long commentId,
            @RequestParam String content) {

        Comments reply = commentService.addReplyToComment(commentId, content, pinId);
        return new ResponseEntity<>(reply, HttpStatus.CREATED);
    }

    @GetMapping("/{pinId}/comments")
    public ResponseEntity<List<Comments>> getPinComments(@PathVariable UUID pinId) {
        List<Comments> comments = commentService.getCommentsForPin(pinId);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/comments/{commentId}")
    public ResponseEntity<Comments> getCommentWithReplies(@PathVariable Long commentId) {
        Comments comment = commentService.getCommentWithReplies(commentId);
        return ResponseEntity.ok(comment);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<Comments> updateComment(
            @PathVariable Long commentId,
            @RequestParam String content) {

        Comments updatedComment = commentService.updateComment(commentId, content);
        return ResponseEntity.ok(updatedComment);
    }

    @GetMapping("/{pinId}/comments/count")
    public ResponseEntity<Long> getCommentCount(@PathVariable UUID pinId) {
        long count = commentService.getCommentCountForPin(pinId);
        return ResponseEntity.ok(count);
    }
}
