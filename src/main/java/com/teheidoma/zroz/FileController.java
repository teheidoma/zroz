package com.teheidoma.zroz;

import io.micrometer.core.instrument.util.IOUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.FileInputStream;

@RestController
@RequestMapping("/gif")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    @SneakyThrows
    @ResponseBody
    @GetMapping(value = "/{id}.gif", produces = MediaType.IMAGE_GIF_VALUE)
    public Mono<byte[]> gif(@PathVariable String id) {
        return fileService.getFile(id);
    }
}
