// PJSR-logging.jsh

// This file is part of FITSFileManager, see copyrigh in FITSFileManager.js


// Support debug and log methods

// HTML characters for the console



// Define the loggin module
var Log = (function() {

   // --- private variables and methods ---------------------------------------
   var debugOn = true;

   // To escape HTML characters for the console
   var escapeMap = { '"': '&quot;', '&': '&amp;', '<': '&lt;', '>': '&gt;' };
   var escapeHTML = function (text) {
        return text.replace(/[\"&<>]/g, function (a) { return escapeMap[a]; });
    };

   // --- public properties and methods ---------------------------------------
   return {
      // Debug log the arguments to the console (separated by spaces) if debug log is active
      // null and undefined are transformed in string
      // arrays are shown as array (first level only)
      // Example:
      //    Log.debug("My list:", [1,2,3,4], "is", null);
      // TODO Support more formatting as needed
      debug: function() {
         var str, arg, i;
         if (debugOn) {
            //var str =  Array.prototype.slice.call(arguments).join("");
            str = "";
            for (i = 0; i<arguments.length; i++) {
               if (i>0) { str += " "};
               arg = arguments[i];
               if (typeof arg === "undefined") {
                  str += "undefined";
               } else if (arg === null) {
                  str += "null";
               } else if (Array.isArray(arg)) {
                  str += "["+arg.toString()+"]"; // Should handle recursively, limit depth, ...
               } else {
                  str += arg.toString();
               }
            }
            Console.writeln(escapeHTML(str));
            Console.flush();
         }
      }
   }

})();

#ifdef DEBUG
debug = Log.debug;
#endif

// Example:
//Log.debug("My list:", [1,2,3,4], "is", null);







