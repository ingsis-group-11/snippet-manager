package snippet_manager.snippet.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import snippet_manager.snippet.model.entities.CodeSnippet;

import java.util.UUID;

@Repository
public interface CodeSnippetRepository extends JpaRepository<CodeSnippet, UUID> {
}
