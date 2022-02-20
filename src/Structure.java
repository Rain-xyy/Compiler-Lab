/**
 * @Author: XiaYu
 * @Date 2021/12/23 20:05
 */
public class Structure extends Type {
    private String name;    //存储非匿名结构体的名字
    private FieldList memberList;   //存储其成员

    public Structure(){
        this.type = Kind.STRUCTURE;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }

    public void addMember(FieldList fieldList){
        if(memberList == null){
            memberList = fieldList;
        }else {
            memberList.add(fieldList);
        }
    }

    public FieldList getMemberList(){
        return memberList;
    }

    public boolean containMember(String name){
        FieldList current = memberList;
        while (current != null){
            if(current.getName().equals(name)){
                return true;
            }
            current = current.getNext();
        }
        return false;
    }

    public Type getFieldType(String name){
        FieldList current = memberList;
        while (current != null){
            if(current.getName().equals(name)){
                return current.getType();
            }
            current = current.getNext();
        }
        return null;
    }
}
