package com.helpdesk.orgs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest
class TenantTransactionExecutorTest {

    private static final UUID ORGANIZATION_A =
            UUID.fromString(
                    "8a568ac9-b1b2-41cc-a19a-86de79f83d4e"
            );

    private static final UUID ORGANIZATION_B =
            UUID.fromString(
                    "11111111-1111-1111-1111-111111111111"
            );

    @Autowired
    private TenantTransactionExecutor tenantTransactionExecutor;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSetTenantContextForEachTransaction() {

        String organizationA =
                tenantTransactionExecutor.execute(
                        ORGANIZATION_A,
                        () -> jdbcTemplate.queryForObject(
                                """
                                SELECT current_setting(
                                    'app.current_org_id',
                                    true
                                )
                                """,
                                String.class
                        )
                );

        assertEquals(
                ORGANIZATION_A.toString(),
                organizationA
        );

        String organizationB =
                tenantTransactionExecutor.execute(
                        ORGANIZATION_B,
                        () -> jdbcTemplate.queryForObject(
                                """
                                SELECT current_setting(
                                    'app.current_org_id',
                                    true
                                )
                                """,
                                String.class
                        )
                );

        assertEquals(
                ORGANIZATION_B.toString(),
                organizationB
        );

        String outsideTransaction =
                jdbcTemplate.queryForObject(
                        """
                        SELECT current_setting(
                            'app.current_org_id',
                            true
                        )
                        """,
                        String.class
                );

        assertEquals("", outsideTransaction);
    }
}
