# Study Sync

A full‑stack AI‑powered learning platform built with **Java 25**, **Spring Boot 3.x**, **Spring AI (Ollama)**, **MongoDB**, and **Stripe**. The app extracts text from PDFs/DOCs, generates multiple‑choice quizzes using a local LLM, and lets users collaborate in real‑time quiz rooms.

## Tech Stack
- **Language**: Java 25
- **Framework**: Spring Boot 3.x, Spring MVC, Spring Security (JWT)
- **AI**: Spring AI + Ollama (local LLM, e.g., `gpt‑oss:120b-cloud`)
- **Database**: MongoDB Atlas
- **Payments**: Stripe Checkout
- **File processing**: Apache PDFBox, Apache POI
- **Build**: Maven Wrapper (`./mvnw`)

## Quick Start
1. **Clone** the repo
   ```bash
   git clone https://github.com/<YOUR_USERNAME>/Study‑Sync.git
   cd Study‑Sync
   ```
2. **Create a `.env` file** (or set environment variables) with the values from `.env.example`.
3. **Run the application**
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```
4. Open `http://localhost:8080` in a browser.

## Environment Variables (`.env.example`)
```
MONGODB_URI=mongodb+srv://<username>:<password>@cluster.mongodb.net/<db>?appName=HelloMongoCluster
STRIPE_PUBLIC_KEY=pk_test_********
STRIPE_SECRET_KEY=sk_test_********
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=gpt-oss:120b-cloud
```

## Security
- Secrets are **never committed**. They are loaded from environment variables at runtime.
- JWT (HS256) secures API endpoints.
- `.gitignore` excludes `uploads/`, `.env`, and `application-dev.properties`.

## License
MIT – see `LICENSE` for details.
