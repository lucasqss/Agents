---
name: model-based-testing
description: Abstract body of knowledge for software test design across all levels — unit, integration, system, acceptance — and both black-box and white-box techniques. Grounded in Myers (The Art of Software Testing) for testing psychology, axioms, integration strategies, and higher-order test types; and in Ammann & Offutt (Introduction to Software Testing) for the Fault/Error/Failure RIPR model and the four coverage-criteria families -> Graph (control & data flow), Logic (predicate / clause / ACC / CACC ≈ MC/DC), Input Space Partitioning (characteristics, blocks, each-choice, base-choice, pairwise, t-wise), and Syntax-Based (grammar / mutation testing). Use when designing a test plan, deriving test cases from requirements or an API surface, choosing or comparing coverage criteria (subsumption), reasoning about RIPR gaps, identifying edge cases, planning black-box end-to-end system tests, applying equivalence partitioning, boundary value analysis, decision tables, state-transition, pairwise, model-based testing, mutation testing, risk-based prioritization, or defining entry/exit criteria. Domain- and framework-agnostic.
---

# Model-Based, Black-Box & White-Box Testing

A reusable, domain-agnostic toolkit for designing principled test plans and test cases. Built on two canonical references:

- **Myers, *The Art of Software Testing*** — testing psychology, axioms, integration strategies, higher-order (system) test types, inspections.
- **Ammann & Offutt, *Introduction to Software Testing*** — the Fault/Error/Failure (RIPR) model, the four coverage-criteria families, and the subsumption hierarchy that lets you compare them.

Apply this skill whenever ad-hoc "click around and see" testing is not enough — i.e., whenever the cost of a missed defect exceeds the cost of deriving cases systematically.

## 0. Foundations

### 0.1 Definition and mindset (Myers)
- **Testing is the process of executing a program with the intent of finding errors.** A "successful" test is one that *finds a previously undetected fault* — not one that passes.
- Confirmation bias is the dominant testing failure mode. If you design tests to show the program works, you will. Design tests to break it.
- Developers should not be the sole testers of their own code. Fresh eyes (peer review, separate tester, or — in our context — a distinct analytic pass) materially raise defect yield.

### 0.2 Myers's axioms (apply to every test plan)
1. A necessary part of every test case is **a definition of the expected output or result** — i.e., an explicit oracle.
2. A programmer should avoid attempting to test their own program; an organization should avoid testing its own work as the sole verification.
3. Inspect the results of each test **thoroughly**. Tests that "pass" but produce wrong intermediate state still failed.
4. Test cases must cover both **valid and unexpected/invalid** input conditions.
5. Examine whether the program does what it should **and** that it does **not** do what it should not.
6. Avoid throwaway test cases — capture them as regression assets.
7. Do **not** plan a testing effort under the tacit assumption that no errors will be found.
8. The probability of more errors in a section is **proportional** to the number already found there (defect clustering).
9. Testing is a **creative, intellectual** activity — at least as much as design.
10. The economics of testing rest on the impossibility of exhaustive testing; therefore choose criteria explicitly and justify them.

### 0.3 Fault, Error, Failure and the RIPR model (Offutt)
- **Fault** — a static defect in code/spec/design.
- **Error** — an incorrect internal state produced when a fault is executed.
- **Failure** — externally observable wrong behavior produced when an error propagates.

For a test to **reveal** a fault, four conditions (**RIPR**) must hold:
- **R**eachability — execution reaches the faulty location.
- **I**nfection — execution at that location produces an incorrect state.
- **P**ropagation — the incorrect state propagates to observable output.
- **R**evealability — the test's assertions (oracle) actually check the propagated output.

Use RIPR diagnostically: when a defect slipped past tests, identify which of R/I/P/R failed. The next test belongs at that link.

### 0.4 Test-design abstraction levels (Offutt)
1. **Test scripts** — concrete steps and data (the artifact a runner executes).
2. **Test automation** — harnesses, fixtures, runners.
3. **Test design** — *how* test values are chosen (the techniques in this skill).
4. **Test requirements** — *what* must be tested (coverage obligations derived from a criterion).

Most teams operate at levels 1–2 and skip 3–4. Real leverage is at 3 and 4 — that is where this skill lives.

## 1. Establish the test basis

Before designing cases, identify what you are testing **against**:

- **Test basis**: requirements, acceptance criteria, API specs, contracts, models, prior defects, regulations.
- **Test object**: the unit / component / system under test (SUT).
- **Test perspective**: black-box (specification-based, opaque internals) vs. white-box (structure-based, source visible) vs. experience-based (error guessing, exploratory).
- **Test level**: unit, integration, system, acceptance (and sub-levels: contract, component, end-to-end).
- **Test type**: functional, performance, security, usability, reliability, recovery, regression, compatibility, accessibility, localization.

If the test basis is missing or weak, surface it. **You cannot design a test for an unstated expectation.**

## 2. Test levels and what each is for

| Level | Scope | What it catches | What it does not |
|-------|-------|-----------------|------------------|
| Unit | One class / function | Logic errors, wrong algorithms | Integration mistakes |
| Integration | Two or more units / a component with its IO | Wrong contracts, wiring, serialization | End-to-end behavior |
| System | The whole application as a black box | End-to-end behavior, NFRs, real config | Internal regressions in detail |
| Acceptance | The system against stakeholder needs | "Did we build the right thing?" | "Did we build it right?" |

Choose the **lowest level that can give you confidence**. Push tests down the pyramid when possible (faster, cheaper, more deterministic); keep system-level tests for behavior that *only* emerges end-to-end.

## 3. Black-box techniques (specification-based)

You see only inputs, outputs, and observable side effects. Internals are opaque. These techniques apply at every level — unit through system. Most of them are special cases of **Input Space Partitioning (ISP)** in Offutt's framework — keep the ISP vocabulary in mind because it lets you reason about coverage formally rather than by gut feel.

### 3.0 Input Space Partitioning (ISP) — the umbrella (Offutt)

ISP gives you a disciplined way to derive black-box tests from any input space:

1. **Identify characteristics of the input domain** — independent attributes of an input that may affect behavior (e.g., for a string: length, alphabet, presence of whitespace, encoding).
2. **Partition each characteristic into blocks** — disjoint, exhaustive equivalence classes (e.g., length: 0 / 1 / 2..max-1 / max / >max).
3. **Choose a combinatorial coverage criterion across characteristics:**
    - **All Combinations (AC)** — every combination of one block from every characteristic. Strongest, usually infeasible.
    - **Each Choice (EC)** — every block from every characteristic appears in at least one test. Weakest useful criterion.
    - **Base Choice (BC)** — pick a base value for each characteristic; one test for the all-base combination, then one test per non-base block while holding others at base. Good fault-cost trade-off.
    - **Pair-Wise (PW)** / **T-Wise** — every pair (or t-tuple) of blocks from any two (or t) characteristics appears together.

Decide and **declare which ISP criterion you are using** in the test plan. The techniques below (EP, BVA, decision tables, pairwise) are concrete instances or specializations of ISP.

### 3.1 Equivalence Partitioning (EP)
Partition each input domain into classes where the system is expected to behave the same way. Pick **one representative per class** — valid and invalid.

Example (age field, valid 18–120):
- Invalid low: `-1, 0, 17`
- Valid: `18..120`
- Invalid high: `121, 999`

### 3.2 Boundary Value Analysis (BVA)
Defects cluster at boundaries. For each boundary, test:
- Just below, on, and just above (3-point); or
- 2-point (on, just over) for cheaper coverage.

For the age example: `17, 18, 19` and `119, 120, 121`.

### 3.3 Decision tables
Use when outputs depend on **combinations of conditions**. Build a table with one column per rule:

| Condition | R1 | R2 | R3 | R4 |
|-----------|----|----|----|----|
| C1 (T/F)  | T  | T  | F  | F  |
| C2 (T/F)  | T  | F  | T  | F  |
| Action    | A1 | A2 | A3 | A4 |

Reveals missing rules, contradictions, and untestable combinations.

### 3.4 State-transition testing
Use when the SUT behaves differently depending on **state**. Build the state machine:
- States, events, transitions, guards, actions.
- Coverage criteria: all states, all transitions, all transition pairs (N+ switch), all paths (rarely feasible).
- Don't forget: invalid events in each state, self-transitions, terminal states.

### 3.5 Cause-effect graphs
For complex logical dependencies that decision tables make unwieldy. Map causes → intermediate nodes → effects with AND/OR/NOT, then derive decision table from the graph.

### 3.6 Pairwise / combinatorial (orthogonal arrays)
When inputs are many independent parameters, full Cartesian coverage explodes. **Pairwise** covers every pair of values across parameters with far fewer cases — empirically catches most combinatorial bugs.

### 3.7 Use-case / scenario testing
Derive cases from end-to-end user flows: main flow, alternative flows, exception flows. Especially valuable at the system level.

### 3.8 Syntax testing
For inputs defined by a grammar (URLs, query languages, file formats): generate valid and invalid forms from the grammar.

### 3.9 Error guessing & exploratory
Experience-based. Maintain a checklist of historically high-yield areas: nulls, empties, zeros, negatives, max/min, off-by-one, Unicode, timezones, daylight saving, leap years, concurrent access, retries, timeouts, partial failures, large payloads, malformed input.

## 4. White-box techniques — Graph & Logic coverage (Offutt)

You see the source. Offutt organizes structural criteria into two formal families that subsume the older "statement / branch / MC/DC" vocabulary.

### 4.1 Graph Coverage

Model the program as a graph (typically the **Control-Flow Graph (CFG)**: nodes = basic blocks, edges = possible transfers; or the **Call Graph**, or a state model). Coverage obligations are then defined on the graph and apply uniformly whether the graph comes from code, design, or specification.

**Control-flow criteria, ordered from weakest to strongest:**

| Criterion | Obligation | Subsumes |
|-----------|------------|----------|
| **Node Coverage (NC)** | Every node executed | — |
| **Edge Coverage (EC)** | Every edge traversed | NC |
| **Edge-Pair Coverage (EPC)** | Every pair of consecutive edges traversed | EC |
| **Complete Path Coverage (CPC)** | Every path traversed | EPC (infeasible with loops) |
| **Prime Path Coverage (PPC)** | Every simple path that is not a proper sub-path of another | EC, plus realistic loop coverage |
| **Simple Round Trip / Complete Round Trip** | Loop-targeting criteria — at least one / every prime path that forms a cycle | partial coverage of looping behavior |

NC ≈ statement coverage. EC ≈ branch/decision coverage. PPC is the practical strong criterion when CPC is infeasible.

**Data-Flow criteria** (on the same graph annotated with definitions and uses of each variable):

- **All-Defs** — for each `def`, at least one def-clear path to *some* `use`.
- **All-Uses** — for each (def, use) pair, at least one def-clear path between them.
- **All-DU-Paths** — every def-clear simple path from each `def` to each `use`.

Use data-flow coverage when defects cluster around state mutation (long-lived variables, accumulators, caches, transactional state).

### 4.2 Logic Coverage (predicates & clauses)

Predicates are boolean expressions controlling flow (`if`, `while`, `assert`, guards). A predicate is a tree of **clauses** (atomic boolean sub-expressions). Logic criteria, weakest to strongest, in roughly the order you'd consider them:

| Criterion | Obligation | Subsumes |
|-----------|------------|----------|
| **Predicate Coverage (PC)** | Every predicate evaluates to true and to false | EC of the surrounding edges |
| **Clause Coverage (CC)** | Every clause evaluates to true and to false (independently) | — (does **not** subsume PC) |
| **Combinatorial Coverage (CoC)** | Every combination of clause truth values | PC, CC, but typically infeasible |
| **Active Clause Coverage (ACC)** | Each clause `c` made to determine the predicate's value, evaluated both ways | PC |
| **General ACC (GACC)** | Other clauses free to vary | weakest ACC |
| **Correlated ACC (CACC)** | Other clauses must make the predicate take both values | GACC; **CACC ≡ MC/DC** (DO-178C) |
| **Restricted ACC (RACC)** | Other clauses must hold the same values across the pair | CACC; harder to satisfy with short-circuit eval |
| **Inactive Clause Coverage (ICC)** | Each clause `c` made to *not* determine the value, evaluated both ways | catches inverted-logic faults that ACC can miss |

**CACC = MC/DC**, which is the safety-critical industry standard. Choose CACC when the cost of a logic-fault failure is high (avionics, medical, finance core, security checks); choose PC or EC otherwise.

### 4.3 Subsumption hierarchy (use to compare criteria)

A criterion **C1 subsumes C2** if every test set satisfying C1 also satisfies C2 — i.e., C1 is strictly stronger. Practical chains worth remembering:

- **Graph (control flow):** `Complete Path ⟶ Prime Path ⟶ Edge-Pair ⟶ Edge ⟶ Node`
- **Graph (data flow):** `All-DU-Paths ⟶ All-Uses ⟶ All-Defs`
- **Logic:** `Combinatorial ⟶ RACC ⟶ CACC (≡ MC/DC) ⟶ GACC ⟶ Predicate`; `Clause` is *not* in this chain (does not subsume PC).
- **Cross-family:** Mutation Coverage (see §5A) empirically subsumes most graph/logic criteria for unit-level fault detection.

When you pick a criterion, **state which weaker criteria it subsumes** (so reviewers know what you are also getting) and **which stronger ones it does not** (so they know what you are not).

### 4.4 Loops, infeasibility, oracles

- **Loop coverage** — exercise loops with zero, one, many, and boundary iterations; combine with PPC for realistic strength.
- **Infeasibility is normal.** Strong criteria produce obligations that no input can satisfy (dead code, contradictory conditions). Treat infeasibility as a finding: either it's dead code (delete) or the criterion is over-specified (relax to next-weaker). Document the decision.
- **Coverage is a floor, not a ceiling.** 100% branch coverage with weak oracles catches nothing — see Myers axiom 1 (expected output) and Offutt's RIPR (revealability).

## 5. Model-Based Testing (MBT)

You build an explicit **model of expected behavior** (FSM, extended FSM, statechart, decision model, Markov chain, BPMN-like flow) and derive test cases mechanically from the model.

When MBT pays off:
- Stateful protocols and workflows.
- High-regression-cost systems.
- Combinatorial state spaces that defeat hand-written cases.

Workflow:
1. **Identify the model** to use (FSM is the common default).
2. **Build the model** from the spec — states, events, guards, expected outputs.
3. **Choose coverage criteria** — state, transition, transition-pair, path.
4. **Generate test cases** mechanically (walk the model, longest-paths, random walks with oracles).
5. **Define the oracle** — how each generated case decides pass/fail.
6. **Maintain the model**, not the cases — that is the leverage of MBT.

## 5A. Syntax-Based Testing & Mutation (Offutt's fourth family)

Syntax-based criteria use a **grammar or syntactic structure** as the source of test obligations. Two flavors matter:

### 5A.1 Grammar-based input testing
Given a grammar describing valid input (BNF, JSON schema, OpenAPI spec, URL grammar, query language):
- **Valid strings** — derive cases that exercise every production at least once (production coverage).
- **Mutated / invalid strings** — apply mutation operators on the grammar (drop terminal, swap non-terminal, repeat production, violate constraint) and verify the SUT *rejects* each. Pairs naturally with Myers's axiom 4 (test invalid inputs explicitly).

### 5A.2 Mutation testing of the program (the gold standard for unit-level fault detection)
Apply small syntactic mutations to the source under test (the **mutation operators** — e.g., relational operator replacement `<` → `≤`, arithmetic operator replacement, statement deletion, constant replacement, negate decision). Each mutated program is a **mutant**.

- A test set **kills** a mutant when at least one test produces a different observable result on the mutant than on the original.
- **Mutation score** = killed mutants / (total mutants − equivalent mutants), where an **equivalent mutant** is syntactically different but semantically identical (no test can distinguish it).

Mutation operationalizes RIPR: to kill a mutant, a test must Reach the mutated location, Infect program state, Propagate to output, and have an oracle that Reveals it. A high mutation score is therefore strong evidence that the test suite actually detects faults — far stronger than line/branch coverage.

**When to use:** unit and integration suites for code where defect cost is high; treat ≥ 75–80% mutation score (excluding equivalent mutants) as a meaningful exit bar.

## 5B. Integration testing strategies (Myers)

When assembling components, **how** you integrate determines which faults you find when, and how expensive isolation is.

| Strategy | What you do | Strengths | Weaknesses |
|----------|-------------|-----------|------------|
| **Big-bang** | Integrate everything at once, then test | No driver/stub work | Faults are hard to localize; late discovery |
| **Top-down** | Integrate top of call tree first, stub lower modules; descend | Early demo of high-level behavior; control flow exercised early | Lower modules tested late; many stubs |
| **Bottom-up** | Integrate leaves first with drivers; ascend | Lower modules thoroughly tested; less stubbing | High-level integration deferred; no working program until late |
| **Sandwich / hybrid** | Top-down for control layer + bottom-up for data layer, meeting in the middle | Balances trade-offs | Higher coordination cost |
| **Risk- / change-driven** | Integrate around the riskiest or most recently changed seams first | Focuses effort where defects cluster (axiom 8) | Requires good risk model |
| **Contract-based** *(modern)* | Each pair tested against a shared contract (consumer-driven contracts), so full integration is a smoke check | Decouples teams; fast feedback | Contracts must be honest; drift kills value |

Always state your integration strategy in the test plan; "we'll just run the e2e suite" is a big-bang strategy in disguise.

## 5C. Higher-Order / system test types (Myers)

System-level testing is not a single activity. Myers enumerates higher-order test types; pick the ones that match the SUT's risk profile and design cases for each explicitly:

- **Facility / capability testing** — every feature in the requirements exists and works.
- **Volume testing** — large amounts of data (large files, long lists, big payloads).
- **Stress testing** — load near or beyond design limits (peak concurrency, burst rates).
- **Performance testing** — meets latency/throughput targets under stated load.
- **Usability testing** — user can complete tasks with acceptable effort/error rate.
- **Security testing** — authn, authz, input sanitization, secrets handling, abuse cases.
- **Storage testing** — disk/memory footprints, growth, cleanup.
- **Configuration testing** — across supported environments, OSes, browsers, dependency versions.
- **Compatibility / conversion testing** — data migrations, protocol upgrades.
- **Installability / deploy testing** — fresh install, upgrade, rollback, partial install recovery.
- **Reliability testing** — sustained operation; MTBF if applicable.
- **Recovery testing** — induced failures (kill process, drop dependency, fill disk) and verified recovery within target.
- **Serviceability / operability testing** — logs, metrics, traces, runbooks, supportability.
- **Documentation testing** — published docs actually match behavior.
- **Procedure testing** — operator runbooks executable as written.

For each type the SUT needs, define explicit entry criteria, expected outputs (Myers axiom 1), and exit criteria — don't fold them into "QA will explore around it".

## 5D. Static techniques — Inspections, walkthroughs, desk checking (Myers)

Dynamic execution is not the only way to find faults. Static techniques routinely catch faults earlier and cheaper:

- **Code inspections (Fagan-style)** — a small group reviews a small artifact against a checklist (initialization, boundary, type, concurrency, error handling, naming). Moderator + reader + author + recorder roles; defects logged, not solved in the room.
- **Walkthroughs** — author leads peers through the artifact; lighter than inspection, useful for knowledge transfer.
- **Desk checking** — solo, structured re-reading of one's own code with a checklist; weakest because of self-bias (Myers axiom 2).
- **Specification / design review** — same techniques applied to requirements and design before code exists; cheapest place to remove faults.

Make at least one static technique part of every plan whose risk tier justifies it; pair it with dynamic testing rather than substituting one for the other.

## 6. System-level black-box testing (whole application)

Treat the SUT as a sealed box. Inputs are public APIs, user interfaces, scheduled triggers, and external events. Outputs are responses, persisted state visible via APIs, emitted messages, files, logs (when documented), and side effects visible to other systems.

Sources for test derivation:
- Requirements and acceptance criteria.
- Public API specs (OpenAPI, AsyncAPI, contracts).
- UI flows / use cases.
- SLA / NFR targets.

Add scenarios that target whole-system properties:
- **End-to-end happy paths** for each headline capability.
- **Cross-feature interactions** — feature A's output is feature B's input.
- **Recovery & resilience** — dependency outage, slow dependency, restart mid-flow, duplicated event, out-of-order event, message loss, partial write.
- **Concurrency** — two clients doing the same thing simultaneously; race on the same resource.
- **NFR slices** — latency, throughput, error budget, security (authn/authz boundaries), data retention.
- **Backward compatibility** — old client / new server, new client / old server.
- **Data lifecycle** — create → read → update → delete → re-create with same key.

Tooling perspective (mention only; choose per project): contract tests, integration test harnesses, end-to-end suites in CI, synthetic monitoring in production.

## 7. Risk-based prioritization

You cannot test everything. Rank candidate areas by **risk = likelihood × impact**.

Likelihood inputs: code churn, complexity, defect history, novelty, team familiarity.
Impact inputs: revenue effect, user reach, data loss, safety, compliance, reputational.

Tier them:
- **R1 — must cover before release** (high likelihood × high impact).
- **R2 — should cover** (one high, one medium).
- **R3 — nice to cover** (low/low).

Make the ranking explicit in the test plan — reviewers can challenge it.

## 8. Entry and exit criteria

**Entry criteria** (what must be true before testing starts):
- Test basis is stable enough.
- SUT is buildable and deployable in the target environment.
- Test data and test environment are available.
- Required dependencies (real or stubbed) are reachable.

**Exit criteria** (what must be true to call testing done):
- Planned test cases executed.
- Defect density and severity below agreed thresholds.
- All R1 risks have at least one passing test.
- Coverage targets met (state, branch, requirement, whichever applies).
- Known defects have status (fix / defer / accept) — none in limbo.

State both up front; don't negotiate them after results are in.

## 9. Defect taxonomy (for reporting & analysis)

Classify each defect: **severity** (effect on users) × **priority** (urgency to fix) × **type** (functional, performance, security, usability, data, compatibility, regression) × **phase introduced** (requirements, design, code, integration, deployment).

Track trends, not just counts — *where* defects come from drives *where* to invest next round.

## 10. Output template — test plan

```
# Test Plan: <feature or release>

## 1. Scope
- In scope:
- Out of scope:

## 2. Test basis
- Requirements: ...
- Specs / contracts: ...
- Models: ...

## 3. Test levels & types
- Unit: <yes/no, scope>
- Integration: ...
- System (black-box): ...
- Acceptance: ...
- Non-functional types: performance / security / ...

## 4. Techniques and coverage criteria chosen
- Black-box (ISP): which characteristics, which blocks, which combinatorial criterion (AC / EC / BC / PW / t-wise) — and why.
- Black-box specializations: EP & BVA on inputs X, Y; decision table on rule set R; state-transition on workflow W; pairwise on configuration matrix C — and why each.
- White-box graph: which control-flow criterion (Node / Edge / Edge-Pair / Prime Path) and which data-flow criterion (All-Defs / All-Uses / All-DU-Paths).
- White-box logic: PC / CACC (≡ MC/DC) / RACC / ICC — and the target % of obligations.
- Syntax-based: grammar-based input testing scope; mutation testing scope and target mutation score (excluding equivalent mutants).
- For each chosen criterion: name what it subsumes and what it does **not** subsume — so reviewers know your floor and your ceiling.

## 5. Risk-based prioritization
| Area | Likelihood | Impact | Tier |
|------|------------|--------|------|

## 6. Test environment & data
- Environment: ...
- Data requirements: ...
- Dependencies (real / stub / mock): ...

## 7. Entry criteria
- ...

## 8. Exit criteria
- ...

## 9. Regression strategy
- What stays in the regression set after this release and why.

## 10. Open risks & assumptions
- ...
```

## 11. Output template — test-case table

| ID | Level | Type | Technique | Coverage obligation | Pre-conditions | Steps / inputs | Expected result (oracle) | Requirement | Risk tier |
|----|-------|------|-----------|---------------------|----------------|----------------|--------------------------|-------------|-----------|
| T1 | System | Functional | EP+BVA (ISP / BC) | Block "age = 17" | User exists | POST /enroll {age:17} | 422 with code AGE_TOO_LOW; no row inserted | R1 | R1 |
| T2 | Unit | Functional | Mutation | Kill ROR `<` → `≤` at `Pricing.discount` | — | discount(100, 0.10) | 90.0 | R3 | R2 |
| T3 | Unit | Functional | CACC (MC/DC) | Predicate `(a ∧ b) ∨ c` — `c` active | — | a=F, b=F, c=T | true | R4 | R2 |

Every test case must trace to **(a)** a technique, **(b)** a coverage obligation or risk, **(c)** a requirement, and **(d)** an explicit oracle (Myers axiom 1). Cases without all four are noise.

## 12. Anti-patterns to call out

- **Test cases without a basis** — "I just thought of this" → fine for exploration, not for a plan.
- **All-system, no-unit** — slow, flaky, expensive feedback loop.
- **All-unit, no-system** — green unit tests, broken integration.
- **Snapshot-only assertions** — they pin behavior without specifying intent.
- **Mock-heavy unit tests that pass while integration fails** — push behavior into integration when mocks would diverge from reality.
- **Coverage as a goal** — 100% line coverage with weak asserts catches nothing.
- **Flaky tests left in the suite** — they teach the team to ignore failures.
- **One giant scenario per feature** — when it fails, you don't know *which* step.
- **No oracle** — a test that runs without checking anything is worse than no test (false confidence).
- **Tests that only the author can run** — environment, data, or credentials hidden in someone's head.
- **Confirmation-bias suites** — tests written to demonstrate the program works rather than to break it (violates Myers's core mindset).
- **Coverage without a stated criterion** — "we have tests" is not a criterion; "we satisfy CACC on the pricing predicates and All-Uses on the cart state" is.
- **Ignoring equivalent mutants** — counting them against your mutation score deflates it dishonestly; flag them and exclude.

## 13. References

This skill distills two canonical sources. When a decision is contested, defer to them:

- **Glenford J. Myers, Tom Badgett, Corey Sandler — *The Art of Software Testing* (3rd ed., Wiley, 2011).** Source for: testing definition and psychology, the ten axioms, module/integration strategies, higher-order system test types, inspections and walkthroughs.
- **Paul Ammann & Jeff Offutt — *Introduction to Software Testing* (2nd ed., Cambridge, 2017).** Source for: the Fault/Error/Failure (RIPR) model, the four coverage-criteria families (Graph, Logic, Input Space Partitioning, Syntax-Based), the subsumption hierarchy, mutation testing, and the test-design abstraction levels.