package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.AnonymizationDemand;
import fr.diginamic.hubevenementiel.enums.RequestStatus;
import fr.diginamic.hubevenementiel.repositories.AnonymizationDemandRepo;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnonymizationDemandService {

    private final AnonymizationDemandRepo anonymizationDemandRepo;


    public AnonymizationDemandService(AnonymizationDemandRepo anonymizationDemandRepo) {
        this.anonymizationDemandRepo = anonymizationDemandRepo;
    }

    public List<AnonymizationDemand> search(int page, int size, RequestStatus status) {
        Pageable pageable = PageRequest.of(page, size);

        if (status != null) {
            return anonymizationDemandRepo.findByRequestStatus(status, pageable).getContent();
        }

        return anonymizationDemandRepo.findAll(pageable).getContent();
    }


}
