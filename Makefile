IMAGE ?= meeny-backend
TAG   ?= local
NAME  ?= meeny-backend
PORT  ?= 8080

.PHONY: build run stop logs restart clean

build:
	docker build -t $(IMAGE):$(TAG) .

run: build
	docker rm -f $(NAME) 2>/dev/null || true
	docker run -d --name $(NAME) -p $(PORT):8080 $(IMAGE):$(TAG)
	@echo "running on http://localhost:$(PORT)"

stop:
	docker rm -f $(NAME) 2>/dev/null || true

logs:
	docker logs -f $(NAME)

restart: stop run

clean: stop
	docker rmi $(IMAGE):$(TAG) 2>/dev/null || true
