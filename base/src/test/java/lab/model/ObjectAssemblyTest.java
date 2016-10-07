package lab.model;

import java.util.*;

import org.xillium.base.beans.*;
import org.xillium.base.model.*;
import org.testng.annotations.*;

/**
 * The default implementation of an object factory that creates objects from class names and arguments.
 */
public class ObjectAssemblyTest {

    @Test(groups={"functional", "model"})
    public void testObjectAssembly() throws Exception {
        Map<String, Address> addresses = new HashMap<>();
        Map<String, User> users = new HashMap<>();
        XMLBeanAssembler addrAssembler = new XMLBeanAssembler(new BurnedInArgumentsObjectFactory(ObjectAssembly.class, addresses, "main"));
        XMLBeanAssembler userAssembler = new XMLBeanAssembler(new BurnedInArgumentsObjectFactory(ObjectAssembly.class, users, "system"));

        addrAssembler.build(ObjectAssemblyTest.class.getResourceAsStream("/address-assembly.xml"));
        System.out.println("Addresses = {");
        System.out.println(Beans.toString(addresses));
        System.out.println("}");

        userAssembler.build(ObjectAssemblyTest.class.getResourceAsStream("/user-assembly.xml"));
        System.out.println("Users = {");
        System.out.println(Beans.toString(users));
        System.out.println("}");
    }
}

