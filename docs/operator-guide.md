# Operator Guide

## First Deployment

1. Define the inspection facility's service area and intake process.
2. Register each inspector and facility (provenance basis for every proposal).
3. Run synthetic operating cases.
4. Enable human-reviewed sign-off for flagged concerns and above-threshold
   supply orders.
5. Measure operating outcomes and audit coverage.

## Minimum Production Controls

- provenance for all operating records (registered inspector + facility)
- source-document basis for every logged inspection record
- always-escalate path for flagged inspection concerns
- human review for above-threshold supply orders
- audit export for all gated actions

## What This Actor Never Does

This actor and its operators must never configure, extend or fork it to:

- authorize a search
- order a seizure
- order a detention
- deny entry
- issue a citation or penalty

These are real customs and border inspectors' legal enforcement authorities.
This actor is a documentation/logistics-coordination robot only — any
observation suggesting one of the above may be warranted is surfaced solely
via `:flag-inspection-concern`, which always escalates immediately to a human
customs officer for review and action. The robot never acts on it.

## Certification

Certified operators must prove that the governor gates every proposal, that
flagged concerns and above-threshold supply orders escalate to humans, and
that no fork has reintroduced an op or rationale path resembling search,
seizure, detention, entry-denial or citation/penalty authority.
