// PJSR-unit-tests-support.jsh


// This file is part of FITSFileManager, see copyrigh in FITSFileManager.js


#define PT_TEST_OK "OK"

// ---------------------------------------------------------------------------------------------------------
// Micro unit testing support
// ---------------------------------------------------------------------------------------------------------

// Test results (a property for each test named as the test function and with the result as a String, PT_TEST_OK if OK
var pT_testResults={};

var pT_replaceAmpsRegExp = new RegExp('&', 'g');

function pT_replaceAmps (txt) {
  return txt.replace(FFM_replaceAmpsRegExp,'&amp;');
}

// List all test results
function pT_showTestResults() {
   Console.writeln();
   Console.writeln("------------------------------------------------------------");
   Console.writeln("Test results: ");
   Console.writeln("------------");
   var  successes =0, failures = 0;
   for (var t in pT_testResults) {
      if (pT_testResults.hasOwnProperty(t)) {
         var result = pT_testResults[t];
         if (result === PT_TEST_OK) {
            Console.writeln("    ", t, ": ", pT_replaceAmps(result));
            successes += 1;
         } else {
            Console.writeln(" ** ", t, ": ", pT_replaceAmps(result));
            failures += 1;
         }
      }
   }
   Console. writeln("" + (successes + failures) + " tests, " + successes + " sucesses, " + failures + ", failures");
   return failures===0;
}

// Execute all properties of 'tests', that must be functions, in a protected environment

function pT_executeTests(tests) {
   var startTime, elapsedTime;
   pT_testResults = {};
   for (var f in tests) {
      if (tests.hasOwnProperty(f)) {
         Console.writeln("Starting test: ", f);
         startTime = Date.now().valueOf();
         try {
            tests[f]();
            elapsedTime = Date.now().valueOf() - startTime;
            pT_testPassed(f);
            Console.writeln("Test completed successfully: " + f + " in " + elapsedTime + " ms");
         } catch (e) {
            elapsedTime = Date.now().valueOf() - startTime;
            pT_testFailed(f,"Exception: " + e);
            Console.writeln("** Test failed: " + f + " after " + elapsedTime + " ms because: ", pT_replaceAmps(e.toString()));
         }
      }
   };
   Console.show();
   pT_showTestResults();
}

// Assert support
function pT_assertNull(t) {
   if (t!==null) {
      throw "'" +  t + "' !== null";
   }
}
function pT_assertEquals(e,a) {
   if (e!==a) {
     throw "'" + e + "' !== '" + a +"'";
   }
}
function pT_assertArrayEquals(e,a) {
   if (e.length!==a.length) {
      var received = "";
      try {
         received = ", received: " + a;
      } catch (e) {
         // ignore
      }
     throw "Lenghts " + e.length + " !== " + a.length + received;
   }
   for (var i=0; i<e.length; i++) {
      if (e[i]!==a[i]) {
        throw "'" + e + "' !== '" + a +"' (index " + i + ")";
      }
   }
}
function pT_assertTrue(e) {
   if (!e) {
      throw "'" + e + "' is not true" ;
   }
}
function pT_assertFalse(e) {
   if (e) {
      throw "'" + e + "' is not false" ;
   }
}
function pT_assertUndefined(u) {
   if (typeof u !== 'undefined') {
     throw "typeof '" + u + "' !== undefined";
   }
}


// ---------------------------------------------------------------------------------------------------------
// Support methods (private)
// ---------------------------------------------------------------------------------------------------------
// Mark a test passed if it does not exist (test must be a string)
function pT_testPassed(test) {
   if (!(pT_testResults.hasOwnProperty(test))) {
      pT_testResults[test] = PT_TEST_OK;
   }
}

// Support methods (private)
// Mark a test failed in all cases (test must be a string), display message about failure
function pT_testFailed(test, msg) {
   pT_testResults[test] = msg;
   Console.writeln("Test ", test, " failed: ", msg);
}

// ---------------------------------------------------------------------------------------------------------
// Methods to support checking the current project for prerequisite
// ---------------------------------------------------------------------------------------------------------
function pT_checkPresenceOfProcesses(requiredProcesses) {
   Console.writeln("  Checking presence of required processes");
   var allProcessInstanceNames = ProcessInstance.icons();
#ifdef DEBUG
   Console.writeln("    vPtest_checkPresenceOfProcesses: All icons on project: ", allProcessInstanceNames);
   Console.writeln("    vPtest_checkPresenceOfProcesses: Required icons: ", requiredProcesses);

#endif
   for (var i=0; i<requiredProcesses.length; i++) {
      var processName = requiredProcesses[i];
      var isProcessPresent =  allProcessInstanceNames.indexOf(processName)>=0;
      if (!isProcessPresent) {
         throw "Process '" + processName + "' not present in current project, reload test project";
      }
   }
}

// Check that all views named in the array of string requiredViews are main views of some window in the project
function pT_checkPresenceOfViews(requiredViews) {

   var thereAreMissingViews = false;

#ifdef DEBUG
   var allValidWindows = ImageWindow.windows.filter(function (w) {return !w.isNull && w.isValidView(w.mainView)});
   Console.writeln("  pT_checkPresenceOfViews: " , allValidWindows.length," Window main views: ", allValidWindows.map(function(w){return w.mainView.id}));
#endif
   Console.writeln("    Checking presence of required views");

   // Check that all required views are present
   for (var i=0; i<requiredViews.length; i++) {
      var viewId = requiredViews[i];
      var view = View.viewById(viewId);
#ifdef DEBUG
      Console.writeln("      pT_checkPresenceOfViews: View ", viewId , " is ", view);
#endif
   if (view.isNull || !view.window.isValidView(view)) {
         thereAreMissingViews = true;
         Console.writeln("View '" + viewId + "' not present in current project");
      }
      Console.writeln("      view " + viewId + " present, uniqueId: " + view.uniqueId);
   }
   if (thereAreMissingViews) {
      throw "Some views are missing in the current project, reload the test project";
   }

}

// Close all windows whose main view is not in the list of required views, assume that all required views are present
function pT_closeNonTestWindows(requiredViews) {

   var requiredViewsUniqueId = [];
   var allValidWindows = ImageWindow.windows.filter(function (w) {return !w.isNull && w.isValidView(w.mainView)});

   // build a list of the uniqueId of all required main views
   for (var i=0; i<requiredViews.length; i++) {
      var viewId = requiredViews[i];
      var view = View.viewById(viewId);
      if (view.isNull || !view.window.isValidView(view)) {
         throw "View '" + viewId + "' not present in current project";
      }
      requiredViewsUniqueId.push(view.uniqueId);
   }


   for (var i=0; i<allValidWindows.length; i++) {
      var currentMainView = allValidWindows[i].mainView;
      var currentViewUniqueId = currentMainView.uniqueId;
      var isRelevantWindow =  requiredViewsUniqueId.indexOf(currentViewUniqueId)>=0;
#ifdef DEBUG
      Console.writeln("    pT_checkPresenceOfViews: Window ", currentMainView.id, " isRelevantWindow: " + isRelevantWindow +",  has index ", requiredViewsUniqueId.indexOf(currentViewUniqueId));
#endif
      if (!isRelevantWindow) {
         // We the user will be requested to confirm
         Console.writeln("    Closing window of view '" + currentMainView.id, "'");
         allValidWindows[i].close();
      }
   }

}
// Close all previews of the window required views, assume that the views are present
function pT_closePreviewsOfTestWindows(requiredViews) {
   Console.writeln("  Closing previews of test windows");
   for (var i=0; i<vPtest_requiredViews.length; i++) {
      var viewId = vPtest_requiredViews[i];
      var view = View.viewById(viewId);
      if (view.isNull || !view.window.isValidView(view)) {
         throw "View '" + viewId + "' not present in current project";
      }
      view.window.deletePreviews();
   }
}









