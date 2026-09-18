package fr.diginamic.hubevenementiel.controllers;

import java.util.List;

import fr.diginamic.hubevenementiel.entities.AppUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.diginamic.hubevenementiel.dtos.club.ClubRequestDto;
import fr.diginamic.hubevenementiel.dtos.club.ClubResponseDto;
import fr.diginamic.hubevenementiel.dtos.club.ClubSummaryResponseDto;
import fr.diginamic.hubevenementiel.entities.Club;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.mappers.ClubMapper;
import fr.diginamic.hubevenementiel.mappers.ClubSummaryMapper;
import fr.diginamic.hubevenementiel.openapi.ClubApi;
import fr.diginamic.hubevenementiel.services.ClubService;

@RestController
@RequestMapping("/clubs")
public class ClubController implements ClubApi {

    private final ClubService clubService;
    private final ClubMapper clubMapper;
    private final ClubSummaryMapper clubSummaryMapper;

    public ClubController(ClubService clubService, ClubMapper clubMapper, ClubSummaryMapper clubSummaryMapper) {
        this.clubService = clubService;
        this.clubMapper = clubMapper;
        this.clubSummaryMapper = clubSummaryMapper;
    }

    @Override
    @GetMapping
    public List<ClubSummaryResponseDto> getClubs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) String city) {
        return clubService.search(page, size, category, city).stream()
                .map(clubSummaryMapper::toDto)
                .toList();
    }

    @Override
    @GetMapping("/{id}")
    public ClubResponseDto getById(@PathVariable Long id) throws HttpException {
        return clubMapper.toDto(clubService.findById(id));
    }

    @Override
    @Secured("ROLE_ADMINISTRATOR")
    @PostMapping
    public ResponseEntity<ClubResponseDto> create(@RequestBody ClubRequestDto requestDto) throws HttpException {
        Club club = clubMapper.toEntity(requestDto);
        Club created = clubService.createClub(club);
        return ResponseEntity.status(HttpStatus.CREATED).body(clubMapper.toDto(created));
    }

    @Override
    @Secured("ROLE_ADMINISTRATOR")
    @PutMapping("/{id}")
    public ClubResponseDto update(@PathVariable Long id, @RequestBody ClubRequestDto requestDto) throws HttpException {
        Club club = clubMapper.toEntity(requestDto);
        Club updated = clubService.updateClub(id, club);
        return clubMapper.toDto(updated);
    }

    @Override
    @Secured("ROLE_ADMINISTRATOR")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) throws HttpException {
        clubService.deleteClub(id);
        return ResponseEntity.noContent().build();
    }

    @Secured("ROLE_ORGANIZER")
    @PostMapping("associateUser/{idUser}/club/{idClub}")
    public ResponseEntity<String> associateUser(@PathVariable Long idUser, @PathVariable Long idClub) throws HttpException{
        clubService.associateUserToClub(idClub, idUser);
        return ResponseEntity.ok("Le member avec l'id: "+idUser+" a bien été ajouté au club avec l'id: "+idClub);
    }
}
