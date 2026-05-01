package com.nexusapp.back_end.bananapro.controller;

import com.nexusapp.back_end.bananapro.model.NanoBanana;
import com.nexusapp.back_end.bananapro.service.ImageRenderingService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/prototype")
@RequiredArgsConstructor
public class PrototypeController {

    private final ImageRenderingService service;

    @PostMapping(value = "/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Gera um prototipo usando IA")
    public Mono<ResponseEntity<NanoBanana>> generate(
            @RequestPart("file") List<MultipartFile> files,
            @RequestPart("prompt") String prompt
    ) {
        return service.generatePrototype(files, prompt)
                .map(ResponseEntity::ok);
    }
}
