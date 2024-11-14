package snippetmanager.webservice.printscript;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClientResponseException;
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
    try {
      Mono<ResponseEntity<String>> response = webClientUtility.postAsync(url, body, String.class);
      return response.block(Duration.ofSeconds(timeOutInSeconds));
    } catch (WebClientResponseException.InternalServerError ex) {

      String errorMessage = ex.getResponseBodyAsString();
      throw new RuntimeException(
          "Compilation failed: " + (errorMessage.isEmpty() ? ex.getStatusText() : errorMessage),
          ex);
    } catch (WebClientResponseException ex) {

      String errorMessage = ex.getResponseBodyAsString();
      throw new RuntimeException(
          "HTTP error: "
              + ex.getStatusCode()
              + " - "
              + (errorMessage.isEmpty() ? ex.getMessage() : errorMessage),
          ex);
    } catch (Exception ex) {

      throw new RuntimeException("Unexpected error during compilation", ex);
    }
  }

  public ResponseEntity<String> test(
      InputStream content,
      String language,
      String version,
      List<String> inputs,
      List<String> outputs) {
    String resource = convertInputStreamToString(content);
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("content", resource);
    body.add("language", language);
    body.add("version", version);

    if (inputs.isEmpty()) {
      body.add("input", new String[0]);
    } else {
      inputs.forEach(input -> body.add("input", input));
    }

    if (outputs.isEmpty()) {
      body.add("output", new String[0]);
    } else {
      outputs.forEach(output -> body.add("output", output));
    }

    String url = printscriptServiceUrl + "/api/test";
    Mono<ResponseEntity<String>> response = webClientUtility.postAsync(url, body, String.class);
    return response.block(Duration.ofSeconds(timeOutInSeconds));
  }

  private String convertInputStreamToString(InputStream inputStream) {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      return reader.lines().collect(Collectors.joining("\n"));
    } catch (IOException e) {
      throw new RuntimeException("Error reading InputStream", e);
    }
  }
}
