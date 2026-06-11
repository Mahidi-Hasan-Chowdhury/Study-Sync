# Study Sync - Project Report

**AI-Powered Collaborative Learning Platform**

---

## 1. Project Overview

**Study Sync** is a cutting-edge, full-stack web application designed to revolutionize the learning experience by transforming static study materials into interactive, AI-generated assessments. The platform leverages modern cloud technologies, artificial intelligence, and real-time collaboration features to create an engaging educational environment for students and educators.

### Project Information
- **Project Name**: Study Sync
- **Version**: 0.0.1-SNAPSHOT
- **Development Period**: 2026
- **Academic Context**: Advanced Java Course Project
- **Achievement**: Infinity AI BuildFest 2026 - Finalist
- **Live URL**: https://study-sync-vdln.onrender.com

---

## 2. Project Objectives

### Primary Objectives
1. **Automated Assessment Generation**: Eliminate manual quiz creation by using AI to generate high-quality questions from uploaded documents
2. **Collaborative Learning**: Enable students to learn together through real-time quiz rooms and shared classroom spaces
3. **Progress Tracking**: Provide comprehensive dashboards for both students and teachers to monitor learning progress
4. **Accessibility**: Make learning materials accessible anytime, anywhere through a web-based platform

### Secondary Objectives
1. Demonstrate proficiency in modern Java enterprise development
2. Implement secure authentication and authorization systems
3. Integrate third-party APIs (AI, Payment, Database)
4. Follow industry best practices for code quality and project structure

---

## 3. System Architecture

### Architecture Overview
```
┌─────────────────────────────────────────────────────────────────┐
│                         Presentation Layer                        │
│                    (Thymeleaf Templates + JavaScript)             │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Application Layer                           │
│                    (Spring Boot Controllers)                      │
│  ┌──────────┐  ┌──────────────┐  ┌──────────┐  ┌─────────────┐ │
│  │   Auth   │  │   Classroom  │  │   Quiz   │  │  Payment    │ │
│  │Controller│  │  Controller  │  │Controller│  │ Controller  │ │
│  └──────────┘  └──────────────┘  └──────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Service Layer                            │
│                   (Business Logic Implementation)                 │
│  ┌──────────┐  ┌──────────────┐  ┌──────────┐  ┌─────────────┐ │
│  │   User   │  │  Classroom   │  │   Quiz   │  │   Quiz AI   │ │
│  │ Service  │  │   Service    │  │ Service  │  │   Service    │ │
│  └──────────┘  └──────────────┘  └──────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Data Access Layer                           │
│                    (Spring Data MongoDB)                         │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                     External Services                            │
│  ┌────────────┐  ┌──────────────┐  ┌──────────────┐             │
│  │   Groq     │  │  MongoDB     │  │   Stripe     │             │
│  │  Cloud AI  │  │   Atlas      │  │   Payments   │             │
│  └────────────┘  └──────────────┘  └──────────────┘             │
└─────────────────────────────────────────────────────────────────┘
```

### Design Patterns Used
1. **MVC (Model-View-Controller)**: Separation of concerns for web layer
2. **Service Layer Pattern**: Business logic abstraction
3. **Repository Pattern**: Data access abstraction
4. **DTO Pattern**: Data transfer between layers
5. **Strategy Pattern**: Different quiz generation strategies

---

## 4. Technology Stack

### Backend Technologies
| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 21+ | Core programming language |
| **Spring Boot** | 3.5.10 | Application framework |
| **Spring Data MongoDB** | 3.x | Database ORM |
| **Spring Security** | 6.x | Authentication & Authorization |
| **Spring AI** | 1.1.2 | AI integration framework |
| **Lombok** | Latest | Code generation/reduction |

### AI & External APIs
| Service | Purpose |
|---------|---------|
| **Groq Cloud LLM** | AI quiz generation (llama-3.3-70b-versatile) |
| **Stripe API** | Payment processing |
| **MongoDB Atlas** | Cloud NoSQL database |

### Frontend Technologies
| Technology | Purpose |
|------------|---------|
| **Thymeleaf** | Server-side template engine |
| **Bootstrap 5** | UI framework |
| **Boxicons** | Icon library |
| **JavaScript** | Client-side interactivity |

### Document Processing
| Library | Version | Purpose |
|---------|---------|---------|
| **Apache PDFBox** | 3.0.1 | PDF text extraction |
| **Apache POI** | 5.2.5 | DOCX/PPTX processing |

### Security Libraries
| Library | Version | Purpose |
|---------|---------|---------|
| **JJWT** | 0.13.0 | JWT token generation/validation |
| **BCrypt** | (via Spring Security) | Password hashing |

---

## 5. Features and Modules

### Module 1: Authentication & Authorization
**Description**: Secure user management with role-based access control

**Features**:
- User registration with email validation
- Secure login with JWT tokens
- Role-based system (Student, Teacher, Admin)
- Password encryption using BCrypt
- Session management with JWT

**Key Classes**:
- `AuthController.java`
- `UserService.java`
- `JwtService.java`
- `SecurityConfig.java`

### Module 2: Quiz Generation
**Description**: AI-powered generation of assessments from uploaded documents

**Features**:
- Upload PDF/DOCX documents
- Automatic text extraction
- AI-generated Multiple Choice Questions (MCQ)
- AI-generated Constructive Questions (CQ)
- Mixed quiz types
- Difficulty levels (Easy, Medium, Hard)
- Configurable question counts
- Time limit calculation

**Key Classes**:
- `QuizController.java`
- `QuizService.java`
- `QuizAiService.java`
- `PdfTextExtractor.java`

### Module 3: Quiz Taking & Attempts
**Description**: Interactive quiz interface with real-time feedback

**Features**:
- Timed quiz sessions
- Question navigation
- Answer submission
- Immediate grading for MCQ
- AI grading for CQ
- Attempt history
- Performance tracking

**Key Classes**:
- `QuizAttemptService.java`
- `QuizAttempt.java`

### Module 4: Real-time Quiz Rooms
**Description**: Collaborative quiz sessions with live competition

**Features**:
- Create/join quiz rooms
- Real-time score updates
- Room participant management
- Leaderboard display
- Room dashboard

**Key Classes**:
- `QuizRoomController.java`
- `QuizRoomService.java`
- `QuizRoom.java`
- `RoomParticipant.java`

### Module 5: Classroom Management
**Description**: Virtual classroom spaces for teachers and students

**Features**:
- Create classrooms with unique access codes
- Join classrooms via access code
- PDF material sharing
- Member management
- Classroom archiving
- Role-specific permissions

**Key Classes**:
- `ClassroomController.java`
- `ClassroomService.java`
- `ClassroomPdfController.java`
- `ClassroomPdfService.java`

### Module 6: Payment Integration
**Description**: Stripe-based payment system for premium features

**Features**:
- Stripe Checkout integration
- Subscription management
- Payment success/failure handling
- Pro user status

**Key Classes**:
- `PaymentController.java`
- `PaymentService.java`

### Module 7: Dashboard & Analytics
**Description**: User-friendly dashboard for progress tracking

**Features**:
- Quiz statistics
- Recent activities
- Classroom overview
- Performance metrics

**Key Classes**:
- `DashboardService.java`
- `DashboardStats.java`

---

## 6. Database Schema

### MongoDB Collections

#### 1. Users Collection
```javascript
{
  "_id": ObjectId,
  "username": String (unique, indexed),
  "email": String (unique, indexed),
  "password": String (BCrypt hashed),
  "role": String ("STUDENT" | "TEACHER" | "ADMIN"),
  "enabled": Boolean,
  "isPro": Boolean,
  "createdAt": LocalDateTime
}
```

#### 2. Quizzes Collection
```javascript
{
  "_id": ObjectId,
  "pdfFileName": String,
  "questions": Array<Question>,
  "quizType": String ("MCQ" | "CQ" | "MIXED"),
  "difficulty": String ("EASY" | "MEDIUM" | "HARD"),
  "questionCount": Number,
  "timeLimitSeconds": Number,
  "createdAt": LocalDateTime,
  "extractedText": String,
  "userId": String (indexed, creator),
  "mcqCount": Number,
  "cqCount": Number
}
```

#### 3. QuizAttempts Collection
```javascript
{
  "_id": ObjectId,
  "userId": String (indexed),
  "quizId": String (indexed),
  "score": Number,
  "totalMarks": Number,
  "answers": Array<UserAnswer>,
  "attemptedAt": LocalDateTime,
  "timeTaken": Number (seconds)
}
```

#### 4. Classrooms Collection
```javascript
{
  "_id": ObjectId,
  "name": String (indexed),
  "description": String,
  "teacherId": String (indexed),
  "accessCode": String (unique, indexed),
  "studentIds": Array<String>,
  "isActive": Boolean,
  "createdAt": LocalDateTime,
  "archivedAt": LocalDateTime
}
```

#### 5. QuizRooms Collection
```javascript
{
  "_id": ObjectId,
  "roomCode": String (unique),
  "quizId": String,
  "hostId": String,
  "status": String ("WAITING" | "ACTIVE" | "COMPLETED"),
  "createdAt": LocalDateTime
}
```

#### 6. ClassroomPdfs Collection
```javascript
{
  "_id": ObjectId,
  "classroomId": String (indexed),
  "fileName": String,
  "storagePath": String,
  "uploadedBy": String,
  "uploadedAt": LocalDateTime
}
```

---

## 7. Security Implementation

### Authentication Flow
1. User submits credentials to `/auth/login`
2. `UserService` validates credentials
3. `JwtService` generates JWT token
4. Token stored in localStorage (client-side)
5. Subsequent requests include `Authorization: Bearer <token>` header
6. `JwtAuthenticationFilter` validates token
7. User context set in SecurityContext

### Authorization Model
```java
public enum UserRole {
    STUDENT,  // Can join classrooms, take quizzes
    TEACHER,  // Can create classrooms, manage materials
    ADMIN     // Full system access
}
```

### Security Features
- **Password Hashing**: BCrypt with salt
- **JWT Validation**: HS256 algorithm with secret key
- **CORS Configuration**: Allowed origins for frontend
- **CSRF Protection**: Disabled for API (stateless)
- **Role-based Access**: Method-level security

---

## 8. API Structure

### Authentication APIs
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/auth/register` | POST | User registration |
| `/auth/login` | POST | User login |
| `/auth/verify` | GET | Token verification |

### Quiz APIs
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/quiz` | GET | Quiz creation page |
| `/quiz/generate` | POST | Generate quiz from PDF |
| `/quiz/my-quizzes` | GET | User's quiz list |
| `/quiz/{id}` | GET | Quiz details |
| `/quiz/{id}/take` | GET | Take quiz |
| `/quiz/{id}/submit` | POST | Submit quiz attempt |
| `/quiz/history` | GET | Quiz attempt history |

### Classroom APIs
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/classroom` | GET | List all classrooms |
| `/classroom/create` | GET/POST | Create classroom |
| `/classroom/join` | GET/POST | Join classroom |
| `/classroom/{id}` | GET | Classroom details |
| `/classroom/{id}/update` | POST | Update classroom |
| `/classroom/{id}/archive` | POST | Archive classroom |
| `/classroom/{id}/leave` | POST | Leave classroom |
| `/classroom/{id}/materials` | GET | Classroom materials |

### Payment APIs
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pricing` | GET | Pricing page |
| `/create-checkout-session` | POST | Create Stripe session |
| `/payment-success` | GET | Payment success callback |

---

## 9. AI Integration Details

### Spring AI Configuration
```properties
spring.ai.openai.api-key=${GROQ_API_KEY}
spring.ai.openai.base-url=https://api.groq.com/openai/v1
spring.ai.openai.chat.options.model=llama-3.3-70b-versatile
```

### Quiz Generation Process
1. User uploads PDF/DOCX file
2. Text extracted using Apache PDFBox/POI
3. Text sent to Groq Cloud LLM via Spring AI
4. AI generates questions based on prompt
5. Questions parsed and stored in MongoDB
6. Quiz created with unique ID

### Prompt Engineering
```
Generate {count} {difficulty} {type} questions from the following text.
Each question must be clear, unambiguous, and test understanding.
Format the response as JSON with question, options (for MCQ), and correct answer.
```

---

## 10. Deployment & Infrastructure

### Development Environment
- **Local Build**: Maven with Spring Boot plugin
- **Configuration**: `.env` file for environment variables
- **Database**: MongoDB local instance or Atlas

### Production Environment
- **Platform**: Render (PaaS)
- **Database**: MongoDB Atlas (cloud-hosted)
- **AI Service**: Groq Cloud API
- **Payment**: Stripe Production API

### Environment Variables Required
```bash
MONGODB_URI=mongodb+srv://...
GROQ_API_KEY=gsk_...
STRIPE_PUBLIC_KEY=pk_...
STRIPE_SECRET_KEY=sk_...
JWT_SECRET=... (optional, uses default)
```

---

## 11. Challenges & Solutions

### Challenge 1: AI Prompt Reliability
**Problem**: Inconsistent response formats from LLM
**Solution**: Structured prompts with clear JSON format requirements and response validation

### Challenge 2: Real-time Updates
**Problem**: Need for live score updates in quiz rooms
**Solution**: Planned WebSocket integration (currently polling-based)

### Challenge 3: Large File Handling
**Problem**: Memory issues with large PDF files
**Solution**: Streaming file upload and chunked processing

### Challenge 4: Environment Configuration
**Problem**: Different configs for dev/prod
**Solution**: Programmatic .env loading with sanitization

---

## 12. Future Enhancements

### Planned Features
1. **WebSocket Support**: True real-time quiz room updates
2. **AI Answer Grading**: Improve CQ evaluation with better prompts
3. **Analytics Dashboard**: Advanced performance analytics
4. **Mobile App**: React Native/Flutter mobile application
5. **Video Integration**: Video lecture support
6. **Whiteboard**: Collaborative drawing tool
7. **Export Features**: PDF export of quiz results
8. **Multi-language**: Support for multiple languages

### Technical Improvements
1. **Caching Layer**: Redis for session management
2. **CDN Integration**: Static asset delivery
3. **API Rate Limiting**: Prevent abuse
4. **Unit Tests**: Comprehensive test coverage
5. **Docker**: Containerized deployment

---

## 13. Conclusion

**Study Sync** represents a modern approach to educational technology, combining:
- Enterprise-grade Java development
- Cutting-edge AI capabilities
- Secure authentication and authorization
- Real-time collaboration features
- Payment integration

The project demonstrates proficiency in full-stack development, API integration, and modern software architecture principles. As a BuildFest finalist, it has been recognized for its innovation and practical value in the education sector.

---

## 14. References

1. [Spring Boot Documentation](https://spring.io/projects/spring-boot)
2. [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
3. [MongoDB Documentation](https://docs.mongodb.com/)
4. [Stripe API Documentation](https://stripe.com/docs/api)
5. [Groq Cloud API](https://console.groq.com/)

---

**Report Generated**: June 10, 2026
**Project Status**: Production Live
**Maintained By**: Advanced Java Course Development Team
