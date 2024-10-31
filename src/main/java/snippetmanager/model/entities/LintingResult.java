package snippetmanager.model.entities;

import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import snippetmanager.util.Result;

@Builder
@Getter
@Setter
public class LintingResult {
  @Id
  private String assetId;

  private Result result;

  public void setResultAsString(String result) {
    String resultEnum = result.toUpperCase();
    this.result = Result.valueOf(resultEnum);
  }

  public String getResultAsString() {
    return result.toString();
  }
}
