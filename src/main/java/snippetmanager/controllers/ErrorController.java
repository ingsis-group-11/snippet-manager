package snippetmanager.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/error")
public class ErrorController {

  @GetMapping()
  public ResponseEntity<String> error() {
    return ResponseEntity.status(500).body("ERROR");
  }

  @GetMapping
  public ResponseEntity<String> topoEndpoint() {
    return ResponseEntity.status(200).body("Topo");
  }
}
