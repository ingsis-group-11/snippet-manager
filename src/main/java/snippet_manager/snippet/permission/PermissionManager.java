package snippet_manager.snippet.permission;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import snippet_manager.snippet.model.dtos.PermissionDTO;
import snippet_manager.snippet.util.WebClientUtility;

import java.time.Duration;

@Component
public class PermissionManager {
  @Autowired
  WebClientUtility webClientUtility;

  private final int timeOutInSeconds = 30;
  @Value("${permission.manager.url}")
  private String permissionManagerUrl;

  public boolean canAccess(Long userId, Long snippetId) {
    System.out.println("Checking permission for user " + userId + " and snippet " + snippetId);
    PermissionDTO permissionDTO = PermissionDTO.builder()
            .snippetId(snippetId)
            .userId(userId)
            .build();

    String url = permissionManagerUrl + "/api/permission/";
    Mono<ResponseEntity<Boolean>> response = webClientUtility.postAsync(
            url,
            permissionDTO,
            Boolean.class
    );

    ResponseEntity<Boolean> result = response.block(Duration.ofSeconds(timeOutInSeconds));

    if (result != null && result.getStatusCode() == HttpStatus.OK) {
      Boolean hasAccess = result.getBody();
      return hasAccess != null && hasAccess;
    } else {
      return false;
    }
  }

  public ResponseEntity<String> newPermission(Long userId, Long snippetId){
    PermissionDTO permissionDTO = PermissionDTO.builder()
            .snippetId(snippetId)
            .userId(userId)
            .build();
    String url = permissionManagerUrl + "/api/permission/new-permision";
    Mono<ResponseEntity<String>> response = webClientUtility.postAsync(url, permissionDTO, String.class);
    return response.block(Duration.ofSeconds(timeOutInSeconds));
  }
}
