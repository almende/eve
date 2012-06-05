/**
 * Module for parsing an ISO 8601 formatted string into a Date object.
 */
module.exports = function (string) {
	if (match = string.match(/^(\d{4})(-(\d{2})(-(\d{2})(T(\d{2}):(\d{2})(:(\d{2})(\.(\d+))?)?(Z|((\+|-)(\d{2}):(\d{2}))))?)?)?$/)) {
		var date = new Date();
		date.setUTCFullYear(Number(match[1]));
		date.setUTCMonth(Number(match[3]) - 1 || 0);
		date.setUTCDate(Number(match[5]) || 0);
		date.setUTCHours(Number(match[7]) || 0);
		date.setUTCMinutes(Number(match[8]) || 0);
		date.setUTCSeconds(Number(match[10]) || 0);
		date.setUTCMilliseconds(Number("." + match[12]) * 1000 || 0);

		if (match[13] && match[13] !== "Z") {
			var h = Number(match[16]) || 0,
			    m = Number(match[17]) || 0;

			h *= 3600000;
			m *= 60000;

			var offset = h + m;
			if (match[15] == "+")
				offset = -offset;

			date = new Date(date.valueOf() + offset);
		}

		return date;
	} else
		throw new Error("Invalid ISO 8601 date given.", __filename);
};

function pad4(n) {
	if (n < 10)
		return "000" + n;
	else if (n < 100)
		return "00" + n;
	else if (n < 1000)
		return "0" + n;
	else
		return n;
}

function pad2(n) {
	if (n < 10)
		return "0" + n;
	else
		return n;
}

/**
 * Returns an ISO 8061 formatted date string.
 */
Date.prototype.toISO8061 = function () {
	return pad4(this.getUTCFullYear()) + "-"
			+ pad2(this.getUTCMonth() + 1) + "-"
			+ pad2(this.getUTCDate()) + "T"
			+ pad2(this.getUTCHours()) + ":"
			+ pad2(this.getUTCMinutes()) + ":"
			+ pad2(this.getUTCSeconds()) + "Z";
};
