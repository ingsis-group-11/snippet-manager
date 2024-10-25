package snippetmanager.webservice.permission;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import snippetmanager.model.dtos.webservice.PermissionDto;
import snippetmanager.util.PermissionType;
import snippetmanager.webservice.WebClientUtility;

@Component
public class PermissionManager {
  @Autowired WebClientUtility webClientUtility;

  private final int timeOutInSeconds = 30;

  @Value("${permission.manager.url}")
  private String permissionManagerUrl;

  public boolean canRead(String userId, String snippetId) {
    PermissionDto body =
        PermissionDto.builder()
            .assetId(snippetId)
            .userId(userId)
            .permission(PermissionType.READ)
            .build();

    return fetchPermissionData(body);
  }

  public boolean canWrite(String userId, String snippetId) {
    PermissionDto body =
        PermissionDto.builder()
            .assetId(snippetId)
            .userId(userId)
            .permission(PermissionType.READ_WRITE)
            .build();

    return fetchPermissionData(body);
  }

  public ResponseEntity<String> createNewPermission(String userId, String snippetId) {
    PermissionDto permissionDto = PermissionDto.builder().assetId(snippetId).userId(userId).build();
    String url = permissionManagerUrl + "/api/permission/";
    Mono<ResponseEntity<String>> response =
        webClientUtility.putAsync(url, permissionDto, String.class);
    return response.block(Duration.ofSeconds(timeOutInSeconds));
  }

  public ResponseEntity<String> deletePermission(String userId, String snippetId) {
    String url = permissionManagerUrl + "/api/permission/" + "user/" + userId + "/" + snippetId;
    Mono<ResponseEntity<String>> response = webClientUtility.deleteAsync(url, String.class);
    return response.block(Duration.ofSeconds(timeOutInSeconds));
  }

  public boolean canDelete(String userId, String snippetId) {
    PermissionDto body =
        PermissionDto.builder()
            .assetId(snippetId)
            .userId(userId)
            .permission(PermissionType.DELETE)
            .build();

    return fetchPermissionData(body);
  }

  private boolean fetchPermissionData(PermissionDto body) {
    String url = permissionManagerUrl + "/api/permission/";
    Mono<ResponseEntity<Boolean>> response = webClientUtility.postAsync(url, body, Boolean.class);

    ResponseEntity<Boolean> result = response.block(Duration.ofSeconds(timeOutInSeconds));

    if (result != null && result.getStatusCode() == HttpStatus.OK) {
      Boolean hasAccess = result.getBody();
      return hasAccess != null && hasAccess;
    } else {
      return false;
    }
  }
}
