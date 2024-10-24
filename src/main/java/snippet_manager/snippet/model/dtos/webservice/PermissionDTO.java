package snippet_manager.snippet.model.dtos.webservice;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import snippet_manager.snippet.util.PermissionType;

@Builder
@Getter
@Setter
public class PermissionDTO {
  private String userId;
  private String assetId;
  private PermissionType permission;
}
