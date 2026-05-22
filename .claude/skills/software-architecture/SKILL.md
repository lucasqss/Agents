---
name: software-architecture
description: Abstract body of knowledge for system- and component-level software architecture. Use when designing a new component, refactoring an existing one, deciding where a responsibility belongs, evaluating structural trade-offs (sync vs. async, monolith vs. service, where to put a boundary), choosing between architectural styles (Clean / Hexagonal / Layered / Event-driven), applying SOLID, GRASP, GoF, DDD, or Connascence reasoning, or producing an architecture proposal. Language- and framework-agnostic.
---

# Software Architecture

A reusable, language-agnostic toolkit for principled architectural reasoning. Apply it whenever a design decision is non-trivial — i.e., when at least two reasonable options exist and the wrong choice is expensive to reverse.

## 1. Frame the problem before designing

Before proposing anything, answer:

- **What problem are we solving?** One sentence.
- **Who are the actors and stakeholders?** Internal callers, external systems, operators, end users.
- **What are the forces?** Functional requirements, non-functional requirements (performance, availability, consistency, security, evolvability, operability, cost), and explicit constraints (team size, deadline, existing tech, regulatory).
- **What is the blast radius of getting it wrong?** Cheap-to-reverse decisions deserve less analysis than one-way doors.

If any of the above are missing, surface the gap before designing.

## 2. Reasoning principles

Use these as lenses — not checklists. The point is to *reason*, not to recite.

### SOLID (object-level)
- **S**ingle Responsibility — one reason to change.
- **O**pen/Closed — extend without modifying.
- **L**iskov Substitution — subtypes must honor supertype contracts.
- **I**nterface Segregation — many small, role-based interfaces beat one fat one.
- **D**ependency Inversion — depend on abstractions, not concretions.

### GRASP (responsibility assignment)
Information Expert, Creator, Controller, Low Coupling, High Cohesion, Polymorphism, Pure Fabrication, Indirection, Protected Variations.

Use GRASP to answer "**where should this responsibility live?**" — usually with the class/module that has the information needed to fulfill it (Information Expert).

### GoF / structural patterns
Strategy, Adapter, Facade, Decorator, Observer, Composite, Command, State, Template Method, Factory, Builder, Chain of Responsibility, Mediator, Repository, Specification, Anti-Corruption Layer.

Reach for a pattern only when it removes pain you can name. Patterns are *vocabulary*, not *goals*.

### Architectural styles
- **Layered** — simple, familiar; risks anemic domain.
- **Clean / Onion / Hexagonal (Ports & Adapters)** — domain at the center, IO at the edges; best when business logic must outlive infrastructure choices.
- **Event-driven / EDA** — loose coupling, async by default; pay in eventual consistency and operational complexity.
- **Microservices** — independent deployability; pay in distributed-systems hazards. Justify the split with a deployment, scaling, or team-autonomy reason — never "modularity alone".
- **Modular monolith** — usually the right default.

### DDD
- **Strategic**: Ubiquitous Language, Bounded Contexts, Context Maps (Shared Kernel, Customer/Supplier, Conformist, Anti-Corruption Layer, Open Host Service, Published Language, Separate Ways).
- **Tactical**: Entities, Value Objects, Aggregates (with one root and one transactional boundary), Domain Events, Domain Services, Repositories, Factories.

### Connascence (coupling, graded)
From weakest to strongest: Name → Type → Meaning → Algorithm → Position → Execution → Timing → Value → Identity. **Move strong connascence into the same module; keep cross-module connascence weak.** A great refactoring heuristic.

### Distributed-systems trade-offs
- **CAP / PACELC** — under partition, choose Consistency or Availability; otherwise choose Latency or Consistency.
- **Consistency models** — strong, causal, read-your-writes, eventual.
- **Idempotency** — every retried side effect must be safe to repeat. Design idempotency keys at the boundary.
- **Failure modes** — partial failure, network split, slow node, message duplication, message loss, out-of-order delivery, clock skew.
- **Transactions across services don't exist** — use Sagas (orchestration or choreography) with compensations.

## 3. The design workflow

1. **Restate the problem and forces** (from §1). Confirm with the user if anything is missing.
2. **Sketch alternatives** — *always at least two*. A single "obvious" answer is a smell; enumerate the rejected options too.
3. **Score each option against the forces** — list pros, cons, risks, reversibility, cost.
4. **Recommend one** with explicit reasoning. Name the principle(s) that drove the choice.
5. **Outline a migration / validation path** — small, observable, reversible steps; what you would measure to know the design is working.
6. **List the open risks** — what could still go wrong, and what would catch it.

## 4. Component-level design checklist

For a new or refactored component:

- [ ] Single, nameable responsibility (SRP).
- [ ] Inputs and outputs are explicit (typed contracts at the boundary).
- [ ] Dependencies point inward toward the domain (DIP).
- [ ] No leaky abstractions — callers do not need to know the implementation to use it correctly.
- [ ] Failure modes documented (what it throws, when it retries, when it gives up).
- [ ] Idempotency story for any side effect.
- [ ] Observable: logs/metrics/traces at the boundary.
- [ ] Testable in isolation without standing up infrastructure.

## 5. Output template

Use this structure for every non-trivial design proposal:

```
# Design: <title>

## Problem
<one-paragraph problem statement>

## Forces & constraints
- Functional: ...
- Non-functional: ...
- Constraints: ...

## Options considered
### Option A — <name>
- Sketch: ...
- Pros: ...
- Cons: ...
- Reversibility: low / medium / high

### Option B — <name>
- Sketch: ...
- Pros: ...
- Cons: ...
- Reversibility: low / medium / high

(Add Option C if a third is genuinely viable.)

## Recommendation
<chosen option> — because <principle(s) and forces that drove the choice>.

## Migration / validation plan
1. Step 1 (small, observable, reversible)
2. Step 2 ...
3. How we will know it is working: <metrics / behavior>

## Open risks
- Risk 1 — mitigation
- Risk 2 — mitigation
```

## 6. Anti-patterns to call out

- Premature microservice extraction.
- Anemic domain model (entities are bags of getters/setters; logic lives in "service" classes by default).
- Distributed monolith (services that must be deployed together).
- Big design up front for a problem that is still being discovered.
- Pattern-driven design — choosing a pattern first, then forcing the problem to fit.
- Cross-cutting "util" / "helper" / "common" packages that accumulate unrelated code.
- Sync chains across services where one async event would do (and vice versa).

## 7. When to stop designing and start prototyping

If two options look equally good after honest analysis, the cheapest experiment that distinguishes them beats more analysis. Recommend a small spike with explicit success criteria.
