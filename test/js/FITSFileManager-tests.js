"use strict";

// FITSFileManager-tests


// This file is part of FITSFileManager, see copyrigh in FITSFileManager.js


#include "PJSR-unit-tests-support.jsh"

#define DEBUG true


#define VERSION "0.8-tests"

#include "../../main/js/PJSR-logging.jsh"

#include "../../main/js/FITSFileManager-helpers.jsh"
#include "../../main/js/FITSFileManager-json.jsh"
#include "../../main/js/FITSFileManager-engine.jsh"
#include "../../main/js/FITSFileManager-parameters.jsh"
#include "../../main/js/FITSFileManager-gui.jsh"



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

   // Test json
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


   // Test helper

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
      // TODO make some real tests
     // pT_assertEquals("string",guiParameters.targetFileNameCompiledTemplate);
   },




}




// ---------------------------------------------------------------------------------------------------------
// Auto execute tests on load
// ---------------------------------------------------------------------------------------------------------



pT_executeTests(ffM_allTests);

