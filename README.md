# 🍃 LeafyBooks (Virtual Book Store)

LeafyBooks is a modern, full-stack Virtual Book Store web application. It provides a platform for users to browse, buy, and sell books. It features a robust authentication system (including Google OAuth), a shopping cart, a checkout process, and a community discussion forum.

## 🌟 Key Features

*   **Authentication & Authorization:** Secure JWT-based login, registration, and password reset via email.
*   **Google OAuth:** Seamless sign-in using Google accounts.
*   **Email Notifications:** Automated welcome emails and password reset links.
*   **Book Inventory:** Browse books by category, view details, and manage inventory (Admin/Seller).
*   **Shopping Cart & Orders:** Add items to cart and process orders securely.
*   **Community Forum:** Engage with other readers by posting and reading community discussions.
*   **Responsive UI:** A clean, airy, and modern Angular interface that works on both desktop and mobile.

---

## 🛠️ Technologies Used

### Backend (Spring Boot)
*   **Java 17**
*   **Spring Boot (3.2.5)** - Core framework for the REST API.
*   **Spring Security & JWT** - For securing endpoints and managing user sessions.
*   **Spring Data MongoDB** - For database interactions.
*   **MongoDB** - NoSQL database for storing users, books, orders, etc.
*   **Spring Boot Mail** - For sending automated emails (SMTP).
*   **Google API Client** - For verifying Google OAuth tokens.
*   **Lombok** - To reduce boilerplate code (Getters, Setters, Constructors).

### Frontend (Angular)
*   **Angular (v17.3.x)** - Component-based frontend framework.
*   **TypeScript** - Strongly typed JavaScript.
*   **RxJS** - For reactive programming and handling asynchronous data streams.
*   **Bootstrap 5** - For responsive layouts and UI components.
*   **FontAwesome** - For iconography.
*   **ngx-toastr** - For elegant, non-blocking toast notifications.

---

## 🚀 Getting Started

Follow these instructions to set up the project locally on your machine.

### Prerequisites

You need to have the following installed:
*   **Node.js** (v18+) and **npm**
*   **Angular CLI** (`npm install -g @angular/cli`)
*   **Java Development Kit (JDK) 17**
*   **Maven**
### 1. Database Setup (MongoDB)
1. The application uses a MongoDB Atlas cluster.
2. The connection string is already configured in `backend/src/main/resources/application.properties`.
3. Ensure your IP address is whitelisted in the MongoDB Atlas dashboard.

### 2. Backend Setup
Navigate to the `backend` directory and run the Spring Boot application:
```bash
cd backend
mvn clean install
mvn spring-boot:run
```
*The backend server will start on `http://localhost:8080`.*

### 3. Frontend Setup
Open a new terminal, navigate to the `frontend` directory, install dependencies, and start the Angular server:
```bash
cd frontend
npm install
npm start
```
*The frontend application will be available at `http://localhost:4200`.*

---

## 📦 Dependencies & Packages Installed

**Backend (`pom.xml`):**
*   `spring-boot-starter-web`
*   `spring-boot-starter-data-mongodb`
*   `spring-boot-starter-security`
*   `spring-boot-starter-validation`
*   `spring-boot-starter-mail`
*   `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (v0.11.5)
*   `google-api-client` (v2.2.0)

**Frontend (`package.json`):**
*   `@angular/core`, `@angular/common`, `@angular/forms`, `@angular/router`
*   `bootstrap`
*   `ngx-toastr`
*   `@auth0/angular-jwt` (for decoding tokens on the client)

---

## 🛡️ Default Credentials
Upon the first startup, the backend automatically creates a default administrator account:
*   **Email/Username:** `admin`
*   **Password:** `admin123`
