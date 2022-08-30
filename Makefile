.PHONY: build
build:
	lein tar

.PHONY: run
run:
	lein run

.PHONY: clean
clean:
	lein clean

.PHONY: package
package:
	lein clean
	lein release patch

.PHONY: clean-data
clean-data:
	rm -rfv local-db/toto.*
