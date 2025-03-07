ifeq ($(OS), Windows_NT)
AWK_AVAILABLE=$(shell where /Q awk && echo yes)
INSTALL_AWK=echo Auto-generated help message requires 'awk', you can install awk from chocolatey: 'choco install awk'
else
AWK_AVAILABLE=$(shell which awk > /dev/null && echo yes)
INSTALL_AWK=echo "Auto-generated help message requires 'awk' $(AWK_AVAILABLE), you can install awk from your package manager, e.g. 'apt-get install awk'"
endif

# Prints this help message
help:
ifeq (yes, $(AWK_AVAILABLE))
	@awk '\
	/^##/{ print $$0; doc = 0; next }\
	/^#/{ if (doc != 1) { comments = $$0; doc = 1 } else { comments = comments "\n\t" $$0 } next }\
	/^([^=])+[=]/{ if (doc == 1) { print "(variable) " $$0 "\n\t" comments "\n"; doc = 0 } next }\
	/^([^:])+:/{ if (doc == 1) { print $$1 "\n\t" comments "\n"; doc = 0 } next }\
	/./{ doc = 0 }\
	' Makefile $(wildcard etc/*.mk)
else
	@$(INSTALL_AWK)
endif