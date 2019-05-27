package org.springframework.asm;

/**
 * 对字段或方法的引用.
 */
public final class Handle {

    /**
     * 此Handle指定的字段或方法的类型.
     * 应该是
     * {@link Opcodes#H_GETFIELD}, {@link Opcodes#H_GETSTATIC},
     * {@link Opcodes#H_PUTFIELD}, {@link Opcodes#H_PUTSTATIC},
     * {@link Opcodes#H_INVOKEVIRTUAL}, {@link Opcodes#H_INVOKESTATIC},
     * {@link Opcodes#H_INVOKESPECIAL}, {@link Opcodes#H_NEWINVOKESPECIAL},
     * {@link Opcodes#H_INVOKEINTERFACE}.
     */
    final int tag;

    /**
     * 拥有此句柄指定的字段或方法的类的内部名称.
     */
    final String owner;

    /**
     * 此句柄指定的字段或方法的名称.
     */
    final String name;

    /**
     * 此句柄指定的字段或方法的描述符.
     */
    final String desc;


    /**
     * 指示所有者是否是接口.
     */
    final boolean itf;

    /**
     * 构造一个新的字段或方法句柄.
     * 
     * @param tag
     *            此Handle指定的字段或方法的种类.
     *            必须是
     *            {@link Opcodes#H_GETFIELD}, {@link Opcodes#H_GETSTATIC},
     *            {@link Opcodes#H_PUTFIELD}, {@link Opcodes#H_PUTSTATIC},
     *            {@link Opcodes#H_INVOKEVIRTUAL}, {@link Opcodes#H_INVOKESTATIC},
     *            {@link Opcodes#H_INVOKESPECIAL}, {@link Opcodes#H_NEWINVOKESPECIAL}或 {@link Opcodes#H_INVOKEINTERFACE}.
     * @param owner
     *            拥有此句柄指定的字段或方法的类的内部名称.
     * @param name
     *            此句柄指定的字段或方法的名称.
     * @param desc
     *            此句柄指定的字段或方法的描述符.
     *
     * @deprecated this constructor has been superseded
     *             by {@link #Handle(int, String, String, String, boolean)}.
     */
    @Deprecated
    public Handle(int tag, String owner, String name, String desc) {
        this(tag, owner, name, desc, tag == Opcodes.H_INVOKEINTERFACE);
    }

    /**
     *构造一个新的字段或方法句柄.
     *
     * @param tag
     *            此Handle指定的字段或方法的种类.
     *            必须是
     *            {@link Opcodes#H_GETFIELD}, {@link Opcodes#H_GETSTATIC},
     *            {@link Opcodes#H_PUTFIELD}, {@link Opcodes#H_PUTSTATIC},
     *            {@link Opcodes#H_INVOKEVIRTUAL}, {@link Opcodes#H_INVOKESTATIC},
     *            {@link Opcodes#H_INVOKESPECIAL}, {@link Opcodes#H_NEWINVOKESPECIAL}或 {@link Opcodes#H_INVOKEINTERFACE}.
     * @param owner
     *            拥有此句柄指定的字段或方法的类的内部名称.
     * @param name
     *            此句柄指定的字段或方法的名称.
     * @param desc
     *            此句柄指定的字段或方法的描述符.
     * @param itf
     *            true 如果所有者是一个接口.
     */
    public Handle(int tag, String owner, String name, String desc, boolean itf) {
        this.tag = tag;
        this.owner = owner;
        this.name = name;
        this.desc = desc;
        this.itf = itf;
    }

    /**
     * 返回此句柄指定的字段或方法的类型.
     * 
     * @return {@link Opcodes#H_GETFIELD}, {@link Opcodes#H_GETSTATIC},
     *         {@link Opcodes#H_PUTFIELD}, {@link Opcodes#H_PUTSTATIC},
     *         {@link Opcodes#H_INVOKEVIRTUAL}, {@link Opcodes#H_INVOKESTATIC},
     *         {@link Opcodes#H_INVOKESPECIAL}, {@link Opcodes#H_NEWINVOKESPECIAL} or {@link Opcodes#H_INVOKEINTERFACE}.
     */
    public int getTag() {
        return tag;
    }

    /**
     * 返回拥有此句柄指定的字段或方法的类的内部名称.
     */
    public String getOwner() {
        return owner;
    }

    /**
     * 返回此句柄指定的字段或方法的名称.
     */
    public String getName() {
        return name;
    }

    /**
     * 返回此句柄指定的字段或方法的描述符.
     */
    public String getDesc() {
        return desc;
    }

    /**
     * 如果此句柄指定的字段或方法的所有者是接口, 则返回true.
     */
    public boolean isInterface() {
        return itf;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Handle)) {
            return false;
        }
        Handle h = (Handle) obj;
        return tag == h.tag && itf == h.itf && owner.equals(h.owner)
                && name.equals(h.name) && desc.equals(h.desc);
    }

    @Override
    public int hashCode() {
        return tag + (itf? 64: 0) + owner.hashCode() * name.hashCode() * desc.hashCode();
    }

    /**
     * 返回此句柄的文本表示形式.
     * 文本表示是:
     * 
     * <pre>
     * 用于类的引用:
     * owner '.' name desc ' ' '(' tag ')'
     * 用于接口的引用:
     * owner '.' name desc ' ' '(' tag ' ' itf ')'
     * </pre>
     * 
     * . 由于此格式是明确的, 因此可以在必要时进行解析.
     */
    @Override
    public String toString() {
        return owner + '.' + name + desc + " (" + tag + (itf? " itf": "") + ')';
    }
}
