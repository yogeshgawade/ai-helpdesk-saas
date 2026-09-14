package com.helpdesk.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalDocumentStorage implements DocumentStorage {

    private final Path rootDirectory;

    public LocalDocumentStorage(
            @Value("${app.storage.local-directory:./local-storage}")
            String rootDirectory
    ) {
        this.rootDirectory = Path.of(rootDirectory)
                .toAbsolutePath()
                .normalize();
    }

    @Override
    public InputStream open(
            UUID organizationId,
            UUID documentId,
            String filename
    ) throws IOException {

        return Files.newInputStream(
                resolvePath(organizationId, documentId, filename)
        );
    }

    @Override
    public void store(
            UUID organizationId,
            UUID documentId,
            String filename,
            InputStream inputStream
    ) throws IOException {

        Path path = resolvePath(
                organizationId,
                documentId,
                filename
        );

        Files.createDirectories(path.getParent());

        Files.copy(
                inputStream,
                path,
                StandardCopyOption.REPLACE_EXISTING
        );
    }

    private Path resolvePath(
            UUID organizationId,
            UUID documentId,
            String filename
    ) {
        String safeFilename = Path.of(filename)
                .getFileName()
                .toString();

        return rootDirectory
                .resolve(organizationId.toString())
                .resolve(documentId.toString())
                .resolve(safeFilename)
                .normalize();
    }
}