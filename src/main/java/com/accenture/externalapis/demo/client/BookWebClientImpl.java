package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import org.springframework.http.HttpStatusCode;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Component
public class BookWebClientImpl implements BookWebClient {

    private static final String BOOKS_PATH = "/books";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    private static final Logger log = LoggerFactory.getLogger(BookWebClientImpl.class);

    public BookWebClientImpl(WebClient.Builder builder, ExternalServiceProperties properties) {
        this.webClient = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public Mono<BookDto> getBookAsync(Long id) {
        return webClient.get()
                .uri(BOOKS_PATH + "/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, res -> Mono.error(
                        new ClientException("Client error from external service: "
                                + res.statusCode() + " for book id=" + id)))
                .onStatus(HttpStatusCode::is5xxServerError, res -> Mono.error(
                        new ClientException("Server error from external service: "
                                + res.statusCode() + " for book id=" + id, true)))
                .bodyToMono(BookApiResponse.class)
                .timeout(TIMEOUT)
                .switchIfEmpty(Mono.error(new ClientException(
                        "Empty response from external service (book id=" + id + ")")))
                .map(book -> toDto(book, id))
                .onErrorMap(this::isNotClientException, e -> new ClientException(
                        "External service unreachable, timed out or returned invalid data (book id=" + id + ")",
                        e,
                        e instanceof WebClientRequestException))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(300))
                        .filter(e -> e instanceof ClientException ce && ce.isRetryable())
                        .doBeforeRetry(signal -> log.warn("Retrying book id={} (attempt {}): {}",
                                id, signal.totalRetries() + 1, signal.failure().getMessage()))
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
    }

    @Override
    public Flux<BookDto> getAllBooksAsync() {
        return webClient.get()
                .uri(BOOKS_PATH)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, res -> Mono.error(
                        new ClientException("Client error from external service: " + res.statusCode())))
                .onStatus(HttpStatusCode::is5xxServerError, res -> Mono.error(
                        new ClientException("Server error from external service: " + res.statusCode())))
                .bodyToFlux(BookApiResponse.class)
                .timeout(TIMEOUT)
                .map(book -> toDto(book, book.id()))
                .onErrorMap(this::isNotClientException, e -> new ClientException(
                        "External service unreachable, timed out or returned invalid data", e));
    }

    @Override
    public Mono<List<BookDto>> getBooksInParallel(Long id1, Long id2) {
        return Mono.zip(getBookAsync(id1), getBookAsync(id2))
                .map(tuple -> List.of(tuple.getT1(), tuple.getT2()));
    }

    private boolean isNotClientException(Throwable e) {
        return !(e instanceof ClientException);
    }

    private BookDto toDto(BookApiResponse book, Long id) {
        if (book.title() == null || book.price() == null) {
            throw new ClientException("Malformed or empty book data from external service (book id=" + id + ")");
        }
        return new BookDto(book.title(), book.author(), book.genre(), book.price());
    }
}
