import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

public class Main {
    public static HashSet<Integer> hashSet = new HashSet<Integer>();    //记录出错的行号
    public static void main(String[] args) {
        String path = args[0];
        String input = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line = "";
            while ((line = br.readLine()) != null) {
                input += line + '\n';
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        CmmLexer lexer = new CmmLexer(CharStreams.fromString(input)) {
            @Override
            public void notifyListeners(LexerNoViableAltException e) {
                String text = _input.getText(Interval.of(_tokenStartCharIndex, _input.index())).trim();
                String morphologyError = "Error type A at Line " + _tokenStartLine + ": Mysterious character " + "\"" + text + "\".";
                //System.err.println(morphologyError);
                throw e;
            }
        };

        List<? extends Token> tokens = lexer.getAllTokens();

        //这上面都是属于词法分析部分的

        ListTokenSource listTokenSource = new ListTokenSource(tokens);
        BufferedTokenStream tokenStream = new BufferedTokenStream(listTokenSource);
        CmmParser parser = new CmmParser(tokenStream) {

            @Override
            public void notifyErrorListeners(Token offendingToken, String msg, RecognitionException e) {
                MyConsoleErrorListener.hasError = true;
                //重写Parser类中的错误输出方法，以实现勘误备选分支
                if(!msg.equals("array size must be an integer constant, not ")) {
                    ++this._syntaxErrors;
                    int line = offendingToken.getLine();
                    int charPositionInLine = offendingToken.getCharPositionInLine();
                    ANTLRErrorListener listener = this.getErrorListenerDispatch();
                    listener.syntaxError(this, offendingToken, line, charPositionInLine, msg, e);
                }else{
                    int line;
                    String text;
                    int startIndex = offendingToken.getTokenIndex(); //当前为";"
                    //往前倒退，直到遇到TYPE
                    while(!CmmParser.VOCABULARY.getSymbolicName(this._input.get(--startIndex).getType()).equals("TYPE")){

                    }
                    for(int i = startIndex + 2; i < offendingToken.getTokenIndex(); i++){
                        Token t = this._input.get(i);
                        switch (CmmParser.VOCABULARY.getSymbolicName(t.getType())){
                            case "ID":
                            case "FLOAT":
                                ++this._syntaxErrors;
                                line = t.getLine();
                                text = t.getText();
                                if(hashSet.contains(line)){
                                    break;
                                }
                                hashSet.add(line);
                                System.err.println("Error type B at Line " + line + ": " + msg + text);
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        };

        //自定义错误信息
        List<? extends ANTLRErrorListener> errorListeners = parser.getErrorListeners();
        for(ANTLRErrorListener listener: errorListeners){
            if(listener instanceof ConsoleErrorListener){
                errorListeners.remove(listener);
            }
        }
        ANTLRErrorListener myConsoleErrorListener = new MyConsoleErrorListener();
        parser.addErrorListener(myConsoleErrorListener);

        ParseTree tree = parser.program();
        ParseTreeWalker walker = new ParseTreeWalker();
        if(!MyConsoleErrorListener.hasError) {
            MyListener listener = new MyListener();
            walker.walk(listener, tree);
        }
    }
}
