package snippet_manager.snippet.webservice.asset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import snippet_manager.snippet.webservice.WebClientUtility;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.time.Duration;

@Component
public class AssetManager {
    @Autowired
    WebClientUtility webClientUtility;

    private final int timeOutInSeconds = 30;


    @Value("${asset.manager.url}")
    private String assetManagerUrl;

    public InputStream getAsset(String container, String assetKey) {
        String url = assetManagerUrl + "v1/asset/" + container + "/" + assetKey;
        return webClientUtility.getInputStream(url);
    }

    public ResponseEntity createAsset(String container, String assetKey, MultipartFile content) throws IOException {
        String url = assetManagerUrl + "v1/asset/" + container + "/" + assetKey;
        Mono<ResponseEntity> response = webClientUtility.putFlux(convertMultipartFileToFlux(content), url, ResponseEntity.class);
        return response.block(Duration.ofSeconds(timeOutInSeconds));
    }

    public ResponseEntity deleteAsset(String container, String assetKey) {
        String url = assetManagerUrl + "v1/asset/" + container + "/" + assetKey;
        Mono<ResponseEntity> response = webClientUtility.delete(url, ResponseEntity.class);
        return response.block(Duration.ofSeconds(timeOutInSeconds));
    }

    private Flux<DataBuffer> convertMultipartFileToFlux(MultipartFile multipartFile) throws IOException {
        InputStream inputStream = multipartFile.getInputStream();

        return Flux.generate(sink -> {
            try {
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                int readBytes = Channels.newChannel(inputStream).read(byteBuffer);

                if (readBytes == -1) {
                    sink.complete();
                } else {
                    byteBuffer.flip();
                    DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(byteBuffer);
                    sink.next(dataBuffer);
                }
            } catch (IOException e) {
                sink.error(e);
            }
        });
    }

}
