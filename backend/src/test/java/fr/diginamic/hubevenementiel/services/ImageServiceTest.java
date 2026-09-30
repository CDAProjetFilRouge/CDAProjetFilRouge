package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.entities.Image;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.repositories.ImageRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private ImageRepo imageRepo;
    @Mock
    private EventService eventService;

    @InjectMocks
    private ImageService imageService;

    private Event event;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        event = new Event();
        event.setId(2L);
        ReflectionTestUtils.setField(imageService, "uploadDir", tempDir.toString());
    }

    private MockMultipartFile validImage() {
        return new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }

    // ---------------------------------------------------------------
    // imageChecker
    // ---------------------------------------------------------------

    @Test
    void imageChecker_nullFile_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> imageService.imageChecker(2L, null));
    }

    @Test
    void imageChecker_emptyFile_throwsBadRequest() {
        MockMultipartFile empty = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[0]);
        assertThrows(BadRequestException.class, () -> imageService.imageChecker(2L, empty));
    }

    @Test
    void imageChecker_missingMimeType_throwsBadRequest() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", null, new byte[]{1});
        assertThrows(BadRequestException.class, () -> imageService.imageChecker(2L, file));
    }

    @Test
    void imageChecker_nonImageMimeType_throwsBadRequest() {
        MockMultipartFile pdf = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1});
        assertThrows(BadRequestException.class, () -> imageService.imageChecker(2L, pdf));
    }

    @Test
    void imageChecker_exactly5MB_passes() throws HttpException {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[5 * 1024 * 1024]);
        when(imageRepo.countByEventId(2L)).thenReturn(0L);

        assertThat(imageService.imageChecker(2L, file)).isTrue();
    }

    @Test
    void imageChecker_5MBPlusOneByte_throwsBadRequest() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[5 * 1024 * 1024 + 1]);

        assertThrows(BadRequestException.class, () -> imageService.imageChecker(2L, file));
    }

    @Test
    void imageChecker_alreadyAtMaxImages_throwsBadRequest() {
        when(imageRepo.countByEventId(2L)).thenReturn(10L);

        assertThrows(BadRequestException.class, () -> imageService.imageChecker(2L, validImage()));
    }

    @Test
    void imageChecker_belowMaxImages_passes() throws HttpException {
        when(imageRepo.countByEventId(2L)).thenReturn(9L);

        assertThat(imageService.imageChecker(2L, validImage())).isTrue();
    }

    // ---------------------------------------------------------------
    // upload
    // ---------------------------------------------------------------

    @Test
    void upload_firstImage_displayOrderIsZero() throws HttpException {
        when(eventService.findById(2L)).thenReturn(event);
        when(imageRepo.countByEventId(2L)).thenReturn(0L);
        when(imageRepo.save(any(Image.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Image result = imageService.upload(2L, validImage());

        assertThat(result.getDisplayOrder()).isZero();
        assertThat(result.getEvent()).isEqualTo(event);
    }

    @Test
    void upload_fileWithoutExtension_doesNotThrow() throws HttpException {
        when(eventService.findById(2L)).thenReturn(event);
        when(imageRepo.countByEventId(2L)).thenReturn(0L);
        when(imageRepo.save(any(Image.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile noExtension = new MockMultipartFile("file", "photo", "image/jpeg", new byte[]{1, 2, 3});

        assertDoesNotThrow(() -> imageService.upload(2L, noExtension));
    }

    // ---------------------------------------------------------------
    // delete
    // ---------------------------------------------------------------

    @Test
    void delete_fileAlreadyMissingOnDisk_doesNotThrow() throws HttpException {
        Image image = new Image();
        image.setId(1L);
        image.setPath("/uploads/inexistant.jpg");
        when(imageRepo.findById(1L)).thenReturn(java.util.Optional.of(image));

        assertDoesNotThrow(() -> imageService.delete(1L));
    }
}
