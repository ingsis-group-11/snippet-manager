package snippetmanager.model.dtos;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
@Builder
public class SnippetIdAuthorDto {
  private String snippetId;
  private String author;
}
