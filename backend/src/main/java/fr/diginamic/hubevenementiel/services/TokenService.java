package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Token;
import fr.diginamic.hubevenementiel.enums.TokenType;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.TokenRepo;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TokenService {

    private final TokenRepo tokenRepo;

    public TokenService(TokenRepo tokenRepo){
        this.tokenRepo = tokenRepo;
    }

    public List<Token> findAllToken() {
        return tokenRepo.findAll();
    }

    public Token findTokenById(UUID id) throws HttpException {
        Optional<Token> token = tokenRepo.findById(id);
        if(token.isEmpty()){
            throw new NotFoundException("No token found with id: "+id);
        }

        return token.get();
    }

    @Transactional
    public void createToken(Token token) throws HttpException {
        if(token.getValue() == null || token.getValue().isEmpty()){
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

    /**
     * Retrieves a token and checks it can still be used.
     *
     * @param tokenValue   value of the token
     * @param expectedType type the token must have
     * @return the token
     * @throws HttpException if the token is unknown, of the wrong type, already used or expired
     */
    public Token getValidToken(String tokenValue, TokenType expectedType) throws HttpException {
        Token token = tokenRepo.findByValue(tokenValue)
                .orElseThrow(() -> new BadRequestException("Token invalide."));

        if (token.getTokenType() != expectedType) {
            throw new BadRequestException("Type de token incorrect.");
        }

        if (token.getUseDate() != null) {
            throw new BadRequestException("Ce token a déjà été utilisé.");
        }

        if (token.getExpirationDateTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Ce token a expiré.");
        }

        return token;
    }

    /**
     * Marks a token as used, so it cannot be consumed twice.
     */
    public void markUsed(Token token) {
        token.setUseDate(LocalDateTime.now());
        tokenRepo.save(token);
    }

    /**
     * Creates and stores a new token for a user.
     *
     * @param user        owner of the token
     * @param type        type of the token
     * @param validity    how long the token stays valid
     * @param pendingData data kept with the token until it is confirmed (empty if none)
     * @return the stored token
     */
    public Token issue(AppUser user, TokenType type, Duration validity, String pendingData) {
        Token token = new Token();
        token.setValue(UUID.randomUUID().toString());
        token.setTokenType(type);
        token.setUser(user);
        token.setCreationDateTime(LocalDateTime.now());
        token.setExpirationDateTime(LocalDateTime.now().plus(validity));
        token.setPendingData(pendingData);
        tokenRepo.save(token);
        return token;
    }

    @Transactional
    public void deleteToken(UUID id) throws  HttpException {
        Optional<Token> token = tokenRepo.findById(id);

        if(token.isEmpty()){
            throw new NotFoundException("Not token found with id: "+id);
        }

        tokenRepo.delete(token.get());
    }
}
