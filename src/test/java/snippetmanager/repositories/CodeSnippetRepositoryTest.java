package snippetmanager.repositories;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import snippetmanager.model.entities.CodeSnippet;
import snippetmanager.util.CodeLanguage;

@DataJpaTest
public class CodeSnippetRepositoryTest {

  @MockBean private CodeSnippetRepository codeSnippetRepository;

  @Test
  void createAndSaveNewSnippet() {
    String assetId = UUID.randomUUID().toString();
    CodeSnippet codeSnippet = new CodeSnippet();
    codeSnippet.setAssetId(assetId);
    codeSnippet.setLanguage(CodeLanguage.PRINTSCRIPT);
    codeSnippet.setVersion("1.1");

    when(codeSnippetRepository.save(any(CodeSnippet.class))).thenReturn(codeSnippet);

    codeSnippetRepository.save(codeSnippet);

    when(codeSnippetRepository.findById(assetId)).thenReturn(Optional.of(codeSnippet));

    Optional<CodeSnippet> findSnippet = codeSnippetRepository.findById(assetId);
    assert findSnippet.isPresent();
    assert findSnippet.get().getAssetId().equals(assetId);
    assert findSnippet.get().getLanguage().equals(CodeLanguage.PRINTSCRIPT);
    assert findSnippet.get().getVersion().equals("1.1");

    verify(codeSnippetRepository, times(1)).save(any(CodeSnippet.class));
    verify(codeSnippetRepository, times(1)).findById(assetId);
  }

  @Test
  void updateExistingSnippet() {
    String assetId = UUID.randomUUID().toString();
    CodeSnippet codeSnippet = new CodeSnippet();
    codeSnippet.setAssetId(assetId);
    codeSnippet.setLanguage(CodeLanguage.PRINTSCRIPT);
    codeSnippet.setVersion("1.1");

    when(codeSnippetRepository.save(any(CodeSnippet.class))).thenReturn(codeSnippet);

    codeSnippetRepository.save(codeSnippet);

    codeSnippet.setVersion("1.2");
    when(codeSnippetRepository.findById(assetId)).thenReturn(Optional.of(codeSnippet));
    codeSnippetRepository.save(codeSnippet);

    Optional<CodeSnippet> updatedSnippet = codeSnippetRepository.findById(assetId);
    assert updatedSnippet.isPresent();
    assert updatedSnippet.get().getVersion().equals("1.2");

    verify(codeSnippetRepository, times(2)).save(any(CodeSnippet.class));
    verify(codeSnippetRepository, times(1)).findById(assetId);
  }

  @Test
  void deleteExistingSnippet() {
    String assetId = UUID.randomUUID().toString();
    CodeSnippet codeSnippet = new CodeSnippet();
    codeSnippet.setAssetId(assetId);
    codeSnippet.setLanguage(CodeLanguage.PRINTSCRIPT);
    codeSnippet.setVersion("1.1");

    when(codeSnippetRepository.save(any(CodeSnippet.class))).thenReturn(codeSnippet);
    when(codeSnippetRepository.findById(assetId)).thenReturn(Optional.empty());
    doNothing().when(codeSnippetRepository).delete(any(CodeSnippet.class));

    codeSnippetRepository.save(codeSnippet);

    codeSnippetRepository.delete(codeSnippet);

    Optional<CodeSnippet> deletedSnippet = codeSnippetRepository.findById(assetId);
    assert deletedSnippet.isEmpty();

    verify(codeSnippetRepository, times(1)).delete(any(CodeSnippet.class));
    verify(codeSnippetRepository, times(1)).findById(assetId);
  }
}
