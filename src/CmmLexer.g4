lexer grammar CmmLexer;

SEMI : ';';
COMMA : ',';
ASSIGNOP : '=' ;
RELOP : '>' | '<' | '>=' | '<=' | '==' | '!=';
PLUS : '+' ;
MINUS : '-' ;
STAR : '*' ;
DIV : '/' ;
AND : '&&';
OR : '||';
DOT : '.';
NOT : '!';
TYPE : 'int' | 'float';
LP : '(';
RP : ')';
LB : '[';
RB : ']';
LC : '{';
RC : '}';
STRUCT : 'struct';
RETURN : 'return';
IF : 'if' ;
ELSE : 'else' ;
WHILE : 'while' ;


INT : DECIMAL | OCTAL | HEXADECIMAL;
FLOAT : (FRACTIONAL_CONSTANT EXPONENT) | (DIGIT_SEQUENCE '.' DIGIT_SEQUENCE);
ID : (LETTER | '_') WORD* ;
WS : [ \t\r\n]+ -> skip ;
SL_COMMENT : '//' .*? '\n' -> skip;
ML_COMMENT : '/*' .*? '*/' -> skip;

fragment
DIGIT : [0-9];
fragment
LETTER : [a-zA-Z];
fragment
WORD : [a-zA-Z0-9_];
fragment
FRACTIONAL_CONSTANT : (DIGIT_SEQUENCE? '.' DIGIT_SEQUENCE) | (DIGIT_SEQUENCE '.');
fragment
EXPONENT : ('e' | 'E') SIGN? DIGIT_SEQUENCE;
fragment
SIGN : PLUS | MINUS;
fragment
DIGIT_SEQUENCE : DIGIT+;
fragment
DECIMAL : '0' | ([1-9] [0-9]*);
fragment
OCTAL : '0' [0-7]+;
fragment
HEXADECIMAL : ('0x' | '0X')[0-9a-fA-F]+;
