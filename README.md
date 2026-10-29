# College Scholarship Eligibility and Application Portal

Scheme-specific conditions are stored as coded, required/verified rules in MySQL. Registered SchemeRuleEvaluator components handle known codes; an unknown required rule remains indeterminate until an evaluator is implemented. Admins can review student-reported institution, admission-route, and qualifying-board details. Student profile edits reset those verification flags. The admin API also exposes scholarship detail and the catalogue supports name/provider, course type, study year, family income, amount, and open-deadline filters.

When adding rules in the administrator JSON criteria field, each additionalRules item is an object with code, description, required, and verified fields. For example: {"code":"CUSTOM_RULE","description":"Check the scheme-specific condition","required":true,"verified":false}. Use a registered evaluator code for an implemented rule; a new unknown code stays indeterminate.

Java 17, Spring Boot, Spring MVC, Spring JDBC, MySQL 8, and a plain HTML/CSS/JavaScript interface for a college scholarship project.

## Current implementation

The implementation includes student registration/login, profile editing, a database-backed scholarship catalogue and matching API, comparison, applications, document upload, admin scheme management and review, status history, and database-derived dashboards.

## Four initial demo schemes

The seed file contains SSP OBC Post-Matric, SSP SC/ST Post-Matric, NSP CSSS, and AICTE Pragati for academic year 2026-27. Supplied income and mark thresholds are provisional demonstration values only. Amounts and deadlines are left empty because no verified values were supplied. `criteria_verified=FALSE` and the UI labels these values provisional. Check current official rules before relying on eligibility. In particular, 85% is not a universal CSSS cutoff; the profile includes qualifying board and merit-position fields. Pragati matching requires recorded female eligibility, institution approval, and admission route, while other current scheme conditions remain unresolved until verified.

The eligibility engine reads criteria from MySQL. Multiple courses, years, categories, genders, and states use related tables; `*` explicitly represents unrestricted values. It evaluates income as an inclusive maximum and marks as an inclusive minimum. Missing information and unverified scheme conditions produce `INDETERMINATE`, not a false positive. The matching endpoint uses the user ID from the server session and never accepts a student ID from the caller.

## Architecture

`static/` contains HTML, CSS, and JavaScript. The browser calls JSON endpoints under `/api`. Spring MVC controllers enforce session roles, `ScholarshipRepository` uses parameterized Spring JDBC queries, and `ScholarshipEligibilityService` contains matching rules. MySQL stores account, profile, normalized criteria, applications, document metadata, and audit history.

## Database setup

1. Install MySQL 8 and run `database/schema.sql`.
2. Run `database/sample-data.sql` to insert four fictional/provisional demonstration scholarship records.
3. Configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in your environment. Defaults target local MySQL database `college_scholarship`, user `root`, and an empty password; change these for your machine.

The SQL scripts are intentionally separate from startup initialization so restarting the app does not erase data.

## Run

Install JDK 17 and Maven 3.9 or later, then from this directory:

```powershell
mvn test
mvn spring-boot:run
```

Open http://localhost:8080. Maven is not installed in the execution environment where this project was generated, so compilation has not yet been verified here. You can override the port with `PORT`.

## Accounts, security, and API

`POST /api/auth/register` creates a student account and BCrypt-hashes its password; registration cannot set roles. Set `ADMIN_EMAIL` and `ADMIN_PASSWORD` (at least 14 characters) to create an administrator account on first startup only. Login creates a server-side session and CSRF token. Mutating API calls require that token. Session cookies are HTTP-only and same-site strict.

- `GET /api/profile`, `PUT /api/profile`: read/update own profile.
- `GET /api/scholarships?academicYear=2026-27`: active catalogue.
- `GET /api/scholarships/matching?academicYear=2026-27`: own matching explanations.
- `GET /api/scholarships/compare?ids=1,2`: compare 2–4 scholarships.
- `POST /api/applications`, `GET /api/applications`: submit and view own applications.
- `GET /api/applications/{id}/history`, `POST /api/applications/{id}/documents`: view history/upload documents.
- `/api/admin/scholarships`, `/api/admin/applications`: admin management and review.
- `/api/dashboard/student`, `/api/admin/summary`: DB-derived statistics.

Student authorization always comes from the session, never a caller-supplied student ID. Status updates are transactional and add an audit-history row. Upload storage names are generated by the server; users cannot choose filesystem paths.

## Configuration and known limitations

Set `LOAD_DEMO_STUDENTS=true` and `DEMO_STUDENT_PASSWORD` (at least 14 characters) to create fictional profiles covering OBC, SC/ST, CSSS, Pragati, income failure, and incomplete information. All six accounts use that configured demo password. Do not enable the fixture loader in real deployments.

The initial scheme data does not verify current government rules or live benefits/deadlines. The portal marks unresolved conditions indeterminate and requires an explicit demonstration acknowledgement before accepting provisional applications. Uploads currently allow PDF/JPG/PNG and 10 MB; production should add content inspection, malware scanning, quotas, HTTPS, and backups. Account recovery and email verification are not implemented.

## Tests

`ScholarshipEligibilityServiceTest` covers income/marks boundaries, category/state/course/year/gender mismatches, missing information, academic-year mismatch, expired/inactive records, and provisional CSSS/Pragati conditions. Run with `mvn test` after Maven is installed. Maven is not available in the generation environment, so Java build and tests have not yet been run. The static JavaScript files passed `node --check`.
