package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.Club;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.ConflictException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.ClubRepo;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ClubService {

    private final ClubRepo clubRepository;


    public ClubService(ClubRepo clubRepository) {
        this.clubRepository = clubRepository;
    }

    public List<Club> findAllClubs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        return clubRepository.findAll(pageable).getContent();
    }

    public Club findById(Long clubId) throws HttpException {
        Optional<Club> optionalClub = clubRepository.findById(clubId);

        if (optionalClub.isEmpty()) {
            throw new NotFoundException("Aucun club trouvé avec cet identifiant.");
        }

        return optionalClub.get();
    }

    public Club findByName(String clubName) throws HttpException {
        Optional<Club> optionalClub = clubRepository.findByName(clubName);

        if (optionalClub.isEmpty()) {
            throw new NotFoundException("Aucun club trouvé avec ce nom.");
        }

        return optionalClub.get();
    }

    public List<Club> findByCategory(int page, int size, Category category) {
        Pageable pageable = PageRequest.of(page, size);

        return clubRepository.findByCategory(category, pageable).getContent();
    }

    public List<Club> findByCity(int page, int size, String cityName) {
        Pageable pageable = PageRequest.of(page, size);

        return clubRepository.findByAddressCity(cityName, pageable).getContent();
    }

    @Transactional
    public Club createClub(Club club) throws HttpException {

        clubChecker(club);

        if (clubRepository.existsByName(club.getName())) {
            throw new ConflictException("Un club avec ce nom existe déjà.");
        }

        return clubRepository.save(club);
    }

    @Transactional
    public Club updateClub(Long id, Club modifiedClub) throws HttpException {

        Club existing = clubRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Club introuvable avec l'id " + id));

        clubChecker(modifiedClub);

        if (!existing.getName().equalsIgnoreCase(modifiedClub.getName())
                && clubRepository.existsByName(modifiedClub.getName())) {
            throw new ConflictException("Un club avec ce nom existe déjà.");
        }

        existing.setName(modifiedClub.getName());
        existing.setCategory(modifiedClub.getCategory());
        existing.setEmail(modifiedClub.getEmail());
        existing.setPhone(modifiedClub.getPhone());
        existing.setEndValidityDate(modifiedClub.getEndValidityDate());
        existing.setAddress(modifiedClub.getAddress());

        return clubRepository.save(existing);
    }

    @Transactional
    public void deleteClub(Long clubId) throws HttpException {

        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new NotFoundException("Aucun club n'a été trouvé avec cet identifiant."));

        clubRepository.delete(club);
    }

    public boolean clubChecker(Club club) throws HttpException {

        if (club == null) {
            throw new BadRequestException("Le club ne peut pas être nul.");
        }

        String name = club.getName();
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Le nom du club doit contenir au moins un caractère.");
        }
        if (name.length() > 150) {
            throw new BadRequestException("Le nom du club ne peut pas dépasser 150 caractères.");
        }

        if (club.getCategory() == null) {
            throw new BadRequestException("Vous devez choisir une catégorie pour le club.");
        }

        String email = club.getEmail();
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Le club doit avoir une adresse email.");
        }
        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$")) {
            throw new BadRequestException("L'adresse email n'est pas valide.");
        }

        String phone = club.getPhone();
        if (phone == null || phone.isBlank()) {
            throw new BadRequestException("Le club doit avoir un numéro de téléphone.");
        }
        if (phone.length() > 20) {
            throw new BadRequestException("Le numéro de téléphone ne peut pas dépasser 20 caractères.");
        }
        if (!phone.matches("^[0-9+ .()-]{6,20}$")) {
            throw new BadRequestException("Le numéro de téléphone n'est pas valide.");
        }

        if (club.getAddress() == null) {
            throw new BadRequestException("Le club doit avoir une adresse.");
        }

        return true;
    }


}
