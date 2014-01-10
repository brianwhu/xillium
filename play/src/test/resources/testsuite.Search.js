importPackage(java.io);
importPackage(javax.swing);

/*
 * A list of words
 */
var words = perf.fileAsArray("/usr/dict/words");
/*
var words = [];
var r = new BufferedReader(new FileReader("/usr/dict/words"));
for (var l; (l = r.readLine()) != null; words.push(l));
*/

/*
 * A random word
 */
function randomWord() {
	return words[Math.floor(Math.random() * words.length)];
}

if (JOptionPane.showConfirmDialog(null, 'Continue with the Test Suite?', "Search", JOptionPane.YES_NO_OPTION) == 1) {
	java.lang.System.exit(0);
}
