package fr.diginamic.hubevenementiel.mappers;

import fr.diginamic.hubevenementiel.dtos.comment.CommentRequestDto;
import fr.diginamic.hubevenementiel.dtos.comment.CommentResponseDto;
import fr.diginamic.hubevenementiel.entities.Comment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

    @Autowired
    private AppUserSummaryMapper appUserSummaryMapper;

    public CommentResponseDto toDto(Comment comment) {
        if (comment == null) {
            return null;
        }

        CommentResponseDto dto = new CommentResponseDto();
        dto.setId(comment.getId());
        dto.setAuthor(appUserSummaryMapper.toDto(comment.getAuthor()));
        dto.setContent(comment.getContent());
        dto.setCreationDate(comment.getCreationDate());

        return dto;
    }

    public Comment toEntity(CommentRequestDto dto) {
        Comment entity = new Comment();
        entity.setContent(dto.getContent());

        return entity;
    }
}
