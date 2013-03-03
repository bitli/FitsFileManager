"use strict";


// FITSFileManager-test-gui


// This file is part of FITSFileManager, see copyrigh in FITSFileManager.js


#include "PJSR-unit-tests-support.jsh"


#define VERSION "1.1-tests"

#define TITLE     "FITSFileManager"

// For debugging inside the gui
#define DEBUG true

#include "../../main/js/PJSR-logging.jsh"

#include "../../main/js/FITSFileManager-helpers.jsh"
#include "../../main/js/FITSFileManager-parameters.jsh"
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


#define TESTRULESETS


//

  // ========================================================================================================================
   // SUPPORT FOR TESTSTextEntryRow

var ffM_TestConfigUi = (function(){

   var addVariable = ffM_Configuration.addVariable;
   var defineVariable = ffM_Configuration.defineVariable;


   // Model object wrap data object and behavior
   var newConfigurationData = function(name, description) {
      var configuration = {
         name: name,
         description: description,
         variableList: [],
        builtins: {
           rank: {format: "%4.4d"},
           count: {format: "%4.4d"},
        }
      };
      var builder = {
         // Operations on the variable list
         addVariable: function(variable) {
            configuration.variableList.push(variable);
            return builder;
         },
         build: function() {
            return configuration;
         },
      }

      return  builder;

   }


   // --------------------------
   // Default definition for test
   var defaultRule = newConfigurationData('Default', 'Common FITS rules')
   .addVariable(defineVariable('type','Type of image (flat, bias, ...)','RegExpList'))
   .addVariable(defineVariable('filter','Filter (clear, red, ...)','RegExpList'))
   .addVariable(defineVariable('exposure','Exposure in seconds','Integer'))
   .addVariable(defineVariable('temp','Temperature in C','Integer'))
   .addVariable(defineVariable('binning','Binning as 1x1, 2x2, ...','IntegerPair'))
   .addVariable(defineVariable('night','night (experimental)','Night'))
   .addVariable(defineVariable('filename','Input file name','FileName'))
   .addVariable(defineVariable('extension','Input file extension','FileExtension'))
   .build();

#ifdef TESTRULESETS
   var testRule = newConfigurationData('Test', 'A test configuration')
   .addVariable(defineVariable('object','Object','Constant'))
   .build();

   var emptyRule = newConfigurationData('Empty', 'An empty configuration')
   .build();
   var largeRule = newConfigurationData('Large', 'Many variables')
   .addVariable(defineVariable('object 1','Object','Constant'))
   .addVariable(defineVariable('object 2','Object','Constant'))
   .addVariable(defineVariable('object 3','Object','Constant'))
   .addVariable(defineVariable('object 4','Object','Constant'))
   .addVariable(defineVariable('object 5','Object','Constant'))
   .addVariable(defineVariable('object 6','Object','Constant'))
   .addVariable(defineVariable('object 7','Object','Constant'))
   .addVariable(defineVariable('object 8','Object','Constant'))
   .addVariable(defineVariable('object 9','Object','Constant'))
   .addVariable(defineVariable('object 10','Object','Constant'))
   .addVariable(defineVariable('object 11','Object','Constant'))
   .addVariable(defineVariable('object 12','Object','Constant'))
   .addVariable(defineVariable('object 13','Object','Constant'))
   .addVariable(defineVariable('object 14','Object','Constant'))
   .addVariable(defineVariable('object 15','Object','Constant'))
   .addVariable(defineVariable('object 16','Object','Constant'))
   .addVariable(defineVariable('object 17','Object','Constant'))
   .addVariable(defineVariable('object 18','Object','Constant'))
   .addVariable(defineVariable('object 19','Object','Constant'))
   .addVariable(defineVariable('object 20','Object','Constant'))
   .build();

#endif


   // Test Rules
   var testConfigurations = [];
   testConfigurations.push(defaultRule);
#ifdef TESTRULESETS
   testConfigurations.push(testRule);
   testConfigurations.push(emptyRule);
   testConfigurations.push(largeRule);
#endif

   return {
      // For tests
      testConfigurations: testConfigurations,
   }
}) ();


function vP_testConfigurationGui()
{

   Console.show();
   Console.writeln("FITSFileManager-test-gui - Actions on the GUI will be logged on the console");

   var configurationSet = ffM_TestConfigUi.testConfigurations;
   // Noe: this log takes forever...
   //Console.writeln("Initiale ruleset:\n" + Log.pp(configurationSet));

   var names=[];
   for (var i=0; i<configurationSet.length; i++) {
      names.push(configurationSet[i].name);
   }
   var dialog =  ffM_GUI_config.makeDialog(null, names);
   dialog.configure(configurationSet, configurationSet[0].name);
   for ( ;; )
   {
      var result = dialog.execute();
      debug("vP_testGuiRuleSet: Result", result);

      if (result) {

         // Writing to the console takes forever... be patient
         Console.writeln("vP_testGuiRuleSet: currentConfigurationName: " + Log.pp(dialog.currentConfigurationName,0,true));
         Console.writeln(Log.pp(dialog.editedConfigurationSet));
         Console.flush();
       }
       if (!result) break;

   }

   Console.writeln("FITSFileManager-test-gui - exiting");

}


vP_testConfigurationGui();
