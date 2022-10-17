package com.teheidoma.zroz;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Service
public class FileService {
    @Value("${storage.dir:~/zroz}")
    private String path;

    public Mono<byte[]> getFile(String id) {
        return Mono.just(new File(path, id + ".gif"))
                .flatMap(this::readFile);
    }

    private Mono<byte[]> readFile(File f) {
        return Mono.fromSupplier(() -> {
            try (FileInputStream fis = new FileInputStream(f)) {
                return StreamUtils.copyToByteArray(fis);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
