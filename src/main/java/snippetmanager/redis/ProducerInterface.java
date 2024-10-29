package snippetmanager.redis;

import snippetmanager.model.dtos.SnippetSendDto;

public interface ProducerInterface {
  void publishEvent(SnippetSendDto snippetInfo);
}
