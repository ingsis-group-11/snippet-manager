package snippetmanager.redis.linter;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.stream.StreamReceiver;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import snippetmanager.services.LintingRuleService;

@Component
public class LintConsumer {

  private final ReactiveRedisTemplate<String, String> redisTemplate;
  private final StreamReceiver<String, MapRecord<String, String, String>> streamReceiver;
  private final String streamKey;

  @Autowired
  LintingRuleService lintingRuleService;

  public LintConsumer(
          @Value("${redis.consumer.lint}") String streamKey,
          ReactiveRedisTemplate<String, String> redisTemplate,
          StreamReceiver<String, MapRecord<String, String, String>> streamReceiver) {
    this.streamKey = streamKey;
    this.redisTemplate = redisTemplate;
    this.streamReceiver = streamReceiver;
  }

  @PostConstruct
  public void startConsuming() {
    Flux<MapRecord<String, String, String>> messageFlux =
            streamReceiver.receive(StreamOffset.fromStart(streamKey));

    messageFlux.doOnNext(this::processMessage).subscribe();
  }

  private void processMessage(MapRecord<String, String, String> message) {

    String messageBody = message.getValue().get("payload");

    try {
      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode jsonNode = objectMapper.readTree(messageBody);

      String assetId = jsonNode.get("assetId").asText();
      String result = jsonNode.get("result").asText();

      lintingRuleService.saveLintResult(assetId, result);
    } catch (Exception e) {
      throw new RuntimeException("Error processing message", e);
    }

  }
}