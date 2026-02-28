.PHONY: help bootstrap doctor doctor-verbose doctor-no-color doctor-json verify verify-fast verify-dry-run verify-continue verify-with-bootstrap verify-json-report verify-frontend verify-backend verify-strict test-frontend test-backend clean check-lockfiles test-automation

help:
	@echo "Available targets:"
	@echo "  bootstrap             Install frontend and backend dependencies"
	@echo "  doctor                Print environment diagnostics"
	@echo "  doctor-verbose        Print diagnostics with debug logging"
	@echo "  doctor-no-color       Print diagnostics without ANSI color output"
	@echo "  doctor-json           Print diagnostics and write JSON summary artifacts"
	@echo "  verify-fast           Run frontend and backend tests only"
	@echo "  verify-dry-run        Print verification commands without executing"
	@echo "  verify-continue       Continue summary collection even if a stage fails"
	@echo "  verify-with-bootstrap Run bootstrap before verification"
	@echo "  verify-json-report    Run verification and emit JSON step summaries"
	@echo "  verify-frontend       Run frontend quality gates"
	@echo "  verify-backend        Run backend quality gates"
	@echo "  verify-strict         Run full verification plus frontend coverage and backend verify"
	@echo "  test-frontend         Run frontend unit tests only"
	@echo "  test-backend          Run backend tests only"
	@echo "  test-automation       Run shell automation smoke tests"
	@echo "  check-lockfiles       Verify deterministic dependency lockfiles"
	@echo "  clean                 Remove generated artifacts"
	@echo "  verify                Run full-stack verification"

bootstrap:
	./scripts/bootstrap.sh

doctor:
	./scripts/doctor.sh

doctor-verbose:
	BPM_RUNNER_LOG_LEVEL=debug ./scripts/doctor.sh

doctor-no-color:
	BPM_RUNNER_NO_COLOR=1 ./scripts/doctor.sh

doctor-json:
	BPM_RUNNER_SUMMARY_FORMAT=json BPM_RUNNER_LOG_TO_FILE=1 ./scripts/doctor.sh

verify-frontend:
	./scripts/verify-frontend.sh

verify-backend:
	./scripts/verify-backend.sh

verify-fast:
	cd frontend && npm run test:ci
	cd backend && ./mvnw -B test

verify-dry-run:
	BPM_RUNNER_DRY_RUN=1 ./scripts/verify-all.sh

verify-continue:
	BPM_RUNNER_CONTINUE_ON_ERROR=1 ./scripts/verify-all.sh

verify-with-bootstrap:
	BPM_VERIFY_INCLUDE_BOOTSTRAP=1 ./scripts/verify-all.sh

verify-json-report:
	BPM_RUNNER_SUMMARY_FORMAT=json BPM_RUNNER_LOG_TO_FILE=1 ./scripts/verify-all.sh

verify-strict:
	BPM_FRONTEND_ENABLE_COVERAGE=1 BPM_BACKEND_ENABLE_VERIFY=1 ./scripts/verify-all.sh

test-frontend:
	cd frontend && npm run test:ci

test-backend:
	cd backend && ./mvnw -B test

test-automation:
	./scripts/test-automation.sh

check-lockfiles:
	@test -f frontend/package-lock.json || (echo "Missing frontend/package-lock.json" && exit 1)
	@test -f package-lock.json || (echo "Missing root package-lock.json" && exit 1)
	@echo "Lockfile check passed"

clean:
	rm -rf frontend/.svelte-kit frontend/build frontend/coverage frontend/test-results .automation
	find backend -type d -name target -prune -exec rm -rf {} +

verify:
	./scripts/verify-all.sh
