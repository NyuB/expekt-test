ifeq ($(OS), Windows_NT)
	PY=py
else
	PY=python3
endif

default: fmt test

test:
	mvn test
	$(PY) -m unittest etc/release_changelog.py

test-promote:
	mvn test -Dnyub.expekt.promote=true

test-promote-no-diff:
	mvn test -Dnyub.expekt.promote=true
	git diff --exit-code

# Apply formatting rules to sources
fmt:
	mvn spotless:apply
	$(PY) -m black release_changelog.py

fmt-check:
	mvn spotless:check
	$(PY) -m black --check release_changelog.py

release_changelog.md: CHANGELOG.md etc/release_changelog.py
	$(PY) etc/release_changelog.py CHANGELOG.md > release_changelog.md

