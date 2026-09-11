# Contributing

`cloud-itonami-isco-3351` accepts contributions to the OSS actor, policy tests,
documentation, examples and open occupation blueprint.

## Development

```bash
kbb -M:dev:test
kbb -M:lint
```

Keep changes small and include tests for policy, audit, store or disclosure
behavior.

## Rules

- Do not commit real inspector, traveler or facility data, credentials or
  operating documents.
- Keep production writes and disclosures behind Customs Inspection Governor.
- **Never add an op, code path or rationale template that authorizes a
  search, orders a seizure, orders a detention, denies entry, or issues a
  citation/penalty.** Those actions are structurally out of scope for this
  actor (see `src/customsinspection/governor.cljk`'s namespace docstring),
  not merely gated — a PR proposing to add such an op, even behind an
  escalation gate, will be rejected regardless of how it is framed.
- Treat this occupation's workflows as high-risk: add tests for permission,
  purpose, safety and audit logging.
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests

PRs should describe:

- what behavior changed
- which policy invariant is affected
- how it was tested
- whether operator or certification docs need updates
