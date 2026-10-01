package fr.diginamic.hubevenementiel.dtos.appUser;

import java.time.LocalDateTime;

public class AppUserSuspensionRequestDto {
    
    private LocalDateTime endSuspensionDate;

    public AppUserSuspensionRequestDto() {
    }

    public AppUserSuspensionRequestDto(LocalDateTime endSuspensionDate) {
        this.endSuspensionDate = endSuspensionDate;
    }

    public LocalDateTime getEndSuspensionDate() {
        return endSuspensionDate;
    }

    public void setEndSuspensionDate(LocalDateTime endSuspensionDate) {
        this.endSuspensionDate = endSuspensionDate;
    }

    
}
