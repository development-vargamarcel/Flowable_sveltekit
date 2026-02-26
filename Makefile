.PHONY: bootstrap doctor verify verify-frontend verify-backend

bootstrap:
	./scripts/bootstrap.sh

doctor:
	./scripts/doctor.sh

verify-frontend:
	./scripts/verify-frontend.sh

verify-backend:
	./scripts/verify-backend.sh

verify:
	./scripts/verify-all.sh
