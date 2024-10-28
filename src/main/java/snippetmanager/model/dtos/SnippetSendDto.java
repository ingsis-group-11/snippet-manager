package snippetmanager.model.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SnippetSendDto {
  private String assetId;
  private String language;
  private String version;
  private String content;
  private String userId;
}
