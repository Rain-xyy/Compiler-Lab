parser grammar CmmParser;

options {
    tokenVocab = CmmLexer;
}

program : extDef* EOF;

extDef : specifier extDecList SEMI  # ExtGlobalVarDec  //全局变量定义
        | specifier SEMI    # ExtEmptyAndStructDec
        | specifier funDec compSt   # ExtFunDec
        ;
extDecList : varDec (COMMA varDec)*;

specifier : TYPE
        | structSpecifier;
structSpecifier : STRUCT optTag LC defList RC
        | STRUCT tag;
optTag : ID
        |
        ;
tag : ID;

varDec : ID (LB INT RB)*;

funDec : ID LP varList RP
        | ID LP RP;

varList : paramDec (COMMA paramDec)*;
paramDec : specifier varDec;

compSt : LC defList stmtList RC;
stmtList : stmt*;
stmt : exp SEMI # ExpStmt
    | compSt    # CompStStmt
    | RETURN exp SEMI   # ReturnStmt
    | IF LP exp RP stmt # IFStmt
    | IF LP exp RP stmt ELSE stmt   # IFStmt
    | WHILE LP exp RP stmt # WhileStmt;

defList : def*;
def : specifier decList SEMI;
decList : dec (COMMA dec)*;
dec : varDec
    | varDec ASSIGNOP exp;

exp : LP exp RP         # BracketExp
    | exp LB exp RB     # ArrayAccessExp
    | exp DOT ID    # StructAccessExp
    | <assoc=right> (MINUS | NOT) exp   # MinusAndNotExp
    | exp (STAR | DIV) exp  # CalExp
    | exp (PLUS | MINUS) exp    # CalExp
    | exp RELOP exp # RelopExp
    | exp AND exp   # AndExp
    | exp OR exp    # OrExp
    | <assoc=right> exp ASSIGNOP exp    # AssignopExp
    | ID LP args RP # FunctionCallArgsExp
    | ID LP RP  # FunctionCallNoArgsExp
    | INT   # IntExp
    | FLOAT # FloatExp
    | ID   # IDExp;


args : exp (COMMA exp)*;
