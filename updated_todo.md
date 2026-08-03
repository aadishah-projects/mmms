## What remains for future sessions

### 🎯 Core features (from `todo.md`, in priority order)
1. **Dynamic per-committee minute template** — *the big one, top priority.* Today only two fixed templates exist (`minute-english-1`, `minute-nepali-1`). Needs: a backend template model owned by each committee, a template editor UI, and the minute generator wired to use the committee's template. Everything below (and the stated end-goal) builds on this.
2. **Email invitation to a meeting** — notify/invite members when a meeting is created. The pieces are half-there: `EmailService` exists (currently only registration invites) and meetings already store invitees — needs a meeting-invite email + a trigger/endpoint.
3. **AI-assisted minute drafting** — generate the full minute from roughly-typed agendas + member details + signatures. Depends on #1; this is the todo's "secondary" end-goal.
4. **Digital signatures in the minute** — the stated goal mentions pulling members' digital signatures into the generated minute; not started.

### 🛠️ Hardening / tech debt (recommended alongside the above)
5. **Tests** — the backend has a JUnit suite, but no tests were added for the 3 new features or 3 bug fixes this session.
6. **Root-cause the over-fetch** — the duplicate-members bug was fixed at the display layer; the `@EntityGraph` still fetches a cartesian product (wasted work). Consider splitting collection fetches or `@BatchSize`.
7. **Audit other endpoints for the same lazy-loading trap** — I fixed committee-overview and extended-summary under `open-in-view=false`; others may still lurk.
8. **Move secrets to config** — hardcoded DB password default (`year=2082@`) and the hardcoded email link base (`localhost:4200`) should come from env/config before any real deployment.

### ✅ Done (so you don't re-scope it)
Secretary assignment · agendas on meeting cards · create-meeting from the committee page · registration/invite emails · the 3 bug fixes above.

Both servers are still running if you want to click through Manage Users or anything else. Suggested next session: start on **#1, the dynamic minute template** — it's the keystone for the rest.