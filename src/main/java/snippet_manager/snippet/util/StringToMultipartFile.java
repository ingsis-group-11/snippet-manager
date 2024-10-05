package snippet_manager.snippet.util;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public class StringToMultipartFile implements MultipartFile {

  private final String content;
  private final String name;
  private final String originalFilename;
  private final String contentType;

  public StringToMultipartFile(String content, String name, String originalFilename, String contentType) {
    this.content = content;
    this.name = name;
    this.originalFilename = originalFilename;
    this.contentType = contentType;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getOriginalFilename() {
    return originalFilename;
  }

  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public boolean isEmpty() {
    return content == null || content.isEmpty();
  }

  @Override
  public long getSize() {
    return content.getBytes().length;
  }

  @Override
  public byte[] getBytes(){
    return content.getBytes();
  }

  @Override
  public InputStream getInputStream() {
    return new ByteArrayInputStream(content.getBytes());
  }

  @Override
  public Resource getResource() {
    return MultipartFile.super.getResource();
  }

  @Override
  public void transferTo(File dest) {

  }

  @Override
  public void transferTo(Path dest) throws IOException, IllegalStateException {
    MultipartFile.super.transferTo(dest);
  }

}

