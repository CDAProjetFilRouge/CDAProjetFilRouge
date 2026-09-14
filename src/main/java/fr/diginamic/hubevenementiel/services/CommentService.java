package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.Comment;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.CommentRepo;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class CommentService {

    CommentRepo commentRepo;

    public CommentService(CommentRepo commentRepo){
        this.commentRepo = commentRepo;
    }

    public List<Comment> getAllComment(){
        return commentRepo.findAll();
    }

    public Comment findCommentById(Long id) throws HttpException {
        Optional<Comment> c = commentRepo.findById(id);
        if(c.isEmpty()){
            throw new NotFoundException("No comment found with id: "+id);
        }

        return c.get();
    }

    public List<Comment> findByAuthorId(Long id, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<Comment> comments = commentRepo.findByAuthorId(id, pageable).getContent();

        if (comments.isEmpty()){
            throw new NotFoundException("No comments found for author with id: "+id);
        }

        return comments;
    }

    public List<Comment> findByEventId(Long id, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<Comment> comments = commentRepo.findByEventId(id, pageable).getContent();

        if(comments.isEmpty()){
            throw new NotFoundException("No comments found with event id: "+id);
        }

        return comments;
    }

    public List<Comment> findByCreationDate(LocalDateTime dateMin, LocalDateTime dateMax, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<Comment> comments = commentRepo.findByCreationDateBetween(dateMin, dateMax, pageable).getContent();

        if(comments.isEmpty()){
            throw new NotFoundException("Not comment found between "+dateMin+" and "+dateMax);
        }

        return comments;
    }

    @Transactional
    public void createComment(Comment comment){
        commentRepo.save(comment);
    }

    @Transactional
    public void updateComment (Comment comment) throws HttpException {
        Optional<Comment> c = commentRepo.findById(comment.getId());

        if(c.isEmpty()){
            throw new NotFoundException("No comment found with id: "+comment.getContent());
        }

        c.get().setAuthor(comment.getAuthor());
        c.get().setContent(comment.getContent());
        c.get().setEvent(comment.getEvent());
        c.get().setCreationDate(comment.getCreationDate());
    }

    @Transactional
    public void deleteComment (Long id) throws HttpException {
        Optional<Comment> c = commentRepo.findById(id);

        if(c.isEmpty()){
            throw new NotFoundException("No comment found with id: "+id);
        }

        commentRepo.delete(c.get());
    }
}
