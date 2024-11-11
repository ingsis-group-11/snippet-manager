package snippetmanager.redis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
public class ConnectionFactory {

  private final String hostName;
  private final int port;

  public ConnectionFactory(
      @Value("${spring.data.redis.host}") String hostName,
      @Value("${spring.data.redis.port}") int port) {
    System.out.println("REIDS_HOST:" + hostName);
    System.out.println("REDIS_PORT:" + port);
    this.hostName = hostName;
    this.port = port;
  }

  @Bean
  public LettuceConnectionFactory redisConnectionFactory() {
    return new LettuceConnectionFactory(new RedisStandaloneConfiguration(hostName, port));
  }
}
