package fr.diginamic.hubevenementiel.controllers;

import java.util.List;

import fr.diginamic.hubevenementiel.security.AppUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import fr.diginamic.hubevenementiel.dtos.comment.CommentResponseDto;
import fr.diginamic.hubevenementiel.entities.Comment;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.mappers.CommentMapper;
import fr.diginamic.hubevenementiel.services.CommentService;

@RestController
@RequestMapping("/events/{eventId}/comments")
public class CommentController {

    private final CommentService commentService;
    private final CommentMapper commentMapper;

    public CommentController(CommentService commentService, CommentMapper commentMapper) {
        this.commentService = commentService;
        this.commentMapper = commentMapper;
    }

    @GetMapping
    public List<CommentResponseDto> getByEvent(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) throws HttpException {
        return commentService.findByEvent(eventId, page, size).stream()
                .map(commentMapper::toDto)
                .toList();
    }

    @PostMapping
    public ResponseEntity<CommentResponseDto> create(@PathVariable Long eventId, @RequestParam String content) throws HttpException {
        AppUserPrincipal principal = (AppUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Comment created = commentService.createComment(eventId, principal.id(), content);
        return ResponseEntity.status(HttpStatus.CREATED).body(commentMapper.toDto(created));
    }

    @Secured({"ROLE_MEMBER", "ROLE_ORGANIZER", "ROLE_ADMINISTRATOR"})
    @PutMapping("/{id}")
    public ResponseEntity<CommentResponseDto> update(@PathVariable Long eventId, @PathVariable Long commentId, @RequestParam String newContent) throws HttpException {
        AppUserPrincipal principal = (AppUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Comment updatedComment = commentService.updateComment(eventId, commentId, newContent, principal);
        return ResponseEntity.status(HttpStatus.OK).body(commentMapper.toDto(updatedComment));
    }

    @Secured({"ROLE_MEMBER", "ROLE_ORGANIZER", "ROLE_ADMINISTRATOR"})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long eventId, @PathVariable Long commentId) throws HttpException {
        AppUserPrincipal principal = (AppUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        commentService.deleteComment(eventId, commentId, principal);
        return ResponseEntity.noContent().build();
    }
}
