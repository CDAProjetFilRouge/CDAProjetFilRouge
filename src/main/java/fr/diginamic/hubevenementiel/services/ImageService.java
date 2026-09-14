package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.Image;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.ImageRepo;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ImageService {

    private final ImageRepo imageRepo;

    public ImageService (ImageRepo imageRepo){
        this.imageRepo = imageRepo;
    }

    public List<Image> getAllImage(){
        return imageRepo.findAll();
    }

    public Image getImageById(Long id) throws HttpException {
        Optional<Image> i = imageRepo.findById(id);

        if(i.isEmpty()){
            throw new NotFoundException("No image found with id: "+id);
        }

        return i.get();
    }

    @Transactional
    public void createImage(Image image){
        imageRepo.save(image);
    }

    @Transactional
    public void updateImage(Image image) throws HttpException {
        Optional<Image> i = imageRepo.findById(image.getId());
        if(i.isEmpty()){
            throw new NotFoundException("No image found with this id: "+image.getId());
        }

        i.get().setFileName(image.getFileName());
        i.get().setPath(image.getPath());
        i.get().setMimeType(image.getMimeType());
        i.get().setSizeByte(image.getSizeByte());
        i.get().setUploadDate(image.getUploadDate());
        i.get().setDisplayOrder(image.getDisplayOrder());
        i.get().setEvent(image.getEvent());
    }
}
