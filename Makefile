.PHONY: help bootstrap doctor verify verify-frontend verify-backend test-frontend test-backend

help:
	@echo "Available targets:"
	@echo "  bootstrap        Install all frontend and backend dependencies"
	@echo "  doctor           Print environment diagnostics"
	@echo "  verify-frontend  Run frontend quality gates"
	@echo "  verify-backend   Run backend quality gates"
	@echo "  test-frontend    Run frontend tests only"
	@echo "  test-backend     Run backend tests only"
	@echo "  verify           Run full-stack verification"

bootstrap:
	./scripts/bootstrap.sh

doctor:
	./scripts/doctor.sh

verify-frontend:
	./scripts/verify-frontend.sh

verify-backend:
	./scripts/verify-backend.sh

test-frontend:
	cd frontend && npm run test:ci

test-backend:
	cd backend && ./mvnw -B test

verify:
	./scripts/verify-all.sh
