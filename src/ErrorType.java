/**
 * @Author: XiaYu
 * @Date 2021/12/24 13:12
 */
public enum ErrorType {
    VarUndefined,   //变量在使用时未经定义
    FuncUndefined,  //函数在调用时未经定义
    VarRedefined,   //变量出现重复定义，或变量与前面定义过的结构体名字重复
    FunCRedefined,  //函数出现重复定义（即同样的函数名出现了不止一次定义）
    AssignMisMatch, //赋值号两边的表达式类型不匹配
    RightHandValue, //赋值号左边出现一个只有右值的表达式
    OptionMisMatch, //操作数类型不匹配或操作数类型与操作符不匹配（例如整型变量与数组变量相加减，或数组（或结构体）变量与数组（或结构体）变量相加减）
    ReturnMisMatch, //return语句的返回类型与函数定义的返回类型不匹配
    ArgsMisMatch,   //函数调用时实参与形参的数目或类型不匹配
    NotArray,       //对非数组型变量使用“[…]”（数组访问）操作符
    NotFunction,    //对普通变量使用“(…)”或“()”（函数调用）操作符,
    VisitArrayNotInteger,   //数组访问操作符“[…]”中出现非整数（例如a[1.5]）
    NotStructure,   //对非结构体型变量使用“.”操作符
    UndefinedInStructure, //访问结构体中未定义过的域
    FaultInStructure,   //结构体中域名重复定义（指同一结构体中），或在定义时对域进行初始化
    StructureRedefined, //结构体的名字与前面定义过的结构体或变量的名字重复
    StructureUndefined, //直接使用未定义过的结构体来定义变量
}
