package snippetmanager.services;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import snippetmanager.model.dtos.UserDto;
import snippetmanager.model.entities.User;
import snippetmanager.repositories.UserRepository;

@Service
public class UserService {
  private UserRepository userRepository;

  @Autowired
  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  private String getUserEmail() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Jwt jwt = (Jwt) authentication.getPrincipal();

    return jwt.getClaimAsString("username");
  }

  public void createUser(String userId) {
    boolean userExists = checkIfUserExists(userId);
    if (!userExists) {
      User user = new User();
      user.setUserId(userId);
      user.setName(getUserEmail());
      userRepository.save(user);
    }
  }

  public String getUserName(String userId) {
    Optional<User> userOptional = userRepository.findById(userId);
    return userOptional.map(User::getName).orElse(null);
  }

  private boolean checkIfUserExists(String userId) {
    Optional<User> userOptional = userRepository.findById(userId);
    return userOptional.isPresent();
  }

  public List<UserDto> getUsers(String currentUserId) {
    List<User> users = userRepository.findAll();
    return users.stream()
        .filter(user -> !Objects.equals(user.getUserId(), currentUserId))
        .map(user -> UserDto.builder().id(user.getUserId()).name(user.getName()).build())
        .toList();
  }
}
