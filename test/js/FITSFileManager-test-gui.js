"use strict";

"use strict";

// FITSFileManager-test-gui


// This file is part of FITSFileManager, see copyrigh in FITSFileManager.js


#include "PJSR-unit-tests-support.jsh"


#define VERSION "0.8-tests"

#define TITLE     "FITSFileManager"

// For debugging inside the gui
#define DEBUG true

#include "../../main/js/PJSR-logging.jsh"

#include "../../main/js/FITSFileManager-text.jsh"


#include <pjsr/Sizer.jsh>
//#include <pjsr/FrameStyle.jsh>
#include <pjsr/TextAlign.jsh>
#include <pjsr/StdIcon.jsh>
#include <pjsr/StdCursor.jsh>
#include <pjsr/StdButton.jsh>
#include <pjsr/FrameStyle.jsh>
#include <pjsr/Color.jsh>

#include <pjsr/ButtonCodes.jsh>
#include <pjsr/FocusStyle.jsh>


#include "../../main/js/FITSFileManager-config-gui.jsh"


//


function vP_testGuiRuleSet()
{

   Console.show();
   Console.writeln("FITSFileManager-test-gui - Actions on the GUI will be logged on the console");

   var ruleSet = ffM_RuleSet_Model.testRules;
   Console.writeln("Initiale ruleset: " + Log.pp(ruleSet));

   var dialog =  ffM_GUI_RuleSet.makeDialog(null, ruleSet);
   for ( ;; )
   {
      var result = dialog.execute();
      debug("result", result);

      if (result) {

         Console.writeln("RESULT:" + Log.pp(dialog.listModel,0,true));

       }
       if (!result) break;

   }

   Console.writeln("FITSFileManager-test-gui - exiting");

}


vP_testGuiRuleSet();
