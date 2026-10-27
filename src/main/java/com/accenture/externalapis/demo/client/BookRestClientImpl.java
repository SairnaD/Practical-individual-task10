package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;

@Component
public class BookRestClientImpl implements BookRestClient {

    private static final String BOOKS_PATH = "/books";

    private final RestClient restClient;

    public BookRestClientImpl(RestClient.Builder builder, ExternalServiceProperties properties) {
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public BookDto getBook(Long id) {
        try {
            BookApiResponse response = restClient.get()
                    .uri(BOOKS_PATH + "/{id}", id)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, res) -> {
                        throw new ClientException(
                                "Client error from external service: " + res.getStatusCode() + " for book id=" + id);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, res) -> {
                        throw new ClientException(
                                "Server error from external service: " + res.getStatusCode() + " for book id=" + id);
                    })
                    .body(BookApiResponse.class);
            return toDto(response, id);
        } catch (ResourceAccessException e) {
            throw new ClientException("External service unreachable or timed out (book id=" + id + ")", e);
        } catch (RestClientException e) {
            throw new ClientException("Failed to process response from external service (book id=" + id + ")", e);
        }
    }

    @Override
    public List<BookDto> getAllBooks() {
        try {
            BookApiResponse[] response = restClient.get()
                    .uri(BOOKS_PATH)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, res) -> {
                        throw new ClientException("Client error from external service: " + res.getStatusCode());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, res) -> {
                        throw new ClientException("Server error from external service: " + res.getStatusCode());
                    })
                    .body(BookApiResponse[].class);

            if (response == null) {
                throw new ClientException("External service returned an empty response");
            }
            return Arrays.stream(response)
                    .map(book -> toDto(book, book == null ? null : book.id()))
                    .toList();
        } catch (ResourceAccessException e) {
            throw new ClientException("External service unreachable or timed out", e);
        } catch (RestClientException e) {
            throw new ClientException("Failed to process response from external service", e);
        }
    }

    private BookDto toDto(BookApiResponse book, Long id) {
        if (book == null || book.title() == null || book.price() == null) {
            throw new ClientException("Malformed or empty book data from external service (book id=" + id + ")");
        }
        return new BookDto(book.title(), book.author(), book.genre(), book.price());
    }
}
