// PJSR-logging.jsh

// This file is part of FITSFileManager, see copyrigh in FITSFileManager.js


// Support debug and log methods

// HTML characters for the console



// Define the loggin module
var Log = (function() {

   // --- private variables and methods for pretty print ----------------
   // From the web, made a little bit more robust
   var maxDepth = 20;

   function pp(object, depth, embedded) {

    var cx = /[\u0000\u00ad\u0600-\u0604\u070f\u17b4\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]/g,
        escapable = /[\\\"\x00-\x1f\x7f-\x9f\u00ad\u0600-\u0604\u070f\u17b4\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]/g,
        gap,
        indent,
        meta = {    // table of character substitutions
            '\b': '\\b',
            '\t': '\\t',
            '\n': '\\n',
            '\f': '\\f',
            '\r': '\\r',
            '"' : '\\"',
            '\\': '\\\\'
        },
        rep;


    function quote(string) {
      // If the string contains no control characters, no quote characters, and no
      // backslash characters, then we can safely slap some quotes around it.
      // Otherwise we must also replace the offending characters with safe escape
      // sequences.

        escapable.lastIndex = 0;
        return escapable.test(string) ? '"' + string.replace(escapable, function (a) {
            var c = meta[a];
            return typeof c === 'string'
                ? c
                : '\\u' + ('0000' + a.charCodeAt(0).toString(16)).slice(-4);
        }) + '"' : '"' + string + '"';
    }

   typeof(depth) == "number" || (depth = 0)

   // Limit depth to avoid recursion issues
   if (depth>maxDepth) return "...";


   typeof(embedded) == "boolean" || (embedded = false)
   var newline = false
   var spacer = function(depth) { var spaces = ""; for (var i=0;i<depth;i++) { spaces += "  "}; return spaces }
   var pretty = ""
   if (      typeof(object) == "undefined" ) { pretty += "undefined" }
   else if ( typeof(object) == "boolean" ||
             typeof(object) == "number" ) {    pretty += object.toString() }
   else if ( typeof(object) == "string" ) {    pretty +=  quote(object) }
   // Avoid error if a function is part of an object
   else if ( typeof(object) == "function" ) {    pretty +=  "function ... (" + object.length + ")" }
   else if (        object  == null) {         pretty += "null" }
   else if ( object instanceof(Array) ) {
      if ( object.length > 0 ) {
         if (embedded) { newline = true }
         var content = "";
         for each (var item in object) { content += pp(item, depth+1) + ",\n" + spacer(depth+1) }
         content = content.replace(/,\n\s*$/, "").replace(/^\s*/,"")
         pretty += "[ " + content + "\n" + spacer(depth) + "]"
      } else { pretty += "[]" }
   }
   else if (typeof(object) == "object") {
      if ( Object.keys(object).length > 0 ){
         if (embedded) { newline = true }
         var content = ""
         for (var key in object) {
         content += spacer(depth + 1) + key.toString() + ": " + pp(object[key], depth+2, true) + ",\n"
         }
         content = content.replace(/,\n\s*$/, "").replace(/^\s*/,"")
         pretty += "{ " + content + "\n" + spacer(depth) + "}"
      } else { pretty += "{}"}
   }
   else { pretty += object.toString() }
   return ((newline ? "\n" + spacer(depth) : "") + pretty)
}



   // --- private variables and methods for debug ---------------------------
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
      },
      pp: pp,
   }

})();

#ifdef DEBUG
var debug = Log.debug;
#endif

// Example:
//Log.debug("My list:", [1,2,3,4], "is", null);







