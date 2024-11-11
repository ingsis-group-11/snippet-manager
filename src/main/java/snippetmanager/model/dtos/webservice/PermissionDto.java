package snippetmanager.model.dtos.webservice;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import snippetmanager.util.PermissionType;

@Builder
@Getter
@Setter
public class PermissionDto {
  private String assetId;
  private PermissionType permission;
}
