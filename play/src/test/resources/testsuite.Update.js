importPackage(java.lang);
importPackage(java.io);

/*
 * A list of all test dDocNames for read test
 */
var ddocnames = perf.dataSourceColumn("RevisionInfo", "dSecurityGroup='Public'", "dDocName");

println(ddocnames.length);
for (var i = 0; i < 10; ++i) {
println(ddocnames[i]);
}

function AnyExistingContentID() {
	return ddocnames[Math.floor(Math.random() * ddocnames.length)];
}

/*
 * Test case listeners
 */
var Update = {
	Retrieve: {
		prepare: function(testcase, request) {
			System.err.println('Sending DOC_INFO for dDocName=' + request.binder.getLocal("dDocName"));
		},

		process: function(testcase, response) {
			var binder = response.binder;
			var info = binder.getResultSet("DOC_INFO").getRows().get(0);
			testcase.dID = info.get("dID");
			testcase.dDocName = info.get("dDocName");
			testcase.dRevLabel = info.get("dRevLabel");
			testcase.dSecurityGroup = info.get("dSecurityGroup");
			testcase.dDocAccount = info.get("dDocAccount");
			testcase.xComments = info.get("xComments");
			System.err.println('Got DOC_INFO when dDocName=' + testcase.dDocName + ": dDocAuthor=" + info.get("dDocAuthor"));
		}
	},

	Change: {
		prepare: function(testcase, request) {
			request.binder.putLocal("dID", testcase.dID);
			request.binder.putLocal("dDocName", testcase.dDocName);
			request.binder.putLocal("dRevLabel", testcase.dRevLabel);
			request.binder.putLocal("dSecurityGroup", testcase.dSecurityGroup);
			request.binder.putLocal("dDocAccount", testcase.dDocAccount);
			//request.binder.putLocal("xComments", testcase.xComments + "(" + new Date() + ")\n");
			request.binder.putLocal("xCSST_NAM", "A1001234");
			System.err.println('Sending UPDATE_DOCINFO for dID=' + request.binder.getLocal("dID") + ',dDocName=' + request.binder.getLocal("dDocName"));
		},

		process: function(testcase, response) {
			var binder = response.binder;
			System.err.println('UPDATE_DOCINFO completed with dDocName=' + testcase.dDocName);
		}
	}
}

