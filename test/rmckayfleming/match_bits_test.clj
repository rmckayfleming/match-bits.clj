(ns rmckayfleming.match-bits-test
  (:require [clojure.test :refer :all]
            [rmckayfleming.match-bits :refer :all]))

(deftest documentation-examples
  (testing "Exact Integers Example"
    (is (match-bits (+ 2r1000 2r0001)
                    2r1000 :nope
                    2r0001 :nope
                    2r10000001 :nope
                    2r1001 :yes!
                    :default)
        :yes!))
  (testing "Bit Masks Examples"
    (is (match-bits (+ 2r1000 2r0001)
                    %1000 :nope
                    %0001 :nope
                    %10000001 :nope
                    %1001 :yes!
                    :default)
        :yes!)
    (is (match-bits 2r10011010
                    2r1010 :nope
                    %1010 :yes!
                    :default)
        :yes!))
  (testing "Wildcards Example"
    (is (match-bits 2r10011010
                    2r10010000 :nope
                    %1001____ :yes!
                    :default)
        :yes!))
  (testing "Pattern Variables Examples"
    (is (match-bits 2r10011010
                    2r10010000 :nope
                    %aaaabbbb [aaaa bbbb]
                    :default)
        [2r1001 2r1010])
    (is (match-bits 2r10011010
                    2r10010000 :nope
                    %aaabbbbb [aaa bbbbb]
                    :default)
        [2r100 2r11010]))
  (testing "Fallthrough Examples"
    (is (= (match-bits 2r10011010
                       2r10010000 :nope
                       (do (println "Default reached!")
                           :yes!))
         :yes!))
    (is (= (with-out-str
             (match-bits 2r10011010
                         2r10010000 :nope
                         (do (println "Default reached!")
                             :yes!)))
           "Default reached!\n"))
    (is (nil? (match-bits 2r10011010
                          2r10010000 :nope)))))

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