package snippetmanager.webservice.permission;

import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import snippetmanager.model.dtos.webservice.PermissionDto;
import snippetmanager.util.enums.PermissionType;
import snippetmanager.webservice.WebClientUtility;

@Component
public class PermissionManager {
  @Autowired WebClientUtility webClientUtility;

  private final int timeOutInSeconds = 30;

  @Value("${permission.manager.url}")
  private String permissionManagerUrl;

  public boolean canRead(String snippetId) {
    PermissionDto body =
        PermissionDto.builder().assetId(snippetId).permission(PermissionType.READ).build();

    return fetchPermissionData(body);
  }

  public boolean canWrite(String snippetId) {
    PermissionDto body =
        PermissionDto.builder().assetId(snippetId).permission(PermissionType.READ_WRITE).build();

    return fetchPermissionData(body);
  }

  public ResponseEntity<List<String>> getSnippetsUserCanRead(
      Integer from, Integer to, String permissionType) {
    String url =
        permissionManagerUrl
            + "/api/permission"
            + "?from="
            + from
            + "&to="
            + to
            + "&permissionType="
            + permissionType;
    Mono<ResponseEntity<List<String>>> response =
        webClientUtility.getAsync(url, new ParameterizedTypeReference<List<String>>() {});
    return response.block(Duration.ofSeconds(timeOutInSeconds));
  }

  public ResponseEntity<List<String>> getSnippetsUserCanWrite(String permissionType) {
    String url = permissionManagerUrl + "/api/permission" + "?permissionType=" + permissionType;
    Mono<ResponseEntity<List<String>>> response =
        webClientUtility.getAsync(url, new ParameterizedTypeReference<List<String>>() {});
    return response.block(Duration.ofSeconds(timeOutInSeconds));
  }

  public ResponseEntity<String> createNewPermission(String snippetId, PermissionType permission) {
    PermissionDto permissionDto =
        PermissionDto.builder().assetId(snippetId).permission(permission).build();
    String url = permissionManagerUrl + "/api/permission";
    Mono<ResponseEntity<String>> response =
        webClientUtility.putAsync(url, permissionDto, String.class);
    return response.block(Duration.ofSeconds(timeOutInSeconds));
  }

  public ResponseEntity<String> deletePermission(String snippetId) {
    String url = permissionManagerUrl + "/api/permission/" + snippetId;
    Mono<ResponseEntity<String>> response = webClientUtility.deleteAsync(url, String.class);
    return response.block(Duration.ofSeconds(timeOutInSeconds));
  }

  public boolean canDelete(String snippetId) {
    PermissionDto body =
        PermissionDto.builder().assetId(snippetId).permission(PermissionType.DELETE).build();

    return fetchPermissionData(body);
  }

  private boolean fetchPermissionData(PermissionDto body) {
    String url = permissionManagerUrl + "/api/permission";
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
