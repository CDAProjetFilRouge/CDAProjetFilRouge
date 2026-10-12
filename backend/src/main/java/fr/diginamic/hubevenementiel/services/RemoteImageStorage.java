package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.exceptions.HttpException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class RemoteImageStorage {

    public static final String KEY_HEADER = "X-Storage-Key";
    public static final String ENDPOINT = "/internal/uploads/";

    private final String baseUrl;
    private final String key;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public RemoteImageStorage(String baseUrl, String key) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.key = key;
    }

    public void put(String storedName, byte[] content, String contentType) throws HttpException {
        HttpRequest request = request(storedName)
                .header("Content-Type", contentType)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(content))
                .build();
        send(request, false);
    }

    public void delete(String storedName) throws HttpException {
        send(request(storedName).DELETE().build(), true);
    }

    private HttpRequest.Builder request(String storedName) {
        return HttpRequest.newBuilder(URI.create(baseUrl + ENDPOINT + storedName))
                .timeout(Duration.ofSeconds(20))
                .header(KEY_HEADER, key);
    }

    private void send(HttpRequest request, boolean missingIsFine) throws HttpException {
        try {
            int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            boolean ok = status >= 200 && status < 300 || missingIsFine && status == 404;
            if (!ok) {
                throw new HttpException("Le stockage distant des images a refusé la requête (code " + status + ").",
                        HttpStatus.BAD_GATEWAY);
            }
        } catch (IOException e) {
            throw new HttpException("Le stockage distant des images est injoignable.", HttpStatus.BAD_GATEWAY);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HttpException("L'envoi vers le stockage distant a été interrompu.", HttpStatus.BAD_GATEWAY);
        }
    }
}
