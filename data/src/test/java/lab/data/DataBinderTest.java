package lab.data;

import java.util.*;

import org.testng.annotations.*;

import org.xillium.data.*;
import org.xillium.base.beans.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Testing Persistence
 */
public class DataBinderTest {
	@Test(groups={"binder"})
	public void testBinder() throws Exception {
        DataBinder binder = new DataBinder();

        assert binder.put("string", "string") == null;
        assert binder.get("string").equals("string");
        assert binder.putNamedObject("object", new HashMap<String, DataBinder>()) == null;
        Map<String, DataBinder> map = binder.getNamedObject("object");
        assert map.size() == 0;
	}
}
