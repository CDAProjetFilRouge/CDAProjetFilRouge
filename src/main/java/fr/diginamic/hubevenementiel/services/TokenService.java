package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.Token;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.TokenRepo;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TokenService {

    private final TokenRepo tokenRepo;

    public TokenService(TokenRepo tokenRepo){
        this.tokenRepo = tokenRepo;
    }

    public List<Token> findAllToken() {
        return tokenRepo.findAll();
    }

    public Token findTokenById(Long id) throws HttpException {
        Optional<Token> token = tokenRepo.findById(id);
        if(token.isEmpty()){
            throw new NotFoundException("No token found with id: "+id);
        }

        return token.get();
    }

    @Transactional
    public void createToken(Token token) throws HttpException {
        if(token.getValue().isEmpty() || token.getValue() == null){
            throw new NotFoundException("Value for this token is empty");
        }else if(token.getTokenType() == null){
            throw new NotFoundException("Token has no type");
        }else if(token.getUser() == null){
            throw new NotFoundException("No user is associated with this token");
        }
        tokenRepo.save(token);
    }

    @Transactional
    public void updateToken(Token token) throws HttpException {
        Optional<Token> tokenDB = tokenRepo.findById(token.getId());

        if(tokenDB.isEmpty()){
            throw new NotFoundException("No token found with id: "+token.getId());
        }

        tokenDB.get().setValue(token.getValue());
        tokenDB.get().setCreationDateTime(token.getCreationDateTime());
        tokenDB.get().setExpirationDateTime(token.getExpirationDateTime());
        tokenDB.get().setUseDate(token.getUseDate());
        tokenDB.get().setPendingData(token.getPendingData());
        tokenDB.get().setTokenType(token.getTokenType());
        tokenDB.get().setUser(token.getUser());
    }

    @Transactional
    public void deleteToken(Long id) throws  HttpException {
        Optional<Token> token = tokenRepo.findById(id);

        if(token.isEmpty()){
            throw new NotFoundException("Not token found with id: "+id);
        }

        tokenRepo.delete(token.get());
    }
}
