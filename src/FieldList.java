/**
 * @Author: XiaYu
 * @Date 2021/12/23 20:10
 */
public class FieldList {
    private String name;    //成员名
    private Type type;      //成员类型
    private FieldList next;

    public FieldList(Type type, String name) {
        this.type = type;
        this.name = name;
    }

    public void add(FieldList fieldList) {
        FieldList current = this;
        while (current.next != null) {
            current = current.next;
        }
        current.next = fieldList;
    }

    public Type getType(){
        return type;
    }

    public int getLength(){
        int length = 1;
        FieldList current = this;
         while (current.next != null){
             current = current.next;
             length++;
         }
         return length;
    }

    public FieldList getNext(){
        return next;
    }

    public String getName(){
        return name;
    }
}
