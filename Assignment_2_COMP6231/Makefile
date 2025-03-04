SRC_DIR := src
BIN_DIR := bin
SOURCES := $(shell find $(SRC_DIR) -name '*.java')
MAIN_SERVER_CLASS := Server.Server
MAIN_CLIENT_CLASS := Client.Client

all: compile

compile:
		mkdir -p $(BIN_DIR)
		javac -d $(BIN_DIR) $(SOURCES)

server: compile
		java -cp $(BIN_DIR) $(MAIN_SERVER_CLASS) -ORBInitialPort 900 -ORBInitialHost localhost

client: compile
		java -cp $(BIN_DIR) $(MAIN_CLIENT_CLASS) -ORBInitialPort 900 -ORBInitialHost localhost

clean:
		rm -rf $(BIN_DIR)
		@echo "==> Clean done."
