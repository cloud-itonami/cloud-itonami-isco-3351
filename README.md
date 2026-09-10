# cloud-itonami-isco-3351

Open Occupation Blueprint for **ISCO-08 3351**: Customs and Border Inspectors.

This repository designs a forkable OSS business for customs-processing
documentation and logistics coordination: a documentation and
logistics-coordination robot manages inspection records, lane-staffing
schedules and equipment-procurement paperwork under a governor-gated
actor, so an inspection facility keeps its own operating records
instead of renting a closed customs-management SaaS.

**This actor has no search, seizure, detention or entry-denial
authority, structurally, not merely by policy gate.** Real customs and
border inspectors hold legal authority to search persons/property,
seize goods, detain individuals, deny entry and issue
citations/penalties. This actor is a documentation/logistics robot
ONLY. Its op-allowlist has exactly four ops — `:log-inspection-record`,
`:schedule-lane-operation`, `:flag-inspection-concern`,
`:coordinate-supply-order` — and no op resembling authorizing a
search, ordering a seizure, ordering a detention, denying entry, or
issuing a citation/penalty exists anywhere in the codebase. That
absence is enforced twice, independently: a closed op-allowlist that
hard-blocks any op outside the four above, and a defense-in-depth text
scan that hard-blocks any proposal whose rationale claims to finalize
or execute one of those actions. See
[`src/customsinspection/governor.kotoba`](src/customsinspection/governor.kotoba)'s
namespace docstring for the full structural argument. Any observation
that MAY warrant a search/seizure/detention/entry-denial is surfaced
ONLY via `:flag-inspection-concern`, which always escalates
immediately to a human customs officer and is never
auto-commit-eligible — the robot never acts on it itself, not even as
a `:propose`.

**Maturity: `:implemented`.** `src/customsinspection/` implements the
`CustomsInspectionActor` as a `langgraph.graph/state-graph`
(`customsinspection.actor`) wired to a `Customs Inspection Advisor`
(`customsinspection.advisor`) and an independent
`CustomsInspectionGovernor` (`customsinspection.governor`), following
the itonami actor pattern (ADR-2607011000): `:intake -> :advise ->
:govern -> :decide -+-> :commit (:ok?) +-> :request-approval
(:escalate?, human-in-the-loop interrupt) +-> :hold (:hard?)`.
HARD invariants (always hold, never overridable): inspector
provenance, no-actuation (`:effect` must be `:propose`), a closed
op-allowlist with no op resembling enforcement action, a
defense-in-depth text scan blocking any rationale that finalizes or
executes an enforcement action, a registered facility basis for any
facility-scoped proposal, and an attached source document before any
inspection record can be logged (logging a record without one is a
fabricated record, not documentation). Always-escalate ops (human
sign-off regardless of confidence, mapping this repo's Trust Controls
in [`docs/business-model.md`](docs/business-model.md)):
`:flag-inspection-concern` (always, never auto-commit-eligible) and
`:coordinate-supply-order` above the facility's registered
supply-order cost threshold.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a
**robot performs the physical/administrative domain work**. Here a
documentation and logistics-coordination robot performs
declaration/manifest/inspection-log data entry, lane-staffing
scheduling and equipment-procurement paperwork under an actor that
proposes actions and an independent **Customs Inspection Governor**
that gates them. The governor never dispatches hardware itself and
never authorizes a search, orders a seizure, orders a detention,
denies entry, or issues a citation/penalty — `:flag-inspection-concern`
actions always require human customs-officer sign-off, and no op
resembling those enforcement actions exists in the allowlist at all.

## Core Contract

```text
inspection facility request (declaration/manifest/staffing/procurement)
        |
        v
Customs Inspection Advisor -> Customs Inspection Governor -> log/schedule/order, or human sign-off
        |
        v
robot actions (gated, documentation/logistics only) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses,
suppress an operating record, or exercise any search, seizure,
detention or entry-denial authority — that authority does not exist in
this actor's vocabulary.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `3351`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
