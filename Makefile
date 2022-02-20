include Makefile.git


DOMAINNAME = 139.224.195.50:3000
ANTLR = java -jar /usr/local/lib/antlr-*-complete.jar -listener -visitor -long-messages
JAVAC = javac -g
JAVA = java


PFILE = $(shell find . -name "CmmParser.g4")
LFILE = $(shell find . -name "CmmLexer.g4")
JAVAFILE = $(shell find . -name "*.java")
ANTLRPATH = $(shell find /usr/local/lib -name "antlr-*-complete.jar")

compile: antlr
	$(call git_commit,"make")
	mkdir -p classes
	$(JAVAC) $(JAVAFILE) -d classes

run: compile
	java -classpath ./classes:$(ANTLRPATH) Main $(FILEPATH)


antlr: $(LFILE) $(PFILE) 
	$(ANTLR) $(PFILE) $(LFILE)


test: compile
	$(call git_commit, "test")
	cd classes && $(JAVA) Main ../tests/test1.cmm


clean:
	rm -f src/*.tokens
	rm -f src/*.interp
	rm -f src/CmmLexer.java src/CmmParser.java src/CmmParserBaseListener.java src/CmmParserBaseVisitor.java src/CmmParserListener.java src/CmmParserVisitor.java
	rm -rf classes


submit: clean
	git gc
	bash ./submit.sh


.PHONY: compile antlr test run clean submit


