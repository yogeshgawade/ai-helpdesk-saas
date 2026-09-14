package com.helpdesk.kb.repository;

import com.pgvector.PGvector;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class KbChunkVectorRepository {

    private final JdbcTemplate jdbcTemplate;

    public KbChunkVectorRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertChunk(
            UUID id,
            UUID documentId,
            UUID organizationId,
            String chunkText,
            float[] embedding,
            int chunkIndex,
            Integer tokenCount
    ) {
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO kb_chunks (
                        id,
                        document_id,
                        organization_id,
                        chunk_text,
                        embedding,
                        chunk_index,
                        token_count
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """);

            statement.setObject(1, id);
            statement.setObject(2, documentId);
            statement.setObject(3, organizationId);
            statement.setString(4, chunkText);
            statement.setObject(5, new PGvector(embedding));
            statement.setInt(6, chunkIndex);

            if (tokenCount == null) {
                statement.setObject(7, null);
            } else {
                statement.setInt(7, tokenCount);
            }

            return statement;
        });
    }

    public List<KbChunkSearchResult> searchSimilar(
            UUID organizationId,
            float[] queryEmbedding,
            int limit
    ) {
        PGvector vector = new PGvector(queryEmbedding);

        return jdbcTemplate.query("""
                SELECT
                    id,
                    document_id,
                    organization_id,
                    chunk_text,
                    chunk_index,
                    token_count,
                    1 - (embedding <=> ?) AS similarity
                FROM kb_chunks
                WHERE organization_id = ?
                ORDER BY embedding <=> ?
                LIMIT ?
                """,
                ps -> {
                    ps.setObject(1, vector);
                    ps.setObject(2, organizationId);
                    ps.setObject(3, vector);
                    ps.setInt(4, limit);
                },
                (rs, rowNum) -> new KbChunkSearchResult(
                        rs.getObject("id", UUID.class),
                        rs.getObject("document_id", UUID.class),
                        rs.getObject("organization_id", UUID.class),
                        rs.getString("chunk_text"),
                        rs.getInt("chunk_index"),
                        (Integer) rs.getObject("token_count"),
                        rs.getDouble("similarity")
                )
        );
    }
}
