# Team Notes (Implementation Review)

## What was completed
- Planning docs were consolidated to reduce conflicting sources; `PLAN.md` is now the canonical tracker.
- Frontend shared utility resilience was improved for malformed API data and duration/date edge cases.
- CSV export now uses a pure serializer (`toCSV`) and explicitly revokes object URLs to avoid browser memory leaks.
- Utility-level regression tests were added to lock in behavior.

## Why this matters
- Prevents noisy UI defects (`$NaN`, odd invalid date rendering) that can confuse end users.
- Improves maintainability by separating pure data transformation logic from browser side effects.
- Gives us safer future refactors through focused unit test coverage.

## Follow-up suggestions
1. Build i18n-aware formatting utilities so currencies/dates are locale-configurable.
2. Add tests for `exportToCSV` with DOM mocking to verify download attributes and BOM option behavior.
3. Promote the queued plan items in `PLAN.md` into milestone tickets with owners and due dates.
