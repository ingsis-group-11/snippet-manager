package printscriptservice.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.austral.ingsis.redis.RedisStreamConsumer;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamReceiver;
import org.springframework.stereotype.Component;
import snippetmanager.redis.linter.LinterRedisResult;
import snippetmanager.services.LintingRuleService;

@Component
public class LintConsumer extends RedisStreamConsumer<String> {

  @Autowired private LintingRuleService lintingRuleService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  public LintConsumer(
          @Value("${redis.consumer.lint}") @NotNull String streamKey,
          @Value("${redis.consumer.group}") @NotNull String consumerGroup,
          @NotNull RedisTemplate<String, String> redis) {
    super(streamKey, consumerGroup, redis);
  }

  @Override
  protected void onMessage(@NotNull ObjectRecord<String, String> objectRecord) {
    try {
      String jsonValue = objectRecord.getValue();
      LinterRedisResult linterRedisResult =
              objectMapper.readValue(jsonValue, LinterRedisResult.class);

      lintingRuleService.saveLintResult(linterRedisResult.getAssetId(), linterRedisResult.getLinterResult());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  @Override
  protected StreamReceiver.StreamReceiverOptions<String, ObjectRecord<String, String>> options() {
    return StreamReceiver.StreamReceiverOptions.builder()
            .pollTimeout(Duration.ofSeconds(5))
            .targetType(String.class)
            .build();
  }
}
