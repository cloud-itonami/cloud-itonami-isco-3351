(ns customsinspection.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [customsinspection.store :as store]
            [customsinspection.advisor :as advisor]
            [customsinspection.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-inspector! st {:inspector-id "inspector-1" :name "J. Rivera"})
    (store/register-facility! st {:facility-id "F-1" :inspector-id "inspector-1"
                                  :name "lane-042"
                                  :max-supply-order-cost 5000})
    st))

(defn- log-op [attached?]
  {:op :log-inspection-record :effect :propose :facility-id "F-1"
   :source-document-attached? attached?
   :confidence 0.9 :stake :low})

(defn- supply-op [cost]
  {:op :coordinate-supply-order :effect :propose :facility-id "F-1"
   :cost cost :confidence 0.9 :stake :low})

(def ^:private req {:inspector-id "inspector-1"})

;; -- routine documentation ops: ok/hard on facility + source-document basis --

(deftest ok-log-inspection-record-with-attached-source-document
  (let [st (fresh-store)
        v (governor/check req {} (log-op true) st)]
    (is (:ok? v))))

(deftest hard-on-missing-source-document
  (testing "logging a record without an attached source document is a fabricated record, not documentation"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (log-op false) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :missing-source-document (:rule %)) (:violations v))))))

(deftest hard-on-unknown-facility
  (let [st (fresh-store)
        v (governor/check req {} (assoc (log-op true) :facility-id "F-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-facility (:rule %)) (:violations v)))))

(deftest hard-on-facility-wrong-inspector
  (let [st (fresh-store)]
    (store/register-inspector! st {:inspector-id "inspector-2" :name "Other"})
    (let [v (governor/check {:inspector-id "inspector-2"} {} (log-op true) st)]
      (is (:hard? v))
      (is (some #(= :facility-wrong-inspector (:rule %)) (:violations v))))))

(deftest hard-on-unregistered-inspector
  (let [st (fresh-store)
        v (governor/check {:inspector-id "nobody"} {} (log-op true) st)]
    (is (:hard? v))
    (is (some #(= :no-inspector (:rule %)) (:violations v)))))

(deftest hard-on-missing-facility-id
  (let [st (fresh-store)
        v (governor/check req {} (dissoc (log-op true) :facility-id) st)]
    (is (:hard? v))
    (is (some #(= :missing-facility (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (log-op true) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

;; -- structural absence of enforcement authority --
;; No such op exists in the advisor's vocabulary, but a buggy or
;; malicious caller could still hand the governor a proposal map
;; directly (bypassing the advisor). Both layers must independently
;; hard-block that, permanently, never overridable via approval.

(deftest hard-on-op-outside-closed-allowlist
  (testing "no op resembling search/seizure/detention/entry-denial/citation exists in the allowlist"
    (doseq [phantom-op [:authorize-search :order-seizure :order-detention
                        :deny-entry :issue-citation :issue-penalty]]
      (let [st (fresh-store)
            v (governor/check req {} (assoc (log-op true) :op phantom-op :confidence 0.99) st)]
        (is (:hard? v) (str phantom-op " must hard-block"))
        (is (some #(= :unknown-op (:rule %)) (:violations v)) (str phantom-op " must trip :unknown-op"))))))

(deftest hard-on-enforcement-authority-scope-exclusion-text
  (testing "a proposal's free text cannot finalize/execute an enforcement action even under a routine op"
    (doseq [phrase ["we authorize the search of the container"
                    "recommend we order the seizure of the goods"
                    "propose to order the detention of the traveler"
                    "advise we deny entry to the passenger"
                    "will issue the citation for the discrepancy"]]
      (let [st (fresh-store)
            v (governor/check req {} (assoc (log-op true) :rationale phrase :confidence 0.99) st)]
        (is (:hard? v) (str "must hard-block: " phrase))
        (is (some #(= :enforcement-authority-scope-exclusion (:rule %)) (:violations v))
            (str "must trip scope-exclusion: " phrase))))))

(deftest scope-exclusion-does-not-false-trip-on-bare-nouns
  (testing "bare nouns describing what was NOT done must not self-trip (only finalization/execution phrases do)"
    (let [st (fresh-store)
          v (governor/check req {}
                             (assoc (log-op true)
                                    :rationale "no search, seizure, detention or entry-denial action proposed or implied")
                             st)]
      (is (not (some #(= :enforcement-authority-scope-exclusion (:rule %)) (:violations v)))))))

;; -- default mock-advisor proposals must never self-trip the governor --
;; This is the dedicated regression test CLAUDE.md calls for: the
;; governor's own scope-exclusion term list must never accidentally
;; match inside the mock advisor's own default rationale text.

(deftest default-mock-advisor-proposals-never-self-trip-scope-exclusion
  (let [st (fresh-store)
        mock (advisor/mock-advisor)
        requests [{:op :log-inspection-record :inspector-id "inspector-1" :facility-id "F-1"
                   :source-document-attached? true :stake :low}
                  {:op :schedule-lane-operation :inspector-id "inspector-1" :facility-id "F-1"
                   :stake :low}
                  {:op :flag-inspection-concern :inspector-id "inspector-1" :facility-id "F-1"
                   :stake :low}
                  {:op :coordinate-supply-order :inspector-id "inspector-1" :facility-id "F-1"
                   :cost 1000 :stake :low}]]
    (doseq [request requests]
      (let [proposal (advisor/-advise mock st request)
            v (governor/check req {} proposal st)]
        (is (not (some #(= :enforcement-authority-scope-exclusion (:rule %)) (:violations v)))
            (str "op " (:op request) " default rationale self-tripped: " (:rationale proposal)))
        (is (not (some #(= :unknown-op (:rule %)) (:violations v)))
            (str "op " (:op request) " unexpectedly rejected by closed allowlist"))))))

;; -- always-escalate ops / thresholds --

(deftest always-escalates-flag-inspection-concern-even-at-high-confidence
  (testing "an observation that MAY warrant search/seizure/detention/entry-denial review is NEVER auto-commit-eligible"
    (let [st (fresh-store)
          v (governor/check req {} {:op :flag-inspection-concern :effect :propose
                                    :facility-id "F-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v))
      (is (not (:ok? v))))))

(deftest escalates-supply-order-above-cost-threshold-even-at-high-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (supply-op 50000) :confidence 0.99) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))

(deftest ok-supply-order-at-exact-threshold-boundary
  (testing "the supply-order cost threshold is inclusive"
    (let [st (fresh-store)
          v (governor/check req {} (supply-op 5000) st)]
      (is (:ok? v)))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (log-op true) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))

(deftest ok-schedule-lane-operation
  (let [st (fresh-store)
        v (governor/check req {} {:op :schedule-lane-operation :effect :propose
                                  :facility-id "F-1" :confidence 0.9 :stake :low} st)]
    (is (:ok? v))))
