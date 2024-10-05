package snippet_manager.snippet.model.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class PermissionDTO {
  private Long userId;
  private Long snippetId;
}
