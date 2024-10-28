package snippetmanager.redis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;

public abstract class RedisStreamProducer {

  @Getter private final String streamKey;
  private final ReactiveRedisTemplate<String, String> redis;

  public RedisStreamProducer(String streamKey, ReactiveRedisTemplate<String, String> redis) {
    this.streamKey = streamKey;
    this.redis = redis;
  }

  public <V> Mono<RecordId> emit(V value) {
    try {
      String json = new ObjectMapper().writeValueAsString(value);
      var record = StreamRecords.newRecord().ofObject(json).withStreamKey(getStreamKey());
      return redis.opsForStream().add(record);
    } catch (Exception e) {
      return Mono.error(e);
    }
  }
}
