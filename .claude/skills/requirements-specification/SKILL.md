---
name: requirements-specification
description: Abstract body of knowledge for software requirements engineering. Use when turning a feature idea or stakeholder ask into structured, testable requirements — user stories, EARS acceptance criteria, Gherkin scenarios, non-functional requirements, traceability matrices, MoSCoW prioritization, definition of ready / done. Includes elicitation prompts, INVEST and testability checks, ambiguity detection, and a requirements-document template. Domain-agnostic.
---

# Requirements Specification

A reusable, domain-agnostic toolkit for writing precise, testable software requirements. Apply it whenever a feature ask is fuzzy, contested, or large enough that you wouldn't want to discover surprises during implementation.

## 1. Elicitation — get the real ask

Before writing anything, surface:

- **Goal**: what business outcome are we trying to produce? (Not the feature — the *outcome*.)
- **Actors**: who initiates this? Who is affected? Who operates it?
- **Trigger**: what event causes the system to act?
- **Success**: how will we know it worked? (Observable, ideally measurable.)
- **Out of scope**: what is *not* part of this work?
- **Constraints**: deadlines, regulations, existing contracts, performance budgets.
- **Assumptions**: things you'll proceed as if true unless told otherwise.

If any of these are missing, ask focused questions before specifying. A short list of crisp questions beats a long requirements doc built on guesses.

### Useful elicitation prompts
- "What problem does this solve, and for whom?"
- "What happens today without it?"
- "What would make us roll this back?"
- "Who else is affected that we haven't talked about?"
- "What is the cheapest version of this that is still worth doing?" (helps MoSCoW)

## 2. User stories (INVEST)

Format:

> **As a** \<role\> **I want** \<capability\> **so that** \<benefit\>.

Each story must be **INVEST**:
- **I**ndependent — minimal coupling to other stories.
- **N**egotiable — invites a conversation; not a contract.
- **V**aluable — to a real user or stakeholder.
- **E**stimable — team can size it.
- **S**mall — fits in a single iteration.
- **T**estable — concrete acceptance criteria exist.

If a story fails one of these, split it or rewrite it.

## 3. Acceptance criteria — EARS

**EARS** (Easy Approach to Requirements Syntax) gives unambiguous, testable criteria. Pick the right pattern per criterion:

- **Ubiquitous** — *The \<system\> shall \<response\>.*
  Always true.
- **Event-driven** — *When \<trigger\>, the \<system\> shall \<response\>.*
- **State-driven** — *While \<state\>, the \<system\> shall \<response\>.*
- **Optional / feature** — *Where \<feature is enabled\>, the \<system\> shall \<response\>.*
- **Unwanted behavior** — *If \<unwanted condition\>, then the \<system\> shall \<response\>.*
- **Complex** — compose the above (e.g., *While X, when Y, the system shall Z.*).

Each EARS sentence is **one** requirement. Split conjunctions ("and", "or") into separate criteria.

## 4. Acceptance criteria — Gherkin (when you need scenarios)

Use **Gherkin** when behavior depends on context and you want concrete examples:

```
Feature: <capability>

  Scenario: <one specific case>
    Given <pre-condition>
    And <another pre-condition>
    When <event>
    Then <observable outcome>
    And <another observable outcome>
```

Rules of thumb:
- One scenario, one behavior. No `Or` chains.
- `Then` must be observable from outside the system.
- Use **Scenario Outline** with Examples tables for equivalence classes and boundaries.
- Background blocks only for *truly* shared setup.

EARS and Gherkin are complementary: EARS captures the rule, Gherkin shows representative examples.

## 5. Non-functional requirements (NFRs)

Use a taxonomy so you don't forget categories. Two common ones:

**FURPS+**: Functionality, Usability, Reliability, Performance, Supportability, plus Design, Implementation, Interface, Physical constraints.

**ISO/IEC 25010**: Functional Suitability, Performance Efficiency, Compatibility, Usability, Reliability, Security, Maintainability, Portability.

Every NFR must be **measurable**:

> Bad: "The system shall be fast."
> Good: "The 95th-percentile response time for `GET /resource` shall be ≤ 300 ms under 100 concurrent users."

Always specify: **metric, target, condition, measurement method**.

## 6. Testability and ambiguity checks

Before finalizing each requirement, ask:

- **Testable?** Can someone write a test (manual or automated) that decides pass/fail without further interpretation?
- **Atomic?** Exactly one rule.
- **Unambiguous?** No vague words: *fast, scalable, user-friendly, robust, secure, intuitive, modern, simple, appropriate, efficient*. Replace with measurable criteria.
- **Necessary?** Tied to a goal or constraint — not "nice to have" smuggled in.
- **Consistent?** Doesn't contradict another requirement.
- **Feasible?** Achievable with available technology/time/budget.
- **Free of implementation details?** Specifies *what*, not *how*.

## 7. Prioritization — MoSCoW

Each requirement is tagged:
- **Must** — release blocked without it.
- **Should** — important but not blocking.
- **Could** — nice if cheap.
- **Won't (this time)** — explicitly out of scope; record it so it doesn't sneak back.

Keep **Must** under ~60% of the total; otherwise it's not prioritization.

## 8. Traceability matrix

| ID | Requirement | Source / stakeholder | Priority | Verifies by | Linked to |
|----|-------------|----------------------|----------|-------------|-----------|
| R1 | ...         | Stakeholder X        | Must     | Test T1, T2 | Story S1  |

Traceability is what lets you answer "why does this exist?" months later and "what breaks if we remove it?" during refactors.

## 9. Definition of Ready / Definition of Done

**Definition of Ready** (a story can be picked up):
- User story written, INVEST-clean.
- Acceptance criteria present (EARS and/or Gherkin), each testable.
- Non-functional constraints stated.
- Dependencies identified.
- Out-of-scope items called out.
- Open questions resolved or explicitly deferred.

**Definition of Done** (a story can be closed):
- All acceptance criteria pass.
- Tests at the appropriate levels exist and pass.
- Non-functional targets verified.
- Traceability updated.
- Stakeholder accepts.

## 10. Output template — requirements document

```
# Requirements: <feature name>

## 1. Goal & context
- Business goal: ...
- Why now: ...
- Stakeholders: ...

## 2. Scope
### In scope
- ...
### Out of scope
- ...

## 3. Actors
- <actor>: <role>

## 4. User stories
- S1. As a <role>, I want <capability>, so that <benefit>.
- S2. ...

## 5. Functional requirements (EARS)
- R1. When <trigger>, the system shall <response>.
- R2. While <state>, the system shall <response>.
- R3. If <unwanted condition>, then the system shall <response>.
- ...

## 6. Scenarios (Gherkin) — for the non-obvious cases
Feature: ...
  Scenario: ...
    Given ...
    When ...
    Then ...

## 7. Non-functional requirements
- N1. Performance: <metric, target, condition, method>.
- N2. Security: ...
- N3. ...

## 8. Assumptions & constraints
- A1. ...
- C1. ...

## 9. Prioritization (MoSCoW)
| ID | Priority |
|----|----------|
| R1 | Must     |
| R2 | Should   |

## 10. Traceability
| ID | Source | Verifies by | Linked to |
|----|--------|-------------|-----------|

## 11. Open questions
- Q1. ...
```

## 11. Anti-patterns to avoid

- **Solutioning in disguise** — "The system shall use Redis" is not a requirement; it's a design choice. Replace with the *property* you actually need (e.g., "p95 read latency ≤ 5 ms").
- **Compound requirements** — "and"/"or" hidden in one sentence. Split.
- **Untestable adjectives** — *fast, user-friendly, scalable, robust*. Replace with measurable criteria.
- **Implicit actors** — "The system shall send a notification" — to whom, by what channel, on what trigger?
- **Hidden NFRs** — performance/security/compliance discovered only at release time.
- **Requirements without traceability** — orphan requirements rot.
- **Frozen requirements** — they will change; design the document for change (versioned, linked, prioritized).
