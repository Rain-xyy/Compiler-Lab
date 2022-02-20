import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

/**
 * @Author: XiaYu
 * @Date 2021/12/1 23:22
 */
public class MyConsoleErrorListener extends ConsoleErrorListener {

    public static boolean hasError = false;

    public MyConsoleErrorListener() {
    }

    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        if(Main.hashSet.contains(line)){
            return;
        }
        Main.hashSet.add(line);
        String errorMessage = "Error type B at Line " + line + ": " + msg;
        System.err.println(errorMessage);
    }

}
