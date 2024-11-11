package snippetmanager.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import snippetmanager.model.entities.Languages;

public interface LanguagesRepository extends JpaRepository<Languages, String> {}
