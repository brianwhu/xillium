importPackage(java.lang);
importPackage(java.io);
importPackage(javax.swing);

/*
 * A list of all test dDocNames for read test
 */
var ddocnames = perf.dataSourceColumn("RevisionInfo", "dDocType='Document'", "dDocName");

println(ddocnames.length);
for (var i = 0; i < 10; ++i) {
println(ddocnames[i]);
}

function AnyExistingContentID() {
	return ddocnames[Math.floor(Math.random() * ddocnames.length)];
}

/*
 */
var DocInfo = {
	prepare: function(request) {
		System.err.println('Sending DOC_INFO for dDocName=' + request.binder.getLocal("dDocName"));
	},

	process: function(response) {
		var binder = response.binder;
		System.err.println('Got DOC_INFO when dDocName=' + binder.getLocal("dDocName") + ": dDocAuthor=" + binder.getResultSet("DOC_INFO").getRows().get(0).get("dDocAuthor"));
	}
}

if (JOptionPane.showConfirmDialog(null, 'Continue with the Test Suite?', "DocInfo", JOptionPane.YES_NO_OPTION) == 1) {
	System.exit(0);
}
