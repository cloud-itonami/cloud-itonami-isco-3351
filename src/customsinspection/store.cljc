(ns customsinspection.store
  "SSoT for the ISCO-08 3351 customs and border inspection documentation
  support actor (itonami actor pattern, ADR-2607011000 / CLAUDE.md
  Actors section; README's 'Robotics premise' — a documentation and
  logistics-coordination robot performs inspection-log data entry,
  lane-staffing scheduling and equipment-procurement paperwork under
  this advisor/governor pair, which never dispatches hardware itself
  and never exercises, simulates exercising, or proposes exercising
  ANY search, seizure, detention or entry-denial authority — that
  authority is not merely gated, it is structurally absent from this
  actor's op-allowlist; see customsinspection.governor). Modeled on
  cloud-itonami-isco-3313's accountingsupport.store.

  Domain:

    inspector — a registered, independently-verified customs/border
                inspector (:inspector-id :name). Analogous to
                accountingsupport's `client` — provenance must be
                established before any proposal for this inspector can
                be considered.
    facility  — a registered inspection facility/lane
                {:facility-id :inspector-id :name
                 :max-supply-order-cost number}. `:max-supply-order-cost`
                is the registered threshold a proposed
                `:coordinate-supply-order` cost must not exceed without
                escalating to human sign-off (NOT a hard block —
                procurement above threshold is legitimate, routine
                administrative work that simply requires a human
                customs officer's approval, unlike this actor's
                complete lack of any op resembling enforcement
                action).
    record    — a committed operating record (a logged inspection
                entry, a lane-staffing schedule, a flagged concern, or
                a supply order) — written ONLY via commit-record!.
                Never a search result, seizure record, detention
                record or entry-denial record — no such record type
                exists because no such op exists.
    ledger    — append-only audit trail, commit or hold."
  )

(defprotocol Store
  (inspector [s inspector-id])
  (facility [s facility-id])
  (records-of [s inspector-id])
  (ledger [s])
  (register-inspector! [s inspector])
  (register-facility! [s f])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (inspector [_ inspector-id] (get-in @a [:inspectors inspector-id]))
  (facility [_ facility-id] (get-in @a [:facilities facility-id]))
  (records-of [_ inspector-id] (filter #(= inspector-id (:inspector-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-inspector! [s insp]
    (swap! a assoc-in [:inspectors (:inspector-id insp)] insp) s)
  (register-facility! [s f]
    (swap! a assoc-in [:facilities (:facility-id f)] f) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:inspectors {} :facilities {} :records [] :ledger []}
                                   seed)))))
