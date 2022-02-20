/**
 * @Author: XiaYu
 * @Date 2021/12/23 20:05
 */
public class Function extends Type {
    private Type returnType;    //返回值类型
    private FieldList paramList;    //参数列表

    public Function(){
        this.type = Kind.FUNCTION;
    }

    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    public void addParamList(Type type, String name){
        FieldList fieldList = new FieldList(type, name);
        if(paramList == null){
            paramList = fieldList;
        }else {
            paramList.add(fieldList);
        }
    }

    public FieldList getParamList(){
        return paramList;
    }

    public Type getReturnType() {
        return returnType;
    }
}
