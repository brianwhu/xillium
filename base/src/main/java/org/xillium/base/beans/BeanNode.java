package org.xillium.base.beans;

import java.lang.reflect.*;
import java.beans.*;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;

public class BeanNode implements TreeNode {
    /**
     * Creates a root node.
     */
    public BeanNode(Object bean) {
        this(bean, null, 0, null);
    }

    protected BeanNode(Object bean, BeanNode parent, int index, String label) {
        _bean = bean;
        if (bean != null) {
            try {
                _properties = Introspector.getBeanInfo(bean.getClass(), Object.class).getPropertyDescriptors();
            } catch (IntrospectionException x) {
                _properties = null;
            }
        }
        _parent = parent;
        _index = index;
        _label = label;
    }

    public BeanNode setFieldsVisible(boolean visible) {
        _fieldsVisible = visible;
        return this;
    }

    public Enumeration<BeanNode> children() {
        if (_list == null) {
            _list = childrenList();
        }
        return Collections.enumeration(_list);
    }

    public boolean getAllowsChildren() {
        return !isLeaf();
    }

    public TreeNode getChildAt(int index) {
        if (_list == null) {
            _list = childrenList();
        }
        return _list.get(index);
    }

    public int getChildCount() {
        if (_list == null) {
            _list = childrenList();
        }
        return _list.size();
    }

    public int getIndex(TreeNode node) {
        return ((BeanNode)node)._index;
    }

    public TreeNode getParent() {
        return _parent;
    }

    public boolean isLeaf() {
        if (_bean == null) {
            return true;
        } else {
            Class<?> type = _bean.getClass();
            return Beans.isPrimitive(type) || Beans.isDisplayable(type);
        }
    }

    public String toString() {
        if (_bean == null) {
            return _label != null ? _label : "[null]";
        } else {
            return _label != null ? (_label + '[' + _bean.getClass().getName() + ']') : _bean.getClass().getName();
        }
    }

    protected List<BeanNode> childrenList() {
        ArrayList<BeanNode> list = new ArrayList<BeanNode>();
        if (isLeaf()) {
            return list;
        } else {
            if (Iterable.class.isInstance(_bean)) {
                Iterator<?> iterator = ((Iterable<?>)_bean).iterator();
                int index = 0;
                while (iterator.hasNext()) {
                    list.add(new BeanNode(iterator.next(), this, index, String.valueOf(index)));
                    ++index;
                }
            } else {
                int index = 0;
                if (_fieldsVisible) {
                    Field[] fields = _bean.getClass().getFields();
                    for (int i = 0; i < fields.length; ++i) {
                        try {
                            list.add(new BeanNode(fields[i].get(_bean), this, index, fields[i].getName()));
                        } catch (Exception x) {
                            list.add(new BeanNode(x, this, index, x.getClass().getName()));
                        }
                        ++index;
                    }
                }
                for (int i = 0; i < _properties.length; ++i) {
                    try {
                        Method reader = _properties[i].getReadMethod();
                        if (reader != null) {
                            list.add(new BeanNode(reader.invoke(_bean), this, index, _properties[i].getDisplayName()));
                        }
                    } catch (Exception x) {
                        list.add(new BeanNode(x, this, index, x.getClass().getName()));
                    }
                    ++index;
                }
            }
            return list;
        }
    }

    private final Object _bean;
    private final int _index;
    private final BeanNode _parent;
    private final String _label;
    private PropertyDescriptor[] _properties;
    private List<BeanNode> _list;
    private boolean _fieldsVisible = false;


    public static void main(String[] args) {
        JFrame frame = new JFrame("Object Inspector");
        //JTree tree = new JTree(new BeanNode(java.util.prefs.Preferences.systemRoot()));
        //JTree tree = new JTree(new BeanNode(new java.io.File("http.conf")));
        JTree tree = new JTree(new BeanNode(new JButton("OK")));
        frame.getContentPane().add(new JScrollPane(tree), java.awt.BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
    }
}
