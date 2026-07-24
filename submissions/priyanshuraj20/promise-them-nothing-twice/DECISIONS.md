# Decisions

## Conflict resolution

The central design challenge was resolving two conflicting business requirements: enforcing strict, auditable rate limits without customer-specific logic while ensuring Northwind's scheduled 02:00–04:00 UTC batch workload (800–1200 RPM) completed without HTTP 429 responses despite a contracted limit of 300 RPM.

I didn't treat this as choosing one stakeholder over the other. Reading both memos together pointed toward a configuration-based solution: exceptions are acceptable when they are expressed through policy and remain auditable, not when they are hidden inside application code. I therefore modelled Northwind's higher quota as a scheduled policy in configuration — 300 RPM normally and 1200 RPM during the batch window — rather than introducing a special code path.


The rate limiter itself remains completely unaware of Northwind or any other customer. It simply enforces the effective policy that has already been resolved for the current request. This separation keeps business policy independent from the enforcement mechanism, satisfying the CTO's requirement for a generic, auditable implementation while also meeting the Support team's operational requirement of avoiding HTTP 429 responses during the scheduled batch window.

What I rejected:
- Hardcoding `if (customerId == "northwind")` or any similar customer-specific logic.
- Keeping Northwind permanently at 300 RPM and treating the resulting failures as an operational problem instead of solving the underlying business requirement.


This also keeps the resolution fair to every other customer. Quotas are enforced independently per customer — Northwind's elevated batch-window allowance doesn't borrow capacity from a shared pool, so no other tenant is affected by it.
---

## Technical design

**Algorithm — Token Bucket.**

Northwind's workload is naturally bursty, especially at the start of the batch window. Token Bucket allows controlled bursts while still enforcing the configured long-term rate. I considered Sliding Window Counter because of its simpler boundary behaviour, but rejected it because it can unnecessarily reject legitimate burst traffic. Fixed Window was ruled out immediately because of the well-known boundary-doubling problem.

**Coordination — Redis with an atomic Lua script.**

The service runs as three stateless application nodes behind a load balancer. Any node-local counter would allow requests distributed across nodes to effectively exceed the intended global quota. Instead, every request evaluates against a shared token bucket stored in Redis.

Refill calculation, token validation, and token consumption all execute inside a single Lua script. Keeping those operations atomic removes read-then-write races and ensures consistent behaviour under concurrent requests from multiple application nodes.

**Fairness and isolation.**

Every customer maps to an independent Redis key, so one customer's traffic — bursty or not — can never consume another customer's tokens. This is what makes Northwind's elevated schedule fair rather than a special privilege at everyone else's expense: it changes Northwind's own ceiling, not anyone else's.

**Policy and enforcement remain separate.**

Customer tiers and scheduled quota policies live entirely in configuration. The Redis script and HTTP filter only receive the effective policy that has already been resolved. This keeps business policy independent from the enforcement mechanism and avoids embedding customer-specific knowledge into infrastructure code.

---

## Verification

The verification harness uses Java 21 virtual threads to generate concurrent requests against the running multi-node deployment. For each scenario it reports admitted requests, rejected requests, and node distribution, allowing the behaviour to be validated without inspecting the implementation.

One implementation detail is worth documenting because it affects repeatability. Token Bucket continuously refills over time. If a 300-request burst takes more than a second to transmit, the bucket legitimately regains a few tokens during the test and may admit slightly more than 300 requests. That is correct Token Bucket behaviour rather than a defect, but it makes wall-clock burst tests non-deterministic.

To obtain reproducible boundary proofs, the verification harness evaluates boundary scenarios using a controlled simulated timestamp. This produces deterministic results (for example, exactly 300 admitted and the 301st rejected) while leaving the production request path unchanged. Under normal production traffic, the expected refill-during-burst behaviour still applies.

The scheduled 1200 RPM Northwind policy required the same live verification rather than relying only on a unit test with a fixed `Instant`. I intentionally avoided trusting a client-supplied time override because that would allow clients to influence policy resolution themselves. Instead, the simulation mechanism is disabled by default, requires an additional shared verification token before activation, and emits a `WARN` log whenever it is used. The shared token is included in this public repository because it exists only to support demonstration and verification; the important design decision is the trust boundary itself: disabled by default, explicitly gated, and auditable.

Confirmed:
- 60 RPM boundary admits requests up to the configured limit and rejects the next request.
- 300 RPM boundary behaves identically.
- 1200 RPM scheduled policy is verified through the same live harness against the running multi-node deployment.
- Requests are distributed across all three application nodes by the load balancer.

What this submission does not attempt to prove:
- Redis failure behaviour.
- Sustained multi-hour load.
- Multi-region deployment.

---

## If I had four more hours

1. **Redis failure strategy.**
   Introduce a well-defined fail-closed mode, together with a conservative local emergency limit, instead of relying entirely on Redis availability.

2. **Dynamic policy reload.**
   Allow quota and schedule changes to be applied without restarting the service by distributing configuration updates through Redis Pub/Sub or a similar mechanism.

3. **Operational metrics.**
   Export admitted and rejected request counts per customer so Support can monitor scheduled batch windows without requiring engineering investigation.

4. **Redis high availability.**
   Replace the single Redis instance with Sentinel or Redis Cluster. The single-node deployment was a deliberate scope decision for the assignment rather than a production recommendation.