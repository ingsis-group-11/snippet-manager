package snippetmanager.redis.formatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import snippetmanager.model.dtos.SnippetSendDto;
import snippetmanager.redis.ProducerInterface;
import snippetmanager.redis.config.RedisStreamProducer;

@Component
public class FormatterProducer extends RedisStreamProducer implements ProducerInterface {

  @Autowired
  public FormatterProducer(
      @Value("${redis.producer.formatter}") String streamKey, ReactiveRedisTemplate<String, String> redis) {
    super(streamKey, redis);
  }

  @Override
  public void publishEvent(SnippetSendDto snippetInfo) {
    System.out.println("Publishing on stream: " + getStreamKey());
    emit(snippetInfo).subscribe();
  }
}
