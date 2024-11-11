package snippetmanager.controllers;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import snippetmanager.model.dtos.UserDto;
import snippetmanager.services.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

  @Autowired private UserService userService;

  private String getUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Jwt jwt = (Jwt) authentication.getPrincipal();
    String userId = jwt.getClaimAsString("sub");
    int position = userId.indexOf("|");

    if (position != -1) {
      userId = userId.substring(position + 1);
    }

    return userId;
  }

  // GET http://localhost:8080/api/users/
  @GetMapping
  public ResponseEntity<List<UserDto>> getUsers() {
    return ResponseEntity.ok(userService.getUsers(getUserId()));
  }
}
