package snippetmanager.services;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
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

  public void createUser(String userId, String name) {
    boolean userExists = checkIfUserExists(userId);
    if (!userExists) {
      User user = new User();
      user.setUserId(userId);
      user.setName(name);
      userRepository.save(user);
    }
  }

  private boolean checkIfUserExists(String userId) {
    Optional<User> userOptional = userRepository.findById(userId);
    return userOptional.isPresent();
  }

  public List<UserDto> getUsers(String currentUserId) {
    // Get all users except the current user
    List<User> users = userRepository.findAll();
    return users.stream()
        .filter(user -> !Objects.equals(user.getUserId(), currentUserId))
        .map(user -> UserDto.builder().userId(user.getUserId()).name(user.getName()).build())
        .toList();
  }
}
