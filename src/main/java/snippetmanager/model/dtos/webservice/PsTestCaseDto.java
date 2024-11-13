package snippetmanager.model.dtos.webservice;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class PsTestCaseDto {
  private String language;
  private String version;
  private String content;
  private List<String> input;
  private List<String> output;
}
