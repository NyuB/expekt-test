default: fmt test

# Run unit tests
test:
	mvn test
	$(PY) -m unittest etc/release_changelog.py

# Promote tests outputs, replacing expected string blocks with the actual tests' outputs
test-promote:
	mvn test -Dnyub.expekt.promote=true

# Check that promoting tests' outputs does not change the sources
# Used to check promotion consistency with equality assertions and code formatting
test-promote-no-diff:
	mvn test -Dnyub.expekt.promote=true
	git diff --exit-code

# Apply formatting rules to sources
fmt:
	mvn spotless:apply
	$(PY) -m black etc/release_changelog.py

# Enforce formatting rules, failing if any source is not correctly formatted
fmt-check:
	mvn spotless:check
	$(PY) -m black --check etc/release_changelog.py

# Extract the changelog for the next release from the global changelog
# This corresponds to the first section in CHANGELOG.md, with the main header removed
release_changelog.md: CHANGELOG.md etc/release_changelog.py
	$(PY) etc/release_changelog.py CHANGELOG.md > release_changelog.md

include etc/help.mk
include etc/py.mk
