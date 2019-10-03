.PHONY: all test compile build clean docs

ORGANIZATION  := org.zerosign
PROJECT       := github4s
BUILDER_IMAGE := $(CI_REGISTRY)/zerosign/images/scala-mill:$(MILL_VERSION)
GITLAB_BRANCH := $(strip $(CI_COMMIT_REF_NAME))
GIT_BRANCH    := $(strip $(shell git rev-parse --abbrev-ref HEAD))
BRANCH        := $(if $(GITLAB_BRANCH),$(GITLAB_BRANCH),$(GIT_BRANCH))
MILL          := $(shell command -v mill 2> /dev/null)
SUBPROJECT    ?= _

export VERSION ?= $(strip $(shell git describe --tags))

all: compile test build docs

compile:
ifndef MILL
	docker container run --mount type=bind,src=$(CURRENT_DIR),dst=/home/developer/$(PROJECT) $(BUILDER_IMAGE) bash -c "mill ${SUBPROJECT}.compile"
else
	bash -c "mill ${SUBPROJECT}.compile"
endif

test:
# ifndef MILL
# 	docker container run --mount type=bind,src=$(CURRENT_DIR),dst=/home/developer/$(PROJECT) $(BUILDER_IMAGE) bash -c "mill ${SUBPROJECT}.test"
# else
# 	bash -c "mill ${SUBPROJECT}.test"
# endif

repl:
ifndef MILL
	docker container run --mount type=bind,src=$(CURRENT_DIR),dst=/home/developer/$(PROJECT) $(BUILDER_IMAGE) bash -c "mill ${SUBPROJECT}.repl"
else
	bash -c "mill ${SUBPROJECT}.repl"
endif

watch:
ifndef MILL
	docker container run --mount type=bind,src=$(CURRENT_DIR),dst=/home/developer/$(PROJECT) $(BUILDER_IMAGE) bash -c "mill $(SUBPROJECT).watch"
else
	bash -c "mill $(SUBPROJECT).watch"
endif

docs:
ifndef MILL
	docker container run --mount type=bind,src=$(CURRENT_DIR),dst=/home/developer/$(PROJECT) $(BUILDER_IMAGE) bash -c "mill $(SUBPROJECT).docJar"
else
	bash -c "mill $(SUBPROJECT).docJar"
endif
