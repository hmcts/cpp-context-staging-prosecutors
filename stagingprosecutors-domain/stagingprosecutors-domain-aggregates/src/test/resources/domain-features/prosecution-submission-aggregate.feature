Feature: ProsecutionSubmission

  Scenario: Receive a sjp prosecution submission

    Given no previous events
    When you receiveSjpSubmission on a ProsecutionSubmission using a sjp case submission
    Then sjp case received

  Scenario: Receive submission

    Given no previous events
    When you receiveSubmission on a ProsecutionSubmission using a case submission
    Then case received


  Scenario: Submission successful

    Given sjp case received
    When you receiveSubmissionSuccessful on a ProsecutionSubmission using a receive submission successful
    Then submission successful

  Scenario: Submission successful with Warnings

    Given sjp case received
    When you receiveSubmissionSuccessfulWithWarnings on a ProsecutionSubmission using a receive submission successful with warnings
    Then submission successful with warnings

  Scenario: Submission rejected

    Given sjp case received
    When you receiveSubmissionRejection on a ProsecutionSubmission using a receive submission rejected
    Then submission rejected

  Scenario: Submission rejected when not pending

    Given no previous events
    When you receiveSubmissionRejection on a ProsecutionSubmission using a receive submission rejected
    Then submission status not changed

  Scenario: Submission successful attempted twice

    Given sjp case received, submission successful
    When you receiveSubmissionSuccessful on a ProsecutionSubmission using a receive submission successful
    Then submission status not changed

#  Scenario: Reject a submission
#  Scenario: Fail a submission
#  Scenario: Submission successful
#  Scenario: Submission successful with warnings
#  Scenarios: reject, fail & success can only be applied to status pending