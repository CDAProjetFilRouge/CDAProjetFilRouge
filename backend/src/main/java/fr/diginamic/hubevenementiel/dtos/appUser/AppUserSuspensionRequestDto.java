package fr.diginamic.hubevenementiel.dtos.appUser;

import java.time.LocalDateTime;

public class AppUserSuspensionRequestDto {
    
    private LocalDateTime suspensionEndDate;

    public AppUserSuspensionRequestDto() {
    }

    public AppUserSuspensionRequestDto(LocalDateTime suspensionEndDate) {
        this.suspensionEndDate = suspensionEndDate;
    }

    public LocalDateTime getSuspensionEndDate() {
        return suspensionEndDate;
    }

    public void setSuspensionEndDate(LocalDateTime suspensionEndDate) {
        this.suspensionEndDate = suspensionEndDate;
    }

    
}
