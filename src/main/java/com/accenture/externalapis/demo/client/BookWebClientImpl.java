package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import com.accenture.externalapis.demo.dto.BookDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

// TODO: Make this class implement BookWebClient.
@Component
public class BookWebClientImpl implements BookWebClient {

    private WebClient webClient;

    public BookWebClientImpl(WebClient.Builder builder, ExternalServiceProperties properties) {
        // TODO (Task 2): this.webClient = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public Mono<BookDto> getBookAsync(Long id) {
        throw new UnsupportedOperationException("Task 2 is not implemented yet");
    }

    @Override
    public Flux<BookDto> getAllBooksAsync() {
        throw new UnsupportedOperationException("Task 2 is not implemented yet");
    }

    @Override
    public Mono<List<BookDto>> getBooksInParallel(Long id1, Long id2) {
        throw new UnsupportedOperationException("Task 2 is not implemented yet");
    }

}
