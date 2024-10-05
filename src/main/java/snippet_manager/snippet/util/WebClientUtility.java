package snippet_manager.snippet.util;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class WebClientUtility {

  private final WebClient webClient;

  public WebClientUtility(WebClient.Builder webClientBuilder) {
    this.webClient = webClientBuilder.build();
  }

  public <T> T get(String url,  Class<T> responseType) {
    return webClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(responseType)
            .block();
  }

  public <T> String post(String url, T requestBody) {
    return webClient.post()
            .uri(url)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .block();
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
}