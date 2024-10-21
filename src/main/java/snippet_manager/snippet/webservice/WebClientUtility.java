package snippet_manager.snippet.webservice;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

@Component
public class WebClientUtility {

  private final WebClient webClient;

  public WebClientUtility(WebClient.Builder webClientBuilder) {
    this.webClient = webClientBuilder
            .filter(authorizationHeaderFilter())
            .build();
  }

  private ExchangeFilterFunction authorizationHeaderFilter() {
    return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
      String token = getCurrentToken();
      if (token != null) {
        clientRequest = ClientRequest.from(clientRequest)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
      }
      return Mono.just(clientRequest);
    });
  }

  public InputStream getInputStream(String url) {
    Flux<DataBuffer> dataBufferFlux = this.webClient.get()
            .uri(url)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .retrieve()
            .bodyToFlux(DataBuffer.class);

    byte[] byteArray = getBytes(dataBufferFlux);

    return new ByteArrayInputStream(byteArray);
  }

  public <T> Mono<T> putFlux(Flux<DataBuffer> dataBufferFlux, String url, Class<T> responseType) {
    return this.webClient.put()
            .uri(url)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(BodyInserters.fromPublisher(dataBufferFlux, DataBuffer.class))
            .retrieve()
            .bodyToMono(responseType);
  }

  private byte[] getBytes(Flux<DataBuffer> dataBufferFlux) {
    byte[] byteArray = dataBufferFlux
            .flatMap(dataBuffer -> Flux.just(dataBuffer.asByteBuffer()))
            .collectList()
            .map(byteBuffers -> {
              int totalSize = byteBuffers.stream().mapToInt(buffer -> buffer.remaining()).sum();
              byte[] bytes = new byte[totalSize];
              int offset = 0;
              for (ByteBuffer buffer : byteBuffers) {
                int remaining = buffer.remaining();
                buffer.get(bytes, offset, remaining);
                offset += remaining;
              }
              return bytes;
            })
            .block();
    return byteArray;
  }

  public <T> Mono<T> getAsync(String url, Class<T> responseType) {
    return webClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(responseType);
  }

  public <T, R> Mono<ResponseEntity<R>> postAsync(String url, T body, Class<R> responseType) {
    return webClient.post()
            .uri(url)
            .bodyValue(body)
            .retrieve()
            .toEntity(responseType);
  }

  private String getCurrentToken() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
      return jwt.getTokenValue();
    }
    return null;
  }

  public <T> Mono<T> delete(String url, Class<T> responseEntityClass) {
    return webClient.delete()
            .uri(url)
            .retrieve()
            .bodyToMono(responseEntityClass);
  }
}