package snippetmanager.redis.linter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LinterRedisResult {
  private String assetId;
  private String linterResult;
}
