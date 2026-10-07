package com.helpdesk.seed;

import com.helpdesk.ai.client.AiServiceClient;
import com.helpdesk.ai.client.dto.DocumentChunkResponse;
import com.helpdesk.ai.client.dto.DocumentProcessResponse;
import com.helpdesk.ai.client.dto.EmbeddingResponse;
import com.helpdesk.auth.Membership;
import com.helpdesk.auth.MembershipRepository;
import com.helpdesk.auth.MembershipRole;
import com.helpdesk.auth.User;
import com.helpdesk.auth.UserRepository;
import com.helpdesk.kb.entity.DocumentStatus;
import com.helpdesk.kb.entity.KnowledgeBaseDocument;
import com.helpdesk.kb.repository.KbChunkVectorRepository;
import com.helpdesk.kb.repository.KnowledgeBaseDocumentRepository;
import com.helpdesk.orgs.Organization;
import com.helpdesk.orgs.OrganizationPlan;
import com.helpdesk.orgs.OrganizationRepository;
import com.helpdesk.orgs.TenantTransactionExecutor;
import com.helpdesk.storage.DocumentStorage;
import com.helpdesk.tickets.entity.Ticket;
import com.helpdesk.tickets.entity.TicketMessage;
import com.helpdesk.tickets.entity.TicketPriority;
import com.helpdesk.tickets.entity.TicketStatus;
import com.helpdesk.tickets.repository.TicketMessageRepository;
import com.helpdesk.tickets.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final String DEMO_ORG_SLUG = "demo-org";
    private static final String DEMO_ORG_NAME = "Demo Organization";

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final KnowledgeBaseDocumentRepository kbDocumentRepository;
    private final KbChunkVectorRepository kbChunkVectorRepository;
    private final DocumentStorage documentStorage;
    private final AiServiceClient aiServiceClient;
    private final PasswordEncoder passwordEncoder;
    private final TenantTransactionExecutor tenantTransactionExecutor;

    public DemoDataSeeder(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            MembershipRepository membershipRepository,
            TicketRepository ticketRepository,
            TicketMessageRepository ticketMessageRepository,
            KnowledgeBaseDocumentRepository kbDocumentRepository,
            KbChunkVectorRepository kbChunkVectorRepository,
            DocumentStorage documentStorage,
            AiServiceClient aiServiceClient,
            PasswordEncoder passwordEncoder,
            TenantTransactionExecutor tenantTransactionExecutor) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.ticketRepository = ticketRepository;
        this.ticketMessageRepository = ticketMessageRepository;
        this.kbDocumentRepository = kbDocumentRepository;
        this.kbChunkVectorRepository = kbChunkVectorRepository;
        this.documentStorage = documentStorage;
        this.aiServiceClient = aiServiceClient;
        this.passwordEncoder = passwordEncoder;
        this.tenantTransactionExecutor = tenantTransactionExecutor;
    }

    @Override
    public void run(String... args) {
        log.info("Checking if demo data seeding is required...");

        Optional<Organization> existingOrg = organizationRepository.findBySlug(DEMO_ORG_SLUG);
        if (existingOrg.isPresent()) {
            log.info("Demo organization already exists (slug: {}). Skipping seed.", DEMO_ORG_SLUG);
            return;
        }

        log.info("Seeding demo data...");
        seedDemoData();
        log.info("Demo data seeding completed successfully.");
    }

    private void seedDemoData() {
        Organization organization = createDemoOrganization();
        List<User> users = createDemoUsers(organization);
        createDemoMemberships(organization, users);
        List<Ticket> tickets = createDemoTickets(organization, users);
        createDemoTicketMessages(tickets, users);
        createDemoKnowledgeBaseDocuments(organization);
    }

    private Organization createDemoOrganization() {
        Organization organization = new Organization(DEMO_ORG_NAME, DEMO_ORG_SLUG);
        organization.setPlan(OrganizationPlan.PRO);
        return organizationRepository.save(organization);
    }

    private List<User> createDemoUsers(Organization organization) {
        List<User> users = new ArrayList<>();

        // Owner
        users.add(createUser("alice@demo.local", "DemoOwner123!", "Alice Johnson"));
        // Admin
        users.add(createUser("bob@demo.local", "DemoAdmin123!", "Bob Smith"));
        // Agents
        users.add(createUser("carol@demo.local", "DemoAgent123!", "Carol Williams"));
        users.add(createUser("david@demo.local", "DemoAgent123!", "David Brown"));
        // Customers
        users.add(createUser("emma@demo.local", "DemoCustomer123!", "Emma Davis"));
        users.add(createUser("frank@demo.local", "DemoCustomer123!", "Frank Miller"));
        users.add(createUser("grace@demo.local", "DemoCustomer123!", "Grace Wilson"));

        return userRepository.saveAll(users);
    }

    private User createUser(String email, String password, String name) {
        return new User(email.toLowerCase().trim(), passwordEncoder.encode(password), name.trim());
    }

    private void createDemoMemberships(Organization organization, List<User> users) {
        List<Membership> memberships = new ArrayList<>();

        memberships.add(new Membership(users.get(0), organization, MembershipRole.OWNER)); // Alice
        memberships.add(new Membership(users.get(1), organization, MembershipRole.ADMIN)); // Bob
        memberships.add(new Membership(users.get(2), organization, MembershipRole.AGENT)); // Carol
        memberships.add(new Membership(users.get(3), organization, MembershipRole.AGENT)); // David
        memberships.add(new Membership(users.get(4), organization, MembershipRole.CUSTOMER)); // Emma
        memberships.add(new Membership(users.get(5), organization, MembershipRole.CUSTOMER)); // Frank
        memberships.add(new Membership(users.get(6), organization, MembershipRole.CUSTOMER)); // Grace

        membershipRepository.saveAll(memberships);
    }

    private List<Ticket> createDemoTickets(Organization organization, List<User> users) {
        List<Ticket> tickets = new ArrayList<>();
        UUID orgId = organization.getId();

        // Open tickets
        tickets.add(createTicket(orgId, users.get(4).getId(), "Unable to access my account after password reset", TicketPriority.HIGH, "Account", TicketStatus.OPEN, users.get(2).getId()));
        tickets.add(createTicket(orgId, users.get(5).getId(), "Billing discrepancy on last invoice", TicketPriority.MEDIUM, "Billing", TicketStatus.OPEN, users.get(3).getId()));
        tickets.add(createTicket(orgId, users.get(6).getId(), "Feature request: dark mode for mobile app", TicketPriority.LOW, "Technical", TicketStatus.OPEN, null));

        // In progress tickets
        tickets.add(createTicket(orgId, users.get(4).getId(), "API rate limiting errors in production", TicketPriority.URGENT, "Technical", TicketStatus.IN_PROGRESS, users.get(2).getId()));
        tickets.add(createTicket(orgId, users.get(5).getId(), "Subscription upgrade not reflecting in dashboard", TicketPriority.HIGH, "Billing", TicketStatus.IN_PROGRESS, users.get(3).getId()));

        // Resolved tickets
        tickets.add(createTicket(orgId, users.get(4).getId(), "How do I export my data?", TicketPriority.LOW, "Account", TicketStatus.RESOLVED, users.get(2).getId()));
        tickets.add(createTicket(orgId, users.get(5).getId(), "Refund request for unused annual plan", TicketPriority.MEDIUM, "Billing", TicketStatus.RESOLVED, users.get(3).getId()));
        tickets.add(createTicket(orgId, users.get(6).getId(), "Shipping address update", TicketPriority.LOW, "Shipping", TicketStatus.RESOLVED, users.get(2).getId()));
        tickets.add(createTicket(orgId, users.get(4).getId(), "Integration with Slack notifications", TicketPriority.MEDIUM, "Technical", TicketStatus.RESOLVED, users.get(3).getId()));

        // Closed tickets
        tickets.add(createTicket(orgId, users.get(5).getId(), "Spam filter blocking legitimate emails", TicketPriority.MEDIUM, "Technical", TicketStatus.CLOSED, users.get(2).getId()));
        tickets.add(createTicket(orgId, users.get(6).getId(), "Question about enterprise pricing", TicketPriority.LOW, "Billing", TicketStatus.CLOSED, users.get(3).getId()));

        // Use tenant transaction executor to respect RLS
        return tenantTransactionExecutor.execute(orgId, () -> {
            // Save all tickets first
            List<Ticket> savedTickets = ticketRepository.saveAll(tickets);

            // Add simulated AI classification to some tickets using repository update methods
            simulateAiClassification(savedTickets.get(0), orgId, "Account", "HIGH", 0.92, "User reports account access issue after password reset");
            simulateAiClassification(savedTickets.get(1), orgId, "Billing", "MEDIUM", 0.88, "Customer reports billing discrepancy");
            simulateAiClassification(savedTickets.get(3), orgId, "Technical", "URGENT", 0.95, "API rate limiting errors in production environment");
            simulateAiClassification(savedTickets.get(6), orgId, "Account", "LOW", 0.85, "User asking about data export functionality");

            // Add simulated AI summary to some tickets using repository update methods
            simulateAiSummary(savedTickets.get(6), orgId, "Customer requested information on how to export their account data. Agent provided step-by-step instructions and confirmed customer was able to complete the export successfully.");
            simulateAiSummary(savedTickets.get(8), orgId, "Customer requested shipping address update for their order. Agent verified the order details and updated the shipping address in the system. Customer confirmed the correct address.");

            return savedTickets;
        });
    }

    private Ticket createTicket(
            UUID organizationId,
            UUID customerId,
            String subject,
            TicketPriority priority,
            String category,
            TicketStatus status,
            UUID assignedAgentId) {

        Ticket ticket = new Ticket(organizationId, customerId, subject, priority, category);
        ticket.setStatus(status);
        if (assignedAgentId != null) {
            ticket.setAssignedAgentId(assignedAgentId);
        }

        // Note: Timestamps will be set by @PrePersist. For demo purposes, we accept current timestamps.
        // Realistic timestamps would require modifying the entity or using direct SQL inserts,
        // which would bypass application invariants.

        return ticket;
    }

    private void simulateAiClassification(Ticket ticket, UUID organizationId, String category, String priority, double confidence, String reason) {
        ticketRepository.updateAiClassification(
                ticket.getId(),
                organizationId,
                category,
                priority,
                confidence,
                reason,
                Instant.now().minusSeconds(300) // 5 minutes ago
        );
    }

    private void simulateAiSummary(Ticket ticket, UUID organizationId, String summary) {
        ticketRepository.updateAiSummary(
                ticket.getId(),
                organizationId,
                summary,
                Instant.now().minusSeconds(600) // 10 minutes ago
        );
    }

    private void createDemoTicketMessages(List<Ticket> tickets, List<User> users) {
        UUID orgId = tickets.get(0).getOrganizationId();

        tenantTransactionExecutor.execute(orgId, () -> {
            // Ticket 0: Account access issue (OPEN)
            createTicketMessage(tickets.get(0).getId(), users.get(4).getId(), "I reset my password yesterday but now I can't log in. It says 'Invalid credentials' even though I'm using the new password I just set.", false);
            createTicketMessage(tickets.get(0).getId(), users.get(2).getId(), "Hi Emma, I'm sorry to hear you're having trouble logging in. Can you confirm you're using the correct email address? Also, did you receive the password reset email confirmation?", false);
            createTicketMessage(tickets.get(0).getId(), users.get(4).getId(), "Yes, I used emma@demo.local and I did get the confirmation email. The password reset seemed to work, but now I can't log in with the new password.", false);
            createTicketMessage(tickets.get(0).getId(), users.get(2).getId(), "Thank you for confirming. I've checked your account and see the password reset was processed successfully. Let me reset it again for you. Please check your email for the new reset link.", false);

            // Ticket 1: Billing discrepancy (OPEN)
            createTicketMessage(tickets.get(1).getId(), users.get(5).getId(), "My last invoice shows $299 but I'm on the $199/month plan. Can you please fix this?", false);
            createTicketMessage(tickets.get(1).getId(), users.get(3).getId(), "Hi Frank, I'll look into this right away. Can you share your invoice number so I can check the billing details?", false);

            // Ticket 3: API rate limiting (IN_PROGRESS)
            createTicketMessage(tickets.get(3).getId(), users.get(4).getId(), "We're getting 429 errors from the API in production. Our usage hasn't changed, is there a new rate limit?", false);
            createTicketMessage(tickets.get(3).getId(), users.get(2).getId(), "Hi Emma, I'm checking the rate limit configuration now. Can you tell me which endpoint you're hitting and approximately how many requests per minute?", false);
            createTicketMessage(tickets.get(3).getId(), users.get(4).getId(), "It's the /api/v1/tickets endpoint, we're making about 100 requests per minute. This worked fine until yesterday.", false);
            createTicketMessage(tickets.get(3).getId(), users.get(2).getId(), "I found the issue - there was a misconfiguration in the rate limiter. I've increased your limit to 200 requests per minute. Please try again and let me know if you're still seeing errors.", false);
            createTicketMessage(tickets.get(3).getId(), users.get(2).getId(), "Internal note: The rate limit was accidentally set to 50 instead of 200 during the last deployment. Need to add better validation to the config changes.", true);

            // Ticket 6: Data export (RESOLVED)
            createTicketMessage(tickets.get(6).getId(), users.get(4).getId(), "How do I export all my ticket data? I need it for our annual report.", false);
            createTicketMessage(tickets.get(6).getId(), users.get(2).getId(), "Hi Emma, you can export your data by going to Settings > Data Export. Click the 'Export All Tickets' button and select your preferred format (CSV or JSON). The export will be emailed to you.", false);
            createTicketMessage(tickets.get(6).getId(), users.get(4).getId(), "Perfect, I found it and the export is processing now. Thank you!", false);
            createTicketMessage(tickets.get(6).getId(), users.get(2).getId(), "You're welcome! Let me know if you have any other questions.", false);

            // Ticket 7: Refund request (RESOLVED)
            createTicketMessage(tickets.get(7).getId(), users.get(5).getId(), "I upgraded to the annual plan by mistake. I only wanted the monthly plan. Can I get a refund for the difference?", false);
            createTicketMessage(tickets.get(7).getId(), users.get(3).getId(), "Hi Frank, I understand the confusion. Since it's been less than 7 days since the upgrade, I can process a refund for you. I'll switch you back to the monthly plan and refund the difference.", false);
            createTicketMessage(tickets.get(7).getId(), users.get(5).getId(), "That would be great, thank you for your help!", false);
            createTicketMessage(tickets.get(7).getId(), users.get(3).getId(), "I've processed the refund and switched you to the monthly plan. You should see the refund on your card within 3-5 business days.", false);

            return null;
        });
    }

    private void createTicketMessage(UUID ticketId, UUID authorId, String body, boolean internalNote) {
        TicketMessage message = new TicketMessage(ticketId, authorId, body, internalNote);
        // Note: Timestamp will be set by @PrePersist
        ticketMessageRepository.save(message);
    }

    private void createDemoKnowledgeBaseDocuments(Organization organization) {
        UUID orgId = organization.getId();

        tenantTransactionExecutor.execute(orgId, () -> {
            List<String> kbDocuments = List.of(
                    createKbDocumentContent("Refund Policy", "REFUND_POLICY"),
                    createKbDocumentContent("Account Management", "ACCOUNT_MANAGEMENT"),
                    createKbDocumentContent("API Rate Limits", "API_RATE_LIMITS"),
                    createKbDocumentContent("Billing FAQ", "BILLING_FAQ")
            );

            for (String content : kbDocuments) {
                processKbDocument(orgId, content);
            }

            return null;
        });
    }

    private String createKbDocumentContent(String title, String type) {
        switch (type) {
            case "REFUND_POLICY":
                return "Refund Policy\n\n" +
                        "Refunds are available within 30 days of purchase for monthly subscriptions and 7 days for annual plans. " +
                        "To request a refund, contact support with your order number. Refunds are processed within 5 business days. " +
                        "Refunds are not available for services that have been actively used for more than 50% of the billing period.";
            case "ACCOUNT_MANAGEMENT":
                return "Account Management\n\n" +
                        "Users can reset their password by clicking 'Forgot Password' on the login page. " +
                        "A reset link will be sent to the registered email address. If you don't receive the email within 5 minutes, " +
                        "check your spam folder or contact support. For security reasons, support cannot reset passwords directly - " +
                        "users must use the self-service reset flow.";
            case "API_RATE_LIMITS":
                return "API Rate Limits\n\n" +
                        "The API has the following rate limits by default:\n" +
                        "- Free plan: 50 requests per minute\n" +
                        "- Pro plan: 200 requests per minute\n" +
                        "- Enterprise plan: 1000 requests per minute\n\n" +
                        "Rate limits are enforced per API key. If you exceed your limit, you'll receive a 429 status code. " +
                        "Contact support to request a limit increase for your plan.";
            case "BILLING_FAQ":
                return "Billing FAQ\n\n" +
                        "Invoices are generated on the 1st of each month for monthly plans and annually on the anniversary date for annual plans. " +
                        "Payment is automatically charged to the card on file. If payment fails, you'll receive an email notification. " +
                        "You can update your payment method in Settings > Billing. All prices are in USD.";
            default:
                return "";
        }
    }

    private void processKbDocument(UUID organizationId, String content) {
        String[] lines = content.split("\n", 2);
        String title = lines[0].trim();
        String body = lines.length > 1 ? lines[1].trim() : "";

        KnowledgeBaseDocument document = new KnowledgeBaseDocument();
        document.setOrganizationId(organizationId);
        document.setTitle(title);
        document.setSourceType("TEXT");
        document.setStatus(DocumentStatus.PROCESSING);
        document = kbDocumentRepository.save(document);

        try {
            // Store the document content
            String filename = title.replaceAll("[^a-zA-Z0-9]", "_") + ".txt";
            try (InputStream inputStream = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))) {
                documentStorage.store(organizationId, document.getId(), filename, inputStream);
            }

            // Process the document with the AI service
            try (InputStream inputStream = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))) {
                DocumentProcessResponse processResponse = aiServiceClient.processDocument(inputStream, filename);

                for (DocumentChunkResponse chunk : processResponse.chunks()) {
                    EmbeddingResponse embeddingResponse = aiServiceClient.createEmbedding(chunk.text());

                    float[] embedding = new float[embeddingResponse.embedding().size()];
                    for (int i = 0; i < embedding.length; i++) {
                        embedding[i] = embeddingResponse.embedding().get(i);
                    }

                    kbChunkVectorRepository.insertChunk(
                            UUID.randomUUID(),
                            document.getId(),
                            organizationId,
                            chunk.text(),
                            embedding,
                            chunk.chunkIndex(),
                            chunk.tokenCount()
                    );
                }
            }

            document.setStatus(DocumentStatus.READY);
            kbDocumentRepository.save(document);

        } catch (Exception e) {
            log.error("Failed to process KB document: {}", title, e);
            document.setStatus(DocumentStatus.FAILED);
            kbDocumentRepository.save(document);
        }
    }
}
