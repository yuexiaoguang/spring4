package org.springframework.asm;

/**
 * 有关异常处理程序块的信息.
 */
class Handler {

    /**
     * 异常处理程序范围的开头 (包括).
     */
    Label start;

    /**
     * 异常处理程序范围的结尾 (不包括).
     */
    Label end;

    /**
     * 异常处理程序代码的开头.
     */
    Label handler;

    /**
     * 此处理程序处理的异常类型的内部名称, 或<tt>null</tt>以捕获任何异常.
     */
    String desc;

    /**
     * 此处理器处理的异常类型的内部名称的常量池索引, 或0以捕获任何异常.
     */
    int type;

    /**
     * 下一个异常处理程序块信息.
     */
    Handler next;

    /**
     * 从给定的异常处理程序中删除start和end之间的范围.
     * 
     * @param h
     *            异常处理程序列表.
     * @param start
     *            要删除的范围的开始.
     * @param end
     *            要删除的范围的结束. Maybe null.
     * 
     * @return 删除的起始-结尾范围的异常处理程序列表.
     */
    static Handler remove(Handler h, Label start, Label end) {
        if (h == null) {
            return null;
        } else {
            h.next = remove(h.next, start, end);
        }
        int hstart = h.start.position;
        int hend = h.end.position;
        int s = start.position;
        int e = end == null ? Integer.MAX_VALUE : end.position;
        // if [hstart,hend[ and [s,e[ intervals intersect...
        if (s < hend && e > hstart) {
            if (s <= hstart) {
                if (e >= hend) {
                    // [hstart,hend[ fully included in [s,e[, h removed
                    h = h.next;
                } else {
                    // [hstart,hend[ minus [s,e[ = [e,hend[
                    h.start = end;
                }
            } else if (e >= hend) {
                // [hstart,hend[ minus [s,e[ = [hstart,s[
                h.end = start;
            } else {
                // [hstart,hend[ minus [s,e[ = [hstart,s[ + [e,hend[
                Handler g = new Handler();
                g.start = end;
                g.end = h.end;
                g.handler = h.handler;
                g.desc = h.desc;
                g.type = h.type;
                g.next = h.next;
                h.end = start;
                h.next = g;
            }
        }
        return h;
    }
}
