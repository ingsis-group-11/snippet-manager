package snippetmanager.webservice.printscript;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;
import snippetmanager.util.enums.CodeLanguage;
import snippetmanager.webservice.WebClientUtility;

@Component
public class PrintscriptManager {
  @Autowired WebClientUtility webClientUtility;

  private final int timeOutInSeconds = 30;

  @Value("${printscript.service.url}")
  private String printscriptServiceUrl;

  public ResponseEntity<String> compile(String code, CodeLanguage language, String version) {
    ByteArrayResource resource = new ByteArrayResource(code.getBytes());

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("code", resource);
    body.add("language", language);
    body.add("version", version);

    String url = printscriptServiceUrl + "/api/compile";
    Mono<ResponseEntity<String>> response = webClientUtility.postAsync(url, body, String.class);
    return response.block(Duration.ofSeconds(timeOutInSeconds));
  }
}
