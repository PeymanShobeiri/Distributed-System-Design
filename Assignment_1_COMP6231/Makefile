SRC_DIR := src
BIN_DIR := bin
LOGS_CLIENT_DIR := $(SRC_DIR)/logs/client
LOGS_SERVER_DIR := $(SRC_DIR)/logs/servers
SOURCES := $(shell find $(SRC_DIR) -name '*.java')
MAIN_SERVER_CLASS := Server.Server
MAIN_CLIENT_CLASS := Client.Client

all: compile

compile:
	mkdir -p $(BIN_DIR)
	javac -d $(BIN_DIR) $(SOURCES)
	@echo "==> Compilation finished."

server: compile
	java -cp $(BIN_DIR) $(MAIN_SERVER_CLASS)

client: compile
	java -cp $(BIN_DIR) $(MAIN_CLIENT_CLASS)

clean:
	rm -rf $(BIN_DIR)
	rm -f $(LOGS_CLIENT_DIR)/*.txt $(LOGS_SERVER_DIR)/*.txt
	@echo "==> Done."

