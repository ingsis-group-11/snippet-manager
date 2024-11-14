package snippetmanager.webservice;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class WebClientUtility {

  private final WebClient webClient;

  public WebClientUtility(WebClient.Builder webClientBuilder) {
    this.webClient = webClientBuilder.filter(authorizationHeaderFilter()).build();
  }

  private String getCurrentToken() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
      return jwt.getTokenValue();
    }
    return null;
  }

  private ExchangeFilterFunction authorizationHeaderFilter() {
    return ExchangeFilterFunction.ofRequestProcessor(
        clientRequest -> {
          String token = getCurrentToken();
          if (token != null) {
            clientRequest =
                ClientRequest.from(clientRequest)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();
          }
          return Mono.just(clientRequest);
        });
  }

  public InputStream getInputStream(String url) {
    Flux<DataBuffer> dataBufferFlux =
        this.webClient
            .get()
            .uri(url)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .retrieve()
            .bodyToFlux(DataBuffer.class);

    System.out.println("DataBufferFlux: " + dataBufferFlux);

    byte[] byteArray = getBytes(dataBufferFlux);

    System.out.println("ByteArray: " + byteArray);

    return new ByteArrayInputStream(byteArray);
  }

  public <T> Mono<ResponseEntity<T>> putFlux(
      Flux<DataBuffer> dataBufferFlux, String url, Class<T> responseType) {
    return this.webClient
        .put()
        .uri(url)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(BodyInserters.fromPublisher(dataBufferFlux, DataBuffer.class))
        .retrieve()
        .toEntity(responseType);
  }

  public <T> Mono<ResponseEntity<T>> getAsync(
      String url, ParameterizedTypeReference<T> responseType) {
    return webClient.get().uri(url).retrieve().toEntity(responseType);
  }

  public <T, R> Mono<ResponseEntity<R>> postAsync(String url, T body, Class<R> responseType) {
    Mono<ResponseEntity<R>> response =
        webClient.post().uri(url).bodyValue(body).retrieve().toEntity(responseType);
    return response;
  }

  public <T, R> Mono<ResponseEntity<R>> putAsync(String url, T body, Class<R> responseType) {
    return webClient.put().uri(url).bodyValue(body).retrieve().toEntity(responseType);
  }

  public <T> Mono<ResponseEntity<T>> deleteAsync(String url, Class<T> responseEntityClass) {
    return webClient.delete().uri(url).retrieve().toEntity(responseEntityClass);
  }

  private byte[] getBytes(Flux<DataBuffer> dataBufferFlux) {
    DataBuffer dataBuffer = DataBufferUtils.join(dataBufferFlux).block();
    assert dataBuffer != null;
    byte[] byteArray = new byte[dataBuffer.readableByteCount()];
    dataBuffer.read(byteArray);
    DataBufferUtils.release(dataBuffer);
    return byteArray;
  }
}
