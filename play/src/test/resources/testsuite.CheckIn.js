importPackage(java.io);
importPackage(javax.swing);

/*
 * Prepare a sequence of fresh content IDs for check-ins
 */

function zero(value) {
	return value < 10 ? '0'+value : value;
}

var ContentSequenceLength = 6;
var Today = new Date();
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

if (JOptionPane.showConfirmDialog(null, 'Continue with the Test Suite?', "CheckIn", JOptionPane.YES_NO_OPTION) == 1) {
	java.lang.System.exit(0);
}
