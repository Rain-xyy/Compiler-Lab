/**
 * @Author: XiaYu
 * @Date 2021/12/23 20:04
 */
public class Array extends Type {
    private int length;
    private Type elementType;

    public Array(){
        this.type = Kind.ARRAY;
    }

    public void setElementType(Type type){
        this.elementType = type;
    }

    public void setLength(int length){
        this.length = length;
    }

    public int getLength(){
        return length;
    }

    public Type getElementType(){
        return this.elementType;
    }
}
