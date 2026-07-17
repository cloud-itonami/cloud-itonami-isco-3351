(ns customsinspection.governor
  "CustomsInspectionGovernor — the independent safety/traceability
  layer named in this repository's README/business-model.md, gating
  every documentation and logistics-coordination proposal an advisor
  may make for a customs/border inspection facility. The governor
  never dispatches hardware itself and never exercises, simulates
  exercising, or approves exercising ANY search, seizure, detention or
  entry-denial authority. Modeled on cloud-itonami-isco-3313's
  accountingsupport.governor.

  == Why this actor cannot become a search/seizure/detention/entry-denial
     authority (structural, not merely gated) ==

  Real customs and border inspectors hold legal authority to search
  persons/property, seize goods, detain individuals, deny entry and
  issue citations/penalties. This actor is a documentation/logistics
  robot ONLY — it helps process paperwork, log inspection records, and
  schedule inspection-lane staffing. It is deliberately built so that
  authority can never leak in through two independent layers:

    Layer 1 — closed op-allowlist (`op-allowlist`, rule :unknown-op).
      The only ops this governor will ever accept are
      `:log-inspection-record`, `:schedule-lane-operation`,
      `:flag-inspection-concern` and `:coordinate-supply-order`. No op
      resembling a search, seizure, detention, entry-denial or
      citation/penalty exists anywhere in this codebase's vocabulary —
      not as a gated/escalated op, not as a `:hold`-by-default op, not
      at all. Any op keyword outside this four-op set — including a
      hypothetical `:authorize-search` or `:order-seizure` a buggy or
      malicious caller might construct directly, bypassing the
      advisor entirely — is unconditionally hard-blocked here. This
      is the primary structural guarantee: the allowlist has nothing
      to point at.

    Layer 2 — defense-in-depth textual scope-exclusion
      (`enforcement-scope-exclusion-phrases`, rule
      :enforcement-authority-scope-exclusion). Even though no op can
      express an enforcement action, this governor also hard-blocks
      any proposal whose free-text `:rationale` claims to finalize or
      execute one of those actions (\"authorize the search\", \"order
      the seizure\", \"order the detention\", \"deny entry\", \"issue
      the citation/penalty\"). This is deliberately phrased as
      finalization/execution ACTION PHRASES, never bare nouns like
      \"search\" or \"seizure\" — a bare-noun list would false-trip on
      completely routine documentation rationale (e.g. this actor's
      own default mock-advisor text describing that NO search action
      is proposed necessarily mentions the word \"search\"). See
      advisor.cljc's docstring and the
      `default-mock-advisor-proposals-never-self-trip-scope-exclusion`
      test in governor_test.clj, which pins this down for every op the
      mock advisor can produce.

  Any observation this actor's robot logs that MAY warrant a
  search/seizure/detention/entry-denial is surfaced ONLY via the
  always-escalating `:flag-inspection-concern` op — reviewed and acted
  on by a human customs officer. The robot never acts on it, not even
  as a `:propose`.

  HARD invariants (:hard? true, ALWAYS :hold, permanent,
  un-overridable):
    1. inspector provenance     — the inspector must be independently
                                  verified/registered.
    2. no-actuation             — proposal :effect must be :propose
                                  (the governor never dispatches
                                  hardware and never itself performs a
                                  search/seizure/detention/entry-denial;
                                  it only gates what the advisor may
                                  log/schedule/flag/order).
    3. closed op-allowlist      — see Layer 1 above (:unknown-op).
    4. enforcement scope exclusion — see Layer 2 above
                                  (:enforcement-authority-scope-exclusion).
    5. facility basis           — a facility-scoped proposal must cite
                                  a REGISTERED facility belonging to
                                  this inspector.
    6. source-document attached — a `:log-inspection-record` proposal
                                  must have
                                  `:source-document-attached?` true
                                  (declaration/manifest/inspection-log
                                  basis) before it can be logged
                                  (logging a record with no attached
                                  source document is a fabricated
                                  record, not documentation).
  ESCALATION invariants (:escalate? true, ALWAYS human customs-officer
  sign-off per business-model.md's Trust Controls — these are
  :high/:safety-critical regardless of confidence):
    7. :op :flag-inspection-concern — always escalates immediately and
       is never auto-commit-eligible; a human customs officer reviews
       and decides whether any further action (which this actor cannot
       itself take) is warranted.
    8. :coordinate-supply-order whose :cost exceeds the facility's
       registered `:max-supply-order-cost`.
    9. low confidence (< `confidence-floor`)."
  (:require [customsinspection.store :as store]
            [clojure.string :as str]))

(def confidence-floor 0.6)

(def ^:private op-allowlist
  #{:log-inspection-record :schedule-lane-operation
    :flag-inspection-concern :coordinate-supply-order})

(def ^:private always-escalate-ops #{:flag-inspection-concern})

;; Deliberately full finalization/execution ACTION PHRASES, never bare
;; nouns — see the namespace docstring's "Layer 2" explanation and the
;; self-tripping-bug guardrail this exists to avoid.
(def ^:private enforcement-scope-exclusion-phrases
  ["authorize the search" "authorized the search" "authorizes the search"
   "authorize a search" "authorized a search" "authorizes a search"
   "order the seizure" "ordered the seizure" "orders the seizure"
   "order a seizure" "ordered a seizure" "orders a seizure"
   "order the detention" "ordered the detention" "orders the detention"
   "order a detention" "ordered a detention" "orders a detention"
   "deny entry" "denied entry" "denies entry" "denying entry"
   "issue the citation" "issued the citation" "issues the citation"
   "issue a citation" "issued a citation" "issues a citation"
   "issue the penalty" "issued the penalty" "issues the penalty"
   "issue a penalty" "issued a penalty" "issues a penalty"])

(defn- scope-excluded-text? [s]
  (let [s (str/lower-case (or s ""))]
    (boolean (some #(str/includes? s %) enforcement-scope-exclusion-phrases))))

(defn- needs-facility? [op]
  (contains? op-allowlist op))

(defn- hard-violations [{:keys [request proposal]} inspector-record f]
  (let [{:keys [op facility-id source-document-attached? rationale]} proposal
        log? (= :log-inspection-record op)]
    (cond-> []
      (nil? inspector-record)
      (conj {:rule :no-inspector :detail "未登録 inspector — provenance が独立に検証されていない"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation
             :detail "effect は :propose のみ許可（governor は取締行為を一切直接実行しない）"})

      (not (contains? op-allowlist op))
      (conj {:rule :unknown-op
             :detail (str "許可されていない op: " (pr-str op)
                          "（捜索の許可・押収の命令・拘束の命令・入国拒否・違反切符の発行に"
                          "相当する op は closed allowlist に一切存在しない）")})

      (scope-excluded-text? rationale)
      (conj {:rule :enforcement-authority-scope-exclusion
             :detail (str "提案文言が捜索の許可／押収の命令／拘束の命令／入国拒否／違反切符の"
                          "発行を確定・実行する記述を含む — たとえ :propose であっても恒久的に"
                          "禁止（allowlist に存在しない権限を文言で回避することを防ぐ多層防御）")})

      (and (needs-facility? op) (nil? facility-id))
      (conj {:rule :missing-facility :detail "facility-id が未指定"})

      (and (needs-facility? op) facility-id (nil? f))
      (conj {:rule :unknown-facility :detail "未登録 facility への提案は不可"})

      (and (needs-facility? op) facility-id f
           (not= (:inspector-id f) (:inspector-id request)))
      (conj {:rule :facility-wrong-inspector :detail "facility が別 inspector の管轄"})

      (and log? (not source-document-attached?))
      (conj {:rule :missing-source-document
             :detail "原始証憑（申告書／マニフェスト等）が添付されていない記録の記入は捏造記録であって文書化ではない"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `customsinspection.store/Store`. Pure — never
  mutates the store, never authorizes a search, orders a seizure,
  orders a detention, denies entry, or issues a citation/penalty."
  [request context proposal store]
  (let [inspector-record (store/inspector store (:inspector-id request))
        f (some->> (:facility-id proposal) (store/facility store))
        hard (hard-violations {:request request :proposal proposal} inspector-record f)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        over-cost? (and (= :coordinate-supply-order (:op proposal))
                        f (number? (:cost proposal))
                        (number? (:max-supply-order-cost f))
                        (> (:cost proposal) (:max-supply-order-cost f)))
        always-risky? (or (contains? always-escalate-ops (:op proposal)) over-cost?)]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
