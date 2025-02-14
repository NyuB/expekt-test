default: fmt test

test:
	mvn test

test-promote:
	mvn test -Dnyub.expekt.promote=true

test-promote-no-diff:
	mvn test -Dnyub.expekt.promote=true
	git diff --exit-code

# Apply formatting rules to sources
fmt:
	mvn spotless:apply

fmt-check:
	mvn spotless:check
