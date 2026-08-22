# MMMS — Meeting Minute Management System

A full-stack system for managing committees, their meetings, and the official
meeting **minutes** (supporting both English and Nepali minute formats).

- **`memin-frontend/`** — Angular 18 single-page app (Angular Material, SCSS).
- **`memin-backend/`** — Spring Boot (Java 23, Maven) REST API with role-based
  access control, committee/meeting/minute domain, invite emails, and Word/Docx
  minute export.

The two talk over HTTP; the frontend's API base URL lives in
`memin-frontend/src/global_constants.ts` (defaults to `http://localhost:8080`).

---

## Running locally

### Backend (no external database required)

The backend normally targets PostgreSQL, but a **`demo` profile** is included that
runs everything on an in-memory **H2** database and seeds sample data, so you can
start it with nothing else installed:

```bash
cd memin-backend/backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

This boots on `http://localhost:8080` and seeds committees, members, meetings and
agendas. It also promotes the demo user below to `DEPARTMENT_HEAD` so the
role-gated screens are reachable.

**Demo login:** username `username` / password `password`

### Frontend

```bash
cd memin-frontend
npm install      # first time only (installs platform-native build tools)
npm start        # serves http://localhost:4200
```

---

## Features added in this iteration

These were built against the priorities in the project's `todo.md`:

### 1. Assign a committee **Secretary** from the committee page
Department heads can now assign, change, or clear a committee's secretary directly
on the committee overview page. A "Secretary" row shows the current secretary and
an **Assign / Change** control (a member picker) that calls
`PATCH /api/committee/{id}/secretary`. Secretaries gain write access to that
committee (create/edit meetings and minutes).

### 2. See each meeting's **agendas** on the meeting view
Meeting cards on the committee overview now list that meeting's agenda items
instead of hiding them inside the minute editor. Agendas are fetched eagerly
(`MeetingRepository.findByCommitteeIdWithAgendas`) so they load reliably even with
`spring.jpa.open-in-view=false`.

### 3. Create a meeting **from the committee page**
A **+ New Meeting** button on the committee overview (visible to department heads
and secretaries) opens the meeting form with the current committee pre-selected —
no need to re-pick the committee from the global create-meeting screen.

### 4. Department Head **System Settings Panel** (Dynamic AI & SMTP Email Configuration)
Department heads can configure AI LLM endpoints and SMTP credentials dynamically without restarting or redeploying the backend:
- **AI Language Model Configuration**: Support for both **Anthropic Messages** (`/v1/messages` format for Claude, Xiaomi Mimo) and **OpenAI-Compatible Chat Completions** (`/v1/chat/completions` format for OpenAI, Groq, Ollama, DeepSeek, OpenRouter, etc.). Includes a built-in connection tester.
- **SMTP Email Configuration**: Dynamic host, port, username, password/app-key, sender from address, frontend URL, and STARTTLS settings with an integrated test email sender.
- **Multi-Domain Email Invitations**: Registration invitations can now be delivered to any valid email address/domain (not restricted to `@pcampus.edu.np`).
- **Access Control**: Strictly restricted to users with the `DEPARTMENT_HEAD` role on both the frontend UI (`/home/settings`) and backend REST endpoints (`/api/settings/**`).

---

## Roadmap (from `todo.md`, not yet done)
- Per-committee **dynamic minute template** (core feature).
- **Email invitations to a meeting** (registration invites already exist).
- Additional custom minute export styling.
