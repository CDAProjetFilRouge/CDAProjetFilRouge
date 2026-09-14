package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import jakarta.transaction.Transactional;
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

    @Transactional
    public void createApp(AppUser appUser) throws HttpException {
        Optional <AppUser> a = userRepo.findByEmail(appUser.getEmail());
        if(a.isEmpty()){
            throw new NotFoundException("AppUser already exists with this email address");
        }
        userRepo.save(appUser);
    }

    @Transactional
    public void updateAppUser(AppUser appUser) throws HttpException {
        Optional<AppUser> a = userRepo.findById(appUser.getId());

        if(a.isEmpty()){
            throw new NotFoundException("No AppUser found with id: "+appUser.getId());
        }

        a.get().setLastName(appUser.getLastName());
        a.get().setFirstName(appUser.getFirstName());
        a.get().setEmail(appUser.getEmail());
        a.get().setHashedPassword(appUser.getHashedPassword());
        a.get().setPhone(appUser.getPhone());
        a.get().setRole(appUser.getRole());
        a.get().setStatus(appUser.getStatus());
        a.get().setSuspensionEndDate(appUser.getSuspensionEndDate());
        a.get().setCreationDate(appUser.getCreationDate());
        a.get().setAddress(appUser.getAddress());
        a.get().setRequesters(appUser.getRequesters());
        a.get().setAdmins(appUser.getAdmins());
        a.get().setLegalDocumentList(appUser.getLegalDocumentList());
        a.get().setClubs(appUser.getClubs());
    }

    @Transactional
    public void deleteAppUser(Long id) throws HttpException {
        Optional<AppUser> a = userRepo.findById(id);
        if(a.isEmpty()){
            throw new NotFoundException("No AppUser was found with id: "+id);
        }

        userRepo.delete(a.get());
    }
}
