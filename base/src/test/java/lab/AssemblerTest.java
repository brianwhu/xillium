package lab;

import java.awt.*;
import javax.swing.*;
import org.xillium.base.beans.*;
import org.testng.annotations.*;


/**
 * The default implementation of an object factory that creates objects from class names and arguments.
 */
public class AssemblerTest {

    @Test(groups={"functional"})
    public void testAssember() throws Exception {
        Object bean = new XMLBeanAssembler(new DefaultObjectFactory()).setPackage("non.existent").build("src/test/java/lab/swing.xml");
        if (bean.getClass() == JFrame.class) {
            JFrame frame = (JFrame)bean;
            frame.pack();
            frame.setVisible(true);
        } else {
            System.out.println(Beans.toString(bean));

            JFrame frame = new JFrame("Object Inspector");
            JTree tree = new JTree(new BeanNode(bean));
            frame.getContentPane().add(new JScrollPane(tree), BorderLayout.CENTER);
            frame.pack();
            frame.setVisible(true);
        }
    }

    @Test(groups={"upscale"})
    public void testUpScale() throws Exception {
        System.out.println("Dimension = " + new XMLBeanAssembler(new DefaultObjectFactory()).build("src/test/java/lab/upscale.xml"));
    }

    @Test(groups={"package"})
    public void testNonCompliant() throws Exception {
        XMLBeanAssembler a = new XMLBeanAssembler(new DefaultObjectFactory());
        System.out.println("Response = " + Beans.toString(a.setPackage("lab.sms").build("src/test/java/lab/response.xml")));
        System.out.println("Properties = " + Beans.toString(a.setPackage(null).build("src/test/java/lab/java-util-properties.xml")));
    }
}
