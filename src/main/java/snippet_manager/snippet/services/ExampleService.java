package snippet_manager.snippet.services;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import snippet_manager.snippet.util.WebClientUtility;

@Service
public class ExampleService {
  private final WebClientUtility webClientUtility;

  public ExampleService(WebClientUtility webClientUtility) {
    this.webClientUtility = webClientUtility;
  }

  public Mono<String> getExample(String url) {
    return webClientUtility.postAsync(url, "hola", String.class);
  }
}
