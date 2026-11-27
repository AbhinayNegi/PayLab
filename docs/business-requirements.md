# Business requirements

Full name, email, password and matching confirmation are required for registration. Full name and email are trimmed, and email is lowercased. Full name allows up to 120 characters. Email must have valid syntax and allows up to 254 characters. Passwords allow 8–128 characters and require an ASCII uppercase letter, lowercase letter and digit. Passwords are compared exactly as entered.

Registration creates a merchant account and routes the browser to sign-in. Using the same email with different casing produces a duplicate conflict. Both the browser and the server validate submitted fields.

Sign-in requires email and password. An unknown email and an incorrect password show the same error. A successful sign-in opens the dashboard. It includes an overview, the user's merchant profile, wallet and transaction sections with empty states, and logout. Direct navigation to the dashboard requires a session and a successful profile request.
