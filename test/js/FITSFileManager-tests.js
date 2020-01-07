"use strict";

// FITSFileManager-tests


// This file is part of FITSFileManager, see copyrigh in FITSFileManager.js


#include "PJSR-unit-tests-support.jsh"

#define DEBUG true


#define VERSION "1.4-tests"

#include "../../main/js/PJSR-logging.jsh"

#include "../../main/js/FITSFileManager-helpers.jsh"
#include "../../main/js/FITSFileManager-engine.jsh"
#include "../../main/js/FITSFileManager-parameters.jsh"
#include "../../main/js/FITSFileManager-gui.jsh"


#include <pjsr/FileMode.jsh>


// var PROJECT_BASE_DIR = File.fullPath(File.extractDrive(#__FILE__)+File.extractDirectory(#__FILE__)+"/../..");
var SCRIPT_DIR = getDirectoryOfFileWithDriveLetter(#__FILE__);
var PROJECT_BASE_DIR = getDirectoryWithDriveLetter(SCRIPT_DIR + "/../..");
var PROJECT_DATA_DIR = PROJECT_BASE_DIR+"/data";

Console.writeln("Project base directory is '" + PROJECT_BASE_DIR + "'.");
Console.writeln("        data directory is '" + PROJECT_DATA_DIR + "'.");

// ---------------------------------------------------------------------------------------------------------
// Unit tests
// ---------------------------------------------------------------------------------------------------------

// Variable resolver factory for a resolver using a single object,
// to make mock variable resolvers for the tests
var ffM_rv = function(obj) {
   return (function(v) {
      if (obj.hasOwnProperty(v)) {
         return obj[v];
      } else {
         // do not use 'undefined' to be 'use strict' friendly
         return null;
      }
   });
}

function makeLookupConverter(aa) {
   var oa = [];
   for (var i=0; i<aa.length; i++) {
      var a = aa[i];
      oa.push({regexp: regExpToString(a[0]), replacement: a[1]});
   }
   return ffM_LookupConverter.makeLookupConverter(oa);
}


var ffM_allTests = {

   // Test that json is present
   test_JSONstringify: function() {
      var o = {s:12, a:[1,2,3]};
      var r = JSON.stringify(o);
      pT_assertEquals('{"s":12,"a":[1,2,3]}', r);
   },
   test_JSONparse: function() {
      var o = JSON.parse('{"s":12,"a":[1,2,3]}');
      var r = JSON.stringify(o);
      pT_assertEquals('{"s":12,"a":[1,2,3]}', r);
   },

   test_getDirectoryOfFileWithDriveLetter : function () {
      let testdir = getDirectoryOfFileWithDriveLetter(#__FILE__);
      pT_assertTrue(testdir.endsWith("/test/js"));
   },

   test_getDirectoryWithDriveLetter : function () {
      let filedir = getDirectoryOfFileWithDriveLetter(#__FILE__);
      let testdir = getDirectoryWithDriveLetter(filedir + "/..");
      pT_assertTrue(testdir.endsWith("/test"));
   },


   // Test that file reading/writing works as expected
   // It is not clear which encoding is used
   test_JSONFile: function() {
      var filePath = PROJECT_DATA_DIR + "/test.json";

      var js = JSON.parse('{"s":12,"a":[1,2,3]}');
      var text = JSON.stringify(js, null, 2);
      var file = new File(filePath,FileMode_Create);
      file.outText(text, DataType_String8 );
      file.close();

      file = new File(filePath, FileMode_Read);
      var buffer = file.read( DataType_ByteArray, file.size );
      file.close();
      File.remove(filePath);
      var loadedText = buffer.toString();
      var loadedJSON = JSON.parse(loadedText);

      pT_assertEquals('{"s":12,"a":[1,2,3]}', JSON.stringify(loadedJSON));

   },

   // Test if Find File works as expected
   test_FindFile: function() {
      var scriptDir = getDirectoryOfFileWithDriveLetter(#__FILE__);
      var find = new FileFind;
      var count = 0;
      if ( find.begin( scriptDir + "/*.js" ) ) {
         do
         {
            if ( find.name != "." && find.name != ".." && find.isFile ) {
               count ++;
               //Console.writeln("*** FILE  " + find.name);
            }
         } while ( find.next() );
      }
      // Number of js files in directory test/js
      pT_assertEquals(3, count);
   },

   // Test helper functions

   test_RegExpToString: function() {
      var r = /[a-z]+/i;
      var s = regExpToString(r);
      pT_assertEquals('/[a-z]+/i', s);
   },
   test_RegExpToString_null: function() {
      var s = regExpToString(null);
      pT_assertEquals('', s);
   },

   test_RegExpFromString_null: function() {
      var s = regExpFromString(null);
      pT_assertEquals(null, s);
   },
   test_RegExpFromString_empty: function() {
      var s = regExpFromString('');
      pT_assertEquals(null, s);
   },
   test_RegExpFromString_1: function() {
      var s = regExpFromString('/^[a-z]+/ig');
      pT_assertEquals('/^[a-z]+/gi', s.toString());
   },
   test_RegExpFromString_2: function() {
      var s = regExpFromString('/[a-z]+/i');
      pT_assertEquals('/[a-z]+/i', s.toString());
   },

   test_RegExpFromUserString_withSlash: function() {
      var s = regExpFromUserString('/[a-z]+/i');
      pT_assertEquals('/[a-z]+/i', s.toString());
   },
   test_RegExpFromUserString_noSlash: function() {
      var s = regExpFromUserString('[a-z]+');
      pT_assertEquals('/[a-z]+/', s.toString());
   },


   test_createUniqueName_empty: function() {
      var n = createUniqueName('prefix',[]);
      pT_assertEquals('prefix_1',n);
   },
   test_createUniqueName_none: function() {
      var n = createUniqueName('prefix',['abc','def','ghi']);
      pT_assertEquals('prefix_1',n);
   },
   test_createUniqueName_one: function() {
      var n = createUniqueName('prefix',['abc','prefix_2','ghi']);
      pT_assertEquals('prefix_3',n);
   },
   test_createUniqueName_other: function() {
      var n = createUniqueName('prefix',['abc','other_2','ghi']);
      pT_assertEquals('prefix_1',n);
   },
   test_createUniqueName_numbered_prefix: function() {
      var n = createUniqueName('prefix_3',['abc','prefix_9','ghi']);
      pT_assertEquals('prefix_10',n);
   },
   test_createUniqueName_numbered_prefix_other: function() {
      //current name is always part of the list
      var n = createUniqueName('prefix_3',['abc','other_9','prefix_3','ghi']);
      pT_assertEquals('prefix_4',n);
   },
   test_createUniqueName_multi: function() {
      var n = createUniqueName('prefix',['abc','prefix_567','prefix_23']);
      pT_assertEquals('prefix_568',n);
   },


   test_deep_copy_null: function() {
      pT_assertEquals(null,deepCopyData(null));
   },
   test_deep_copy_base: function() {
      pT_assertEquals(6,deepCopyData(6));
   },
   test_deep_copy_date: function() {
      var s = new Date();
      var d = deepCopyData(s);
      pT_assertEquals(s.toString(),d.toString());
      d.setFullYear(d.getFullYear()+1);
      pT_assertEquals(s.getFullYear()+1,d.getFullYear());
   },
   test_deep_copy_regexp: function() {
      var s = /[a-z]+/i;
      var d = deepCopyData(s);
      pT_assertEquals(s.toString(),d.toString());
   },
   test_deep_copy_array: function() {
      var s = [1,2,3];
      var d = deepCopyData(s);
      //Log.debug(Log.pp(d));
      pT_assertArrayEquals([1,2,3],d);
      s[1] = 4;
      pT_assertEquals(2,d[1]);
   },
   test_deep_copy_array_empty: function() {
      var d = deepCopyData([]);
      //Log.debug(Log.pp(d));
      pT_assertArrayEquals([],d);
   },
   test_deep_copy_object: function() {
      var s = {k:1,v:3};
      var d= deepCopyData(s);
      //Log.debug(Log.pp(d));
      pT_assertEquals(3,d.v);
      d.v=5;
      pT_assertEquals(3,s.v);
   },
   test_deep_copy: function() {
      var s = {k:1,v: new Date(), a:[1,3, {anobj: 5}]};
      var d= deepCopyData(s);
      //Log.debug(Log.pp(d));
      pT_assertEquals(5,d.a[2].anobj);
      d.a[2].anobj=7;
      pT_assertEquals(5,s.a[2].anobj);
      pT_assertEquals(7,d.a[2].anobj);
   },


   // Test our mock variable resolver
   test_ffM_rv_found: function() {
      pT_assertEquals("toto", ffM_rv({titi: "toto"})("titi"));
   },
   test_ffM_rv_not_found: function() {
      pT_assertNull(ffM_rv({titi: "toto"})("toto"));
   },

   // ---------------------------------------------------------------------------------------------------------
   // Test of converter
   // Note that converter first test of the MATCH (anyhwere) of the regexp,
   // then replace by the conversion result, doing pattern replacement ON THE CONVERSION RESULT
   // not on the string received
   // ---------------------------------------------------------------------------------------------------------
   // Convert array of array of regexp,replacement to the object format and submit to real converter

   testConverter_empty: function () {
      var c = makeLookupConverter([]);
      pT_assertNull(c.convert("thereIsNoConversionDefined"));
   },
   testConverter_single_conversion: function () {
      var c = makeLookupConverter([
            [ /abc/, "matched"]
         ]);
      pT_assertNull(c.convert("nomatch"));
      pT_assertEquals("matched",c.convert("abc"));
   },
   testConverter_valid_second_conversion: function () {
      var c = makeLookupConverter([
            [ /first/, "match1"],
            [ /second/, "match2" ],
         ]);
      pT_assertNull(c.convert("nomatch"));
      pT_assertEquals("match2",c.convert("second"));
      pT_assertEquals("match1",c.convert("first"));
   },
   testConverter_valid_last_conversion: function () {
      var c = makeLookupConverter([
            [ /first/,  "match1"],
            [ /second/, "match2" ],
            [ /third/,  "match3" ]
         ]);
      pT_assertNull(c.convert("nomatch"));
      pT_assertEquals("match3",c.convert("third"));
      pT_assertEquals("match2",c.convert("second"));
      pT_assertEquals("match1",c.convert("first"));
   },
   testConverter_detect_partial: function () {
      var c = makeLookupConverter([
            [ /TEXT/, "matched"]
         ]);
      pT_assertNull(c.convert("nomatch"));
      pT_assertEquals("matched",c.convert("aTEXTinside"));
   },
   testConverter_obey_anchored: function () {
      var c = makeLookupConverter([
            [ /^TEXT/, "matched"]
         ]);
      pT_assertNull(c.convert("aTEXTinside"));
   },
   testConverter_obey_ignore_case: function () {
      var c = makeLookupConverter([
            [ /TEXT/i, "Matched"]
         ]);
         pT_assertEquals("Matched",c.convert("isText"));
         pT_assertEquals("Matched",c.convert("isTEXT"));
         pT_assertEquals("Matched",c.convert("is-text"));
   },

   // Last chance handler
   testConverter_sourceGroupReference_match_any_char: function () {
      // This case &0; is optimized
      var c = makeLookupConverter([
            [ /TEXT/, "specificMatch"],
            [ /./, "&0;"] // Match any character
         ]);
      pT_assertEquals("allchars",c.convert("allchars"));
      pT_assertEquals("specificMatch",c.convert("aTEXT"));
   },
   testConverter_sourceGroupReference_match_any: function () {
      // This case &0; is optimized
      var c = makeLookupConverter([
            [ /TEXT/, "specificMatch"],
            [ /.*/, "&0;" ]
         ]);
      pT_assertEquals("allchars",c.convert("allchars"));
      pT_assertEquals("specificMatch",c.convert("aTEXT"));
   },

   testConverter_sourceGroupReference_whole_inside: function () {
      var c = makeLookupConverter([
            [ /.*/, "_&0;/"]
         ]);
      pT_assertEquals("_red/",c.convert("red"));
   },
   testConverter_sourceGroupReference_keep_case: function () {
      var c = makeLookupConverter([
            [ /.*/, "&0;"]
         ]);
      pT_assertEquals("lowerUPPER",c.convert("lowerUPPER"));
   },

   testConverter_sourceGroupReference_remove_special_chars: function () {
      var c = makeLookupConverter([
            [ /.*/, "&0;"]
         ]);
      pT_assertEquals("a_bunch_of_stuff",c.convert("^a bunch%of$stuff"));
   },

   testConverter_sourceGroupReference_remove_special_chars_in_groups: function () {
      var c = makeLookupConverter([
            [ /(.*)-(.*)/, "_&1;*&2;/"]
         ]);
      pT_assertEquals("_a_lot_of*this_stuff/",c.convert("^a lot%of-this$stuff"));
   },

   testConverter_sourceGroupReference_subgroup: function () {
      var c = makeLookupConverter([
            [ /TEXT/, "specificMatch"],
            [ /filter-(.*)/, "_&1;/"]
         ]);
      pT_assertEquals("specificMatch",c.convert("TEXT"));
      pT_assertEquals("_red/",c.convert("filter-red"));
   },
   testConverter_multiple_back_references: function () {
      var c = makeLookupConverter([
            [ /FIRST/, "&0;"],
            [ /SECOND/i, "&0;"],
            [ /THIRD/i, "&0;"],
         ]);
      pT_assertNull(c.convert("nomatch"));
      // Currently back refernce as convert to lower case
      pT_assertEquals("aFIRST",c.convert("aFIRST"));
      pT_assertEquals("THEsecond",c.convert("THEsecond"));
      pT_assertEquals("isThird",c.convert("isThird"));
      pT_assertNull(c.convert("nomatchagain"));
   },
   testConverter_sourceGroupReference_multiple_ref: function () {
      var c = makeLookupConverter([
            [ /([0-9]+)-filter-([a-z]+)/, "&2;_&1;/"],
         ]);
      pT_assertEquals("green_450/",c.convert("450-filter-green"));
   },
   // This may help discover subtle bugs with improper lexical scoping
   testConverter_sourceGroupReference_multiple_ref_and_tests: function () {
      var c = makeLookupConverter([
            [ /BEFORE/, "specificMatchBefore"],
            [ /([0-9]+)-filter-([a-z]+)/, "&2;_&1;/"],
            [ /AFTER/, "specificMatchAfter"],
            [ /UNUSED/, "unused"],
         ]);
      pT_assertEquals("specificMatchBefore",c.convert("BEFORE"));
      pT_assertEquals("specificMatchAfter",c.convert("AFTER"));
      pT_assertEquals("red_123/",c.convert("123-filter-red"));
   },
   // This may help discover subtle bugs with improper lexical scoping
   testConverter_twoOfThem: function () {
      var c1 = makeLookupConverter([
            [ /abc/,   "toto"],
            [ /def/,   "ti&0;ti"]
         ]);
      var c2 = makeLookupConverter([
            [ /123/, "NUMBER"]
         ]);
      pT_assertNull(c1.convert("123"));
      pT_assertEquals("tidefti",c1.convert("def"));
      pT_assertNull(c2.convert("abc"));
      pT_assertEquals("NUMBER",c2.convert("hereAnumber:123"));
   },


   // ---------------------------------------------------------------------------------------------------------
   // Test of template mechanism
   // ---------------------------------------------------------------------------------------------------------

   testTemplate_text: function () {
      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"textonly");
      pT_assertEquals("literalRule('textonly')",t.toString());
      pT_assertEquals(0, templateErrors.length);
   },
   testTemplate_inside: function () {
      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"beg&VAR;end");
      pT_assertEquals("literalRule('beg'),lookUpRule('VAR':[onFound:copyValue()]:[onMissing:reject()]),literalRule('end')",t.toString());
      pT_assertEquals(0, templateErrors.length);
   },
   testTemplate_start: function () {
      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"&VAR;atend");
      pT_assertEquals("lookUpRule('VAR':[onFound:copyValue()]:[onMissing:reject()]),literalRule('atend')",t.toString());
      pT_assertEquals(0, templateErrors.length);
   },
   testTemplate_end: function () {
      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"atbeg&VAR;");
      pT_assertEquals("literalRule('atbeg'),lookUpRule('VAR':[onFound:copyValue()]:[onMissing:reject()])",t.toString());
      pT_assertEquals(0, templateErrors.length);
   },
   testTemplate_alone: function () {
      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"&VAR;");
      pT_assertEquals("lookUpRule('VAR':[onFound:copyValue()]:[onMissing:reject()])",t.toString());
      pT_assertEquals(0, templateErrors.length);
   },
   testTemplate_two: function () {
      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"beg&VAR1;middle&VAR2;end");
      pT_assertEquals("literalRule('beg'),lookUpRule('VAR1':[onFound:copyValue()]:[onMissing:reject()]),literalRule('middle'),lookUpRule('VAR2':[onFound:copyValue()]:[onMissing:reject()]),literalRule('end')",t.toString());
      pT_assertEquals(0, templateErrors.length);
   },
   testTemplate_together: function () {
      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"beg&VAR1;&VAR2;end");
      pT_assertEquals("literalRule('beg'),lookUpRule('VAR1':[onFound:copyValue()]:[onMissing:reject()]),lookUpRule('VAR2':[onFound:copyValue()]:[onMissing:reject()]),literalRule('end')",t.toString());
      pT_assertEquals(0, templateErrors.length);
   },

   testTemplate_present: function () {
      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"beg&VAR:present;end");
      pT_assertEquals("literalRule('beg'),lookUpRule('VAR':[onFound:formatValueAs('present')]:[onMissing:reject()]),literalRule('end')",t.toString());
      pT_assertEquals(0, templateErrors.length);
   },
   testTemplate_missing: function () {
      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"beg&VAR?missing;end");
      pT_assertEquals("literalRule('beg'),lookUpRule('VAR':[onFound:copyValue()]:[onMissing:copyLiteral('undefined')]),literalRule('end')",t.toString());
      pT_assertEquals(0, templateErrors.length);
   },
   testTemplate_present_missing: function () {
      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"beg&VAR:present?missing;end");
      pT_assertEquals("literalRule('beg'),lookUpRule('VAR':[onFound:formatValueAs('present')]:[onMissing:copyLiteral('present')]),literalRule('end')",t.toString());
      pT_assertEquals(0, templateErrors.length);
   },
   testTemplate_none: function () {
      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"beg&NONE:;end");
      pT_assertEquals("literalRule('beg'),lookUpRule('NONE':[onFound:copyLiteral('')]:[onMissing:reject()]),literalRule('end')",t.toString());
      pT_assertEquals(0, templateErrors.length);
   },
   testTemplate_opt: function () {
      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"beg&OPT?;end");
      pT_assertEquals("literalRule('beg'),lookUpRule('OPT':[onFound:copyValue()]:[onMissing:copyLiteral('')]),literalRule('end')",t.toString());
      pT_assertEquals(0, templateErrors.length);
   },
   testTemplate_error: function () {
      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"beg&no-semi-colon-end");
      pT_assertEquals(1, templateErrors.length);
   },

   // Test resolver
   testResolve_text: function () {
      var templateErrors = [];
      var errors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"textonly");
      pT_assertEquals(0, templateErrors.length);
      pT_assertEquals("textonly",t.expandTemplate(errors,null));
      pT_assertEquals(0, errors.length);
   },
   testResolve_var: function () {
      var templateErrors = [];
      var errors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"&var;");
      pT_assertEquals(0, templateErrors.length);
      pT_assertEquals("value",t.expandTemplate(errors,ffM_rv({'var':'value'})));
      pT_assertEquals(0, errors.length);
   },
   testResolve_var_found: function () {
      var templateErrors = [];
      var errors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"&var:found;");
      pT_assertEquals(0, templateErrors.length);
      pT_assertEquals("found",t.expandTemplate(errors,ffM_rv({'var':'value'})));
      pT_assertEquals(0, errors.length);
   },
   testResolve_var_found_2: function () {
      var templateErrors = [];
      var errors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"&var:found?notfound;");
      pT_assertEquals(0, templateErrors.length);
      pT_assertEquals("found",t.expandTemplate(errors,ffM_rv({'var':'value'})));
      pT_assertEquals(0, errors.length);
   },
   testResolve_var_found_empty: function () {
      var templateErrors = [];
      var errors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"&var:;");
      pT_assertEquals(0, templateErrors.length);
      pT_assertEquals("",t.expandTemplate(errors,ffM_rv({'var':'value'})));
      pT_assertEquals(0, errors.length);
   },
   testResolve_var_not_found: function () {
      var templateErrors = [];
      var errors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"&var?notfound;");
      pT_assertEquals(0, templateErrors.length);
      pT_assertEquals("notfound",t.expandTemplate(errors,ffM_rv({'NOPE':'value'})));
      pT_assertEquals(0, errors.length);
   },
   testResolve_var_optional: function () {
      var templateErrors = [];
      var errors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"&var?;");
      pT_assertEquals(0, templateErrors.length);
      pT_assertEquals("",t.expandTemplate(errors,ffM_rv({'NOPE':'value'})));
      pT_assertEquals(0, errors.length);
   },
   testResolve_var_complex: function () {
      var templateErrors = [];
      var errors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"AB,&v1;,CD,&v2:v2fmt?v2missing;,EF,&v3?;,GH,&v4?v4opt;,IJ,&v5;,KL");
      pT_assertEquals(0, templateErrors.length);
      pT_assertEquals("AB,v1val,CD,v2fmt,EF,,GH,v4opt,IJ,v5val,KL",
            t.expandTemplate(errors,ffM_rv({v1:'v1val', v2:'NO', v5:'v5val'})));
      pT_assertEquals(0, errors.length);
   },

   testResolve_error: function () {
      var templateErrors = [];
      var errors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"abc&required;def");
      pT_assertEquals(0, templateErrors.length);
      t.expandTemplate(errors,ffM_rv({v1:'v1val', v2:'NO', v5:'v5val'}));
      pT_assertEquals("No value for the variable 'required'", errors.join(""));
   },

   // ---------------------------------------------------------------------------------------------------------
   // Test of gui parameters and settings
   // ---------------------------------------------------------------------------------------------------------
   testSettings: function () {
      var guiParameters = new FFM_GUIParameters();
      // This ensures that there are default settings in first execution
      guiParameters.loadSettings();
      guiParameters.initializeParametersToDefaults();
      guiParameters.saveSettings();

      guiParameters.loadSettings();
      // Test soem values
      pT_assertEquals('&1;_&binning;_&temp;C_&type;_&exposure;s_&filter;_&count;&extension;',guiParameters.targetFileNameTemplate);
      pT_assertEquals('/([^-_.]+)(?:[._-]|$)/',guiParameters.sourceFileNameRegExp.toString());
      pT_assertEquals('&rank;',guiParameters.orderBy);
      pT_assertEquals('&targetDir;',guiParameters.groupByTemplate);
    
   },

   // ---------------------------------------------------------------------------------------------------------
   // Test of configurations
   // ---------------------------------------------------------------------------------------------------------
   testDefaultConfiguration: function() {
 
      ffM_Configuration.restoreDefaultConfiguration();
      
      var c = ffM_Configuration.getConfigurationTable();
      pT_assertTrue(c[0].name,"Default");
   },

   testSaveLoadConfiguration: function() {
      var filePath = PROJECT_DATA_DIR + "/testDefault.ffm-configs";

      var userConf = deepCopyData(ffM_Configuration.getConfigurationTable());
      var result = ffM_Configuration.saveConfigurationFile(filePath, userConf);
      pT_assertNull(result);

      var resultAndData = ffM_Configuration.loadConfigurationFile(filePath);
      pT_assertEmptyArray(resultAndData.messages);
      // Do not use pT_assertEquals, as error message is too long
      pT_assertTrue(JSON.stringify(userConf) === JSON.stringify(resultAndData.configurations));
   },

   testLoadConfigurationNoFile: function() {
      var filePath = PROJECT_DATA_DIR + "/NON-EXISTANT-FILE.ffm-configs";
      var resultAndData = ffM_Configuration.loadConfigurationFile(filePath);
      pT_assertNull(resultAndData.configurations);
      pT_assertTrue(resultAndData.messages[0].endsWith("is not readable."));
   },

   testLoadConfigurationSyntaxError: function() {
      var filePath = PROJECT_DATA_DIR+"/badConfigSyntax.ffm-configs";
      var resultAndData = ffM_Configuration.loadConfigurationFile(filePath);
      pT_assertNull(resultAndData.configurations);
      pT_assertTrue(resultAndData.messages[0].endsWith("not JSON format."));
   },

   testLoadConfigurationSentinelError: function() {
      var filePath = PROJECT_DATA_DIR+"/badConfigSentinel.ffm-configs";
      var resultAndData = ffM_Configuration.loadConfigurationFile(filePath);
      pT_assertNull(resultAndData.configurations);
      pT_assertTrue(resultAndData.messages[0].endsWith("(missing or bad 'sentinel')."));
   },

   testLoadConfigurationVersionError: function() {
      var filePath = PROJECT_DATA_DIR+"/badConfigVersion.ffm-configs";
      var resultAndData = ffM_Configuration.loadConfigurationFile(filePath);
      pT_assertNull(resultAndData.configurations);
      pT_assertTrue(resultAndData.messages[0].endsWith("than current version " + PARAMETERS_VERSION + "."));
   },


   testValidateConfigurationDataNotArray: function() {
      var msgs = [];
      var r = ffM_Configuration.validateConfigurationData(123,msgs);
      pT_assertEquals("Not array",msgs[0]);
      pT_assertNull(r);
   },

   testValidateConfigurationDataEmptyArray: function() {
      var msgs = [];
      var r = ffM_Configuration.validateConfigurationData([], msgs);
      pT_assertEquals("Empty array",msgs[0]);
      pT_assertNull(r);
   },


  testValidateConfigurationDataBadShow: function() {
      var msgs = [];
      var r = ffM_Configuration.validateConfigurationData(
         [{name:"astring", description:"aString",  variableList:
            [{name:'v', description:'d', show:"badstring", resolver:'aResolver', parameters: []}]}],msgs);
      pT_assertEquals("'variableList[0].show' not boolean",msgs[0]);
      pT_assertNull(r);
   },

 testValidateConfigurationDataBadParameters: function() {
      var msgs = [];
      var r = ffM_Configuration.validateConfigurationData(
         [{name:"astring", description:"aString", variableList:
            [{name:'v', description:'d', show:false, resolver:'aResolver', parameters: 'shouldbeobject'}]}],msgs);
      pT_assertEquals("'variableList[0].parameters' not object",msgs[0]);
      pT_assertNull(r);
   },


   // ---------------------------------------------------------------------------------------------------------
   // Test of helper functions
   // ---------------------------------------------------------------------------------------------------------

   testJulianDay: function() {
      var jd = julianDay(new Date(2015,0,23,23,52));
      pT_assertEquals(2457046,jd);
   },

   testDateFormatter: function() {
      var date = new Date(2015,112,23,22,67,32,15); // end in 15 ms

      pT_assertEquals("Nothing, % is escaped.",formatDate("Nothing, %% is escaped.", date));
      pT_assertEquals("Year Y:2024, y:24.",formatDate("Year Y:%Y, y:%y.", date));
      pT_assertEquals("YY-MM-DD hh:mm:ss.lll: 2024-05-23 23:07:32.015",formatDate("YY-MM-DD hh:mm:ss.lll: %Y-%m-%d %H:%M:%S.%L", date));
      pT_assertEquals("Julian day: 2460454.",formatDate("Julian day: %j.", date));
   },

   testDateParser: function() {
      pT_assertEquals(new Date(1995,12,23,13,45,32).getTime(),parseFITSDateTime("1995-12-23T13:45:32").getTime());
      pT_assertEquals(new Date(1995,12,23,13,45,32,432).getTime(),parseFITSDateTime("1995-12-23T13:45:32.432").getTime());
      pT_assertEquals(new Date(1995,12,23,13,45,32,430).getTime(),parseFITSDateTime("1995-12-23T13:45:32.43").getTime());
      pT_assertEquals(new Date(1995,12,23,13,45,32,900).getTime(),parseFITSDateTime("1995-12-23T13:45:32.9").getTime());
      pT_assertEquals(new Date(2012,2,28).getTime(),parseFITSDateTime("2012-02-28").getTime());
   },



}




// ---------------------------------------------------------------------------------------------------------
// Auto execute tests on load
// ---------------------------------------------------------------------------------------------------------



pT_executeTests(ffM_allTests);

