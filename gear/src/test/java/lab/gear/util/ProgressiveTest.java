package lab.gear.util;

import javax.sql.DataSource;
import javax.annotation.Resource;

import java.util.*;
import java.sql.*;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.context.*;
import org.springframework.context.support.GenericApplicationContext;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.springframework.transaction.annotation.Transactional;
import org.testng.annotations.*;

import org.xillium.base.beans.*;
import org.xillium.core.*;
import org.xillium.core.conf.*;
import org.xillium.core.management.*;
import org.xillium.gear.util.*;
import org.hsqldb.persist.HsqlProperties;
import org.hsqldb.*;


public class ProgressiveTest {
    public static class Controller extends ManagedComponent {
    }

    // the steps to go through, defined as values in an enum type.
    public enum OperationState {
        PERFORM_TASK_1,
        PERFORM_TASK_2,
        PERFORM_TASK_3
    }

    @Test(groups={"progressive"})
    public void progressive() throws Exception {
        // start an HSQL server
        HsqlProperties props = new HsqlProperties();
        props.setProperty("server.database.0", "file:target/test-classes/hsqldb/progressive");
        props.setProperty("server.dbname.0", "xdb");
        Server server = new Server();
        server.setProperties(props);
        server.start();
        try { Thread.sleep(3000L); } catch (Exception x) {}

        // load application context
        GenericApplicationContext gac = new GenericApplicationContext();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(gac);
        reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        reader.loadBeanDefinitions(new InputStreamResource(getClass().getResourceAsStream("/progressive.application.xml")));
        gac.refresh();

        final Persistence persistence = (Persistence)gac.getBean("persistence");
        final Progressive progressive = (Progressive)gac.getBean("progressive");
        final Controller controller = new Controller();

        // a Progressive.State object, used to keep track of the current process state
        final Progressive.State state = new Progressive.State(progressive, "progressive");

        // start a progress monitor in a separate thread
        new Thread() {
            public void run() {
                System.out.println("PROGRESS MONITOR start");
                Progressive.State last = new Progressive.State(), current = null;
                do {
                    try { Thread.sleep(100L); } catch (Exception x) {}
                    current = persistence.doReadOnly(null, new Persistence.Task<Progressive.State, Void>() {
                        public Progressive.State run(Void facility, Persistence persistence) throws Exception {
                            return persistence.getObject("-/RecallFullInformation", state);
                        }
                    });
                    if (current != null && (!Objects.equals(current.state, last.state) || !Objects.equals(current.previous, last.previous))) {
                        System.out.println(">>>> PROGRESS: " + current);
                        last = current;
                    }
                } while (last.previous == null || !last.previous.equals("PERFORM_TASK_3"));
                System.out.println("PROGRESS MONITOR close");
            }
        }.start();

        // start a resolver in a separate thread
        new Thread() {
            public void run() {
                System.out.println("PROBLEM RESOLVER start");
                Progressive.State current = null;
                do {
                    try { Thread.sleep(100L); } catch (Exception x) {}
                    current = persistence.doReadOnly(null, new Persistence.Task<Progressive.State, Void>() {
                        public Progressive.State run(Void facility, Persistence persistence) throws Exception {
                            return persistence.getObject("-/RecallFullInformation", state);
                        }
                    });
                    if (current != null && current.param != null && current.param.length() > 0) {
                        System.out.println(">>>> RESOLVER: " + current.param);
                        try { Thread.sleep(3000L); } catch (Exception x) {}
                        current.param = null;
                        progressive.markAttempt(current);
                        System.out.println(">>>> RESOLVER: problem resolved");
                    }
                } while (current == null || current.previous == null || !current.previous.equals("PERFORM_TASK_3"));
                System.out.println("PROBLEM RESOLVER close");
            }
        }.start();

        // the process logic inside a VitalTask, which depends on an instance of Reporting for exception reporting.
        new VitalTask<Controller, Void>(controller, state, new Runnable() {
            public void run() {
                System.out.println("**** Prepared for another retry");
            }
        }) {
            protected Void execute() throws Exception {
                // do work in PERFORM_TASK_1
                progressive.doStateful(state, OperationState.PERFORM_TASK_1, null, new Persistence.Task<Void, Void>() {
                    public Void run(Void facility, Persistence persistence) throws Exception {
                        try { Thread.sleep(1000L); } catch (Exception x) {}
                        if (getAge() < 1) throw new RuntimeException("AGE:" + getAge());
                        System.out.println(OperationState.PERFORM_TASK_1);
                        return null;
                    }
                });

                // do work in PERFORM_TASK_2
                progressive.doStateful(state, OperationState.PERFORM_TASK_2, null, new Persistence.Task<Void, Void>() {
                    public Void run(Void facility, Persistence persistence) throws Exception {
                        try { Thread.sleep(1000L); } catch (Exception x) {}
                        if (getAge() < 2) throw new RuntimeException("AGE:" + getAge());
                        System.out.println(OperationState.PERFORM_TASK_2);
                        return null;
                    }
                });

                // do work in PERFORM_TASK_3
                progressive.doStateful(state, OperationState.PERFORM_TASK_3, null, new Persistence.Task<Void, Void>() {
                    public Void run(Void facility, Persistence persistence) throws Exception {
                        try { Thread.sleep(1000L); } catch (Exception x) {}
                        if (getAge() < 3) throw new RuntimeException("AGE:" + getAge());
                        System.out.println(OperationState.PERFORM_TASK_3);
                        return null;
                    }
                });

                return null;
            }
        }.runAsInterruptible();
    }
}

