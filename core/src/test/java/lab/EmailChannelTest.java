package lab;

import org.xillium.base.beans.*;
import org.xillium.core.util.EmailChannel;
import org.testng.annotations.*;
import com.dumbster.smtp.*;


public class EmailChannelTest {
    private static final String SUBJECT = "email channel";
    private static final String MESSAGE = "message delivery success";

    @Test(groups={"email"})
    public void test() throws Exception {
        SimpleSmtpServer server = SimpleSmtpServer.start(2525);

        EmailChannel channel = (EmailChannel)new XMLBeanAssembler(new DefaultObjectFactory()).build("src/test/java/lab/email-channel.xml");
        channel.sendMessage(SUBJECT, MESSAGE);

        server.stop();

        assert server.getReceivedEmailSize() == 1;
        SmtpMessage email = (SmtpMessage)server.getReceivedEmail().next();
        System.err.println(email.getHeaderValue("From"));
        System.err.println(email.getHeaderValue("To"));
        System.err.println(email.getHeaderValue("Subject"));
        assert email.getHeaderValue("Subject").equals(SUBJECT);
        System.err.println(email.getBody());
        assert email.getBody().equals(MESSAGE);
    }
}
