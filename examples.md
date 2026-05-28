# Examples

This file is the canonical quick-reference examples companion to `UsageAndDocumentationAndExamples.md`.

## Example: post-change documentation updates

1. Implement a code or configuration change.
2. Increment the repository version to the next semantic value.
3. Update `UsageAndDocumentationAndExamples.md` with behavior and validation updates.
4. Add or refresh examples in this file.
5. Update `PLAN.md` and/or `notes.md` when planning or historical context changes.

## Example: patch increment

- Before change: `1.8.2`
- After backward-compatible fix: `1.8.3`

## Example: quick health check

```bash
./scripts/doctor.sh
./scripts/verify-all.sh frontend
```

## Example: strict CI-like verification

```bash
BPM_RUNNER_SUMMARY_FORMAT=json \
BPM_RUNNER_LOG_TO_FILE=1 \
BPM_FRONTEND_ENABLE_COVERAGE=1 \
BPM_FRONTEND_ENABLE_BROWSER_SMOKE=1 \
BPM_BACKEND_ENABLE_VERIFY=1 \
./scripts/verify-all.sh
```
