package fr.diginamic.hubevenementiel.services;

import com.sun.net.httpserver.HttpServer;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.entities.Image;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.repositories.ImageRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageServiceRemoteStorageTest {

    private record Received(String method, String path, String key, String contentType, byte[] body) {
    }

    @Mock
    private ImageRepo imageRepo;
    @Mock
    private EventService eventService;

    @InjectMocks
    private ImageService imageService;

    private HttpServer server;
    private final List<Received> received = new CopyOnWriteArrayList<>();
    private int responseStatus = 204;
    private Path uploadDir;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        uploadDir = tempDir;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            received.add(new Received(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst("X-Storage-Key"),
                    exchange.getRequestHeaders().getFirst("Content-Type"),
                    exchange.getRequestBody().readAllBytes()));
            exchange.sendResponseHeaders(responseStatus, -1);
            exchange.close();
        });
        server.start();

        ReflectionTestUtils.setField(imageService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(imageService, "storageUrl", "http://127.0.0.1:" + server.getAddress().getPort() + "/");
        ReflectionTestUtils.setField(imageService, "storageKey", "secret-key");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private void stubUpload() throws HttpException {
        Event event = new Event();
        event.setId(2L);
        when(eventService.findById(2L)).thenReturn(event);
        when(imageRepo.countByEventId(2L)).thenReturn(0L);
        lenient().when(imageRepo.save(any(Image.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void upload_withRemoteStorage_sendsTheFileAndKeepsNothingLocally() throws Exception {
        stubUpload();
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        Image saved = imageService.upload(2L, file);

        assertThat(received).hasSize(1);
        Received request = received.get(0);
        assertThat(request.method()).isEqualTo("PUT");
        assertThat(request.path()).isEqualTo("/internal/uploads/" + saved.getPath().replace("/uploads/", ""));
        assertThat(request.key()).isEqualTo("secret-key");
        assertThat(request.contentType()).isEqualTo("image/jpeg");
        assertThat(request.body()).containsExactly(1, 2, 3);
        try (Stream<Path> files = Files.list(uploadDir)) {
            assertThat(files).isEmpty();
        }
    }

    @Test
    void upload_whenRemoteRefuses_failsAndSavesNothing() throws Exception {
        stubUpload();
        responseStatus = 403;
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1});

        HttpException error = assertThrows(HttpException.class, () -> imageService.upload(2L, file));

        assertThat(error.getStatus().value()).isEqualTo(502);
    }

    @Test
    void delete_withRemoteStorage_sendsDeleteAndToleratesMissingFile() throws Exception {
        Image image = new Image();
        image.setPath("/uploads/3f2b8c1e-9d4a-4e7b-8f10-2a6c5d9e1b34.jpg");
        when(imageRepo.findById(1L)).thenReturn(Optional.of(image));
        responseStatus = 404;

        imageService.delete(1L);

        assertThat(received).hasSize(1);
        assertThat(received.get(0).method()).isEqualTo("DELETE");
        assertThat(received.get(0).path()).isEqualTo("/internal/uploads/3f2b8c1e-9d4a-4e7b-8f10-2a6c5d9e1b34.jpg");
    }

    @Test
    void upload_withoutKey_staysLocal() throws Exception {
        stubUpload();
        ReflectionTestUtils.setField(imageService, "storageKey", "");
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        imageService.upload(2L, file);

        assertThat(received).isEmpty();
        try (Stream<Path> files = Files.list(uploadDir)) {
            assertThat(files).hasSize(1);
        }
    }
}
