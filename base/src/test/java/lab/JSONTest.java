package lab;

import org.xillium.base.beans.JSONBuilder;
import java.util.*;
import org.testng.annotations.*;


public class JSONTest {
	private String buildJSON(int count) {
		Object[] a =  new Object[] { "Tue May 01 23:48:18 EDT 2012", "Can't do that", 34.6, true, null, "Help", new Object[] { 1, 2, 3, 4 } };
        List<String> b = new ArrayList<String>();
        b.add("Monkey");
        b.add("Chimp");
        b.add("Dog");
        b.add("Cat");
        b.add("\u6f22\u7d00\tGood Day");
		JSONBuilder pw = new JSONBuilder(1024*count);
		long time = System.currentTimeMillis();
		for (int i = 0; i < count; ++i) {
			pw.append('{').serialize("name", "Dave's Lounge")
						  .append(',')
						  .serialize("flag", false)
						  .append(',')
						  .serialize("size", 102.33)
						  .append(',')
						  .serialize("desc", "A long\nhistory\nof bitter \"struggle\"s")
						  .append(',')
						  .serialize("name1", "John's Lounge")
						  .append(',')
						  .serialize("flag1", true)
						  .append(',')
						  .serialize("size1", 930002.33)
						  .append(',')
						  .serialize("desc1", "A long\nhistory\nof bitter \"struggle\"s")
						  .append(',')
						  .serialize("name2", "John's Lounge")
						  .append(',')
						  .serialize("flag2", true)
						  .append(',')
						  .serialize("size2", 930002.33)
						  .append(',')
						  .serialize("desc2", b)
						  .append(',')
						  .serialize("options", a)
			.append('}');
		}
		time = System.currentTimeMillis() - time;
		//pw.println();
		//pw.flush();
		//System.out.print("Size: "); System.out.println(bytes.toByteArray().length);
        String json = pw.toString();
		if (count == 1) System.out.println(json);
		System.out.print("Size: "); System.out.println(pw.length());
		System.out.print("Time: "); System.out.println(time);
        return json;
	}

    private final String json = "{\"name\":\"Dave's Lounge\",\"flag\":false,\"size\":102.33,\"desc\":\"A long\\nhistory\\nof bitter \\\"struggle\\\"s\",\"name1\":\"John's Lounge\",\"flag1\":true,\"size1\":930002.33,\"desc1\":\"A long\\nhistory\\nof bitter \\\"struggle\\\"s\",\"name2\":\"John's Lounge\",\"flag2\":true,\"size2\":930002.33,\"desc2\":[\"Monkey\",\"Chimp\",\"Dog\",\"Cat\",\"\u6f22\u7d00\\tGood Day\"],\"options\":[\"Tue May 01 23:48:18 EDT 2012\",\"Can't do that\",34.6,true,null,\"Help\",[1,2,3,4]]}";
    @Test(groups={"JSON"})
    public void runSanityCheck() {
        System.out.println("Expecting");
        System.out.println(json);
        assert buildJSON(1).equals(json) : "JSON serialization error";
    }

    @Test(groups={"JSON"})
    public void runPerformance() {
        buildJSON(10000);
    }
}
