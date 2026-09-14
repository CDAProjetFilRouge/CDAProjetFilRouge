package fr.diginamic.hubevenementiel.dtos.comment;

public class CommentRequestDto {

    private String content;

    public CommentRequestDto() {
    }

    public CommentRequestDto(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}