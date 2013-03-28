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
public class BinderToJsonTest {
    public static class Binder {
        public Map<String, Object> params;
        public Map<String, CachedResultSet> tables;
    }

	@Test(groups={"json"})
	public void testBinderToJSON() throws Exception {
        Properties props = new Properties();
        props.load(getClass().getResourceAsStream("/validation/SubmitPurchaseOrderData.properties"));

        DataBinder binder = new DataBinder().load(props);
        String json = binder.toJSON();
        System.out.println(json);

        Binder deserialized = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(json, Binder.class);
        if (deserialized.params.size() != binder.size()) throw new RuntimeException("wrong number of parameters");
        for (String key: binder.keySet()) {
            if (deserialized.params.get(key) == null || !deserialized.params.get(key).equals(binder.get(key))) {
                throw new RuntimeException("failure with parameter " + key);
            }
        }
	}
}
