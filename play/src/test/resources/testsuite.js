importPackage(java.io);
importPackage(javax.swing);

/*
 * Current time
 */
var Today = new Date();

/*
 * A random member in a provided array
 */
function randomize(array) {
	return array[Math.floor(Math.random() * array.length)];
}

/*
 * Prepare a sequence of fresh content IDs for check-ins
 */

function zero(value) {
	return value < 10 ? '0'+value : value;
}

var ContentSequenceLength = 6;
var ContentPrefix = 'T' + Today.getFullYear() + zero(Today.getMonth()+1) + zero(Today.getDate())
		+ '-' + zero(Today.getHours()) + zero(Today.getMinutes()) + zero(Today.getSeconds()) + '-';
var ContentSequence = 1;

/*
 * Next new content ID
 */
function NextContentID() {
	var seq = new String(ContentSequence++);
	var pre = ContentPrefix;
	for (var i = seq.length; i < ContentSequenceLength; ++i) {
		pre += '0';
	}
	return pre + seq;
}

/*
 * A list of all test dIDs for deletion
 */
/* ONLY FOR DELETE CASE
var DeleteTargets = perf.dataSourceColumn("RevisionInfo", "dDocAuthor='Brian'", "dID");

var DeleteSequence = 0;

function NextContentToDelete() {
	if (DeleteSequence < DeleteTargets.length) {
println("Sequence = " + DeleteSequence);
		return DeleteTargets[DeleteSequence++];
	} else {
		return null;
	}
}
*/

if (JOptionPane.showConfirmDialog(null, 'Continue with the Test Suite?', "Word of the Moment", JOptionPane.YES_NO_OPTION) == 1) {
	java.lang.System.exit(0);
}
