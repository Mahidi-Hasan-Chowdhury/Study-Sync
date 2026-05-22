# Study Sync 🚀

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![MongoDB](https://img.shields.io/badge/MongoDB-Atlas-47A248?style=for-the-badge&logo=mongodb&logoColor=white)](https://www.mongodb.com/atlas)
[![Stripe](https://img.shields.io/badge/Stripe-Payments-626CD9?style=for-the-badge&logo=stripe&logoColor=white)](https://stripe.com/)

**Study Sync** is a cutting-edge, AI-powered full-stack learning platform designed to revolutionize how students and educators interact with study materials. By leveraging local LLMs and modern web technologies, it transforms static documents into interactive, collaborative learning experiences.

---

## ✨ Key Features

- 📄 **Smart Document Processing**: Automatically extract text from PDFs and Word documents using Apache PDFBox and POI.
- 🤖 **AI-Powered Quiz Generation**: Seamlessly generate high-quality Multiple Choice Questions (MCQs) from your study materials using **Spring AI** and **Ollama** (local LLM).
- 👥 **Real-time Quiz Rooms**: Collaborate and compete with peers in real-time quiz rooms powered by WebSockets.
- 💳 **Secure Payments**: Integrated **Stripe Checkout** for premium features and subscription management.
- 🔐 **Robust Security**: Secure API endpoints with **JWT (JSON Web Tokens)** and role-based access control.
- 📊 **Insightful Dashboard**: Track your learning progress, quiz attempts, and performance metrics.

---

## 🛠️ Tech Stack

- **Backend**: Java 25, Spring Boot 3.x
- **AI Integration**: Spring AI + Ollama (Local LLM Support)
- **Database**: MongoDB Atlas (NoSQL)
- **Security**: Spring Security, JWT (HS256)
- **File Handling**: Apache PDFBox, Apache POI
- **Payments**: Stripe API
- **Build Tool**: Maven

---

## 🚀 Getting Started

### Prerequisites

- **Java 25** installed.
- **Ollama** installed and running locally (for AI features).
- **MongoDB** instance (local or Atlas).
- **Stripe Account** (for payment testing).

### Installation

1. **Clone the Repository**
   ```bash
   git clone https://github.com/<YOUR_USERNAME>/Study-Sync.git
   cd Study-Sync
   ```

2. **Configure Environment Variables**
   Create a `.env` file in the root directory (refer to `.env.example`):
   ```properties
   MONGODB_URI=mongodb+srv://<username>:<password>@cluster.mongodb.net/studysync
   STRIPE_PUBLIC_KEY=pk_test_your_public_key
   STRIPE_SECRET_KEY=sk_test_your_secret_key
   OLLAMA_BASE_URL=http://localhost:11434
   OLLAMA_MODEL=llama3 # or your preferred model
   ```

3. **Run the Application**
   ```bash
   ./mvnw spring-boot:run
   ```

4. **Access the App**
   Open [http://localhost:8080](http://localhost:8080) in your favorite browser.

---

## 🔒 Security & Privacy

- **Zero-Secret Commitment**: Sensitive credentials are never committed to version control. All configuration is handled via environment variables.
- **Data Protection**: User data and documents are handled with industry-standard security practices.
- **Local AI**: By using Ollama, your study data stays private and is processed locally on your machine.

---

## 📜 License

Distributed under the MIT License. See `LICENSE` for more information.

---

## 🤝 Contributing

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

*Developed with ❤️ as a part of the Advanced Java Course.*
# Trigger redeploy
