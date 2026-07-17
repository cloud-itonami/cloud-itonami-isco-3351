# Business Model: Customs and Border Inspection Documentation Support

## Classification

- Repository: `cloud-itonami-isco-3351`
- ISCO-08: `3351`
- Occupation: Customs and Border Inspectors
- Social impact: trade-facilitation, border-integrity, public-transparency

## Customer

- customs and border inspection facilities
- inspection-lane operators

## Offer

- inspection-record documentation (declaration/manifest/inspection-log entry)
- inspection-lane staffing and queue-management scheduling
- inspection-equipment procurement coordination
- always-escalating concern flagging for human customs-officer review

## Scope Exclusion (not an offer)

- search, seizure, detention or entry-denial authority — this actor never
  performs, simulates performing, or proposes performing any of these; they
  are structurally absent from the op-allowlist, not a withheld feature.
- citation/penalty issuance — same structural exclusion.

## Revenue

- monthly facility retainer
- per-record documentation fee

## Trust Controls

- no inspection record logged without an attached source document
  (declaration/manifest/inspection-log basis)
- no supply order above the facility's registered cost threshold without
  governor escalation to human sign-off
- every flagged inspection concern always escalates immediately to a human
  customs officer and is never auto-commit-eligible
- documentation and scheduling records are auditable, not editable
- no op, code path, or approved proposal can ever authorize a search, order a
  seizure, order a detention, deny entry, or issue a citation/penalty
