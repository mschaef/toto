.PHONY: build
build:
	lein tar

.PHONY: run
run:
	lein cljfmt check
	lein run

.PHONY: format
format:
	lein cljfmt fix

.PHONY: clean
clean:
	lein clean

.PHONY: package
package:
	lein clean
	lein compile
	lein release patch

.PHONY: clean-data
clean-data:
	rm -rfv local-db/toto.*
