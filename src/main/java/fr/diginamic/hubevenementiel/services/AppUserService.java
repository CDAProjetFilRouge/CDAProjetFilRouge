package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import org.aspectj.weaver.ast.Not;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AppUserService {

    private UserRepo userRepo;

    public AppUserService(UserRepo userRepo){
        this.userRepo = userRepo;
    }

    public List<AppUser> getAll(){
        return userRepo.findAll();
    }

    public AppUser findById(Long id) throws NotFoundException {
        AppUser user = userRepo.findById(id).orElseThrow(() -> new NotFoundException("User not found with id: "+id));

        return user;
    }

    public List<AppUser> findByLastName(String lastName, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<AppUser> users = userRepo.findByLastName(lastName, pageable).getContent();
        if(users.isEmpty()){
            throw new NotFoundException("No users found with last name: "+lastName);
        }

        return  users;
    }

    public List<AppUser> findByFirstName(String firstName, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<AppUser> users = userRepo.findByFirstName(firstName, pageable).getContent();
        if(users.isEmpty()){
            throw new NotFoundException("No users found with last name: "+firstName);
        }

        return users;
    }

    public AppUser findByEmail(String email) throws HttpException {
        AppUser user = userRepo.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found with this email: "+email));

        return user;
    }

    public List<AppUser> findByRole(Role role, int page, int size) throws HttpException{
        Pageable pageable = PageRequest.of(page, size);
        List<AppUser> users = userRepo.findByRole(role, pageable).getContent();
        if(users.isEmpty()){
            throw new NotFoundException("No users found with role: "+role);
        }

        return users;
    }

    public List<AppUser> findByStatus(AccountStatus status, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<AppUser> users = userRepo.findByStatus(status, pageable).getContent();
        if(users.isEmpty()){
            throw new NotFoundException("No users found with status: "+status);
        }

        return users;
    }

    public List<AppUser> findBySuspensionEndDate(LocalDateTime dateMin, LocalDateTime dateMax, int page, int size) throws HttpException{
        Pageable pageable = PageRequest.of(page, size);
        List<AppUser> users = userRepo.findBySuspensionEndDateBetween(dateMin, dateMax, pageable).getContent();
        if(users.isEmpty()){
            throw new NotFoundException("No users found with end date between "+dateMin+" and "+dateMax);
        }

        return users;
    }

    public List<AppUser> findByCreationDateBetween(LocalDate dateMin, LocalDate dateMax, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<AppUser> users = userRepo.findByCreationDateBetween(dateMin, dateMax, pageable).getContent();
        if(users.isEmpty()){
            throw new NotFoundException("No users found with creation date between"+dateMin+" and "+dateMax);
        }

        return users;
    }
}
