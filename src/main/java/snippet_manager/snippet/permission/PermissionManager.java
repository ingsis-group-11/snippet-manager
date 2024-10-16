package snippet_manager.snippet.permission;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import snippet_manager.snippet.model.dtos.PermissionDTO;
import snippet_manager.snippet.util.PermissionType;
import snippet_manager.snippet.util.WebClientUtility;

import java.time.Duration;
import java.util.UUID;

@Component
public class PermissionManager {
  @Autowired
  WebClientUtility webClientUtility;

  private final int timeOutInSeconds = 30;
  @Value("${permission.manager.url}")
  private String permissionManagerUrl;

  public boolean canRead(Long userId, UUID snippetId) {
    PermissionDTO body = PermissionDTO.builder()
            .snippetId(snippetId.toString())
            .userId(userId)
            .permission(PermissionType.READ)
            .build();

    return fetchPermissionData(body);
  }

  public boolean canWrite(Long userId, UUID snippetId) {
    PermissionDTO body = PermissionDTO.builder()
            .snippetId(snippetId.toString())
            .userId(userId)
            .permission(PermissionType.READ_WRITE)
            .build();

    return fetchPermissionData(body);
  }

  public ResponseEntity<String> createNewPermission(Long userId, UUID snippetId){
    PermissionDTO permissionDTO = PermissionDTO.builder()
            .snippetId(snippetId.toString())
            .userId(userId)
            .build();
    String url = permissionManagerUrl + "/api/permission/new-permision";
    Mono<ResponseEntity<String>> response = webClientUtility.postAsync(url, permissionDTO, String.class);
    return response.block(Duration.ofSeconds(timeOutInSeconds));
  }

  public boolean canDelete(Long userId, UUID snippetId) {
    PermissionDTO body = PermissionDTO.builder()
            .snippetId(snippetId.toString())
            .userId(userId)
            .permission(PermissionType.DELETE)
            .build();

    return fetchPermissionData(body);
  }

  private boolean fetchPermissionData(PermissionDTO body) {
    String url = permissionManagerUrl + "/api/permission/";
    Mono<ResponseEntity<Boolean>> response = webClientUtility.postAsync(
            url,
            body,
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
}
