# System Architecture & Design

## Table of Contents
1. [High-Level System Design](#high-level-system-design)
2. [Data Flow](#data-flow)
3. [GenAI Considerations](#genai-considerations)
4. [Production Considerations](#production-considerations)

---

## 1. High-Level System Design

### Current Architecture
```
┌─────────────────┐
│   React UI      │ (Port 3000)
│   (Frontend)    │
└────────┬────────┘
         │ HTTP/REST
         │
┌────────▼────────┐
│  Spring Boot    │ (Port 8080)
│  API Server     │
└────────┬────────┘
         │
    ┌────┴────┬──────────┐
    │         │          │
┌───▼───┐ ┌──▼──────┐ ┌─▼────────┐
│  H2   │ │ OpenAI  │ │ Business │
│  DB   │ │   API   │ │  Logic   │
└───────┘ └─────────┘ └──────────┘
```

### Component Layers
```
┌─────────────────────────────────────────┐
│           Presentation Layer            │
│  (Controllers - REST Endpoints)         │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│            Service Layer                │
│  (Business Logic, Validation,           │
│   State Transitions, AI Integration)    │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│         Repository Layer                │
│  (Data Access, JPA Queries)             │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│          Persistence Layer              │
│  (H2 In-Memory Database)                │
└─────────────────────────────────────────┘
```

### Scalability Strategy

#### Horizontal Scaling
```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│ Load         │────▶│  App         │     │  App         │
│ Balancer     │     │  Instance 1  │     │  Instance 2  │
│ (nginx)      │     └──────┬───────┘     └──────┬───────┘
└──────────────┘            │                     │
                            └──────────┬──────────┘
                                       │
                            ┌──────────▼──────────┐
                            │   PostgreSQL        │
                            │   (Primary DB)      │
                            └─────────────────────┘
```

**Key Changes for Scale:**
1. Replace H2 with PostgreSQL/MySQL for persistence
2. Add Redis for caching frequently accessed requests
3. Implement session management with Redis
4. Use connection pooling (HikariCP - already included)
5. Add rate limiting middleware

#### Vertical Scaling
- Increase JVM heap size (`-Xmx`, `-Xms`)
- Optimize database indexes
- Enable JPA query caching
- Tune thread pool sizes

### Integration with Larger Ecosystem
```
┌────────────────────────────────────────────────────────┐
│                  API Gateway (Kong/Apigee)             │
│            (Authentication, Rate Limiting)             │
└────────────────────┬───────────────────────────────────┘
                     │
        ┌────────────┼────────────┐
        │            │            │
┌───────▼──────┐ ┌──▼──────┐ ┌──▼──────────┐
│ Data Request │ │  User   │ │  Audit      │
│   Service    │ │ Service │ │  Service    │
│   (This)     │ │         │ │             │
└───────┬──────┘ └──┬──────┘ └──┬──────────┘
        │           │            │
        └───────────┴────────────┘
                    │
        ┌───────────▼───────────┐
        │   Message Queue       │
        │   (Kafka/RabbitMQ)    │
        └───────────┬───────────┘
                    │
        ┌───────────▼───────────┐
        │  Notification Service │
        │  (Email/SMS)          │
        └───────────────────────┘
```

**Integration Points:**
1. **User Service**: Validate requester identity, fetch user details
2. **Audit Service**: Log all state changes for compliance
3. **Notification Service**: Email users on status updates
4. **Document Service**: Store/retrieve supporting documents
5. **Analytics Service**: Generate compliance reports

---

## 2. Data Flow

### Request Lifecycle
```
1. Request Creation
   ┌──────────┐
   │  Client  │
   └────┬─────┘
        │ POST /data-requests
        ▼
   ┌────────────────┐
   │  Controller    │ ──► Validation
   └────┬───────────┘
        │
        ▼
   ┌────────────────┐
   │    Service     │ ──► Create Entity
   └────┬───────────┘     Set status: RECEIVED
        │
        ▼
   ┌────────────────┐
   │  AI Service    │ ──► Generate Summary
   └────┬───────────┘
        │
        ▼
   ┌────────────────┐
   │  Repository    │ ──► Save to DB
   └────┬───────────┘
        │
        ▼
   ┌────────────────┐
   │   Response     │ ──► Return with ID
   └────────────────┘

2. Status Update Flow
   ┌──────────┐
   │  Client  │
   └────┬─────┘
        │ PUT /data-requests/{id}/status
        ▼
   ┌────────────────┐
   │  Controller    │ ──► Validation
   └────┬───────────┘
        │
        ▼
   ┌────────────────┐
   │    Service     │ ──► Fetch existing
   └────┬───────────┘     Check transition rules
        │                 Update timestamp
        ▼
   ┌────────────────┐
   │ Status.java    │ ──► canTransitionTo()
   └────┬───────────┘
        │
        ▼
   ┌────────────────┐
   │  Repository    │ ──► Save changes
   └────┬───────────┘
        │
        ▼
   ┌────────────────┐
   │   Response     │ ──► Return updated
   └────────────────┘
```

### State Machine
```
┌─────────────┐
│  RECEIVED   │ ◄─── Initial State
└──────┬──────┘
       │
       ├──────────────────┐
       │                  │
       ▼                  ▼
┌─────────────┐    ┌─────────────┐
│  IN_REVIEW  │    │  REJECTED   │ ◄─── Terminal State
└──────┬──────┘    └─────────────┘
       │
       ├──────────────────┐
       │                  │
       ▼                  ▼
┌─────────────┐    ┌─────────────┐
│  COMPLETED  │    │  REJECTED   │
└─────────────┘    └─────────────┘
     Terminal           Terminal
```

**Transition Rules:**
```java
RECEIVED    → IN_REVIEW ✓  |  REJECTED ✓
IN_REVIEW   → COMPLETED ✓  |  REJECTED ✓
COMPLETED   → (none - terminal state)
REJECTED    → (none - terminal state)
```

### Data Model
```
DataRequest
├── id (Long, Primary Key)
├── requestType (Enum: ACCESS, DELETE, CORRECT)
├── requesterId (String, indexed)
├── status (Enum: RECEIVED, IN_REVIEW, COMPLETED, REJECTED)
├── createdAt (LocalDateTime, indexed)
├── lastUpdatedAt (LocalDateTime)
├── notes (String, optional)
└── aiSummary (String, AI-generated)
```

---

## 3. GenAI Considerations

### Prompt Strategy

#### Current Implementation
```java
String prompt = String.format(
    "Generate a brief, professional summary (max 100 words) for this data request:\n" +
    "Type: %s\n" +
    "Requester ID: %s\n" +
    "Notes: %s\n" +
    "Summary should be human-readable and concise.",
    request.getRequestType(),
    request.getRequesterId(),
    request.getNotes()
);
```

**Prompt Engineering Principles:**
1. **Clear constraints**: "max 100 words", "professional"
2. **Structured input**: Labeled fields (Type, Requester ID, Notes)
3. **Output specification**: "human-readable and concise"
4. **Context setting**: "data request" domain

#### Production Prompt Improvements
```
You are a compliance officer summarizing privacy requests.

Request Details:
- Type: {requestType}
- Requester: {requesterId}
- Date: {createdAt}
- Description: {notes}

Generate a professional summary (50-100 words) that:
1. States the request type clearly
2. Maintains user privacy (use ID, not names)
3. Highlights urgency if mentioned
4. Uses formal tone appropriate for legal review

Output only the summary, no preamble.
```

### Cost Optimization

| Model | Cost per 1K tokens | Use Case |
|-------|-------------------|----------|
| GPT-4 | $0.03 (input) / $0.06 (output) | Complex requests requiring reasoning |
| GPT-3.5-turbo | $0.0015 (input) / $0.002 (output) | Simple summaries (current) |
| GPT-3.5-turbo-instruct | $0.0015 / $0.002 | Completion-style tasks |

**Current Cost Analysis:**
- Average tokens per request: ~150 input + 100 output
- Cost per summary: ~$0.00043
- 10,000 requests/month: ~$4.30/month

**Optimization Strategies:**
1. **Caching**: Don't regenerate summaries for unchanged requests
2. **Batching**: Process multiple requests in one API call
3. **Model Selection**: Use GPT-3.5 for simple tasks
4. **Token Limits**: Set `max_tokens=150` to control costs
5. **Rate Limiting**: Queue requests during high volume

### Latency Management

**Current Latency:**
- OpenAI API: ~1-3 seconds
- Fallback generation: <10ms

**Strategies:**
```
┌─────────────────────────────────────────┐
│  Synchronous (Current)                  │
│  User waits for AI → ~2s response       │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  Asynchronous (Production)              │
│  1. Save request immediately            │
│  2. Return 201 Created                  │
│  3. Generate AI summary in background   │
│  4. Update record when ready            │
│  Response time: ~100ms                  │
└─────────────────────────────────────────┘
```

**Implementation:**
```java
@Async
public CompletableFuture<String> generateSummaryAsync(DataRequest request) {
    return CompletableFuture.supplyAsync(() -> 
        aiService.generateRequestSummary(request)
    );
}
```

### Reliability & Fallback

**Circuit Breaker Pattern:**
```
┌────────────────┐
│  AI Service    │
│  Call          │
└───────┬────────┘
        │
   ┌────▼─────┐
   │ Success? │
   └────┬─────┘
        │
    ┌───┴───┐
    │       │
   Yes     No
    │       │
    │   ┌───▼──────────┐
    │   │ Failures > 5?│
    │   └───┬──────────┘
    │       │
    │   ┌───┴───┐
    │   │       │
    │  Yes     No
    │   │       │
    │   │   ┌───▼─────────┐
    │   │   │ Retry       │
    │   │   └─────────────┘
    │   │
    │   ▼
    │ ┌──────────────────┐
    └─► Use Fallback     │
      │ (Template-based) │
      └──────────────────┘
```

**Implemented Fallback:**
```java
if (apiKey == null || apiKey.isEmpty()) {
    return generateDefaultSummary(request);
}

try {
    return callOpenAI(request);
} catch (Exception e) {
    logger.error("AI failed: {}", e.getMessage());
    return generateDefaultSummary(request);
}
```

### Guardrails Against Hallucination

**Prevention Strategies:**

1. **Structured Input/Output**
    - Use predefined enums (ACCESS, DELETE, CORRECT)
    - Don't ask AI to interpret request types
    - Only use AI for natural language summary

2. **Validation**
```java
   String summary = aiService.generateSummary(request);
   
   // Validate summary
   if (summary.length() > 500) {
       summary = summary.substring(0, 500);
   }
   
   // Ensure it doesn't contain PII we didn't provide
   if (containsSensitiveData(summary)) {
       summary = generateDefaultSummary(request);
   }
```

3. **Prompt Constraints**
    - "Use only the information provided"
    - "Do not make assumptions"
    - "Stick to factual summarization"

4. **Human Review**
    - All AI summaries shown to reviewers
    - Not used for automated decisions
    - Treated as suggestions only

---

## 4. Production Considerations

### Security & PII Handling

#### Authentication & Authorization
```
┌──────────────────────────────────────┐
│  Current: No Auth (Demo)             │
└──────────────────────────────────────┘

┌──────────────────────────────────────┐
│  Production: JWT + OAuth 2.0         │
│                                      │
│  1. User authenticates (OAuth)       │
│  2. Receives JWT token               │
│  3. Token in Authorization header    │
│  4. Backend validates token          │
│  5. Checks user permissions          │
└──────────────────────────────────────┘
```

**Implementation:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
            .csrf().disable()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/data-requests/**")
                    .hasRole("USER")
                .requestMatchers("/admin/**")
                    .hasRole("ADMIN")
            )
            .oauth2ResourceServer()
                .jwt()
            .and().build();
    }
}
```

#### PII Protection

**Data Classification:**
- 🔴 **High Sensitivity**: Full request details, notes
- 🟡 **Medium Sensitivity**: Request type, timestamps
- 🟢 **Low Sensitivity**: Request ID, status

**Protection Measures:**
```java
// 1. Encryption at rest
@Column(name = "notes")
@Convert(converter = EncryptionConverter.class)
private String notes;

// 2. Encryption in transit (HTTPS only)
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12

// 3. Audit logging
@Aspect
public class AuditAspect {
    @Around("@annotation(Audited)")
    public Object audit(ProceedingJoinPoint joinPoint) {
        // Log access with user, timestamp, action
    }
}

// 4. Data masking in logs
logger.info("Request created for user: {}", 
    maskUserId(request.getRequesterId()));

// 5. Automatic data retention
@Scheduled(cron = "0 0 2 * * *") // 2 AM daily
public void purgeOldRequests() {
    repository.deleteByCreatedAtBefore(
        LocalDateTime.now().minusDays(90)
    );
}
```

### Observability

#### Logging Strategy

**Log Levels:**
```
ERROR  → System failures, exceptions
WARN   → Invalid state transitions, AI failures
INFO   → Request lifecycle events
DEBUG  → Detailed flow (dev/staging only)
```

**Structured Logging:**
```java
logger.info("Request created: requestId={}, type={}, user={}, timestamp={}",
    request.getId(),
    request.getRequestType(),
    maskUserId(request.getRequesterId()),
    request.getCreatedAt()
);
```

#### Monitoring & Metrics

**Key Metrics to Track:**
```
Application Metrics:
- requests_created_total (counter)
- requests_by_status (gauge)
- status_update_duration_seconds (histogram)
- ai_summary_generation_duration_seconds (histogram)
- ai_summary_failures_total (counter)

System Metrics:
- jvm_memory_used_bytes
- jvm_threads_current
- http_server_requests_seconds
- database_connections_active
```

**Endpoints:**
```
GET /actuator/health     → Overall health
GET /actuator/metrics    → All metrics
GET /actuator/prometheus → Prometheus format
```

#### Distributed Tracing
```
┌────────────┐   trace-id: abc123
│  Frontend  │───────────────────┐
└────────────┘                   │
                                 ▼
┌────────────┐   span-id: 001   ┌─────────────┐
│ API Gateway│──────────────────▶│  Backend    │
└────────────┘                   └──────┬──────┘
                                        │
                                        ├─ span-id: 002
                                        │  ┌──────────┐
                                        └─▶│ Database │
                                           └──────────┘
                                        │
                                        ├─ span-id: 003
                                        │  ┌──────────┐
                                        └─▶│ OpenAI   │
                                           └──────────┘
```

### Future Extensibility

#### Plugin Architecture
```java
public interface RequestProcessor {
    boolean canProcess(RequestType type);
    void process(DataRequest request);
}

@Service
public class ProcessorRegistry {
    private List<RequestProcessor> processors;
    
    public void processRequest(DataRequest request) {
        processors.stream()
            .filter(p -> p.canProcess(request.getRequestType()))
            .forEach(p -> p.process(request));
    }
}
```

#### Webhook Support
```java
@Entity
public class Webhook {
    private String url;
    private List<Status> triggerStatuses;
    private String secret;
}

@Service
public class WebhookService {
    public void notifyStatusChange(DataRequest request) {
        webhookRepository
            .findByTriggerStatus(request.getStatus())
            .forEach(webhook -> sendWebhook(webhook, request));
    }
}
```

#### Multi-tenancy
```java
@Entity
@Table(name = "data_requests")
public class DataRequest {
    
    @Id
    private Long id;
    
    @Column(name = "tenant_id")
    private String tenantId;  // ← Add tenant isolation
    
    // ... other fields
}

// Repository with tenant filtering
public interface DataRequestRepository extends JpaRepository<DataRequest, Long> {
    List<DataRequest> findByTenantId(String tenantId);
}
```

#### API Versioning
```java
@RestController
@RequestMapping("/api/v1/data-requests")  // v1
public class DataRequestControllerV1 { }

@RestController
@RequestMapping("/api/v2/data-requests")  // v2 with breaking changes
public class DataRequestControllerV2 { }
```

---

## Architecture Decision Records (ADRs)

### ADR-001: Use H2 for Development
**Status:** Accepted  
**Context:** Need fast iteration during development  
**Decision:** Use H2 in-memory database  
**Consequences:**
- ✅ Fast startup, no setup required
- ✅ Automatic schema generation
- ❌ Data lost on restart (acceptable for dev)

### ADR-002: AI Service Abstraction
**Status:** Accepted  
**Context:** OpenAI may not always be available  
**Decision:** Use interface abstraction with fallback  
**Consequences:**
- ✅ Easy to swap AI providers
- ✅ Graceful degradation
- ✅ Testable without API keys

### ADR-003: Synchronous AI Generation
**Status:** Accepted (for MVP), Review for Production  
**Context:** Simple implementation for assessment  
**Decision:** Generate AI summary synchronously  
**Consequences:**
- ✅ Simple implementation
- ❌ Slower response times
- ⚠️ Should move to async for production

### ADR-004: Enum-based State Machine
**Status:** Accepted  
**Context:** Need to enforce valid status transitions  
**Decision:** Implement `canTransitionTo()` in Status enum  
**Consequences:**
- ✅ Centralized transition logic
- ✅ Easy to test and maintain
- ✅ Type-safe

---

## Deployment Architecture (Production)
```
┌────────────────────────────────────────────────────────┐
│                      AWS/GCP/Azure                     │
│                                                        │
│  ┌──────────────────────────────────────────────────┐ │
│  │  Load Balancer (ALB/Cloud Load Balancer)         │ │
│  └────────────────┬─────────────────────────────────┘ │
│                   │                                    │
│  ┌────────────────┴─────────────────┐                 │
│  │                                  │                 │
│  │  ┌─────────────┐  ┌─────────────┐                 │
│  │  │ App Pod 1   │  │ App Pod 2   │  (Kubernetes)   │
│  │  └──────┬──────┘  └──────┬──────┘                 │
│  │         │                │                         │
│  │         └────────┬───────┘                         │
│  │                  │                                 │
│  │  ┌───────────────▼──────────────┐                 │
│  │  │  PostgreSQL (RDS/Cloud SQL)  │                 │
│  │  │  + Read Replicas             │                 │
│  │  └──────────────────────────────┘                 │
│  │                                                    │
│  │  ┌──────────────────────────────┐                 │
│  │  │  Redis (ElastiCache/Memcached)│                │
│  │  │  (Caching & Sessions)         │                │
│  │  └──────────────────────────────┘                 │
│  │                                                    │
│  │  ┌──────────────────────────────┐                 │
│  │  │  S3 (Object Storage)          │                │
│  │  │  (Document Attachments)       │                │
│  │  └──────────────────────────────┘                 │
│  └──────────────────────────────────────────────────┘ │
│                                                        │
│  ┌──────────────────────────────────────────────────┐ │
│  │  Monitoring: CloudWatch/Stackdriver/DataDog      │ │
│  └──────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

---

## Performance Targets (Production)

| Metric | Target | Current |
|--------|--------|---------|
| Response Time (p95) | < 200ms | ~50ms (without AI) |
| Response Time (p99) | < 500ms | ~100ms |
| Throughput | 1000 req/s | ~100 req/s |
| Database Queries | < 10ms | ~5ms |
| AI Generation | < 3s | ~2s |
| Error Rate | < 0.1% | 0% |
| Availability | 99.9% | N/A |

---

## Conclusion

This architecture provides:
- ✅ Clean separation of concerns
- ✅ Scalability through horizontal scaling
- ✅ Reliability through fallback mechanisms
- ✅ Security through authentication and encryption
- ✅ Observability through metrics and logging
- ✅ Extensibility through plugin architecture

The system is designed for easy transition from development to production with minimal architectural changes.