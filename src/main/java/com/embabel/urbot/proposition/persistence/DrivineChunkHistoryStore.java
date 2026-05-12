package com.embabel.urbot.proposition.persistence;

import java.util.Map;

import org.drivine.manager.CascadeType;
import org.drivine.manager.GraphObjectManager;
import org.drivine.manager.PersistenceManager;
import org.drivine.query.QuerySpecification;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.embabel.dice.incremental.AnalysisBookmark;
import com.embabel.dice.incremental.BookmarkKey;
import com.embabel.dice.incremental.ChunkHistoryStore;
import com.embabel.dice.incremental.HashKey;
import com.embabel.dice.incremental.ProcessedChunkRecord;

/**
 * Neo4j/Drivine implementation of ChunkHistoryStore for tracking processed chunks.
 */
@Service
@Transactional
public class DrivineChunkHistoryStore implements ChunkHistoryStore {

    private static final Logger logger = LoggerFactory.getLogger(DrivineChunkHistoryStore.class);

    private final GraphObjectManager graphObjectManager;
    private final PersistenceManager persistenceManager;

    public DrivineChunkHistoryStore(GraphObjectManager graphObjectManager, PersistenceManager persistenceManager) {
        this.graphObjectManager = graphObjectManager;
        this.persistenceManager = persistenceManager;
    }

    @Override
    @Nullable
    public AnalysisBookmark getLastBookmark(@NonNull BookmarkKey key) {
        var query = """
                MATCH (c:ProcessedChunk {sourceId: $sourceId})
                RETURN c.sourceId AS sourceId, c.endIndex AS endIndex, c.processedAt AS processedAt
                ORDER BY c.processedAt DESC
                LIMIT 1
                """;

        var spec = QuerySpecification
                .withStatement(query)
                .bind(Map.of("sourceId", key.getSourceId()));

        try {
            var result = persistenceManager.getOne(spec);
            if (result == null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            var row = (Map<String, Object>) result;
            return new AnalysisBookmark(
                    (String) row.get("sourceId"),
                    ((Number) row.get("endIndex")).intValue(),
                    java.time.Instant.parse(row.get("processedAt").toString())
            );
        } catch (Exception e) {
            logger.debug("No bookmark found for source {}: {}", key.getSourceId(), e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isProcessed(@NonNull HashKey key) {
        var query = """
                MATCH (c:ProcessedChunk {contentHash: $hash})
                RETURN count(c) > 0 AS exists
                """;

        var spec = QuerySpecification
                .withStatement(query)
                .bind(Map.of("hash", key.getContentHash()));

        try {
            var result = persistenceManager.getOne(spec);
            if (result == null) {
                return false;
            }
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            logger.debug("Error checking if processed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void recordProcessed(@NonNull ProcessedChunkRecord record) {
        var node = new ProcessedChunkNode(
                record.getContentHash(),
                record.getSourceId(),
                record.getStartIndex(),
                record.getEndIndex(),
                record.getProcessedAt()
        );

        graphObjectManager.save(node, CascadeType.NONE);
        logger.debug("Recorded processed chunk: {} [{}-{}]",
                record.getSourceId(), record.getStartIndex(), record.getEndIndex());
    }
}
