import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;

//todo !! nullPointerException我感觉都是因为其它语法单元的错误导致了当前语法单元在需要访问时找不到出错的语法单元

/**
 * @Author: XiaYu
 * @Date 2021/12/1 20:43
 */

public class MyListener extends CmmParserBaseListener {
    ParseTreeProperty<Type> values = new ParseTreeProperty<>(); //向父节点传递变量类型
    IdentityHashMap<ParserRuleContext, Type> expValues = new IdentityHashMap<>();
    HashMap<String, Structure> structureDef = new HashMap<>();  //保存结构体的定义
    HashMap<Integer, Integer> errorMap = new HashMap<>();   //保存行号与错误类型的关系

    @Override
    public void exitExtGlobalVarDec(CmmParser.ExtGlobalVarDecContext ctx) { //todo !!! important 没有考虑数组、结构体
        Type type = values.get(ctx.specifier());    //得到类型
        if (type == null) {   //type为null，说明该类型的结构体未定义就使用
            return;
        }
        CmmParser.ExtDecListContext extDecList = ctx.extDecList();
        List<CmmParser.VarDecContext> varDecContextList = extDecList.varDec();
        for (CmmParser.VarDecContext varDecContext : varDecContextList) {
            String ID = varDecContext.ID().getText();
            if (SymbolTable.symbolTable.contains(ID) || structureDef.containsKey(ID)) {
                error3(varDecContext.ID());
            } else {
                //SymbolTable.symbolTable.put(ID, createType(type.type));     //todo 保存全局普通变量进入符号表
                SymbolTable.symbolTable.put(ID, getVarDecContextType(type, varDecContext));
            }
        }
    }


    @Override
    public void exitExtEmptyAndStructDec(CmmParser.ExtEmptyAndStructDecContext ctx) {

    }

    @Override
    public void enterExtFunDec(CmmParser.ExtFunDecContext ctx) {
        //检查函数是否重定义
        if (SymbolTable.symbolTable.contains(ctx.funDec().ID().getText()) || structureDef.containsKey(ctx.funDec().ID().getText())) {
            error4(ctx.funDec().ID());
            ctx.children = null;    //清空该函数
        }
    }

    @Override
    public void exitExtFunDec(CmmParser.ExtFunDecContext ctx) { //函数
        if (ctx.children == null) {
            return;
        }
        Type type = values.get(ctx.specifier());    //type里面保存的是函数的返回值类型
        String ID = ctx.funDec().ID().getText();
        Function realType = (Function) SymbolTable.symbolTable.get(ID);
        realType.setReturnType(type);   //返回引用，直接修改即可
    }


    @Override
    public void enterStructSpecifier(CmmParser.StructSpecifierContext ctx) {
        TerminalNode node = getNonAnonymousStructureID(ctx);    //保存ID的节点
        String ID;
        if (node == null) {
            return;     //todo opTag为空
        }
        ID = node.getText();

        if ((ctx.optTag() != null)) { //创建一种新的结构体类型
            if (structureDef.containsKey(ID) || SymbolTable.symbolTable.contains(ID)) {   //结构体定义重复
                error16(node.getSymbol().getLine(), ID);
                ctx.children = null;
            }
        }
    }

    @Override
    public void exitStructSpecifier(CmmParser.StructSpecifierContext ctx) {     //匿名结构体以""的形式进入结构体定义表，且允许被覆盖，因为匿名结构体至多只用一次
        if (ctx.children == null) { //结构体名字重复，直接返回
            return;
        }

        String ID;
        TerminalNode node = getNonAnonymousStructureID(ctx);    //保存ID的节点
        if (node == null) {
            ID = "";     //todo opTag为空
        } else {
            ID = node.getText();
        }

//        if (!(ctx.parent.parent instanceof CmmParser.ExtDefContext)) { //todo important 非结构体定义直接返回，因为如果这里是非结构体定义，那么就说明这是用来创建新的结构体而不是定义一种新的结构体
//            return;                                                    //修改：非结构定义也可能是在结构体或函数内部创建新的结构体
//        }

        Structure realType = new Structure();
        if (ctx.parent.parent instanceof CmmParser.ExtFunDecContext) {             //这边的StructSpecifierContext不是新定义一种结构体类型，而是函数返回值类型
            if (this.structureDef.get(ID) == null) { //当前结构体未定义
                return;
            } else {
                Structure type = structureDef.get(ID);
                FieldList memberList = type.getMemberList();
                while (memberList != null) {
                    realType.addMember(new FieldList(memberList.getType(), memberList.getName()));
                    memberList = memberList.getNext();
                }
            }
        } else if (ctx.parent.parent instanceof CmmParser.ExtEmptyAndStructDecContext && ctx.optTag() != null) {     //当前为结构体定义
            //还需要设置域内变量
            CmmParser.DefListContext defListContext = ctx.defList();
            if (defListContext == null) {    //结构体定义内无参数

            } else {
                List<CmmParser.DefContext> defContextList = defListContext.def();
                for (CmmParser.DefContext defContext : defContextList) {
                    CmmParser.DecListContext decListContext = defContext.decList();
                    List<CmmParser.DecContext> decContextList = decListContext.dec();

                    for (CmmParser.DecContext decContext : decContextList) {
//                        if(decContext.getChildCount() > 1 && decContext.getChild(1).getText().equals("=")){     //结构体中的变量在定义时赋值，直接丢弃该变量 改变到deflist中判断，不然到这里判断太迟了点
//                            error15(decContext.start.getLine(), decContext.varDec().getText());
//                            SymbolTable.symbolTable.remove(decContext.varDec().ID().getText());
//                            continue;
//                        }
                        CmmParser.VarDecContext varDecContext = decContext.varDec();
                        String subName = varDecContext.ID().getSymbol().getText(); //name表示结构体中的变量名
                        Type subType = SymbolTable.symbolTable.get(subName);
                        FieldList fieldList = new FieldList(subType, subName);
                        realType.addMember(fieldList);
                    }
                }
            }
            structureDef.put(ID, realType);  //todo 结构体的定义既进入符号表，用专门的区域来保存
            //SymbolTable.symbolTable.put(ID, realType);  //符号表中也保存一份，防止名字重复
        } else if (ctx.parent.parent instanceof CmmParser.ExtGlobalVarDecContext) {
            if (ctx.optTag() != null) {   //STRUCT optTag LC defList RC，要加入结构体定义表
                CmmParser.DefListContext defListContext = ctx.defList();
                if (defListContext == null) {    //结构体定义内无参数

                } else {
                    List<CmmParser.DefContext> defContextList = defListContext.def();
                    for (CmmParser.DefContext defContext : defContextList) {
                        CmmParser.DecListContext decListContext = defContext.decList();
                        List<CmmParser.DecContext> decContextList = decListContext.dec();

                        for (CmmParser.DecContext decContext : decContextList) {
//                        if(decContext.getChildCount() > 1 && decContext.getChild(1).getText().equals("=")){     //结构体中的变量在定义时赋值，直接丢弃该变量 改变到deflist中判断，不然到这里判断太迟了点
//                            error15(decContext.start.getLine(), decContext.varDec().getText());
//                            SymbolTable.symbolTable.remove(decContext.varDec().ID().getText());
//                            continue;
//                        }
                            CmmParser.VarDecContext varDecContext = decContext.varDec();
                            String subName = varDecContext.ID().getSymbol().getText(); //name表示结构体中的变量名
                            Type subType = SymbolTable.symbolTable.get(subName);
                            FieldList fieldList = new FieldList(subType, subName);
                            realType.addMember(fieldList);
                        }
                    }
                }
                structureDef.put(ID, realType);  //todo 结构体的定义既进入符号表，用专门的区域来保存
            }
        } else {  //在结构体或者函数内部
            if (ctx.optTag() != null) {   //STRUCT optTag LC defList RC，要加入结构体定义表
                CmmParser.DefListContext defListContext = ctx.defList();
                if (defListContext == null) {    //结构体定义内无参数

                } else {
                    List<CmmParser.DefContext> defContextList = defListContext.def();
                    for (CmmParser.DefContext defContext : defContextList) {
                        CmmParser.DecListContext decListContext = defContext.decList();
                        List<CmmParser.DecContext> decContextList = decListContext.dec();

                        for (CmmParser.DecContext decContext : decContextList) {
//                        if(decContext.getChildCount() > 1 && decContext.getChild(1).getText().equals("=")){     //结构体中的变量在定义时赋值，直接丢弃该变量 改变到deflist中判断，不然到这里判断太迟了点
//                            error15(decContext.start.getLine(), decContext.varDec().getText());
//                            SymbolTable.symbolTable.remove(decContext.varDec().ID().getText());
//                            continue;
//                        }
                            CmmParser.VarDecContext varDecContext = decContext.varDec();
                            String subName = varDecContext.ID().getSymbol().getText(); //name表示结构体中的变量名
                            Type subType = SymbolTable.symbolTable.get(subName);
                            FieldList fieldList = new FieldList(subType, subName);
                            realType.addMember(fieldList);
                        }
                    }
                }
                structureDef.put(ID, realType);  //todo 结构体的定义既进入符号表，用专门的区域来保存
            }
        }
        values.put(ctx, realType);  //要写入传给父节点
    }


    @Override
    public void exitSpecifier(CmmParser.SpecifierContext ctx) {
        String text = ctx.getText();
        Type type;
        if (ctx.getChild(0) instanceof TerminalNode) {        //int/float
            type = getType(text);
        } else {     //结构体类型
            if (ctx.structSpecifier().children == null) { //结构体重定义直接返回
                return;
            }

            TerminalNode node = getNonAnonymousStructureID(ctx.structSpecifier());
            String ID;
            if (node == null) {
                ID = "";     //optag为空
            } else {
                ID = node.getText();
            }
            type = new Structure();
            if (!structureDef.containsKey(ID)) {
                error17(ctx.getStart().getLine(), ID);
                return;
            }
            Structure defType = structureDef.get(ID);
            FieldList memberList = defType.getMemberList();
            while (memberList != null) {
                ((Structure) type).addMember(new FieldList(memberList.getType(), memberList.getName()));
                memberList = memberList.getNext();
            }
        }
        values.put(ctx, type);
    }


    @Override
    public void exitFunDec(CmmParser.FunDecContext ctx) {
        CmmParser.ExtFunDecContext extFunDecContext = (CmmParser.ExtFunDecContext) ctx.parent;
        Type returnType = values.get(extFunDecContext.specifier());
        Function realType = new Function(); //此节点看不到返回值的类型，让其父节点去补充返回值的类型，它可以看到参数的类型，以及函数名 !!!更正前面的话，因为有可能发生递归调用，所以不能等到整个函数都定义完了才设置其返回值类型，必须在这里设置
        realType.setReturnType(returnType);
        if (ctx.varList() != null) {
            for (CmmParser.ParamDecContext paramDecContext : ctx.varList().paramDec()) {
                //对每一个参数做处理
                if (values.get(paramDecContext) == null) {    //说明当前参数与之前某个符号重定义
                    continue;
                }
                realType.addParamList(values.get(paramDecContext), paramDecContext.varDec().ID().getText());
            }
        }
        SymbolTable.symbolTable.put(ctx.ID().getText(), realType);  //保存函数定义进入符号表
        expValues.put(ctx, realType);
    }


    @Override
    public void exitParamDec(CmmParser.ParamDecContext ctx) {
        CmmParser.VarDecContext varDecContext = ctx.varDec();
        String ID = varDecContext.ID().getText();
        if (SymbolTable.symbolTable.contains(ID) || structureDef.containsKey(ID)) {
            error3(varDecContext.ID());
        } else {
//            SymbolTable.symbolTable.put(ID, createType(type.type)); //todo 保存参数进入符号表
//            values.put(ctx, createType(type.type));
            SymbolTable.symbolTable.put(ID, getParamDecType(ctx));
            values.put(ctx, getParamDecType(ctx));
        }
    }


    @Override
    public void exitReturnStmt(CmmParser.ReturnStmtContext ctx) {   //递归要特殊处理，因为在递归时returnType()会始终为null
        //return语句的父节点是stmtList，再父节点是compSt,再父节点是extDef，这就是函数定义的语法单元
        ParserRuleContext extFunDecContext = (ParserRuleContext) ctx.parent;
        while (!(extFunDecContext instanceof CmmParser.ExtFunDecContext)) {
            extFunDecContext = (ParserRuleContext) extFunDecContext.parent;
        }
        if (ctx.exp().start.getText().equals(((CmmParser.ExtFunDecContext) extFunDecContext).funDec().ID().getText())) {
            return;     //递归调用，返回值类型肯定一致
        }

        Type returnType = expValues.get(ctx.exp());
        Type realTurnType = values.get(((CmmParser.ExtFunDecContext) extFunDecContext).specifier());

        if (returnType.type == Kind.ERROR) {  //todo 返回语句中的exp有问题
            return;
        }

        if (realTurnType.type != returnType.type) {
            error8(ctx.getStart().getLine());
        } else if (realTurnType.type == Kind.STRUCTURE) {     //如果是结构体类型要特殊判断，要深入到每一个结构体当中去
            //todo
            if (!structIsEqual((Structure) realTurnType, (Structure) returnType)) {
                //结构体不结构等价
                error8(ctx.getStart().getLine());
            }
        }
    }


    @Override
    public void exitWhileStmt(CmmParser.WhileStmtContext ctx) {
        super.exitWhileStmt(ctx);
    }

    @Override
    public void enterDefList(CmmParser.DefListContext ctx) {
        super.enterDefList(ctx);
    }

    @Override
    public void exitDefList(CmmParser.DefListContext ctx) {
        super.exitDefList(ctx);
    }

    @Override
    public void enterDef(CmmParser.DefContext ctx) {
        super.enterDef(ctx);
    }

    @Override
    public void exitDef(CmmParser.DefContext ctx) { //处理全部变量和局部变量的定义
        Type type = values.get(ctx.specifier());    //得到类型
        CmmParser.DecListContext decListContext = ctx.decList();
        List<CmmParser.DecContext> decContextList = decListContext.dec();
        for (CmmParser.DecContext dec : decContextList) {   //处理每一个局部变量
            CmmParser.VarDecContext varDec = dec.varDec();
            String ID = varDec.ID().getText();
            if (SymbolTable.symbolTable.contains(ID) || structureDef.containsKey(ID)) { //变量重定义
                if (ctx.parent.parent instanceof CmmParser.StructSpecifierContext) {    //结构体中的变量重复定义
                    error15(varDec.ID());
                } else {    //普通变量重复定义
                    error3(varDec.ID());
                }
            } else {
                if (dec.getChildCount() > 1 && dec.getChild(1).getText().equals("=") && dec.parent.parent.parent.parent instanceof CmmParser.StructSpecifierContext) {    //结构体中变量定义时赋值
                    error15(dec.start.getLine(), dec.varDec().getText());
                    //continue  //todo 这边不确定是否要continue，即不把定义赋值的变量加入符号表
                }
                if (varDec.getChildCount() == 1) {
                    //SymbolTable.symbolTable.put(ID, createType(type.type)); //todo 保存普通变量或结构体的定义进入符号表
                    if (type == null) {   //结构体类型去创建新的结构体变量，如果该结构体类型未定义则不会将该变量加入符号表
                        return;
                    }
                    SymbolTable.symbolTable.put(ID, type);
                    if (type.type == Kind.STRUCTURE) {    //结构体类型
                        Structure struct = (Structure) SymbolTable.symbolTable.get(ID);
                        //String name = getNonAnonymousStructureID(ctx.specifier().structSpecifier()).getSymbol().getText();
                        TerminalNode node = getNonAnonymousStructureID(ctx.specifier().structSpecifier());  //todo opTag为空
                        String name;
                        if (node == null) {
                            return;
                        } else {
                            name = node.getSymbol().getText();
                        }
                        struct.setName(name);       //设置名字，这里的名字是结构体定义的名字，而不是此结构体的名字
//                        Structure structureDef;       //下面的都在specifier里面做过了
//                        if ((structureDef = this.structureDef.get(name)) == null) { //当前结构体未定义
//                            error17(ctx.specifier().start.getLine(), name);
//                            return;
//                        }
//                        FieldList memberList = structureDef.getMemberList();
//                        while (memberList != null) {
//                            struct.addMember(new FieldList(memberList.getType(), memberList.getName()));
//                            memberList = memberList.getNext();
//                        }

                    }
                } else { //数组定义  //todo 设置数组的长度
                    List<TerminalNode> ints = varDec.INT(); //数组长度
                    Array res = new Array();
                    for (int i = ints.size() - 1; i >= 0; i--) {
                        if (i == ints.size() - 1) {
                            res.setElementType(type);   //最外层的数组元素
                            res.setLength(Integer.parseInt(ints.get(i).getText()));
                            continue;
                        }
                        Array temp = new Array();
                        temp.setLength(Integer.parseInt(ints.get(i).getText()));    //数组内层
                        temp.setElementType(res);
                        res = temp;
                    }
                    SymbolTable.symbolTable.put(ID, res); //todo 保存数组定义进入符号表
                }
            }
        }
    }

    @Override
    public void enterDecList(CmmParser.DecListContext ctx) {
        super.enterDecList(ctx);
    }

    @Override
    public void exitDecList(CmmParser.DecListContext ctx) {
        super.exitDecList(ctx);
    }

    @Override
    public void enterDec(CmmParser.DecContext ctx) {
        super.enterDec(ctx);
    }

    @Override
    public void exitDec(CmmParser.DecContext ctx) {
        super.exitDec(ctx);
    }

    @Override
    public void enterAndExp(CmmParser.AndExpContext ctx) {
        super.enterAndExp(ctx);
    }

    @Override
    public void exitAndExp(CmmParser.AndExpContext ctx) {   //todo 不确定是否是要放入int
//        super.exitAndExp(ctx);
        CmmParser.ExpContext left = ctx.exp(0);
        CmmParser.ExpContext right = ctx.exp(1);
        if (expValues.get(left) == Error.error) {    //左边未定义（已经在其它地方报过错

        } else if (expValues.get(right) == Error.error) {

        } else if (expValues.get(left).type != expValues.get(right).type || expValues.get(left).type != Kind.INT || expValues.get(right).type != Kind.INT) {
            error7(left.getStart().getLine());
        } else {
            //expValues.put(ctx, createType(expValues.get(left).type));
            expValues.put(ctx, new Int());
            return;
        }
        expValues.put(ctx, Error.error);
    }

    @Override
    public void enterStructAccessExp(CmmParser.StructAccessExpContext ctx) {
        super.enterStructAccessExp(ctx);
    }

    @Override
    public void exitStructAccessExp(CmmParser.StructAccessExpContext ctx) {
        CmmParser.ExpContext expContext = ctx.exp();
        if (expValues.get(expContext) == Error.error) {

        } else if (!(expValues.get(expContext) instanceof Structure)) { //对非结构体型变量使用"."操作符
            error13(ctx.start.getLine());
        } else if (!((Structure) expValues.get(expContext)).containMember(ctx.ID().getText())) {    //使用了结构体中未定义的变量
            error14(ctx.start.getLine(), ctx.ID().getSymbol().getText());
        } else {
            Structure structureType = (Structure) expValues.get(expContext);
            Type type = structureType.getFieldType(ctx.ID().getText());
            expValues.put(ctx, type);
            return;
        }
        expValues.put(ctx, Error.error);
    }

    @Override
    public void enterAssignopExp(CmmParser.AssignopExpContext ctx) {
        super.enterAssignopExp(ctx);
    }

    @Override
    public void exitAssignopExp(CmmParser.AssignopExpContext ctx) {
        //todo 判断类型是否兼容
        CmmParser.ExpContext left = ctx.exp(0);
        CmmParser.ExpContext right = ctx.exp(1);
        if (expValues.get(left) == Error.error) {
            //左边有未经定义使用的
        } else if (expValues.get(right) == Error.error) {

        } else if (!isLeftValue(left)) {
            error6(left.getStart().getLine());
            //expValues.put(ctx, createType(expValues.get(left).type));
//            expValues.put(ctx, expValues.get(left));
        } else if (expValues.get(left).type != expValues.get(right).type) {
            error5(left.getStart().getLine());
        } else if (expValues.get(left).type == Kind.STRUCTURE) {
            if (structIsEqual((Structure) expValues.get(left), (Structure) expValues.get(right))) { //结构体结构等价
                expValues.put(ctx, expValues.get(left));
                return;
            } else {
                error5(left.getStart().getLine());
            }
        } else if (expValues.get(left).type == Kind.ARRAY) {
            if (arrayIsEqual((Array) expValues.get(left), (Array) expValues.get(right))) { //结构体结构等价
                expValues.put(ctx, expValues.get(left));
                return;
            } else {
                error5(left.getStart().getLine());
            }
        } else {
            expValues.put(ctx, expValues.get(left));
            return;
        }
        expValues.put(ctx, Error.error);
    }

    @Override
    public void enterArrayAccessExp(CmmParser.ArrayAccessExpContext ctx) {
        super.enterArrayAccessExp(ctx);
    }

    @Override
    public void exitArrayAccessExp(CmmParser.ArrayAccessExpContext ctx) {
        //CmmParser.IDExpContext idExp = (CmmParser.IDExpContext) ctx.exp(0);
        //考虑到这边可能是多维数组，还可能是结构体中的数组，所以不能直接取id，要不断深入下去，直到其为ID类型
        CmmParser.ExpContext temp = ctx.exp(0);
        CmmParser.ExpContext expContext = ctx.exp(1);
        Type type = expValues.get(temp);    //获取数组访问左半部分的类型
        if (type == Error.error) {

        } else if (!(type instanceof Array)) {   //对非数组型变量使用“[…]”（数组访问）操作符
            error10(temp.start.getLine(), temp.getText());
        } else if (!(expValues.get(expContext) instanceof Int) && expValues.get(expContext) != Error.error) {     //数组访问操作符“[…]”中出现非整数（例如a[1.5]）
            error12(expContext.start.getLine(), expContext.getText());
        } else {
            //expValues.put(ctx, createType(((Array) SymbolTable.symbolTable.get(idExp.ID().getText())).getElementType().type)); //对于exp类型的，必须要放进去，不然可能出现NullException
            expValues.put(ctx, ((Array) type).getElementType());
            return;
        }
        expValues.put(ctx, Error.error);

//        while (!(temp instanceof CmmParser.IDExpContext)){
//            temp = ((CmmParser.ArrayAccessExpContext)temp).exp(0);
//        }
//        CmmParser.IDExpContext idExp = (CmmParser.IDExpContext)temp;
//        CmmParser.ExpContext expContext = ctx.exp(1);
//        if (!(SymbolTable.symbolTable.get(idExp.ID().getText()) instanceof Array)) {    //对非数组型变量使用“[…]”（数组访问）操作符
//            error10(idExp.ID().getSymbol().getLine(), idExp.ID().getText());
//        } else if (!(expContext instanceof CmmParser.IntExpContext)) {     //数组访问操作符“[…]”中出现非整数（例如a[1.5]）
//            error12(expContext.start.getLine(), expContext.getText());
//        } else {
//            //expValues.put(ctx, createType(((Array) SymbolTable.symbolTable.get(idExp.ID().getText())).getElementType().type)); //对于exp类型的，必须要放进去，不然可能出现NullException
//            expValues.put(ctx, ((Array) SymbolTable.symbolTable.get(idExp.ID().getText())).getElementType());
//        }
    }

    @Override
    public void enterIDExp(CmmParser.IDExpContext ctx) {
        super.enterIDExp(ctx);
    }

    @Override
    public void exitIDExp(CmmParser.IDExpContext ctx) {
        Type type = SymbolTable.symbolTable.get(ctx.ID().getText());
        if (type == null) {
            //当前变量未定义
            error1(ctx.ID());
        } else {
            //expValues.put(ctx, createType(type.type)); //标记当前表达式的类型        //todo !important
            expValues.put(ctx, type);
            return;
        }
        expValues.put(ctx, Error.error);
    }

    @Override
    public void enterFloatExp(CmmParser.FloatExpContext ctx) {
        super.enterFloatExp(ctx);
    }

    @Override
    public void exitFloatExp(CmmParser.FloatExpContext ctx) {
        expValues.put(ctx, new Float());
    }

    @Override
    public void enterBracketExp(CmmParser.BracketExpContext ctx) {
        super.enterBracketExp(ctx);
    }

    @Override
    public void exitBracketExp(CmmParser.BracketExpContext ctx) {
        //expValues.put(ctx, createType(expValues.get(ctx.exp()).type));
        expValues.put(ctx, expValues.get(ctx.exp()));
    }

    @Override
    public void enterRelopExp(CmmParser.RelopExpContext ctx) {
        super.enterRelopExp(ctx);
    }

    @Override
    public void exitRelopExp(CmmParser.RelopExpContext ctx) {   //todo 我也不知道对不对
//        super.exitRelopExp(ctx);
        expValues.put(ctx, new Int());  //比较表达式的值为int类型
    }

    @Override
    public void enterOrExp(CmmParser.OrExpContext ctx) {
        super.enterOrExp(ctx);
    }

    @Override
    public void exitOrExp(CmmParser.OrExpContext ctx) {
//        super.exitOrExp(ctx);
        expValues.put(ctx, new Int());
    }

    @Override
    public void exitFunctionCallArgsExp(CmmParser.FunctionCallArgsExpContext ctx) {
        String ID = ctx.ID().getText();
        if (SymbolTable.symbolTable.get(ID) == null) {  //检查该函数有没有定义过
            error2(ctx.ID());
            expValues.put(ctx, Error.error);
        } else if (!(SymbolTable.symbolTable.get(ID) instanceof Function)) {    //不是函数
            error11(ctx.ID().getSymbol().getLine(), ID);
            expValues.put(ctx, Error.error);
        } else {
            CmmParser.ArgsContext argsContext = ctx.args();
            List<CmmParser.ExpContext> expContextList = argsContext.exp();  //实参列表
            boolean applicable = true;
            FieldList paramList = ((Function) SymbolTable.symbolTable.get(ID)).getParamList();
            if (paramList == null || (Objects.requireNonNull(paramList).getLength() != expContextList.size())) {    //实参与形参长度是否相等
                applicable = false;
            } else {
                for (CmmParser.ExpContext expContext : expContextList) {
                    if (expValues.get(expContext).type != paramList.getType().type) { //实参形参类型不匹配
                        applicable = false;
                    } else if (expValues.get(expContext).type == Kind.STRUCTURE && !structIsEqual((Structure) expValues.get(expContext), (Structure) paramList.getType())) {
                        applicable = false;
                    }
                    paramList = paramList.getNext();
                }
            }
            if (!applicable) {    //参数类型或个数不匹配
                paramList = ((Function) SymbolTable.symbolTable.get(ID)).getParamList();
                String realFunDescription = ID + "(";
                String fakeFunDescription = ID + "(";
                while (paramList != null) {
                    realFunDescription += paramList.getType().type.toString().toLowerCase() + ", ";
                    paramList = paramList.getNext();
                }
                realFunDescription = realFunDescription.substring(0, realFunDescription.length() - 2);
                realFunDescription += ")";
                for (CmmParser.ExpContext expContext : expContextList) {
                    fakeFunDescription += expValues.get(expContext).type.toString().toLowerCase() + ", ";
                }
                fakeFunDescription = fakeFunDescription.substring(0, fakeFunDescription.length() - 2);
                fakeFunDescription += ")";
                String s = "Function \"" + realFunDescription + "\"" + " is not applicable for arguments \"" + fakeFunDescription + "\"";
                error9(ctx.start.getLine(), s);
            }
            //expValues.put(ctx, createType(((Function) SymbolTable.symbolTable.get(ID)).getReturnType().type));
            expValues.put(ctx, ((Function) SymbolTable.symbolTable.get(ID)).getReturnType());

        }
    }


    @Override
    public void enterMinusAndNotExp(CmmParser.MinusAndNotExpContext ctx) {
        super.enterMinusAndNotExp(ctx);
    }

    @Override
    public void exitMinusAndNotExp(CmmParser.MinusAndNotExpContext ctx) {
//        super.exitMinusAndNotExp(ctx);
        expValues.put(ctx, expValues.get(ctx.exp()));
    }

    @Override
    public void enterCalExp(CmmParser.CalExpContext ctx) {
        super.enterCalExp(ctx);
    }

    @Override
    public void exitCalExp(CmmParser.CalExpContext ctx) {
        CmmParser.ExpContext left = ctx.exp(0);
        CmmParser.ExpContext right = ctx.exp(1);
        if (expValues.get(left) == Error.error) {    //左边未定义（已经在其它地方报过错

        } else if (expValues.get(right) == Error.error) {

        } else if (expValues.get(left).type != expValues.get(right).type) {
            error7(left.getStart().getLine());
        } else {
            //expValues.put(ctx, createType(expValues.get(left).type));
            expValues.put(ctx, expValues.get(left));
            return;
        }
        expValues.put(ctx, Error.error);
    }

    @Override
    public void enterFunctionCallNoArgsExp(CmmParser.FunctionCallNoArgsExpContext ctx) {
        super.enterFunctionCallNoArgsExp(ctx);
    }

    @Override
    public void exitFunctionCallNoArgsExp(CmmParser.FunctionCallNoArgsExpContext ctx) {
        String ID = ctx.ID().getText();
        if (SymbolTable.symbolTable.get(ID) == null) {  //检查该函数有没有定义过
            error2(ctx.ID());
            expValues.put(ctx, Error.error);
        } else if (!(SymbolTable.symbolTable.get(ID) instanceof Function)) {    //不是函数
            error11(ctx.ID().getSymbol().getLine(), ID);
            expValues.put(ctx, Error.error);
        } else {
            boolean applicable = true;
            FieldList paramList = ((Function) SymbolTable.symbolTable.get(ID)).getParamList();
            if (paramList != null) {
                applicable = false;
            }

            if (!applicable) {    //参数类型或个数不匹配
                paramList = ((Function) SymbolTable.symbolTable.get(ID)).getParamList();
                String realFunDescription = ID + "(";
                String fakeFunDescription = ID + "(";
                while (paramList != null) {
                    realFunDescription += paramList.getType().type.toString().toLowerCase() + ", ";
                    paramList = paramList.getNext();
                }
                realFunDescription = realFunDescription.substring(0, realFunDescription.length() - 2);
                realFunDescription += ")";

                fakeFunDescription = fakeFunDescription.substring(0, fakeFunDescription.length() - 2);
                fakeFunDescription += ")";
                String s = "Function \"" + realFunDescription + "\"" + " is not applicable for arguments \"" + fakeFunDescription + "\"";
                error9(ctx.start.getLine(), s);
            }
            //expValues.put(ctx, createType(((Function) SymbolTable.symbolTable.get(ID)).getReturnType().type));
            expValues.put(ctx, ((Function) SymbolTable.symbolTable.get(ID)).getReturnType());

        }
    }

    @Override
    public void enterIntExp(CmmParser.IntExpContext ctx) {
        super.enterIntExp(ctx);
    }

    @Override
    public void exitIntExp(CmmParser.IntExpContext ctx) {
        expValues.put(ctx, new Int());
    }

    @Override
    public void enterArgs(CmmParser.ArgsContext ctx) {
        super.enterArgs(ctx);
    }

    @Override
    public void exitArgs(CmmParser.ArgsContext ctx) {
        super.exitArgs(ctx);
    }

    @Override
    public void enterEveryRule(ParserRuleContext ctx) {
        super.enterEveryRule(ctx);
    }

    @Override
    public void exitEveryRule(ParserRuleContext ctx) {
        super.exitEveryRule(ctx);
    }

    @Override
    public void visitTerminal(TerminalNode node) {
        super.visitTerminal(node);
    }

    @Override
    public void visitErrorNode(ErrorNode node) {
        super.visitErrorNode(node);
    }

    public Type getType(String text) {
        switch (text) {
            case "int":
                return new Int();
            case "float":
                return new Float();
            default:
                return null;
        }
    }


    public void error1(TerminalNode node) {
//        if (errorMap.containsKey(node.getSymbol().getLine()) && errorMap.get(node.getSymbol().getLine()) < 1) {
//            return;
//        } else {
//            errorMap.put(node.getSymbol().getLine(), 1);
//        }
        String msg = "Error type 1 at Line " + node.getSymbol().getLine() + ": " + "Undefined variable \"" + node.getText() + "\"";
        printErr(msg);
    }

    public void error2(TerminalNode node) {
//        if (errorMap.containsKey(node.getSymbol().getLine()) && errorMap.get(node.getSymbol().getLine()) < 2) {
//            return;
//        } else {
//            errorMap.put(node.getSymbol().getLine(), 2);
//        }
        String msg = "Error type 2 at Line " + node.getSymbol().getLine() + ": " + "Undefined function \"" + node.getText() + "\"";
        printErr(msg);
    }

    public void error3(TerminalNode node) {
//        if (errorMap.containsKey(node.getSymbol().getLine()) && errorMap.get(node.getSymbol().getLine()) < 3) {
//            return;
//        } else {
//            errorMap.put(node.getSymbol().getLine(), 3);
//        }
        String msg = "Error type 3 at Line " + node.getSymbol().getLine() + ": " + "Redefined variable \"" + node.getText() + "\"";
        printErr(msg);
    }

    public void error4(TerminalNode node) {
//        if (errorMap.containsKey(node.getSymbol().getLine()) && errorMap.get(node.getSymbol().getLine()) < 4) {
//            return;
//        } else {
//            errorMap.put(node.getSymbol().getLine(), 4);
//        }
        String msg = "Error type 4 at Line " + node.getSymbol().getLine() + ": " + "Redefined function \"" + node.getText() + "\"";
        printErr(msg);
    }

    public void error5(int line) {
//        if (errorMap.containsKey(line) && errorMap.get(line) < 5) {
//            return;
//        } else {
//            errorMap.put(line, 5);
//        }
        String msg = "Error type 5 at Line " + line + ": " + "Type mismatched for assignment";
        printErr(msg);
    }

    public void error6(int line) {
//        if (errorMap.containsKey(line) && errorMap.get(line) < 6) {
//            return;
//        } else {
//            errorMap.put(line, 6);
//        }
        String msg = "Error type 6 at Line " + line + ": " + "The left-hand side of an assignment must be a variable";
        printErr(msg);
    }

    public void error7(int line) {
//        if (errorMap.containsKey(line) && errorMap.get(line) < 7) {
//            return;
//        } else {
//            errorMap.put(line, 7);
//        }
        String msg = "Error type 7 at Line " + line + ": " + "Type mismatched for operands";
        printErr(msg);
    }

    public void error8(int line) {
//        if (errorMap.containsKey(line) && errorMap.get(line) < 8) {
//            return;
//        } else {
//            errorMap.put(line, 8);
//        }
        String msg = "Error type 8 at Line " + line + ": " + "Type mismatched for return";
        printErr(msg);
    }

    public void error9(int line, String s) {
//        if (errorMap.containsKey(line) && errorMap.get(line) < 9) {
//            return;
//        } else {
//            errorMap.put(line, 9);
//        }
        String msg = "Error type 9 at Line " + line + ": " + s;
        printErr(msg);
    }

    public void error10(int line, String s) {
//        if (errorMap.containsKey(line) && errorMap.get(line) < 10) {
//            return;
//        } else {
//            errorMap.put(line, 10);
//        }
        String msg = "Error type 10 at Line " + line + ": " + "\"" + s + "\"" + " is not an array";
        printErr(msg);
    }

    public void error11(int line, String s) {
//        if (errorMap.containsKey(line) && errorMap.get(line) < 11) {
//            return;
//        } else {
//            errorMap.put(line, 11);
//        }
        String msg = "Error type 11 at Line " + line + ": " + "\"" + s + "\"" + " is not a function";
        printErr(msg);
    }

    public void error12(int line, String s) {
//        if (errorMap.containsKey(line) && errorMap.get(line) < 12) {
//            return;
//        } else {
//            errorMap.put(line, 12);
//        }
        String msg = "Error type 12 at Line " + line + ": " + "\"" + s + "\"" + " is not an integer";
        printErr(msg);
    }

    public void error13(int line) {
//        if (errorMap.containsKey(line) && errorMap.get(line) < 13) {
//            return;
//        } else {
//            errorMap.put(line, 13);
//        }
        String msg = "Error type 13 at Line " + line + ": " + "Illegal use of \".\"";
        printErr(msg);
    }

    public void error14(int line, String s) {
//        if (errorMap.containsKey(line) && errorMap.get(line) < 14) {
//            return;
//        } else {
//            errorMap.put(line, 14);
//        }
        String msg = "Error type 14 at Line " + line + ": " + "Non-existent field " + "\"" + s + "\"";
        printErr(msg);
    }

    public void error15(TerminalNode node) {
//        if (errorMap.containsKey(node.getSymbol().getLine()) && errorMap.get(node.getSymbol().getLine()) < 15) {
//            return;
//        } else {
//            errorMap.put(node.getSymbol().getLine(), 15);
//        }
        String msg = "Error type 15 at Line " + node.getSymbol().getLine() + ": Redefined field " + node.getSymbol().getText();
        printErr(msg);
    }

    public void error15(int line, String s) {
//        if (errorMap.containsKey(line) && errorMap.get(line) < 15) {
//            return;
//        } else {
//            errorMap.put(line, 15);
//        }
        String msg = "Error type 15 at line " + line + ": init struct member " + s + " when define struct";
        printErr(msg);
    }

    public void error16(int line, String s) {
//        if (errorMap.containsKey(line) && errorMap.get(line) < 16) {
//            return;
//        } else {
//            errorMap.put(line, 16);
//        }
        String msg = "Error type 16 at Line " + line + ": Duplicated structure name " + "\"" + s + "\"";
        printErr(msg);
    }

    public void error17(int line, String s) {
//        if (errorMap.containsKey(line) && errorMap.get(line) < 17) {
//            return;
//        } else {
//            errorMap.put(line, 17);
//        }
        String msg = "Error type 17 at Line " + line + ": Undefined structure " + "\"" + s + "\"";
        printErr(msg);
    }


    public void printErr(String msg) {
        System.err.println(msg);
    }

    public Type createType(Kind kind) {
        switch (kind) {
            case INT:
                return new Int();
            case FLOAT:
                return new Float();
            case FUNCTION:
                return new Function();
            case ARRAY:
                return new Array();
            case STRUCTURE:
            default:
                return new Structure();
        }
    }

    public boolean isLeftValue(ParserRuleContext ctx) {
        return (ctx instanceof CmmParser.IDExpContext || ctx instanceof CmmParser.ArrayAccessExpContext
                || ctx instanceof CmmParser.AssignopExpContext || ctx instanceof CmmParser.StructAccessExpContext
                || ctx instanceof CmmParser.BracketExpContext) && !(expValues.get(ctx) instanceof Function);
    }


    public TerminalNode getNonAnonymousStructureID(CmmParser.StructSpecifierContext structSpecifierContext) {
        TerminalNode node;
        if (structSpecifierContext.optTag() != null) {
            node = structSpecifierContext.optTag().ID();
        } else {
            node = structSpecifierContext.tag().ID();
        }
        return node;
    }

    public boolean structIsEqual(Structure structure1, Structure structure2) {
//        if (structure1.getName().equals(structure2.getName())) {  //同一类型的结构体
//            return true;
//        }

        FieldList memberList1 = structure1.getMemberList();
        FieldList memberList2 = structure2.getMemberList();
        if (memberList1 == null || memberList2 == null) {
            if (memberList1 == memberList2) { //均为null
                return true;
            }
            return false;   //只有一个为null
        }

        if (memberList1.getLength() != memberList2.getLength()) {
            return false;
        }

        //length肯定相等
        while (memberList1 != null) {
            if (memberList1.getType().type != memberList2.getType().type) {
                return false;
            } else if (memberList1.getType().type == Kind.STRUCTURE) { //成员也是结构体
                if (!structIsEqual((Structure) memberList1.getType(), (Structure) memberList2.getType())) {    //结构体类型不等
                    return false;
                }
            } else if (memberList1.getType().type == Kind.ARRAY) {
                if (!arrayIsEqual((Array) memberList1.getType(), (Array) memberList2.getType())) {
                    return false;
                }
            }
            memberList1 = memberList1.getNext();
            memberList2 = memberList2.getNext();
        }

        return true;

    }

    public boolean arrayIsEqual(Array array1, Array array2) {
        if (array1.getElementType().type != array2.getElementType().type) {   //基类型不同
            return false;
        }

        if (array1.getElementType().type == Kind.ARRAY) { //基类型也是数组
            return arrayIsEqual((Array) array1.getElementType(), (Array) array2.getElementType());
        }

        if (array1.getElementType().type == Kind.STRUCTURE) { //基类型是结构体
            return structIsEqual((Structure) array1.getElementType(), (Structure) array2.getElementType());
        }
        return true;
    }

    private Type getParamDecType(CmmParser.ParamDecContext paramDecContext) {
        Type rawType = values.get(paramDecContext.specifier());
        CmmParser.VarDecContext varDecContext = paramDecContext.varDec();
        if (varDecContext.getChildCount() != 1) { //数组类型
            List<TerminalNode> ints = varDecContext.INT();
            Array res = new Array();
            for (int i = ints.size() - 1; i >= 0; i--) {
                if (i == ints.size() - 1) {
                    res.setElementType(rawType);
                    res.setLength(Integer.parseInt(ints.get(i).getText()));
                    continue;
                }
                Array temp = new Array();
                temp.setLength(Integer.parseInt(ints.get(i).getText()));
                temp.setElementType(res);
                res = temp;
            }
            return res;
        } else {
            return rawType;
        }
    }

    private Type getVarDecContextType(Type type, CmmParser.VarDecContext varDecContext) {
        if (varDecContext.getChildCount() != 1) { //数组类型
            List<TerminalNode> ints = varDecContext.INT();
            Array res = new Array();
            for (int i = ints.size() - 1; i >= 0; i--) {
                if (i == ints.size() - 1) {
                    res.setElementType(type);
                    res.setLength(Integer.parseInt(ints.get(i).getText()));
                    continue;
                }
                Array temp = new Array();
                temp.setLength(Integer.parseInt(ints.get(i).getText()));
                temp.setElementType(res);
                res = temp;
            }
            return res;
        } else {
            return type;
        }
    }
}
