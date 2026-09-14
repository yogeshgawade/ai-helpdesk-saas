package com.helpdesk.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalDocumentStorageTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldStoreAndOpenDocument() throws Exception {
        LocalDocumentStorage storage =
                new LocalDocumentStorage(tempDirectory.toString());

        UUID organizationId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        String filename = "refund-policy.txt";
        String content = "Refunds are available within 30 days.";

        storage.store(
                organizationId,
                documentId,
                filename,
                new ByteArrayInputStream(
                        content.getBytes(StandardCharsets.UTF_8)
                )
        );

        Path expectedPath = tempDirectory
                .resolve(organizationId.toString())
                .resolve(documentId.toString())
                .resolve(filename);

        assertTrue(Files.exists(expectedPath));

        try (InputStream inputStream =
                     storage.open(
                             organizationId,
                             documentId,
                             filename
                     )) {

            String actualContent = new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );

            assertEquals(content, actualContent);
        }
    }
}
