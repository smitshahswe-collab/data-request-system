# **Data Request Processing System**

A full-stack application for managing privacy-related data requests (Access, Delete, Correct) with AI-powered request summaries.

## Tech Stack

### **Backend:**
* Java 17
* Spring Boot 3.2.1
* Spring Data JPA
* H2 Database (in-memory)
* Maven
* OpenAI API (via RestTemplate)

### **Frontend:**
* React 18
* Axios
* CSS3

### **AI Integration:**
* OpenAI API (with fallback for missing API key)

---

## **Features**

### **Backend (Spring Boot)**
* RESTful APIs for creating, listing, viewing, and updating request status 
* Valid request lifecycle: `RECEIVED` → `IN_REVIEW` → `COMPLETED` / `REJECTED`
* In-memory H2 database with JPA 
* AI-generated human-readable summaries using OpenAI (with graceful fallback)
* Global exception handling and validation 
* Unit and controller tests

### **Frontend (React + Create React App)**
* Responsive UI for creating and managing requests 
* Real-time request list with details view 
* Status update workflow with valid transitions 
* AI summary prominently displayed
* Basic error/success feedback

---

## Prerequisites

* Java 17 or higher
* Node.js 16+ and npm
* Maven 3.6+
* (Optional) OpenAI API key

---

## Setup Instructions

### 1. Clone the Repository
```bash
git clone <your-repo-url>
cd data-request-system
```

### 2. Backend Setup

#### Install Dependencies
```bash
mvn clean install
```

#### Configure (Optional - for real AI summaries)
Edit `src/main/resources/application.properties`:
```properties
openai.api.key=your-actual-api-key-here
```

Or set environment variable:
```bash
export OPENAI_API_KEY=your-actual-api-key-here
```

**Note:** The system works without an API key using template-based summaries.

#### Run Backend
```bash
mvn spring-boot:run
```

Or run `DataRequestSystemApplication.java` from your IDE.

Backend will start on: **http://localhost:8080**

### 3. Frontend Setup
```bash
cd frontend
npm install
npm start
```

Frontend will start on: **http://localhost:3000**

---

## Running Tests

### Backend Tests
```bash
mvn test
```

### Frontend Tests
```bash
cd frontend
npm test
```

---

## API Documentation

### Base URL
```
http://localhost:8080/data-requests
```

### Endpoints

#### 1. Create Data Request
```bash
POST /data-requests
Content-Type: application/json

{
  "requestType": "ACCESS",
  "requesterId": "user123",
  "notes": "I need access to my personal data"
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "requestType": "ACCESS",
  "requesterId": "user123",
  "status": "RECEIVED",
  "notes": "I need access to my personal data",
  "aiSummary": "User user123 is requesting access to their personal data. Additional notes: I need access to my personal data",
  "createdAt": "2025-01-08T10:30:00",
  "lastUpdatedAt": "2025-01-08T10:30:00"
}
```

#### 2. Get All Requests
```bash
GET /data-requests
```

#### 3. Get Request by ID
```bash
GET /data-requests/1
```

#### 4. Filter Requests
```bash
# By status
GET /data-requests?status=RECEIVED

# By type
GET /data-requests?requestType=ACCESS

# By both
GET /data-requests?status=IN_REVIEW&requestType=DELETE
```

#### 5. Update Request Status
```bash
PUT /data-requests/1/status
Content-Type: application/json

{
  "status": "IN_REVIEW"
}
```

**Valid Status Transitions:**
- `RECEIVED` → `IN_REVIEW` or `REJECTED`
- `IN_REVIEW` → `COMPLETED` or `REJECTED`
- `COMPLETED` → (terminal state, no transitions)
- `REJECTED` → (terminal state, no transitions)

**Error Response (400 Bad Request):**
```json
{
  "timestamp": "2025-01-08T10:35:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Cannot transition from COMPLETED to IN_REVIEW"
}
```

---

## Sample API Testing Flow
```bash
# 1. Create a request
curl -X POST http://localhost:8080/data-requests \
  -H "Content-Type: application/json" \
  -d '{
    "requestType": "DELETE",
    "requesterId": "user456",
    "notes": "Please delete my account data"
  }'

# 2. List all requests
curl http://localhost:8080/data-requests

# 3. Get specific request
curl http://localhost:8080/data-requests/1

# 4. Update status to IN_REVIEW
curl -X PUT http://localhost:8080/data-requests/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "IN_REVIEW"}'

# 5. Update status to COMPLETED
curl -X PUT http://localhost:8080/data-requests/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "COMPLETED"}'

# 6. Try invalid transition (should fail)
curl -X PUT http://localhost:8080/data-requests/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "IN_REVIEW"}'
```

---

## How I Used GenAI Tools During Development
The assignment explicitly encouraged the thoughtful use of GenAI tools. I used them selectively as productivity aids while retaining full ownership of design, architecture, and critical implementation decisions.

### What I Delegated to AI:

1. **Code completion and suggestions**
   - Cursor provided inline suggestions for repetitive patterns (e.g., getters/setters with Lombok, basic controller method signatures, Axios call templates). This sped up writing boilerplate without affecting logic.

2. **Rapid prototyping of UI structure**  
  - I used Cursor and ChatGPT to generate initial React component layouts and CSS ideas. I then significantly refactored and refined the output to achieve the desired responsiveness, accessibility, and visual polish.

3. **Test scaffolding**  
   - Cursor helped create test class skeletons and basic assertion templates. I designed and wrote all meaningful test scenarios and edge cases myself.

4. **Documentation and prompts**  
   - Used ChatGPT for initial template ideas for README structure and OpenAI prompt phrasing. All final documentation and prompts were written and refined by me.

### What I Reviewed and Corrected:

While GenAI tools assisted with initial suggestions and scaffolding, I personally reviewed, refined, and took full responsibility for all critical aspects of the implementation:

1. **Business Logic**
    - Status transition validation logic
    - Error handling strategies
    - State management approach

2. **AI Service Implementation**
    - Prompt engineering for request summaries
    - Fallback logic when API key is missing
    - Error handling for API timeouts

3. **Security Considerations**
    - CORS configuration
    - Input validation rules
    - Error message sanitization

4. **Architecture Decisions**
    - Layered architecture implementation
    - Service abstraction patterns
    - Repository query methods

### What I Intentionally Did NOT Use AI For:

1. **Core Architecture Design**
    - Decision to use clean layered architecture
    - Choice of state transition pattern
    - API endpoint design philosophy

2. **Critical Business Rules**
    - Status transition rules (RECEIVED → IN_REVIEW → COMPLETED)
    - Validation requirements
    - Error response formats

3. **Testing Strategy**
    - Which components to test
    - Test coverage decisions
    - Mock vs integration test choices

4. **AI Integration Approach**
    - Decision to use interface abstraction
    - Fallback strategy design
    - Prompt template design

### GenAI Tools Used:
- Cursor for code completion
- ChatGPT for documentation templates and initial prompt ideas

---

## Future Enhancements

- Persistent database (PostgreSQL)
- User authentication & authorization
- Email notifications on status changes
- Advanced AI features (sentiment analysis, auto-categorization)
- Request assignment workflow
- Audit logging
- Request attachments support

---

## Known Limitations

- In-memory database (data lost on restart)
- No authentication/authorization
- No file upload support
- Basic AI summaries (could be more sophisticated)
- No pagination for large datasets