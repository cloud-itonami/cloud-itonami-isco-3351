# Governance

`cloud-itonami-isco-3351` is an OSS open-occupation blueprint. Governance covers
both code and the operator model.

## Maintainers

Maintainers may merge changes that preserve these invariants:

- the Advisor cannot directly dispatch robot actions or disclose records.
- Customs Inspection Governor remains independent of the advisor.
- hard policy violations cannot be overridden by human approval.
- the op-allowlist never gains an op resembling authorizing a search,
  ordering a seizure, ordering a detention, denying entry, or issuing a
  citation/penalty — this is a structural exclusion, not a policy that can
  be relaxed by maintainer discretion.
- every commit, hold and approval path is auditable.
- real inspector/traveler/facility data stays outside Git.

## Decision Records

Architecture decisions live in `docs/adr/`. Changes to the trust model,
storage contract, public business model, operator certification, license, or
(especially) the op-allowlist's scope should add or update an ADR.

## Operator Governance

Anyone may fork and operate independently. itonami.cloud certification is a
separate trust mark and should require security, audit, support and data-flow
review.

Certified operators can lose certification for:

- bypassing policy checks
- mishandling inspector/traveler/facility data
- misrepresenting certification status
- failing to respond to security incidents
- hiding material changes to customer-facing operation
- attempting to extend this actor's scope into any search, seizure,
  detention, entry-denial or citation/penalty function
