default: fmt test

test:
	mvn test

test-promote:
	mvn test -Dnyub.expekt.promote=true

# Apply formatting rules to sources
fmt:
	mvn spotless:apply

fmt-check:
	mvn spotless:check
