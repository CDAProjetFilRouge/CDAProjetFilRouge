package fr.diginamic.hubevenementiel.dtos.appUser;

import java.util.List;

public record PageResponseDto<T>(List<T> content, long totalElements, int totalPages, int page, int size) {
} 
