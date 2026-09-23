package com.helpdesk.redis;

import com.helpdesk.auth.MembershipRole;
import com.helpdesk.exception.ForbiddenException;
import com.helpdesk.orgs.OrganizationContext;
import com.helpdesk.orgs.OrganizationContextHolder;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TicketSummarizationDlqService {

    private final RedisTemplate<String, String> redisTemplate;

    public TicketSummarizationDlqService(
            RedisTemplate<String, String> redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    public int reprocessForOrganization(UUID organizationId) {

        OrganizationContext context = OrganizationContextHolder.get();

        if (context == null) {
            throw new IllegalStateException(
                    "Organization context not set"
            );
        }

        if (!context.getOrganizationId().equals(organizationId)) {
            throw new ForbiddenException(
                    "Organization access denied"
            );
        }

        MembershipRole role = context.getRole();

        if (role != MembershipRole.OWNER
                && role != MembershipRole.ADMIN) {
            throw new ForbiddenException(
                    "Only owners and admins can reprocess DLQ messages"
            );
        }

        List<MapRecord<String, Object, Object>> messages =
                redisTemplate.opsForStream().range(
                        RedisStreamConfig.TICKET_SUMMARIZATION_DLQ,
                        Range.unbounded()
                );

        int reprocessed = 0;

        for (MapRecord<String, Object, Object> message : messages) {

            Map<Object, Object> values = message.getValue();

            if (!organizationId.toString().equals(
                    String.valueOf(values.get("organizationId"))
            )) {
                continue;
            }

            Map<String, String> retryValues = new HashMap<>();

            retryValues.put(
                    "ticketId",
                    String.valueOf(values.get("ticketId"))
            );

            retryValues.put(
                    "organizationId",
                    String.valueOf(values.get("organizationId"))
            );

            retryValues.put("retryCount", "0");

            redisTemplate.opsForStream().add(
                    RedisStreamConfig.TICKET_SUMMARIZATION_STREAM,
                    retryValues
            );

            redisTemplate.opsForStream().delete(
                    RedisStreamConfig.TICKET_SUMMARIZATION_DLQ,
                    message.getId()
            );

            reprocessed++;
        }

        return reprocessed;
    }
}
