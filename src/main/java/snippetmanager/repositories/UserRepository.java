package snippetmanager.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import snippetmanager.model.entities.User;

public interface UserRepository extends JpaRepository<User, String> {}
