package snippet_manager.snippet.model.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class SnippetSendDTO {
  private String assetId;
  private String language;
  private String version;
  private String content;
}
