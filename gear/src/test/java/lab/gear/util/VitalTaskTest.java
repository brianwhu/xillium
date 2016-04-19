package lab.gear.util;

import org.xillium.core.management.*;
import org.xillium.gear.util.*;
import org.testng.annotations.*;


public class VitalTaskTest extends ManagedComponent {
    @Test(groups={"vital"})
    public void test() throws Exception {
        VitalTask<VitalTaskTest, Void> task = new VitalTask<VitalTaskTest, Void>(this, new Runnable() {
            public void run() {
                System.out.println("**** Preparation done before trial");
            }
        }) {
            protected Void execute() throws Exception {
                Thread.sleep(2000L);
                throw new Exception();
            }
        };

        Thread t = new Thread(task);
        t.start();
        Thread.sleep(10000L);
        t.interrupt();
        t.join();
        System.out.println(task.getInterruptedException());
        System.out.println(task.getAge());

        assert task.getAge() >= 0;
        assert task.getAge() <= 5;
        assert task.getInterruptedException() != null;
    }
}
