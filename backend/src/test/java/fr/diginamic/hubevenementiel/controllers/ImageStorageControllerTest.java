package fr.diginamic.hubevenementiel.controllers;

import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.ConflictException;
import fr.diginamic.hubevenementiel.exceptions.ForbiddenException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageStorageControllerTest {

    private static final String KEY = "secret-key";
    private static final String NAME = "3f2b8c1e-9d4a-4e7b-8f10-2a6c5d9e1b34.jpg";

    private final ImageStorageController controller = new ImageStorageController();
    private Path dir;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        dir = tempDir;
        ReflectionTestUtils.setField(controller, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(controller, "storageKey", KEY);
    }

    private MockHttpServletRequest image(byte[] content) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("image/jpeg");
        request.setContent(content);
        return request;
    }

    @Test
    void store_validRequest_writesTheFile() throws Exception {
        var response = controller.store(NAME, KEY, image(new byte[]{1, 2, 3}));

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(Files.readAllBytes(dir.resolve(NAME))).containsExactly(1, 2, 3);
    }

    @Test
    void store_keyNotConfigured_isNotFound() {
        ReflectionTestUtils.setField(controller, "storageKey", "");

        assertThrows(NotFoundException.class, () -> controller.store(NAME, KEY, image(new byte[]{1})));
    }

    @Test
    void store_missingKey_isForbidden() {
        assertThrows(ForbiddenException.class, () -> controller.store(NAME, null, image(new byte[]{1})));
    }

    @Test
    void store_wrongKey_isForbidden() {
        assertThrows(ForbiddenException.class, () -> controller.store(NAME, "other", image(new byte[]{1})));
        assertThat(dir.resolve(NAME)).doesNotExist();
    }

    @Test
    void store_pathTraversalName_isRejected() {
        assertThrows(BadRequestException.class, () -> controller.store("../evil.jpg", KEY, image(new byte[]{1})));
        assertThrows(BadRequestException.class, () -> controller.store("photo.jpg", KEY, image(new byte[]{1})));
    }

    @Test
    void store_nonImageContentType_isRejected() {
        MockHttpServletRequest request = image(new byte[]{1});
        request.setContentType("application/pdf");

        assertThrows(BadRequestException.class, () -> controller.store(NAME, KEY, request));
    }

    @Test
    void store_emptyBody_isRejected() {
        assertThrows(BadRequestException.class, () -> controller.store(NAME, KEY, image(new byte[0])));
    }

    @Test
    void store_tooLarge_isRejected() {
        byte[] big = new byte[5 * 1024 * 1024 + 1];

        assertThrows(BadRequestException.class, () -> controller.store(NAME, KEY, image(big)));
        assertThat(dir.resolve(NAME)).doesNotExist();
    }

    @Test
    void store_existingFile_isConflictAndNotOverwritten() throws Exception {
        Files.write(dir.resolve(NAME), new byte[]{9});

        assertThrows(ConflictException.class, () -> controller.store(NAME, KEY, image(new byte[]{1})));
        assertThat(Files.readAllBytes(dir.resolve(NAME))).containsExactly(9);
    }

    @Test
    void remove_existingFile_deletesIt() throws Exception {
        Files.write(dir.resolve(NAME), new byte[]{1});

        var response = controller.remove(NAME, KEY);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(dir.resolve(NAME)).doesNotExist();
    }

    @Test
    void remove_missingFile_isNotFound() {
        assertThrows(NotFoundException.class, () -> controller.remove(NAME, KEY));
    }

    @Test
    void remove_wrongKey_isForbidden() throws Exception {
        Files.write(dir.resolve(NAME), new byte[]{1});

        assertThrows(ForbiddenException.class, () -> controller.remove(NAME, "other"));
        assertThat(dir.resolve(NAME)).exists();
    }
}
