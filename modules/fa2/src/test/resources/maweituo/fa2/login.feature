Feature: Login with 2FA

Scenario: User login with 2FA
  When User send "POST" request to /login
  Then the response on /login code should be 200
  And the response on /login should match json:
      """
      "Verification code sent to email"
      """
  And user send "POST" request to /login_verify
  Then the response on /login_verify code should be 200
  And the response on /login_verify should match json:
      """
      "Auth success!"
      """