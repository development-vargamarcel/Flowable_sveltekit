.PHONY: help bootstrap doctor verify verify-frontend verify-backend verify-strict test-frontend test-backend clean check-lockfiles

help:
	@echo "Available targets:"
	@echo "  bootstrap        Install frontend and backend dependencies"
	@echo "  doctor           Print environment diagnostics"
	@echo "  verify-frontend  Run frontend quality gates"
	@echo "  verify-backend   Run backend quality gates"
	@echo "  verify-strict    Run full verification plus frontend coverage"
	@echo "  test-frontend    Run frontend unit tests only"
	@echo "  test-backend     Run backend tests only"
	@echo "  check-lockfiles  Verify deterministic dependency lockfiles"
	@echo "  clean            Remove generated artifacts"
	@echo "  verify           Run full-stack verification"

bootstrap:
	./scripts/bootstrap.sh

doctor:
	./scripts/doctor.sh

verify-frontend:
	./scripts/verify-frontend.sh

verify-backend:
	./scripts/verify-backend.sh

verify-strict:
	./scripts/verify-all.sh
	cd frontend && npm run test:coverage

test-frontend:
	cd frontend && npm run test:ci

test-backend:
	cd backend && ./mvnw -B test

check-lockfiles:
	@test -f frontend/package-lock.json || (echo "Missing frontend/package-lock.json" && exit 1)
	@test -f package-lock.json || (echo "Missing root package-lock.json" && exit 1)
	@echo "Lockfile check passed"

clean:
	rm -rf frontend/.svelte-kit frontend/build frontend/coverage frontend/test-results
	find backend -type d -name target -prune -exec rm -rf {} +

verify:
	./scripts/verify-all.sh
