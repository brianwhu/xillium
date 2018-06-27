package lab.data.persistence;

import org.xillium.base.Open;
import org.xillium.base.util.EnvironmentReference;


public class StatementConfiguration implements EnvironmentReference {
    public static class Control implements Open {
        public String name;
    }

    public Open call() {
        Control control = new Control();
        control.name = "Open Source";
        return control;
    }
}
