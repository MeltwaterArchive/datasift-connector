module.exports.getCurrentTimestampMs = function() {
	return (new Date().getTime());
}

module.exports.getCurrentTimestamp = function() {
	return module.exports.getCurrentTimestampMs() / 1000;
}

module.exports.stringToTimestampMs = function(str) {
	return (new Date(str).getTime())
}
module.exports.stringToTimestamp = function(str) {
	return module.exports.stringToTimestampMs(str) / 1000
}
module.exports.randomInt = function(min, max) {
	return Math.floor(Math.random() * (max - min)) + min
}

// Comparison functions for arrays and objects.

Array.prototype.equals = function (array) {
	// If the other array is a falsy value, return.
	if (!array) {
		return false
	}

	// Compare lengths - can save a lot of time.
	if (this.length != array.length) {
		return false
	}

	for (var i = 0, l=this.length; i < l; i++) {
		// Check if we have nested arrays.
		if (this[i] instanceof Array && array[i] instanceof Array) {
			// Recurse into the nested arrays.
			if (!this[i].equals(array[i])) {
				return false
			}
		} else if (this[i] != array[i]) {
			// Warning - two different object instances will never be equal: {x:20} != {x:20}.
			return false
		}
	}
	return true
}

Object.prototype.equals = function(object2) {
	// For the first loop, we only check for types.
	for (propName in this) {
		// Check for inherited methods and properties - like .equals itself
		// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/hasOwnProperty
		// Return false if the return value is different.
		if (this.hasOwnProperty(propName) != object2.hasOwnProperty(propName)) {
			return false
		}
		// Check instance type
		else if (typeof this[propName] != typeof object2[propName]) {
			// Different types => not equal.
			return false
		}
	}
	// Now a deeper check using other objects property names.
	for (propName in object2) {
		// We must check instances anyway, there may be a property that only exists in object2.
		// I wonder, if remembering the checked values from the first loop would be faster or not.
		if (this.hasOwnProperty(propName) != object2.hasOwnProperty(propName)) {
			return false
		} else if (typeof this[propName] != typeof object2[propName]) {
			return false
		}
		// If the property is inherited, do not check any more (it must be equa if both objects inherit it).
		if (!this.hasOwnProperty(propName)) {
			continue;
		}

		// Now the detail check and recursion.

		// This returns the script back to the array comparing.
		/** REQUIRES Array.equals **/
		if (this[propName] instanceof Array && object2[propName] instanceof Array) {
			// Recurse into the nested arrays
			if (!this[propName].equals(object2[propName])) {
				return false;
			}
		} else if (this[propName] instanceof Object && object2[propName] instanceof Object) {
			// Recurse into another objects.
			// console.log("Recursing to compare ", this[propName],"with",object2[propName], " both named \""+propName+"\"");
			if (!this[propName].equals(object2[propName])) {
				return false;
			}
		}
		//Normal value comparison for strings and numbers.
		else if(this[propName] != object2[propName]) {
			return false
		}
	}
	// If everything passed, let's say YES.
	return true
}
