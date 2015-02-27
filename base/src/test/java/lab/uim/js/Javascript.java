package lab.uim.js;

import java.awt.event.*;
import javax.swing.*;
import javax.script.*;

public class Javascript implements ActionListener {
    static final ScriptEngine _engine = new ScriptEngineManager().getEngineByName("JavaScript");

    String _script;

    public Javascript() throws ScriptException {
        _engine.eval("function alert(o){javax.swing.JOptionPane.showMessageDialog(null,o);}");
    }

    public void addJComponent(JComponent component, String id) {
System.err.println("Javascript.addJComponent@" + id);
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
