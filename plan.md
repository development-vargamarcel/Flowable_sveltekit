# Implementation Plan: Reliability, Verification, and Developer-Workflow Hardening

Version target for this work: **1.4.0** (minor release)

This plan defines a full-day, production-quality implementation pass. Every item below is implemented and verified in this change set.

## Major improvements (implemented)

1. Add a shared shell utility library to centralize logging, guardrails, and command wrappers.
2. Add explicit sectioned logging with timestamps for every automation script.
3. Add resilient command-existence checks with actionable error messages.
4. Add repository-root and git-worktree detection helper used by all scripts.
5. Add npm command wrapper that strips legacy proxy env keys causing noisy npm warnings.
6. Add reusable elapsed-time timer helpers and use them in verification scripts.
7. Expand `doctor.sh` to validate required tools for frontend and backend workflows.
8. Expand `doctor.sh` to report git branch and commit metadata for reproducibility.
9. Expand `doctor.sh` to validate lockfile presence for deterministic installs.
10. Expand `doctor.sh` to report Java and Maven details with clearer diagnostics.
11. Upgrade `bootstrap.sh` with deterministic, quieter frontend install behavior via wrapper.
12. Upgrade `bootstrap.sh` to prefetch backend dependencies with visible stage boundaries.
13. Harden `verify-frontend.sh` with staged checks and explicit quality-gate messaging.
14. Harden `verify-backend.sh` with preflight checks and timing output.
15. Harden `verify-all.sh` with aggregated phase orchestration and total duration reporting.
16. Expand `Makefile` with consistent phony targets and additional utility targets (`clean`, lockfile checks, strict verify).
17. Add comments in shared script code to clarify why legacy npm proxy env is stripped.
18. Improve workflow documentation and examples for all new commands and expected outputs.
19. Bump semantic versions consistently across frontend, backend, and repository docs to `1.4.0`.
20. Add a concise `changelog` entry describing the current release changes.
21. Review and align existing docs with implemented automation behavior (no stale commands).
22. Execute and verify all quality gates end-to-end after implementation.

## Verification checklist

- Run `./scripts/doctor.sh`
- Run `./scripts/bootstrap.sh`
- Run `./scripts/verify-frontend.sh`
- Run `./scripts/verify-backend.sh`
- Run `./scripts/verify-all.sh`
- Run `make verify`

All commands above were run as part of this implementation pass.
