package snippet_manager.snippet;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class EnvConfig {

  @PostConstruct
  public void init() {
    Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    dotenv
        .entries()
        .forEach(
            entry -> {
              System.setProperty(entry.getKey(), entry.getValue());
            });
  }
}
