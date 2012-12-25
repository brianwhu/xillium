package lab.data.persistence;

import java.util.*;
import javax.sql.DataSource;
import javax.annotation.Resource;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.testng.annotations.*;

import org.xillium.data.persistence.*;
import org.xillium.base.beans.*;


/**
 * Testing Persistence
 */
@ContextConfiguration(locations={"/application-context.xml"})
@TransactionConfiguration(transactionManager="transactionManager", defaultRollback=false)
@Transactional
public class TestPersistence extends AbstractTransactionalTestNGSpringContextTests {
//	private static final Date date1 = java.sql.Date.valueOf("2011-09-04");
//	private static final Date date2 = java.sql.Date.valueOf("2011-09-07");
//	private static final String CUSTOMER_1_ID    = "US0000001";
//	private static final String CUSTOMER_1_FIRST = "Goerge";
//	private static final String CUSTOMER_1_PHONE = "222-555-0231";
//	private static final String CUSTOMER_1_LAST  = "Washington";
//	private static final String CUSTOMER_2_ID    = "US0000002";
//	private static final String CUSTOMER_2_FIRST = "John";
//	private static final String CUSTOMER_2_PHONE = "222-555-0232";
//	private static final String CUSTOMER_2_LAST  = "Adams";
//	private static final String CUSTOMER_3_ID    = "US0000003";
//	private static final String CUSTOMER_3_FIRST = "Thomas";
//	private static final String CUSTOMER_3_PHONE = "222-555-0233";
//	private static final String CUSTOMER_3_LAST  = "Jefferson";
//	private static final String CUSTOMER_4_ID    = "US0000004";
//	private static final String CUSTOMER_4_FIRST = "James";
//	private static final String CUSTOMER_4_PHONE = "222-555-0234";
//	private static final String CUSTOMER_4_LAST  = "Madison";

	// JSR 250 annotation injecting the appointmentsDao bean. Similar to the Spring @Autowired annotation
	@Resource
	private DataSource dataSource;

    // MEMBERSHIP(EMAIL VARCHAR(64) NOT NULL PRIMARY KEY,FIRST_NAME VARCHAR(32) NOT NULL,LAST_NAME VARCHAR(32) NOT NULL)
    public static class Membership implements org.xillium.data.DataObject {
        String email;
        public String firstName;
        public String lastName;
    }

	@BeforeClass
	public void startup() {
System.err.println(applicationContext);
System.err.println(dataSource);
System.err.println(getClass().getResource("/object-mapped.xml"));
	}

	@AfterClass
	public void cleanup() {
	}

	@Test(groups={"object"})
	public void testObjectMappedQuery() throws Exception {
        XMLBeanAssembler assembler = new XMLBeanAssembler(new DefaultObjectFactory());
        assembler.build(getClass().getResourceAsStream("/object-mapped.xml"));
        @SuppressWarnings("unchecked")
		ObjectMappedQuery<Membership> selectMemberships = (ObjectMappedQuery<Membership>)StorageConfiguration.getParametricStatement("SelectAllMemberships");
        List<Membership> memberships = selectMemberships.getResults(DataSourceUtils.getConnection(dataSource), null);
        System.err.println("***testObjectMappedQuery: # of results = " + memberships.size());
        for (Membership membership: memberships) {
            System.err.println(membership.email);
            System.err.println(membership.firstName);
            System.err.println(membership.lastName);
        }
        //System.err.println(Beans.toString(memberships));
	}
}
