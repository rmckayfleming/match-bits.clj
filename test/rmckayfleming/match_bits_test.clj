(ns rmckayfleming.match-bits-test
  (:require [clojure.test :refer :all]
            [rmckayfleming.match-bits :refer :all]))

(deftest exact-matches
  (testing "Matches exact numbers"
    (is (match-bits 2r1001
                    2r1001 true
                    false))))

(deftest masked-matches
  (testing "Matches against masks"
    (is (match-bits 2r1001
                    %1001 true
                    false))
    (is (match-bits 2r10011001
                    %1001 true
                    false))
    (is (match-bits 2r10011001
                    %0000 false
                    %0001 false
                    %0010 false
                    %0011 false
                    %0100 false
                    %0101 false
                    %0110 false
                    %0111 false
                    %1000 false
                    %1001 true
                    false))))

(deftest destructuring
  (testing "Destructures bits into symbols"
    (is (= (match-bits 2r10010000
                       %aaaabbbb [aaaa bbbb])
           [2r1001 2r0000]))
    (is (= (match-bits 2r10010000
                       %aaabbbbb [aaa bbbbb])
           [2r100 2r10000]))))

(deftest destructuring-masks
  (testing "Destructures bits into symbols with specific matches"
    (is (= (match-bits 2r10011010
                       %1001aaaa aaaa)
           2r1010))
    (is (= (match-bits 2r10011010
                       %1000aaaa false
                       %1001aaaa aaaa)
           2r1010))))

(deftest exact-and-masks
  (testing "Mixed literal numbers and masks"
    (is (match-bits 2r10011010
                     2r10011010 true
                     %10011010 false))
    (is (match-bits 2r10011010
                    %10011010 true
                    2r10011010 false))
    (is (match-bits 2r10011010
                    %10011000 false
                    2r10011000 false
                    true))))