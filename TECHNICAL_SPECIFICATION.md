# Online Math and English Skills Assessment Platform - Technical Design Specification

## 1. Introduction
This document serves as detailed technical blueprint for the Online Math and English Skills Assessment Platform. It is designed to guide developers through implementation with minimal ambiguity.

**Technology Stack:**
- **Backend:** Java 17+, Spring Boot 3.x
- **Frontend:** React 18 (TypeScript recommended), Vite, TailwindCSS (for styling as per standard web app guidelines)
- **Database:** PostgreSQL
- **PDF Generation:** iText or OpenPDF

---

## 2. Database Design (Schema)

### 2.1 Database Schema (ERD Representation)

The system requires the following tables. 

#### **Table: `students`**
| Column Name | Data Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | Primary Key | Unique identifier for the student |
| `first_name` | VARCHAR(100) | Not Null | Student's first name |
| `last_name` | VARCHAR(100) | Not Null | Student's last name |
| `mobile_number` | VARCHAR(20) | Not Null | Contact number (formatting validation required) |
| `created_at` | TIMESTAMP | Default NOW() | Registration timestamp |

#### **Table: `exams`**
*Note: The requirements imply a single "Exam" definition, but we design for extensibility (Math vs English sections).*
| Column Name | Data Type | Constraints | Description |
|---|---|---|---|
| `id` | LONG | Primary Key, Auto-Inc | Unique exam ID |
| `title` | VARCHAR(255) | Not Null | e.g., "9th Grade Assessment 2024" |
| `time_limit_seconds`| INTEGER | Not Null | Total time allowed for the exam |

#### **Table: `questions`**
| Column Name | Data Type | Constraints | Description |
|---|---|---|---|
| `id` | LONG | Primary Key, Auto-Inc | Unique question ID |
| `exam_id` | LONG | Foreign Key -> exams(id) | Assoc. with specific exam |
| `subject` | VARCHAR(50) | MATH, ENGLISH | Subject enumeration |
| `content` | TEXT | Not Null | The question text |
| `options` | JSONB / TEXT | Not Null | JSON array of options e.g. `["A", "B", "C", "D"]` |
| `correct_index` | INTEGER | Not Null | Index of the correct option (0-3) |

#### **Table: `exam_sessions`**
Tracks a specific student's attempt at an exam.
| Column Name | Data Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | Primary Key | Session token/ID |
| `student_id` | UUID | Foreign Key -> students(id) | The student taking the exam |
| `exam_id` | LONG | Foreign Key -> exams(id) | The exam being taken |
| `start_time` | TIMESTAMP | Not Null | When the user clicked "Start" |
| `submit_time` | TIMESTAMP | Null | When the user clicked "Submit" |
| `status` | VARCHAR(20) | STARTED, SUBMITTED, FINISHED | Current state of the session |

#### **Table: `student_responses`**
| Column Name | Data Type | Constraints | Description |
|---|---|---|---|
| `id` | LONG | Primary Key, Auto-Inc | |
| `session_id` | UUID | Foreign Key -> exam_sessions(id) | Link to the active session |
| `question_id` | LONG | Foreign Key -> questions(id) | Link to the specific question |
| `selected_index` | INTEGER | Nullable | The option index chosen by student |
| `submitted_at` | TIMESTAMP | NOW() | Last update timestamp |

**Composite Unique Constraint:** `(session_id, question_id)` - Prevents duplicate responses for the same question.

#### **Table: `exam_results`** (Optional - for caching)
| Column Name | Data Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | Primary Key | |
| `session_id` | UUID | Foreign Key -> exam_sessions(id), Unique | One result per session |
| `math_score` | DECIMAL(5,2) | | Percentage score for Math |
| `english_score` | DECIMAL(5,2) | | Percentage score for English |
| `math_correct` | INTEGER | | Number of correct Math answers |
| `math_total` | INTEGER | | Total Math questions |
| `english_correct` | INTEGER | | Number of correct English answers |
| `english_total` | INTEGER | | Total English questions |
| `calculated_at` | TIMESTAMP | Default NOW() | When the result was calculated |

---

## 3. Backend Implementation Details (Spring Boot)

### 3.1 Domain Entities (JPA)

**Package:** `com.college.assessment.domain`

```java
@Entity
@Table(name = "students")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @NotBlank(message = "First name is mandatory")
    private String firstName;
    
    @NotBlank(message = "Last name is mandatory")
    private String lastName;
    
    @NotBlank(message = "Mobile number is mandatory")
    @Pattern(regexp = "^\\d{10}$", message = "Mobile number must be 10 digits")
    private String mobileNumber;
    
    // Getters, Setters, Constructor
}
```

```java
@Entity
@Table(name = "exam_sessions")
public class ExamSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(optional = false)
    private Student student;
    
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    private List<StudentResponse> responses;
    
    @Enumerated(EnumType.STRING)
    private SessionStatus status; // STARTED, SUBMITTED, COMPLETED
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
```

### 3.2 API Layer (Controllers & DTOs)

All API responses should be wrapped in a standard generic wrapper: `ApiResponse<T>`.

#### **3.2.1 Student Registration Controller**
**Endpoint:** `POST /api/v1/students`
*   **Request Body (`RegisterStudentDto`):**
    ```json
    {
      "firstName": "John",
      "lastName": "Doe",
      "mobileNumber": "1234567890"
    }
    ```
*   **Response:** `201 Created`
    ```json
    {
      "success": true,
      "data": {
        "studentId": "uuid-string",
        "nextAction": "/instructions"
      }
    }
    ```
*   **Logic:** 
    1. Validate inputs (`@Valid`).
    2. Save `Student` entity.
    3. Return ID for session initiation.

#### **3.2.2 Exam Flow Controller**
**Endpoint:** `POST /api/v1/exams/start`
*   **Query Param:** `studentId`
*   **Logic:** 
    1. Check if student already has a generic active session. If so, return it.
    2. If not, create new `ExamSession` with status `STARTED`.
    3. Record `startTime`.
*   **Response:**
    ```json
    {
      "sessionId": "uuid-string",
      "durationSeconds": 3600,
      "startTime": "2024-03-20T10:00:00Z"
    }
    ```

**Endpoint:** `GET /api/v1/exams/{sessionId}/questions`
*   **Query Param:** `page` (default 0), `size` (default 5)
*   **Logic:**
    1. Retrieve questions for the exam.
    2. Sort by ID or sequence order.
    3. Return paginated result.
    4. **Critical:** Do NOT expose `correct_index` in this response.
*   **Response:**
    ```json
    {
      "questions": [
        {
          "id": 101,
          "content": "What is 2+2?",
          "options": ["3", "4", "5", "6"],
          "selectedOption": null // or existing selection if autosaved
        }
      ],
      "totalPages": 4,
      "currentPage": 0,
      "isLastPage": false
    }
    ```

**Endpoint:** `PUT /api/v1/exams/{sessionId}/answers`
*   **Description:** Autosave answers.
*   **Request Body (`SubmitAnswerDto`):**
    ```json
    [
      { "questionId": 101, "selectedOptionIndex": 1 },
      { "questionId": 102, "selectedOptionIndex": 0 }
    ]
    ```
*   **Logic:**
    1. Validate `sessionId` exists and is `STARTED`.
    2. Iterate through answers, update or insert `StudentResponse` records.

**Endpoint:** `POST /api/v1/exams/{sessionId}/submit`
*   **Logic:**
    1. Mark session status as `SUBMITTED`.
    2. Provide summary stats for the review page.
*   **Response:**
    ```json
    {
        "answeredCount": 45,
        "totalCount": 50,
        "unansweredCount": 5
    }
    ```

**Endpoint:** `POST /api/v1/exams/{sessionId}/finish`
*   **Logic:**
    1. Mark session status as `COMPLETED`.
    2. Prevent any further modification.
*   **Response:**
    ```json
    {
        "reportDownloadUrl": "/api/v1/reports/{sessionId}/download"
    }
    ```

**Endpoint:** `GET /api/v1/reports/{sessionId}/download`
*   **Response Content-Type:** `application/pdf`
*   **Logic:**
    1. Fetch all `StudentResponse` entries.
    2. Compare with `Question.correctIndex`.
    3. Calculate score per subject (Math/English).
    4. Generate PDF using `iText` or `OpenPDF`.

### 3.3 Service Layer Logic

#### **Class: `AssessmentService`**
*   `startExam(UUID studentId)`: Creates session.
*   `submitAnswers(UUID sessionId, List<AnswerDto> answers)`: Transactional. Updates responses.
*   `calculateResult(UUID sessionId)`: Internal method to compute scores.

#### **Class: `ReportService`**
*   `generatePdf(UUID sessionId)`: Builds the PDF document structure (Header with student info, Score Summary, feedback).

#### **Class: `StudentService`**
*   `registerStudent(StudentRegistrationRequest request)`: Validates and creates new student.
*   `findStudentById(UUID id)`: Retrieves student details.
*   `checkDuplicateRegistration(String mobileNumber)`: Prevents duplicate registrations.

### 3.3.1 Repository Layer (Custom Queries)

**Interface: `StudentResponseRepository`**
```java
public interface StudentResponseRepository extends JpaRepository<StudentResponse, Long> {
    List<StudentResponse> findBySessionId(UUID sessionId);
    
    Optional<StudentResponse> findBySessionIdAndQuestionId(UUID sessionId, Long questionId);
    
    @Query("SELECT COUNT(sr) FROM StudentResponse sr WHERE sr.sessionId = :sessionId AND sr.selectedIndex IS NOT NULL")
    int countAnsweredBySessionId(@Param("sessionId") UUID sessionId);
}
```

**Interface: `ExamSessionRepository`**
```java
public interface ExamSessionRepository extends JpaRepository<ExamSession, UUID> {
    Optional<ExamSession> findByStudentIdAndStatus(UUID studentId, SessionStatus status);
    
    @Query("SELECT es FROM ExamSession es WHERE es.student.id = :studentId AND es.status IN :statuses")
    List<ExamSession> findActiveSessionsByStudent(@Param("studentId") UUID studentId, 
                                                   @Param("statuses") List<SessionStatus> statuses);
}
```

**Interface: `QuestionRepository`**
```java
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByExamIdOrderById(Long examId);
    
    Page<Question> findByExamId(Long examId, Pageable pageable);
    
    @Query("SELECT COUNT(q) FROM Question q WHERE q.examId = :examId")
    int countByExamId(@Param("examId") Long examId);
}
```

### 3.4 Exam Grading/Checking Flow (Detailed)

The exam checking happens **server-side** after the student finishes the exam. Here's the complete flow:

#### **Step 1: During Exam (Autosave)**
*   **When:** Student navigates between pages or clicks "Next".
*   **Action:** Frontend calls `PUT /api/v1/exams/{sessionId}/answers`
*   **Backend Logic:**
    ```java
    // In AssessmentService.submitAnswers()
    for (AnswerDto answer : answers) {
        StudentResponse response = responseRepository
            .findBySessionIdAndQuestionId(sessionId, answer.getQuestionId())
            .orElse(new StudentResponse());
        
        response.setSessionId(sessionId);
        response.setQuestionId(answer.getQuestionId());
        response.setSelectedIndex(answer.getSelectedOptionIndex());
        response.setSubmittedAt(LocalDateTime.now());
        
        responseRepository.save(response);
    }
    // Note: NO grading happens here, just saving responses
    ```

#### **Step 2: Exam Submission**
*   **When:** Student clicks "Submit Exam" on the last page.
*   **Action:** Frontend calls `POST /api/v1/exams/{sessionId}/submit`
*   **Backend Logic:**
    ```java
    // In AssessmentService.submitExam()
    ExamSession session = sessionRepository.findById(sessionId)
        .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
    
    session.setStatus(SessionStatus.SUBMITTED);
    session.setSubmitTime(LocalDateTime.now());
    sessionRepository.save(session);
    
    // Return summary stats (answered vs unanswered)
    return getSummaryStats(sessionId);
    ```

#### **Step 3: Exam Finish (Triggers Grading)**
*   **When:** Student clicks "Finish Exam" on the review page.
*   **Action:** Frontend calls `POST /api/v1/exams/{sessionId}/finish`
*   **Backend Logic:**
    ```java
    // In AssessmentService.finishExam()
    ExamSession session = sessionRepository.findById(sessionId)
        .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
    
    // Mark as completed
    session.setStatus(SessionStatus.COMPLETED);
    sessionRepository.save(session);
    
    // TRIGGER GRADING HERE
    ExamResult result = calculateResult(sessionId);
    
    return FinishExamResponse.builder()
        .reportDownloadUrl("/api/v1/reports/" + sessionId + "/download")
        .build();
    ```

#### **Step 4: Grading Logic (Internal Service Method)**
*   **Service:** `AssessmentService.calculateResult(UUID sessionId)`
*   **Logic:**
    ```java
    private ExamResult calculateResult(UUID sessionId) {
        // 1. Fetch the exam session
        ExamSession session = sessionRepository.findById(sessionId)
            .orElseThrow();
        
        // 2. Fetch all student responses for this session
        List<StudentResponse> responses = responseRepository
            .findBySessionId(sessionId);
        
        // 3. For each response, fetch the corresponding question
        //    and compare selectedIndex with correctIndex
        int mathCorrect = 0, mathTotal = 0;
        int englishCorrect = 0, englishTotal = 0;
        
        for (StudentResponse response : responses) {
            Question question = questionRepository
                .findById(response.getQuestionId())
                .orElseThrow();
            
            boolean isCorrect = (response.getSelectedIndex() != null && 
                                response.getSelectedIndex().equals(question.getCorrectIndex()));
            
            if (question.getSubject() == Subject.MATH) {
                mathTotal++;
                if (isCorrect) mathCorrect++;
            } else if (question.getSubject() == Subject.ENGLISH) {
                englishTotal++;
                if (isCorrect) englishCorrect++;
            }
        }
        
        // 4. Calculate percentages
        double mathScore = (mathTotal > 0) ? (mathCorrect * 100.0 / mathTotal) : 0;
        double englishScore = (englishTotal > 0) ? (englishCorrect * 100.0 / englishTotal) : 0;
        
        // 5. Store or return result
        return ExamResult.builder()
            .mathScore(mathScore)
            .englishScore(englishScore)
            .mathCorrect(mathCorrect)
            .mathTotal(mathTotal)
            .englishCorrect(englishCorrect)
            .englishTotal(englishTotal)
            .build();
    }
    ```

#### **Step 5: PDF Generation**
*   **When:** Student clicks "Download Report" or directly accesses the download URL.
*   **Action:** Frontend calls `GET /api/v1/reports/{sessionId}/download`
*   **Backend Logic:**
    ```java
    // In ReportService.generatePdf()
    public byte[] generatePdf(UUID sessionId) {
        // 1. Re-calculate or fetch cached result
        ExamResult result = assessmentService.calculateResult(sessionId);
        
        // 2. Fetch student info
        ExamSession session = sessionRepository.findById(sessionId).orElseThrow();
        Student student = session.getStudent();
        
        // 3. Build PDF using iText/OpenPDF
        Document document = new Document();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        
        document.open();
        document.add(new Paragraph("Assessment Report"));
        document.add(new Paragraph("Student: " + student.getFullName()));
        document.add(new Paragraph("Math Score: " + result.getMathScore() + "%"));
        document.add(new Paragraph("English Score: " + result.getEnglishScore() + "%"));
        document.close();
        
        return baos.toByteArray();
    }
    ```

#### **Key Design Decisions:**
1.  **No Real-Time Grading:** Grading only happens when student clicks "Finish", not during autosave.
2.  **Stateless Grading:** The `calculateResult()` method can be called multiple times (idempotent) - it always recalculates from stored responses.
3.  **Security:** The `correct_index` is NEVER sent to the frontend during the exam. Only backend has access.
4.  **Performance:** For large exams, consider caching the result after first calculation to avoid re-querying on every PDF download.

### 3.5 Error Handling & Validation

#### **Global Exception Handler**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(InvalidSessionStateException.class)
    public ResponseEntity<ApiResponse<?>> handleInvalidState(InvalidSessionStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Validation failed", errors));
    }
}
```

#### **Custom Exceptions**
```java
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

public class InvalidSessionStateException extends RuntimeException {
    public InvalidSessionStateException(String message) {
        super(message);
    }
}

public class DuplicateRegistrationException extends RuntimeException {
    public DuplicateRegistrationException(String message) {
        super(message);
    }
}
```

#### **Validation Rules**

**Student Registration:**
- First Name: 2-50 characters, letters only
- Last Name: 2-50 characters, letters only
- Mobile: Exactly 10 digits, numeric only

**Exam Submission:**
- Session must exist
- Session status must be STARTED (cannot submit if already SUBMITTED or COMPLETED)
- All answers must reference valid question IDs

**Answer Autosave:**
- Selected index must be between 0-3 (for 4-option questions)
- Question must belong to the exam associated with the session

### 3.6 Performance Optimizations

#### **Caching Strategy**
```java
@Service
public class ExamResultCacheService {
    
    @Autowired
    private ExamResultRepository resultRepository;
    
    @Cacheable(value = "examResults", key = "#sessionId")
    public ExamResult getOrCalculateResult(UUID sessionId) {
        return resultRepository.findBySessionId(sessionId)
            .orElseGet(() -> calculateAndCache(sessionId));
    }
    
    private ExamResult calculateAndCache(UUID sessionId) {
        ExamResult result = assessmentService.calculateResult(sessionId);
        resultRepository.save(result);
        return result;
    }
}
```

#### **Database Indexing**
```sql
-- Critical indexes for performance
CREATE INDEX idx_exam_sessions_student_status ON exam_sessions(student_id, status);
CREATE INDEX idx_student_responses_session ON student_responses(session_id);
CREATE INDEX idx_questions_exam ON questions(exam_id);
CREATE INDEX idx_students_mobile ON students(mobile_number);
```

#### **Pagination Best Practices**
- Default page size: 5 questions per page
- Maximum page size: 20 (prevent abuse)
- Use cursor-based pagination for large datasets in admin views

---

## 4. Frontend Implementation Details (React)

### 4.1 Application Structure (Routes)
Using `react-router-dom`:
1.  `/` -> **RegistrationPage**
2.  `/instructions` -> **InstructionsPage** (Protected: requires `studentId` in context/storage)
3.  `/exam` -> **ExamRunnerPage** (Protected: requires `sessionId`)
4.  `/review` -> **ReviewPage**
5.  `/result` -> **ResultPage** (Download link)

### 4.2 State Management
*   Use React **Context API** (`ExamContext`) to store:
    *   `studentInfo`: { name, id }
    *   `sessionId`: UUID
    *   `timeLeft`: Integer (managed by a global timer hook)
    *   `answers`: Map<questionId, optionIndex> (Optimistic UI updates)

### 4.3 Component Details

#### **`RegistrationPage.tsx`**
*   **Formik** or **React Hook Form** for validation.
*   **Fields:** First Name, Last Name, Mobile.
*   **OnSubmit:** Call `POST /students`. Save `studentId` to `localStorage` (for crash recovery). Redirect to `/instructions`.

#### **`ExamRunnerPage.tsx`**
*   **Layout:**
    *   Header: "Math & English Assessment" | Timer (Right aligned, Sticky)
    *   Body: List of 5 `QuestionCard` components.
    *   Footer: "Previous" (Disabled), "Next" / "Submit" buttons.
*   **Logic:**
    *   `useEffect`: Load questions for `page` `0`. 
    *   `handleNext()`: 
        1. Fire generic "save answers" API call in background (non-blocking).
        2. Increment page index.
        3. Fetch next 5 questions.
    *   **Timer Hook:** Decrement every second. If 0 -> force call `handleSubmit()`.

#### **`ReviewPage.tsx`**
*   **Display:**
    *   "You have answered X out of Y questions."
    *   "Are you sure you want to finish?"
*   **Actions:**
    *   "Return to Exam" (if logic allows, though reqs say "Finish Exam permanent").
    *   "Finish Exam" -> Call `POST /finish` -> Redirect to `/result`.

#### **`ResultPage.tsx`**
*   **Display:** "Assessment Complete. Thank you, [Name]."
*   **Action:** "Download Report" button (triggers `window.open(API_PDF_URL)`).

---

## 5. Security & Edge Cases

### 5.1 Security Measures

1.  **Session Isolation:** 
    - All exam APIs must validate that the `sessionId` exists and belongs to a valid session.
    - Prevent session hijacking by validating session state transitions.
    
2.  **CORS Configuration:**
    ```java
    @Configuration
    public class WebConfig implements WebMvcConfigurer {
        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173", "https://yourdomain.com")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowCredentials(true);
        }
    }
    ```

3.  **Rate Limiting:**
    - Implement rate limiting on registration endpoint (max 5 attempts per IP per hour)
    - Prevent exam answer spam (max 100 autosave requests per session)

4.  **SQL Injection Prevention:**
    - Use JPA parameterized queries exclusively
    - Never concatenate user input into queries

5.  **XSS Prevention:**
    - Sanitize all user inputs (student names, question content)
    - Use `@JsonProperty` annotations to control serialization

### 5.2 Edge Cases & Handling

1.  **No Back Button:** 
    - Use React Router's `useBlocker` hook to warn users
    - Disable browser back button during active exam
    ```typescript
    useEffect(() => {
      const handleBeforeUnload = (e: BeforeUnloadEvent) => {
        e.preventDefault();
        e.returnValue = '';
      };
      window.addEventListener('beforeunload', handleBeforeUnload);
      return () => window.removeEventListener('beforeunload', handleBeforeUnload);
    }, []);
    ```

2.  **Crash Recovery:**
    - Store `sessionId` and current `pageIndex` in `localStorage`
    - On app mount, check for active session and resume
    - Backend returns previously saved answers when fetching questions

3.  **Autosave Failure:**
    - Implement exponential backoff retry (3 attempts)
    - Store failed answers in `localStorage` as backup
    - Show connection status indicator to user
    - On reconnection, sync all pending answers

4.  **Timer Expiration:**
    - Server-side validation: Check if `currentTime - startTime > timeLimitSeconds`
    - Auto-submit exam when timer reaches 0
    - Prevent answer submission after time expires

5.  **Duplicate Registration:**
    - Check if mobile number already exists
    - Option 1: Reject with error message
    - Option 2: Allow but flag as duplicate for admin review

6.  **Concurrent Session Prevention:**
    - Check if student already has an active (STARTED) session
    - If yes, return existing session instead of creating new one
    - Prevents multiple simultaneous exam attempts

7.  **Incomplete Exam Handling:**
    - Sessions in STARTED state for >24 hours should be auto-expired
    - Scheduled job to clean up abandoned sessions
    ```java
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    public void expireAbandonedSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        List<ExamSession> abandoned = sessionRepository
            .findByStatusAndStartTimeBefore(SessionStatus.STARTED, cutoff);
        abandoned.forEach(session -> {
            session.setStatus(SessionStatus.EXPIRED);
            sessionRepository.save(session);
        });
    }
    ```

8.  **Question Not Found:**
    - If a question is deleted while student is taking exam, skip it gracefully
    - Don't count missing questions in final score calculation

## 6. Phase 2: Administrative Module (Detailed)

This phase implements the back-office functionality for Managers, Teachers, and Admins.

### 6.1 Database Extensions (Security & Roles)

#### **Table: `app_users`**
Stores staff credentials.
| Column Name | Data Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | Primary Key | |
| `username` | VARCHAR(50) | Unique, Not Null | Login ID |
| `password_hash` | VARCHAR(255) | Not Null | BCrypt encoded password |
| `role` | VARCHAR(20) | Not Null | ADMIN, MANAGER, TEACHER |
| `full_name` | VARCHAR(100) | | Display name |

### 6.2 Security Implementation
*   **Authentication:** Stateless JWT (JSON Web Token).
*   **Library:** Spring Security + `jjwt` (or similar).
*   **Flow:**
    1.  User POSTs credentials to `/auth/login`.
    2.  Server validates and returns `accessToken` (valid for 8 hours).
    3.  Frontend attaches `Authorization: Bearer <token>` to all `/api/admin/**` requests.
*   **RBAC (Role-Based Access Control):**
    *   `@PreAuthorize("hasRole('MANAGER')")` -> Student Data
    *   `@PreAuthorize("hasRole('TEACHER')")` -> Exam Data
    *   `@PreAuthorize("hasRole('ADMIN')")` -> All Access

### 6.3 Admin API Layer (Role-Specific)

#### **6.3.1 Authentication**
*   **Endpoint:** `POST /api/v1/auth/login` (Returns Role Checks)

#### **6.3.2 Manager Workflows (Student Data Focus)**
*Access Level: MANAGER, ADMIN*

**Endpoint:** `GET /api/v1/manager/dashboard-stats`
*   **Response:**
    ```json
    {
      "totalStudents": 150,
      "studentsRegisteredToday": 12,
      "examsCompleted": 100
    }
    ```

**Endpoint:** `GET /api/v1/manager/students`
*   **Query:** `page`, `size`, `sortBy` (RegistrationDate, Name)
*   **Response:** List of `StudentSummaryDto`.

**Endpoint:** `GET /api/v1/manager/students/{id}/contact-info`
*   **Description:** Protected endpoint to view sensitive mobile numbers.
*   **Audit Logging:** Access to this endpoint is logged in `access_logs`.

**Endpoint:** `GET /api/v1/manager/reports/download-all`
*   **Response:** ZIP file containing all generated PDF reports.

#### **6.3.3 Teacher Workflows (Content Focus)**
*Access Level: TEACHER, ADMIN*

**Endpoint:** `GET /api/v1/teacher/exams`
*   **Response:** List of exams created by the teacher (or all exams).

**Endpoint:** `POST /api/v1/teacher/exams`
*   **Body:** `CreateExamDto`
*   **Logic:** Creates a new empty exam shell.

**Endpoint:** `POST /api/v1/teacher/exams/{examId}/questions`
*   **Body:** `List<QuestionDto>` or Single `QuestionDto`
*   **Description:** Add questions to a specific exam.

**Endpoint:** `DELETE /api/v1/teacher/questions/{questionId}`
*   **Description:** Soft delete a question.

**Endpoint:** `GET /api/v1/teacher/analytics/question-performance`
*   **Description:** (Bonus) Shows which questions are most frequently missed.

### 6.4 Service Layer Architecture (Refined)

To maintain separation of concerns:

#### **Class: `StudentManagementService`**
*   **Role:** MANAGER context.
*   **Methods:**
    *   `getStudentDirectory(Pageable p)`
    *   `getStudentContactDetails(UUID studentId)`: Logs access.
    *   `exportStudentData()`: Generates CSV.

#### **Class: `ExamAuthoringService`**
*   **Role:** TEACHER context.
*   **Methods:**
    *   `createExamDraft(String title)`
    *   `addQuestion(Long examId, Question q)`
    *   `updateQuestion(Long questionId, Question q)`
    *   `publishExam(Long examId)`: Makes it available to students.

#### **Class: `AdminService`**
*   **Role:** ADMIN context.
*   **Methods:**
    *   `createUser(UserDto user)`: Create new Manager/Teacher accounts.
    *   `rotateSystemLogs()`: Maintenance tasks.

### 6.5 Frontend Admin Portal (Role-Based Views)

The frontend will use a `Layout` that conditionally renders the sidebar based on `user.role`.

#### **6.5.1 Manager View**
*   **Route:** `/manager/students`
*   **Component:** `StudentDirectory`
    *   **Columns:** Name, Registered At, Status (Exam Taken/Not).
    *   **Action:** "View Contact" (Reveals phone number in modal).
*   **Route:** `/manager/reports`
    *   **Component:** `BulkDownloadUtil`

#### **6.5.2 Teacher View**
*   **Route:** `/teacher/exams`
*   **Component:** `ExamDashboard`
    *   **List:** Shows active assessments.
    *   **Button:** "Create New Assessment".
*   **Route:** `/teacher/exams/:id/edit`
    *   **Component:** `QuestionEditor`
    *   **Interface:** Split screen. Left side = List of questions. Right side = Edit form for selected question.
    *   **Preview:** Button to see how the question looks to a student.

#### **6.5.3 Admin View**
*   **Route:** `/admin/users`
    *   **Capabilities:** Create/Delete Managers and Teachers. Reset passwords.

### 6.6 Implementation Sequence (Phase 2)
1.  **Backend:**
    1.  Setup Spring Security & JWT Filter.
    2.  Implement `AuthController`.
    3.  Create `app_users` table and seed initial Admin account.
    4.  Implement `StudentManagementService` & Controllers.
    5.  Implement `ExamAuthoringService` & Controllers.
2.  **Frontend:**
    1.  Create `/admin`, `/manager`, `/teacher` route guards.
    2.  Build `Login` page.
    3.  Build Role-specific Dashboards.

### 6.7 Detailed Class Structure (Backend Additions)

**Package:** `com.college.assessment.config`
*   `SecurityConfig.java`: Configures `SecurityFilterChain`, CORS, and CSRF.
*   `JwtTokenProvider.java`: Generates and validates tokens.

**Package:** `com.college.assessment.dto.admin`
*   `AdminLoginRequest.java`
*   `AdminStudentViewDto.java`: Excludes sensitive internal IDs where possible, formats dates.
*   `QuestionCreationDto.java`

## 7. Project Structure

### 7.1 Backend (Spring Boot)
Directory: `backend/src/main/java/com/college/assessment`

```
├── AssessmentApplication.java
├── config/
│   ├── SecurityConfig.java       # JWT, CORS, Role-based access
│   ├── WebConfig.java            # MVC settings
│   └── SwaggerConfig.java        # API Documentation setup
├── controller/
│   ├── AuthController.java       # Login endpoint
│   ├── StudentController.java    # Registration
│   ├── ExamController.java       # Student exam taking flow
│   ├── admin/
│   │   ├── AdminStudentController.java
│   │   └── AdminExamController.java
│   └── validation/               # Custom validators
├── domain/
│   ├── User.java                 # Admin/Manager/Teacher entity
│   ├── Student.java
│   ├── Exam.java
│   ├── Question.java
│   ├── ExamSession.java
│   └── StudentResponse.java
├── dto/
│   ├── request/
│   │   ├── LoginRequest.java
│   │   ├── StudentRegistrationRequest.java
│   │   ├── ExamSubmissionRequest.java
│   └── response/
│       ├── AuthResponse.java
│       ├── ExamPaperResponse.java
│       └── StudentSummaryResponse.java
├── repository/
│   ├── UserRepository.java
│   ├── StudentRepository.java
│   ├── ExamRepository.java
│   ├── QuestionRepository.java
│   └── ExamSessionRepository.java
├── service/
│   ├── AuthService.java
│   ├── StudentService.java
│   ├── ExamService.java
│   ├── ReportService.java        # PDF Generation Logic
│   └── impl/                     # Service Implementations
└── exception/
    ├── GlobalExceptionHandler.java
    ├── ResourceNotFoundException.java
    └── UnauthorizedException.java
```

### 7.2 Frontend (React + Vite)
Directory: `frontend/src`

```
├── App.tsx                       # Main Routing Logic
├── main.tsx                      # Entry point
├── assets/                       # Static images, fonts
├── components/
│   ├── common/
│   │   ├── Button.tsx
│   │   ├── InputField.tsx
│   │   ├── Card.tsx
│   │   └── Modal.tsx
│   ├── layout/
│   │   ├── Navbar.tsx
│   │   ├── Sidebar.tsx           # Admin Sidebar
│   │   └── Footer.tsx
│   ├── student/
│   │   ├── QuestionCard.tsx
│   │   └── Timer.tsx
│   └── admin/
│       ├── StudentTable.tsx
│       └── QuestionEditor.tsx
├── context/
│   ├── AuthContext.tsx           # Stores JWT & Role
│   └── ExamContext.tsx           # Stores current exam state
├── hooks/
│   ├── useAuth.ts
│   ├── useExamSession.ts
│   └── useDebounce.ts
├── pages/
│   ├── public/
│   │   ├── LandingPage.tsx
│   │   └── LoginPage.tsx
│   ├── student/
│   │   ├── InstructionsPage.tsx
│   │   ├── ExamPage.tsx
│   │   └── ResultPage.tsx
│   └── admin/
│       ├── Dashboard.tsx
│       ├── StudentDirectory.tsx
│       └── ExamManagement.tsx
├── services/
│   ├── api.ts                    # Axios instance with interceptors
│   ├── authService.ts
│   └── examService.ts
├── styles/
│   └── index.css                 # Tailwind directives
└── utils/
    ├── helpers.ts
    └── validators.ts
```

---

## 8. Deployment & Configuration

### 8.1 Environment Variables

**Backend (`application.properties` / `application.yml`)**
```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/assessment_db}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:password}
  jpa:
    hibernate:
      ddl-auto: ${DDL_AUTO:validate}
    show-sql: ${SHOW_SQL:false}
  
jwt:
  secret: ${JWT_SECRET:your-256-bit-secret-key-change-in-production}
  expiration: ${JWT_EXPIRATION:28800000} # 8 hours in milliseconds

app:
  cors:
    allowed-origins: ${CORS_ORIGINS:http://localhost:5173}
  exam:
    default-time-limit: 3600 # seconds
    questions-per-page: 5
```

**Frontend (`.env`)**
```bash
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_APP_NAME="9th Grade Assessment Platform"
```

### 8.2 Database Schema Generation (JPA)

**For this assignment, we'll use JPA's automatic schema generation instead of manual migrations.**

**Configuration in `application.yml`:**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop  # For development: drops and recreates schema on restart
      # ddl-auto: update     # For testing: updates schema without dropping data
      # ddl-auto: validate   # For production: only validates schema
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
    show-sql: true
```

**Schema Generation Notes:**
- JPA will automatically create tables based on `@Entity` annotations
- Foreign keys created from `@ManyToOne`, `@OneToMany` relationships
- Indexes created using `@Table(indexes = {...})` annotation
- Unique constraints from `@Column(unique = true)` or `@Table(uniqueConstraints = {...})`

**Example Index Annotation:**
```java
@Entity
@Table(name = "exam_sessions", indexes = {
    @Index(name = "idx_student_status", columnList = "student_id, status")
})
public class ExamSession {
    // ...
}
```

**Initial Data Seeding:**
Create a `DataSeeder` component to populate initial data:
```java
@Component
public class DataSeeder implements CommandLineRunner {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public void run(String... args) {
        // Seed admin user if not exists
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            admin.setFullName("System Administrator");
            userRepository.save(admin);
        }
    }
}
```

### 8.3 Docker Configuration

**Backend Dockerfile**
```dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN ./mvnw dependency:go-offline
COPY src src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Frontend Dockerfile**
```dockerfile
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

**docker-compose.yml**
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: assessment_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/assessment_db
      DB_USERNAME: postgres
      DB_PASSWORD: password
    depends_on:
      - postgres

  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend

volumes:
  postgres_data:
```

### 8.4 Testing Strategy

**Unit Tests (Backend)**
- Service layer: Mock repositories, test business logic
- Repository layer: Use `@DataJpaTest` with H2 in-memory database
- Controller layer: Use `@WebMvcTest` with MockMvc

**Integration Tests**
- End-to-end exam flow: Registration → Start → Answer → Submit → Finish → Download
- Use TestContainers for PostgreSQL

**Frontend Tests**
- Component tests: Vitest + React Testing Library
- E2E tests: Playwright or Cypress

### 8.5 Monitoring & Logging

**Logging Configuration**
```yaml
logging:
  level:
    com.college.assessment: INFO
    org.springframework.web: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
  file:
    name: logs/application.log
```

**Key Metrics to Monitor**
- Active exam sessions count
- Average exam completion time
- API response times (p95, p99)
- Database connection pool usage
- Failed autosave attempts
- PDF generation time

---

## 9. API Documentation (OpenAPI/Swagger)

**Swagger Configuration**
```java
@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Assessment Platform API")
                .version("1.0")
                .description("API for 9th Grade Math & English Assessment Platform"));
    }
}
```

**Access Swagger UI:** `http://localhost:8080/swagger-ui.html`

---

## 10. Future Enhancements (Out of Scope for MVP)

1. **Adaptive Testing:** Adjust question difficulty based on student performance
2. **Multi-language Support:** i18n for English/Arabic interfaces
3. **Analytics Dashboard:** Detailed insights on question performance, student demographics
4. **Email Notifications:** Send exam results to students via email
5. **Mobile App:** Native iOS/Android apps using React Native
6. **Video Proctoring:** Optional webcam monitoring during exams
7. **Question Bank Management:** Import/export questions via CSV/Excel
8. **Scheduled Exams:** Time-bound exam availability windows
9. **Certificate Generation:** Auto-generate completion certificates
10. **Social Login:** OAuth2 integration for admin users
