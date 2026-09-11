package fr.diginamic.hubevenementiel.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.diginamic.hubevenementiel.entities.Comment;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.services.CommentService;

@RestController
@RequestMapping("/events/{eventId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public List<Comment> getByEvent(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return commentService.findByEvent(eventId, page, size);
    }

    @PostMapping
    public ResponseEntity<Comment> create(
            @PathVariable Long eventId,
            @RequestParam Long authorId,
            @RequestParam String content) throws HttpException {
        Comment created = commentService.create(eventId, authorId, content);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long eventId, @PathVariable Long id) throws HttpException {
        commentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
