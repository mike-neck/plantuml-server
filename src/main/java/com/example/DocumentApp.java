package com.example;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.EnableWebFlux;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import reactor.core.publisher.Mono;

@SpringBootApplication
@EnableWebFlux
@RestController
public class DocumentApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentApp.class);

    private static final ExecutorService SERVICE = Executors.newFixedThreadPool(2);

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(SERVICE::shutdown));
        SpringApplication.run(DocumentApp.class, args);
    }

    @GetMapping(value = "/uml/{base64}", produces = "image/svg+xml")
    Mono<String> convertToSvg(@PathVariable("base64") final String base64) {
        final CompletableFuture<String> document = decodeToDocument(base64);
        return Mono.fromFuture(document)
                .flatMap(this::documentToSourceReader)
                .flatMap(this::convertImage);
    }

    private CompletableFuture<String> decodeToDocument(
            final @PathVariable("base64") String base64) {
        return CompletableFuture.supplyAsync(() -> {
            final byte[] bytes = Base64.getDecoder().decode(base64);
            return new String(bytes, StandardCharsets.UTF_8);
        });
    }

    private Mono<SourceStringReader> documentToSourceReader(final String document) {
        final CompletableFuture<SourceStringReader> stringReader =
                CompletableFuture.supplyAsync(() -> new SourceStringReader(document, "UTF-8"));
        return Mono.fromFuture(stringReader);
    }

    private Mono<String> convertImage(final SourceStringReader sourceReader) {
        final CompletableFuture<String> xml =
                CompletableFuture.supplyAsync(() -> generateImage(sourceReader));
        return Mono.fromFuture(xml);
    }

    private String generateImage(final SourceStringReader sourceReader) {
        try {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            final String xml = sourceReader.generateImage(byteArrayOutputStream, new FileFormatOption(FileFormat.SVG));
            LOGGER.info("output: {}", xml);
            return new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.info("error on converting document: {}", e);
            throw new IllegalArgumentException(e);
        }
    }
}
