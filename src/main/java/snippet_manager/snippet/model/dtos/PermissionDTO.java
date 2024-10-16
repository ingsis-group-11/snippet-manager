package snippet_manager.snippet.model.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import snippet_manager.snippet.util.PermissionType;

@Builder
@Getter
@Setter
public class PermissionDTO {
  private Long userId;
  private String snippetId;
  private PermissionType permission;
}
