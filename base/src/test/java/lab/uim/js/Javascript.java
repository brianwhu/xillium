package lab.uim.js;

import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.script.*;

public class Javascript implements ActionListener {
    static final ScriptEngine _engine = new ScriptEngineManager().getEngineByName("JavaScript");

    String _script;
    //Map<String, JComponent> _components = new HashMap<String, JComponent>();

    public Javascript() {
        try {
            _engine.eval(
            "importPackage(java.io);importPackage(javax.swing);function alert(o){JOptionPane.showMessageDialog(null,o);}"
            );
            //_engine.getContext().setAttribute("components", _components, ScriptContext.GLOBAL_SCOPE);
        } catch (ScriptException x) {
            x.printStackTrace();
        }
    }

    public void addJComponent(JComponent component, String id) {
System.err.println("Javascript.addJComponent@" + id);
        //_components.put(id, component);
//System.err.println("Javascript.addJComponent:" + _components.get(id));
        _engine.getContext().setAttribute(id, component, ScriptContext.GLOBAL_SCOPE);
    }

    public void set(String script) {
        _script = script;
    }

    public void actionPerformed(ActionEvent e) {
        try {
            _engine.eval(_script);
        } catch (ScriptException x) {
            JOptionPane.showMessageDialog(null, x);
        }
    }
}
