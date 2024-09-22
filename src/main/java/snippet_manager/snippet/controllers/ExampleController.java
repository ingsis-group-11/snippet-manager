package snippet_manager.snippet.controllers;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import snippet_manager.snippet.services.ExampleService;

@RequestMapping("/example")
@RestController
public class ExampleController {

  private final ExampleService exampleService;

  public ExampleController(ExampleService exampleService) {
    this.exampleService = exampleService;
  }

  @PostMapping("/send-data")
  public Mono<String> sendData(@RequestBody String requestData) {
    return exampleService.getExample(requestData);
  }
}
