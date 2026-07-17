(ns customsinspection.advisor
  "Customs Inspection Advisor — the advisor named in this repository's
  README, proposing a customs-documentation/logistics-coordination
  operation (log an inspection record, schedule lane staffing, flag an
  inspection concern for human review, or coordinate a supply order)
  from an inspection-lane request. Swappable mock/llm; the advisor
  ONLY proposes — `customsinspection.governor` checks facility basis
  and source-document attachment independently, always escalates
  flagged concerns and above-threshold supply orders, and — this is
  the actor's defining constraint — has NO op it can ever propose that
  resembles authorizing a search, ordering a seizure, ordering a
  detention, denying entry, or issuing a citation/penalty; those verbs
  do not exist anywhere in this namespace or in
  `customsinspection.governor`'s closed op-allowlist. Modeled on
  cloud-itonami-isco-3313's advisor.

  A proposal: {:op :log-inspection-record|:schedule-lane-operation|
                    :flag-inspection-concern|:coordinate-supply-order
               :effect :propose :facility-id str
               :source-document-attached? boolean :cost number
               :stake kw :confidence n :rationale str}

  IMPORTANT (self-tripping-bug guardrail, see governor.cljc): default
  rationale text below is phrased around DOCUMENTATION verbs (log,
  schedule, flag, coordinate) and deliberately never uses the
  finalization/execution phrases the governor's defense-in-depth
  scope-exclusion scan looks for (\"authorize the search\", \"order the
  seizure\", etc.) — governor_test.clj asserts this holds for every op
  this advisor can produce."
  )

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake inspector-id facility-id
                              source-document-attached? cost]
                       :as request}]
  {:op op
   :effect :propose
   :facility-id facility-id
   :source-document-attached? (boolean source-document-attached?)
   :cost cost
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op) " for inspector " inspector-id
                   " at facility " facility-id
                   " — documentation/logistics-coordination only, no"
                   " search, seizure, detention or entry-denial action"
                   " proposed or implied")})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a customs-inspection documentation and logistics-coordination
   advisor. Given a request, propose an :op (only :log-inspection-record,
   :schedule-lane-operation, :flag-inspection-concern or
   :coordinate-supply-order — no other op exists), the :facility-id,
   whether a source document is attached, a :cost when relevant, an
   honest :confidence and a :stake. You have NO authority to search,
   seize, detain, deny entry, or issue a citation/penalty, and must
   never phrase a proposal as if you did — if an observation suggests
   one of those actions may be warranted, the only correct response is
   :flag-inspection-concern, which always requires human customs-officer
   sign-off. The governor independently checks facility registration,
   source-document attachment and supply-order cost thresholds.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
