Feature: MaterialSubmission

  Scenario: Receive a material submission

    Given no previous events
    When you submitMaterial on a MaterialSubmission using a material submission
    Then material submitted

  Scenario: Receive a material submission

    Given no previous events
    When you submitMaterial on a MaterialSubmission using a cps material submission
    Then cps material submitted

  Scenario: Receive a submission successful acknowledgement after a material submission

    Given material submitted
    When you receiveMaterialSubmissionSuccessful on a MaterialSubmission using a receive material submission successful
    Then material submission successful


  Scenario: Receive a rejected submission acknowledgement after a material submission

    Given material submitted
    When you rejectMaterial on a MaterialSubmission using a receive material rejection
    Then material submission rejected