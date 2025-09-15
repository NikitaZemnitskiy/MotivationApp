# Buseiny – lightweight self-improvement gamification

**Stack:** Java 21, Spring Boot 3, Spring Security (basic), static UI (HTML/CSS/JS), JSON file for persistence. Lombok and Java records reduce boilerplate.

## Run
1. Install Java 21 and Maven.
2. In the project root:
   ```bash
   mvn spring-boot:run
   ```
3. Open [http://localhost:8080](http://localhost:8080) in your browser.
4. When prompted for credentials:
   - **User:** `Anna` / `rabota`
   - **Admin:** `user-admin` / `admin`

Swagger UI is available at `/swagger-ui.html` after start.

## Storage
All data is stored in `data/app-state.json` (path configured in `application.yml`). The file is created automatically on first launch.

## Features
- Daily tasks (MINUTES and CHECK kinds) defined in `dailyTasks`
- Streaks for any task with `streakEnabled`
- Weekly minutes goal from the first MINUTES task with `weeklyMinutesGoal`
- Shop and one-time goals
- Weekly progress and countdown
- Roles: USER marks tasks, ADMIN manages shop/goals/dailyTasks via JSON

## Build JAR
```bash
mvn -DskipTests package
java -jar target/buseiny-app-1.0.0.jar
```

## Note
The UI is simple and self-contained using Tailwind CDN and vanilla JS.
