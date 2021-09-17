.PHONY: build
build:
	lein tar

.PHONY: run
run:
	lein run

.PHONY: clean
clean:
	lein clean

.PHONY: clean-data
clean-data:
	rm -rfv local-db/toto.*
