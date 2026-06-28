package dev.daniel.fiscalflow.web;

import dev.daniel.fiscalflow.application.DocumentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1")
public class DocumentController {
    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @PostMapping("/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiModels.DocumentResponse create(
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody ApiModels.CreateDocumentRequest request
    ) {
        return ApiModels.DocumentResponse.from(service.create(idempotencyKey, request.toCommand()));
    }

    @GetMapping("/documents")
    public List<ApiModels.DocumentResponse> list() {
        return service.list().stream().map(ApiModels.DocumentResponse::from).toList();
    }

    @GetMapping("/documents/{id}")
    public ApiModels.DocumentDetailResponse get(@PathVariable UUID id) {
        return new ApiModels.DocumentDetailResponse(
                ApiModels.DocumentResponse.from(service.get(id)),
                service.timeline(id).stream().map(ApiModels.TimelineEventResponse::from).toList()
        );
    }

    @PostMapping("/documents/{id}/submit")
    public ApiModels.DocumentResponse submit(@PathVariable UUID id) {
        return ApiModels.DocumentResponse.from(service.submit(id));
    }

    @PostMapping("/documents/{id}/retry")
    public ApiModels.DocumentResponse retry(@PathVariable UUID id) {
        return ApiModels.DocumentResponse.from(service.retry(id));
    }

    @GetMapping("/dashboard")
    public ApiModels.DashboardResponse dashboard() {
        return ApiModels.DashboardResponse.from(service.dashboard());
    }
}
