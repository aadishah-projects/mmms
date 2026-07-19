# MMMS — Demo Script

A step-by-step walkthrough of the three features added this iteration:
1. Assign a **Secretary** from the committee page
2. See each meeting's **agendas** on the committee overview
3. Create a meeting **from the committee page**

Estimated time: ~5 minutes.

---

## 0. Start the app

Two terminals.

**Backend (in-memory H2 — no database install needed):**
```bash
cd memin-backend/backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```
Wait for `Started MmmsBackendApplication` — it listens on `http://localhost:8080`
and seeds sample committees, meetings and agendas.

**Frontend:**
```bash
cd memin-frontend
npm install     # first run only
npm start
```
Opens on `http://localhost:4200`.

> Windows note: run `npm install` fresh — the shipped `node_modules` contained
> macOS-only build binaries.

---

## 1. Log in

1. Go to `http://localhost:4200`.
2. Username: **`username`**  Password: **`password`**
3. Click **Sign In**.

You land on **My Committees**. This user is a **DEPARTMENT_HEAD**, so the
role-gated controls below are visible.

*Talking point:* login returns the user's role and whether they're a secretary;
the UI uses that to decide who can assign secretaries and create meetings.

---

## 2. Open a committee

1. Click the **Academic Committee** card (it already has meetings with agendas).
   - Or go directly to
     `http://localhost:4200/committee-details/overview?committeeId=1`.

You're on the committee **overview**: committee info on the left, meeting cards in
the middle, calendar on the right.

---

## 3. Feature 1 — Assign a Secretary

1. In the committee info panel, find the **Secretary** row → it reads
   **"Not assigned"** with an **Assign** button.
2. Click **Assign**. A member dropdown appears with **Save** / **Cancel**.
3. Pick a member (e.g. **विकाश लामा**).
4. Click **Save**.
5. The row now shows the chosen secretary and the button changes to **Change**.

*What to point out:*
- Only department heads see this control.
- Saving calls `PATCH /api/committee/1/secretary?memberId=...`; choosing nothing and
  saving clears the secretary.
- A secretary gains **write access** to that committee (can create/edit its
  meetings and minutes) — that's why the next feature's button also appears for them.
- The member dropdown lists each committee member exactly once.

---

## 4. Feature 2 — Agendas on each meeting

1. Look at the **Meetings** section.
2. The **"Syllabus Update Discussion"** card lists its agenda items directly under
   the date/time/location (4 bullet points in Nepali).
3. Meetings with no agenda entries show **"No agendas"** instead.

*What to point out:*
- Previously agendas were only visible inside the minute editor — this fixes the
  todo item *"we cannot see the agendas of each meeting."*
- Agendas are fetched eagerly on the backend, so they load reliably even though the
  app runs with `spring.jpa.open-in-view=false`.

---

## 5. Feature 3 — Create a meeting from the committee page

1. Still on the committee overview, click **+ New Meeting** (above the Meetings
   list).
2. The meeting form opens with the committee **already selected** as
   *Academic Committee* — the URL is `/home/create-meeting?committeeId=1`.
3. Fill in a title, place, date, time, add an agenda/decision, and (optionally)
   submit to create the meeting.

*What to point out:*
- No need to re-pick the committee from the global create-meeting screen.
- The button is only shown to department heads and secretaries (write access).

---

## 6. Reset between runs

The H2 database is in-memory, so **restarting the backend resets all data** to the
seed state (secretary cleared, sample meetings restored). To clear the secretary
without restarting:
```bash
curl -s -u username:password http://localhost:8080/api/login -c ck.txt
curl -s -X PATCH -b ck.txt http://localhost:8080/api/committee/1/secretary
```

---

## Quick recap for the audience

| Feature | Where | One-line pitch |
|---|---|---|
| Secretary assignment | Committee overview → Secretary row | Heads delegate write access to a committee secretary |
| Agendas on meetings | Committee overview → meeting cards | Agendas are visible at a glance, not buried in the editor |
| Create meeting in place | Committee overview → + New Meeting | Add a meeting to *this* committee in one click |
