Feature: In-place Edit
  As a learner, I want to be able to edit note title and description in-place in Article view

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title   | titleIDN  | description      | descriptionIDN   |
      | Storm   | Indonesia | Heavy Rain       | Bahasa Indonesia |

  Scenario: Note update with in-place editing
    Given I open the "article" view of note "Storm"
    When I click note title "Storm"
    And I change the title to "Thunderstorm" in-place-edit mode
    And I click note description "Heavy Rain"
    And I change the description to "Very Heavy rain and wind" in-place-edit mode
    Then The title should change to "Thunderstorm"
    And The description should change to "Very Heavy rain and wind"
