package dev.daniel.fiscalflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import dev.daniel.fiscalflow.application.OutboxJobExecutor;
import dev.daniel.fiscalflow.domain.OutboxStatus;
import dev.daniel.fiscalflow.infrastructure.persistence.OutboxEventRepository;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(properties = {"app.seed-demo=false", "app.outbox-scheduling-enabled=false"})
class FiscalFlowIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    OutboxEventRepository outbox;

    @Autowired
    OutboxJobExecutor executor;

    @Test
    void createsIdempotentlyAndProcessesThroughTheOutbox() throws Exception {
        String payload = """
                {
                  "externalReference": "INV-INTEGRATION-001",
                  "documentType": "INVOICE",
                  "provider": "DEMO_FAST",
                  "customerTaxId": "20123456789",
                  "customerName": "Integration Test SAC",
                  "totalAmount": 420.50,
                  "currency": "PEN"
                }
                """;

        String first = mvc.perform(post("/api/v1/documents")
                        .header("Idempotency-Key", "integration-key-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String replay = mvc.perform(post("/api/v1/documents")
                        .header("Idempotency-Key", "integration-key-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode firstJson = mapper.readTree(first);
        JsonNode replayJson = mapper.readTree(replay);
        assertThat(replayJson.get("id").asText()).isEqualTo(firstJson.get("id").asText());

        String id = firstJson.get("id").asText();
        mvc.perform(post("/api/v1/documents/{id}/submit", id))
                .andExpect(status().isOk());

        outbox.findTop20ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                List.of(OutboxStatus.PENDING, OutboxStatus.RETRY), Instant.now()
        ).forEach(event -> executor.process(event.getId()));

        String detail = mvc.perform(get("/api/v1/documents/{id}", id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String statusValue = mapper.readTree(detail).get("document").get("status").asText();
        assertThat(statusValue).isEqualTo("ACCEPTED");
    }
}
