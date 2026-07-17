(ns customsinspection.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [customsinspection.actor :as actor]
            [customsinspection.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-inspector! st {:inspector-id "inspector-1" :name "J. Rivera"})
    (store/register-facility! st {:facility-id "F-1" :inspector-id "inspector-1"
                                  :name "lane-042"
                                  :max-supply-order-cost 5000})
    st))

(deftest commits-a-valid-log-inspection-record
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:inspector-id "inspector-1" :op :log-inspection-record :stake :low
                 :facility-id "F-1" :source-document-attached? true}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "inspector-1"))))))

(deftest holds-a-log-inspection-record-missing-source-document
  (testing "logging a record with no attached source document is a fabricated record — hard hold, no human override path"
    (let [st (fresh-store)
          graph (actor/build-graph {:store st})
          request {:inspector-id "inspector-1" :op :log-inspection-record :stake :low
                   :facility-id "F-1" :source-document-attached? false}
          result (actor/run-request! graph request {} "thread-2")]
      (is (= :hold (:disposition (:state result))))
      (is (empty? (store/records-of st "inspector-1"))))))

(deftest interrupts-then-approves-flagged-concern-on-human-approval
  (testing "a flagged inspection concern always escalates and is never auto-commit-eligible; only a human customs officer's approval advances it"
    (let [st (fresh-store)
          graph (actor/build-graph {:store st})
          request {:inspector-id "inspector-1" :op :flag-inspection-concern :stake :low
                   :facility-id "F-1"}
          interrupted (actor/run-request! graph request {} "thread-3")]
      (is (= :interrupted (:status interrupted)))
      (is (empty? (store/records-of st "inspector-1")))
      (let [resumed (actor/approve! graph "thread-3")]
        (is (= :done (:status resumed)))
        (is (= 1 (count (store/records-of st "inspector-1"))))))))

(deftest interrupts-then-approves-over-threshold-supply-order-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:inspector-id "inspector-1" :op :coordinate-supply-order :stake :low
                 :facility-id "F-1" :cost 50000}
        interrupted (actor/run-request! graph request {} "thread-4")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "inspector-1")))
    (let [resumed (actor/approve! graph "thread-4")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "inspector-1")))))))

(deftest commits-a-valid-schedule-lane-operation
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:inspector-id "inspector-1" :op :schedule-lane-operation :stake :low
                 :facility-id "F-1"}
        result (actor/run-request! graph request {} "thread-5")]
    (is (= :done (:status result)))
    (is (= 1 (count (store/records-of st "inspector-1"))))))

(deftest holds-an-attempted-enforcement-authority-op
  (testing "even a directly-constructed request bypassing the advisor cannot reach commit for an op resembling enforcement action — no human-approval path exists for a hard hold"
    (let [st (fresh-store)
          graph (actor/build-graph {:store st})
          request {:inspector-id "inspector-1" :op :authorize-search :stake :low
                   :facility-id "F-1"}
          result (actor/run-request! graph request {} "thread-6")]
      (is (= :hold (:disposition (:state result))))
      (is (empty? (store/records-of st "inspector-1"))))))
