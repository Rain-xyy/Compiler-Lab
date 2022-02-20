/**
 * @Author: XiaYu
 * @Date 2022/1/4 20:19
 */
public class Error extends Type {
    private Error(){
        this.type = Kind.ERROR;
    }

    public static Error error = new Error();
}
