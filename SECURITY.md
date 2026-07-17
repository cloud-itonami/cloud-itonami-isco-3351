# Security Policy

This project handles customs and border inspection documentation workflows.
Treat vulnerabilities as potentially high impact even when the demo data is
synthetic — this occupation carries real government enforcement authority in
the physical world, and this actor's entire design premise is that none of
that authority is reachable through this codebase.

## Do Not Disclose Publicly

Report privately before opening public issues for:

- credential exposure
- real inspector, traveler or facility data exposure
- authorization bypass
- Customs Inspection Governor bypass
- audit-ledger tampering
- over-disclosure in reports or exports
- unsafe robot action dispatch
- **any path by which a proposal, op or rationale could be interpreted as
  authorizing a search, ordering a seizure, ordering a detention, denying
  entry, or issuing a citation/penalty** — report this as a critical-severity
  design flaw even if no exploit is demonstrated, since the entire actor's
  safety case rests on that surface being structurally empty

## Reporting

Use GitHub private vulnerability reporting when available for the repository.
If that is unavailable, contact the repository maintainers through the
gftdcojp organization before publishing details.

Include:

- affected commit or version
- reproduction steps
- expected and actual behavior
- impact on inspector/traveler/facility data, policy enforcement or audit
  logging
- suggested fix, if known

## Production Guidance

- Store secrets outside Git.
- Keep real inspector/traveler/facility data outside this repository.
- Run policy tests before deployment.
- Export and review audit logs regularly.
- Use least privilege for operators and service accounts.
