package lab.data.persistence;

import java.util.*;
import java.io.*;
import javax.sql.DataSource;
import javax.annotation.Resource;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.testng.annotations.*;

import org.xillium.data.*;
import org.xillium.data.persistence.*;
import org.xillium.data.persistence.xml.*;
import org.xillium.base.beans.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;


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
        System.err.println("***testObjectMappedQuery: # of params = " + selectMemberships.getParameters().length);
        assert selectMemberships.getParameters().length == 0 : "***testObjectMappedQuery: # of params should be zero";
        List<Membership> memberships = selectMemberships.getResults(DataSourceUtils.getConnection(dataSource), null);
        System.err.println("***testObjectMappedQuery: # of results = " + memberships.size());
        for (Membership membership: memberships) {
            System.err.println(membership.email);
            System.err.println(membership.firstName);
            System.err.println(membership.lastName);
        }
        //System.err.println(Beans.toString(memberships));
	}

	@Test(groups={"object"})
    public void testResultSet2Xml() throws Exception {
        XMLBeanAssembler assembler = new XMLBeanAssembler(new DefaultObjectFactory());
        assembler.build(getClass().getResourceAsStream("/object-mapped.xml"));
        @SuppressWarnings("unchecked")
        ParametricQuery selectMemberships = (ParametricQuery)StorageConfiguration.getParametricStatement("SelectAllMemberships");
        //StringWriter writer = new StringWriter();
        selectMemberships.executeSelect(DataSourceUtils.getConnection(dataSource), null, new ResultSetStreamer("members", new OutputStreamWriter(System.out)));
        System.err.println("***testResultSet2Xml: done");
    }

    @Test(groups={"object"})
    public void testDataObjectClassGen() throws Exception {
        XMLBeanAssembler assembler = new XMLBeanAssembler(new DefaultObjectFactory());
        assembler.build(getClass().getResourceAsStream("/object-mapped.xml"));

        StorageConfiguration.getParametricStatement("CreateMembership").executeInsert(DataSourceUtils.getConnection(dataSource), null, false);

        @SuppressWarnings("unchecked")
        ObjectMappedQuery<Membership> selectMembership = (ObjectMappedQuery<Membership>)StorageConfiguration.getParametricStatement("SelectMembership");

        Class<? extends DataObject> c = selectMembership.getDataObjectClass("lab.persistence.test");
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        System.out.print("testDataObjectClassGen: Request = "); System.out.println(mapper.writeValueAsString(mapper.readTree(DataObject.Util.describe(c))));

        Properties p = new Properties();
        p.load(getClass().getResourceAsStream("/object-mapped.properties"));
        DataObject t = new org.xillium.data.validation.Reifier().collect(c.newInstance(), new DataBinder().load(p));
        System.out.print("testDataObjectClassGen: Data = " + Beans.toString(t));
        List<Membership> memberships = selectMembership.getResults(DataSourceUtils.getConnection(dataSource), t);
        System.err.println("***testDataObjectClassGen: # of results = " + memberships.size());
        assert memberships.size() == 1;
        for (Membership membership: memberships) {
            System.err.println(membership.email);
            System.err.println(membership.firstName);
            System.err.println(membership.lastName);
        }
        //System.err.println(Beans.toString(memberships));
        int rows = StorageConfiguration.getParametricStatement("DeleteMembership").executeUpdate(DataSourceUtils.getConnection(dataSource), t);
        assert rows == 1;
    }


}
