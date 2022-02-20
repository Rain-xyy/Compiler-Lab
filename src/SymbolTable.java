import java.util.Hashtable;

/**
 * @Author: XiaYu
 * @Date 2021/12/23 20:35
 */
public class SymbolTable {
    public static SymbolTable symbolTable = new SymbolTable();
    public static Hashtable<String, Type> hashtable = new Hashtable<String,Type>();   //记录了符号名和类型信息的对应关系



    public void put(String s, Type type){
        hashtable.put(s, type);
    }

    public Type get(String s){
        return hashtable.get(s);
    }

    public boolean contains(String s){
        return hashtable.get(s) != null;
    }

    public Type remove(String s){
        return hashtable.remove(s);
    }

    private SymbolTable(){

    }

}
