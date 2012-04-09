package lab.data.persistence;

import java.util.*;
import java.util.Date;
import javax.annotation.Resource;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataAccessException;
import org.testng.annotations.*;


/**
 * Testing Persistence
 */
@ContextConfiguration(locations={"/persistence-test.xml"})
@TransactionConfiguration(transactionManager="transactionManager", defaultRollback=false)
@Transactional
public class TestPersistence extends AbstractTransactionalTestNGSpringContextTests {
	private static final Date date1 = java.sql.Date.valueOf("2011-09-04");
	private static final Date date2 = java.sql.Date.valueOf("2011-09-07");
	private static final String CUSTOMER_1_ID    = "US0000001";
	private static final String CUSTOMER_1_FIRST = "Goerge";
	private static final String CUSTOMER_1_PHONE = "222-555-0231";
	private static final String CUSTOMER_1_LAST  = "Washington";
	private static final String CUSTOMER_2_ID    = "US0000002";
	private static final String CUSTOMER_2_FIRST = "John";
	private static final String CUSTOMER_2_PHONE = "222-555-0232";
	private static final String CUSTOMER_2_LAST  = "Adams";
	private static final String CUSTOMER_3_ID    = "US0000003";
	private static final String CUSTOMER_3_FIRST = "Thomas";
	private static final String CUSTOMER_3_PHONE = "222-555-0233";
	private static final String CUSTOMER_3_LAST  = "Jefferson";
	private static final String CUSTOMER_4_ID    = "US0000004";
	private static final String CUSTOMER_4_FIRST = "James";
	private static final String CUSTOMER_4_PHONE = "222-555-0234";
	private static final String CUSTOMER_4_LAST  = "Madison";

	// JSR 250 annotation injecting the appointmentsDao bean. Similar to the Spring @Autowired annotation
	//@Resource
	//private AppointmentsDao appointmentsDao;

	@BeforeClass
	public void startup() {
System.err.println(applicationContext);
	}

	@AfterClass
	public void cleanup() {
	}

/*
	@Test
	public void testInsertAppointmentSuccess() {
		String w = insertAppointment(date2, 16, CUSTOMER_2_ID);
        assert appointmentsDao.findAll().size() == 5 : "There should be 5 total appointments";
        appointmentsDao.deleteAppointment(appointmentsDao.findByWindow(w));
	}
*/
}
