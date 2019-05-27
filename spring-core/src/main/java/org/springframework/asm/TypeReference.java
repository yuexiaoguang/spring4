package org.springframework.asm;

/**
 * 对类, 字段, 方法声明, 或指令中出现的类型的引用.
 * 这样的引用指定引用类型出现的类的部分
 * (e.g. 'extends', 'implements' 或 'throws'子句, 'new'指令, 'catch'子句, 类型转换, 局部变量声明, etc).
 */
public class TypeReference {

    /**
     * 指向泛型类的类型参数的类型引用的种类.
     * See {@link #getSort getSort}.
     */
    public final static int CLASS_TYPE_PARAMETER = 0x00;

    /**
     * 指向泛型方法的类型参数的类型引用的种类.
     * See {@link #getSort getSort}.
     */
    public final static int METHOD_TYPE_PARAMETER = 0x01;

    /**
     * 指向类的超类或它实现的接口之一的类型引用的种类.
     * See {@link #getSort getSort}.
     */
    public final static int CLASS_EXTENDS = 0x10;

    /**
     * 指向泛型类的类型参数的绑定的类型引用的种类.
     * See {@link #getSort getSort}.
     */
    public final static int CLASS_TYPE_PARAMETER_BOUND = 0x11;

    /**
     * 指向泛型方法的类型参数的绑定的类型引用的种类.
     * See {@link #getSort getSort}.
     */
    public final static int METHOD_TYPE_PARAMETER_BOUND = 0x12;

    /**
     * 指向字段类型的类型引用类型的种类.
     * See {@link #getSort getSort}.
     */
    public final static int FIELD = 0x13;

    /**
     * 指向方法的返回类型的类型引用的种类.
     * See {@link #getSort getSort}.
     */
    public final static int METHOD_RETURN = 0x14;

    /**
     * 指向方法的接收器类型的类型引用的种类.
     * See {@link #getSort getSort}.
     */
    public final static int METHOD_RECEIVER = 0x15;

    /**
     * 指向方法的形式参数的类型的类型引用的种类.
     * See {@link #getSort getSort}.
     */
    public final static int METHOD_FORMAL_PARAMETER = 0x16;

    /**
     * 指向方法的throws子句中声明的异常类型的类型引用的种类.
     * See {@link #getSort getSort}.
     */
    public final static int THROWS = 0x17;

    /**
     * 指向方法中的局部变量的类型的类型引用的种类.
     * See {@link #getSort getSort}.
     */
    public final static int LOCAL_VARIABLE = 0x40;

    /**
     * 指向方法中的资源变量类型的类型引用的种类.
     * See {@link #getSort getSort}.
     */
    public final static int RESOURCE_VARIABLE = 0x41;

    /**
     * 指向方法中'catch'子句的异常类型的类型引用的种类.
     * See {@link #getSort getSort}.
     */
    public final static int EXCEPTION_PARAMETER = 0x42;

    /**
     * 指向'instanceof'指令中声明的类型的类型引用的种类.
     * See {@link #getSort getSort}.
     */
    public final static int INSTANCEOF = 0x43;

    /**
     * 指向'new'指令创建的对象的类型的类型引用的种类.
     * See {@link #getSort getSort}.
     */
    public final static int NEW = 0x44;

    /**
     * 指向构造函数引用的接收器类型的类型引用的种类.
     * See {@link #getSort getSort}.
     */
    public final static int CONSTRUCTOR_REFERENCE = 0x45;

    /**
     * 指向方法引用的接收器类型的类型引用的种类.
     * See {@link #getSort getSort}.
     */
    public final static int METHOD_REFERENCE = 0x46;

    /**
     * 指向在显式或隐式cast指令中声明的类型的类型引用的种类.
     * See {@link #getSort getSort}.
     */
    public final static int CAST = 0x47;

    /**
     * 指向构造函数调用中的泛型构造函数的类型参数的类型引用的种类.
     * See {@link #getSort getSort}.
     */
    public final static int CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT = 0x48;

    /**
     * 指向方法调用中泛型方法的类型参数的类型引用的种类.
     * See {@link #getSort getSort}.
     */
    public final static int METHOD_INVOCATION_TYPE_ARGUMENT = 0x49;

    /**
     * 指向构造函数引用中泛型构造函数的类型参数的类型引用的种类.
     * See {@link #getSort getSort}.
     */
    public final static int CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT = 0x4A;

    /**
     * 指向方法引用中的泛型方法的类型参数的类型引用的种类.
     * See {@link #getSort getSort}.
     */
    public final static int METHOD_REFERENCE_TYPE_ARGUMENT = 0x4B;

    /**
     * Java类文件格式的类型引用值.
     */
    private int value;

    /**
     * @param typeRef
     *            类型引用的int编码值, 在与类型注解相关的访问方法中接收, 如visitTypeAnnotation.
     */
    public TypeReference(int typeRef) {
        this.value = typeRef;
    }

    /**
     * 返回给定种类的类型引用.
     * 
     * @param sort
     *            {@link #FIELD FIELD}, {@link #METHOD_RETURN METHOD_RETURN},
     *            {@link #METHOD_RECEIVER METHOD_RECEIVER},
     *            {@link #LOCAL_VARIABLE LOCAL_VARIABLE},
     *            {@link #RESOURCE_VARIABLE RESOURCE_VARIABLE},
     *            {@link #INSTANCEOF INSTANCEOF}, {@link #NEW NEW},
     *            {@link #CONSTRUCTOR_REFERENCE CONSTRUCTOR_REFERENCE}, or
     *            {@link #METHOD_REFERENCE METHOD_REFERENCE}.
     * 
     * @return 给定种类的类型引用.
     */
    public static TypeReference newTypeReference(int sort) {
        return new TypeReference(sort << 24);
    }

    /**
     * 返回对泛型类或方法的类型参数的引用.
     * 
     * @param sort
     *            {@link #CLASS_TYPE_PARAMETER CLASS_TYPE_PARAMETER}或
     *            {@link #METHOD_TYPE_PARAMETER METHOD_TYPE_PARAMETER}.
     * @param paramIndex
     *            类型参数索引.
     * 
     * @return 对泛型类或方法的类型参数的引用.
     */
    public static TypeReference newTypeParameterReference(int sort,
            int paramIndex) {
        return new TypeReference((sort << 24) | (paramIndex << 16));
    }

    /**
     * 返回对泛型类或方法的类型参数绑定的引用.
     * 
     * @param sort
     *            {@link #CLASS_TYPE_PARAMETER CLASS_TYPE_PARAMETER}或
     *            {@link #METHOD_TYPE_PARAMETER METHOD_TYPE_PARAMETER}.
     * @param paramIndex
     *            类型参数索引.
     * @param boundIndex
     *            上述类型参数中的类型绑定索引.
     * 
     * @return 对泛型类或方法的类型参数绑定的引用.
     */
    public static TypeReference newTypeParameterBoundReference(int sort,
            int paramIndex, int boundIndex) {
        return new TypeReference((sort << 24) | (paramIndex << 16)
                | (boundIndex << 8));
    }

    /**
     * 返回类的'implements'子句对超类或接口的引用.
     * 
     * @param itfIndex
     *            类的'implements'子句中的接口的索引, 或-1引用类的超类.
     * 
     * @return 对类的给定超类型的引用.
     */
    public static TypeReference newSuperTypeReference(int itfIndex) {
        itfIndex &= 0xFFFF;
        return new TypeReference((CLASS_EXTENDS << 24) | (itfIndex << 8));
    }

    /**
     * 返回对方法的形式参数类型的引用.
     * 
     * @param paramIndex
     *            形式参数索引.
     * 
     * @return 对给定方法形式参数的类型的引用.
     */
    public static TypeReference newFormalParameterReference(int paramIndex) {
        return new TypeReference((METHOD_FORMAL_PARAMETER << 24)
                | (paramIndex << 16));
    }

    /**
     * 返回方法的'throws'子句中对异常类型的引用.
     * 
     * @param exceptionIndex
     *            方法的'throws'子句中的异常的索引.
     * 
     * @return 对给定异常类型的引用.
     */
    public static TypeReference newExceptionReference(int exceptionIndex) {
        return new TypeReference((THROWS << 24) | (exceptionIndex << 8));
    }

    /**
     * 返回对方法的'catch'子句中声明的异常的类型的引用.
     * 
     * @param tryCatchBlockIndex
     *            try catch块的索引 (使用visitTryCatchBlock访问它们的顺序).
     * 
     * @return 对给定异常的类型的引用.
     */
    public static TypeReference newTryCatchReference(int tryCatchBlockIndex) {
        return new TypeReference((EXCEPTION_PARAMETER << 24)
                | (tryCatchBlockIndex << 8));
    }

    /**
     * 返回对构造函数或方法调用或引用中类型参数的类型的引用.
     * 
     * @param sort
     *            {@link #CAST CAST},
     *            {@link #CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT
     *            CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT},
     *            {@link #METHOD_INVOCATION_TYPE_ARGUMENT
     *            METHOD_INVOCATION_TYPE_ARGUMENT},
     *            {@link #CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT
     *            CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT}, or
     *            {@link #METHOD_REFERENCE_TYPE_ARGUMENT
     *            METHOD_REFERENCE_TYPE_ARGUMENT}.
     * @param argIndex
     *            类型参数的索引.
     * 
     * @return 对给定类型参数的类型的引用.
     */
    public static TypeReference newTypeArgumentReference(int sort, int argIndex) {
        return new TypeReference((sort << 24) | argIndex);
    }

    /**
     * 返回此类型引用的种类.
     * 
     * @return {@link #CLASS_TYPE_PARAMETER CLASS_TYPE_PARAMETER},
     *         {@link #METHOD_TYPE_PARAMETER METHOD_TYPE_PARAMETER},
     *         {@link #CLASS_EXTENDS CLASS_EXTENDS},
     *         {@link #CLASS_TYPE_PARAMETER_BOUND CLASS_TYPE_PARAMETER_BOUND},
     *         {@link #METHOD_TYPE_PARAMETER_BOUND METHOD_TYPE_PARAMETER_BOUND},
     *         {@link #FIELD FIELD}, {@link #METHOD_RETURN METHOD_RETURN},
     *         {@link #METHOD_RECEIVER METHOD_RECEIVER},
     *         {@link #METHOD_FORMAL_PARAMETER METHOD_FORMAL_PARAMETER},
     *         {@link #THROWS THROWS}, {@link #LOCAL_VARIABLE LOCAL_VARIABLE},
     *         {@link #RESOURCE_VARIABLE RESOURCE_VARIABLE},
     *         {@link #EXCEPTION_PARAMETER EXCEPTION_PARAMETER},
     *         {@link #INSTANCEOF INSTANCEOF}, {@link #NEW NEW},
     *         {@link #CONSTRUCTOR_REFERENCE CONSTRUCTOR_REFERENCE},
     *         {@link #METHOD_REFERENCE METHOD_REFERENCE}, {@link #CAST CAST},
     *         {@link #CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT
     *         CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT},
     *         {@link #METHOD_INVOCATION_TYPE_ARGUMENT
     *         METHOD_INVOCATION_TYPE_ARGUMENT},
     *         {@link #CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT
     *         CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT}, or
     *         {@link #METHOD_REFERENCE_TYPE_ARGUMENT
     *         METHOD_REFERENCE_TYPE_ARGUMENT}.
     */
    public int getSort() {
        return value >>> 24;
    }

    /**
     * 返回此类型引用引用的类型参数的索引.
     * 此方法只能用于种类为{@link #CLASS_TYPE_PARAMETER CLASS_TYPE_PARAMETER},
     * {@link #METHOD_TYPE_PARAMETER METHOD_TYPE_PARAMETER},
     * {@link #CLASS_TYPE_PARAMETER_BOUND CLASS_TYPE_PARAMETER_BOUND}或
     * {@link #METHOD_TYPE_PARAMETER_BOUND METHOD_TYPE_PARAMETER_BOUND}的类型引用.
     * 
     * @return 类型参数的索引.
     */
    public int getTypeParameterIndex() {
        return (value & 0x00FF0000) >> 16;
    }

    /**
     * 返回类型参数绑定的索引, 在类型参数{@link #getTypeParameterIndex}中, 由此类型引用引用.
     * 此方法只能用于种类为
     * {@link #CLASS_TYPE_PARAMETER_BOUND CLASS_TYPE_PARAMETER_BOUND}或
     * {@link #METHOD_TYPE_PARAMETER_BOUND METHOD_TYPE_PARAMETER_BOUND}的类型引用.
     * 
     * @return 类型参数绑定索引.
     */
    public int getTypeParameterBoundIndex() {
        return (value & 0x0000FF00) >> 8;
    }

    /**
     * 返回此类型引用引用的类的"超类型"的索引.
     * 此方法只能用于种类为{@link #CLASS_EXTENDS CLASS_EXTENDS}的类型引用.
     * 
     * @return 类的'implements'子句中的接口的索引,
     *         或 -1 如果此类型引用引用超类的类型.
     */
    public int getSuperTypeIndex() {
        return (short) ((value & 0x00FFFF00) >> 8);
    }

    /**
     * 返回此类型引用引用其类型的形式参数的索引.
     * 此方法只能用于种类为{@link #METHOD_FORMAL_PARAMETER METHOD_FORMAL_PARAMETER}的类型引用.
     * 
     * @return 形式参数的索引.
     */
    public int getFormalParameterIndex() {
        return (value & 0x00FF0000) >> 16;
    }

    /**
     * 返回异常的索引, 在方法的'throws'子句中,  该类型的类型由此类型引用引用.
     * 此方法只能用于种类为{@link #THROWS THROWS}的类型引用.
     * 
     * @return 方法的'throws'子句中异常的索引.
     */
    public int getExceptionIndex() {
        return (value & 0x00FFFF00) >> 8;
    }

    /**
     * 返回try catch块的索引 (使用visitTryCatchBlock访问它们的顺序), 其'catch'类型由此类型引用引用.
     * 此方法只能用于种类为{@link #EXCEPTION_PARAMETER EXCEPTION_PARAMETER}的类型引用.
     * 
     * @return 方法的'throws'子句中异常的索引.
     */
    public int getTryCatchBlockIndex() {
        return (value & 0x00FFFF00) >> 8;
    }

    /**
     * 返回此类型引用引用的类型参数的索引.
     * 此方法只能用于种类为{@link #CAST CAST},
     * {@link #CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT},
     * {@link #METHOD_INVOCATION_TYPE_ARGUMENT METHOD_INVOCATION_TYPE_ARGUMENT},
     * {@link #CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT},或
     * {@link #METHOD_REFERENCE_TYPE_ARGUMENT METHOD_REFERENCE_TYPE_ARGUMENT}的类型引用.
     * 
     * @return 类型参数的索引.
     */
    public int getTypeArgumentIndex() {
        return value & 0xFF;
    }

    /**
     * 返回此类型引用的int编码值, 适用于与类型注解相关的访问方法, 如visitTypeAnnotation.
     * 
     * @return 此类型引用的int编码值.
     */
    public int getValue() {
        return value;
    }
}
