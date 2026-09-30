package fr.diginamic.hubevenementiel.dtos.comment;


import fr.diginamic.hubevenementiel.dtos.appUser.AppUserSummaryResponseDto;

import java.time.LocalDateTime;

public class CommentResponseDto {

    private Long id;
    private AppUserSummaryResponseDto author;
    private String content;
    private LocalDateTime creationDate;

    public CommentResponseDto() {
    }

    public CommentResponseDto(Long id, AppUserSummaryResponseDto author, String content, LocalDateTime creationDate) {
        this.id = id;
        this.author = author;
        this.content = content;
        this.creationDate = creationDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppUserSummaryResponseDto getAuthor() {
        return author;
    }

    public void setAuthor(AppUserSummaryResponseDto author) {
        this.author = author;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }
}
