"use strict";

// FITSFileManager-tests


// Copyright (c) 2012 Jean-Marc Lugrin


#include "PJSR-unit-tests-support.jsh"

#define DEBUG true


#define VERSION "0.3"

#include "../../main/js/FITSFileManager-helpers.jsh"
#include "../../main/js/FITSFileManager-engine.jsh"
#include "../../main/js/FITSFileManager-gui.jsh"


// ---------------------------------------------------------------------------------------------------------
// Check environment for tests
// ---------------------------------------------------------------------------------------------------------



// ---------------------------------------------------------------------------------------------------------
// Unit tests
// ---------------------------------------------------------------------------------------------------------


var ffM_rv = function(obj) {
   return (function(v) {
      return obj[v];
   });
}

var ffM_allTests = {
   test_ffM_rv_found: function() {
      pT_assertEquals("toto", ffM_rv({titi: "toto"})("titi"));
   },
   test_ffM_rv_not_found: function() {
      pT_assertUndefined(ffM_rv({titi: "toto"})("toto"));
   },

   // ---------------------------------------------------------------------------------------------------------
   // Test of converter
   // ---------------------------------------------------------------------------------------------------------
   testConverter_empty: function () {
      var c = new FFM_Converter();
      pT_assertNull(c.convert("anything"));
   },
   testConverter_one: function () {
      var c = new FFM_Converter();
      c.conversions = [{regexp: /abc/, replacement: "toto"}];
      pT_assertNull(c.convert("anything"));
      pT_assertEquals("toto",c.convert("abc"));
   },
   testConverter_second: function () {
      var c = new FFM_Converter();
      c.conversions = [{regexp: /abc/, replacement: "toto"},
                       {regexp: /def/, replacement: "titi"}];
      pT_assertNull(c.convert("anything"));
      pT_assertEquals("titi",c.convert("def"));
   },
   testConverter_backreference: function () {
      var c = new FFM_Converter();
      c.conversions = [{regexp: /abc/, replacement: "toto"},
                       {regexp: /(def)/, replacement: "ti$1ti"}];
      pT_assertNull(c.convert("anything"));
      pT_assertEquals("tidefti",c.convert("def"));
   },
   testConverter_towOfThem: function () {
      var c1 = new FFM_Converter();
      c1.conversions = [{regexp: /abc/, replacement: "toto"},
                       {regexp: /(def)/, replacement: "ti$1ti"}];
      var c2 = new FFM_Converter();
      c2.conversions = [{regexp: /123/, replacement: "NMB"}];
      pT_assertNull(c1.convert("123"));
      pT_assertEquals("tidefti",c1.convert("def"));
      pT_assertNull(c2.convert("abc"));
      pT_assertEquals("aNMBz",c2.convert("a123z"));
   },

   // ---------------------------------------------------------------------------------------------------------
   // Test of filter conversion
   // ---------------------------------------------------------------------------------------------------------
   testConvertFilter_base: function () {
      var f = convertFilter("some blue");
      pT_assertEquals("blue",f);
   },

   // ---------------------------------------------------------------------------------------------------------
   // Test of type conversion
   // ---------------------------------------------------------------------------------------------------------
   testConvertType_base: function () {
      var t = convertType("FLAT");
      pT_assertEquals("flat",t);
   },

   // ---------------------------------------------------------------------------------------------------------
   // Test of template mechanism
   // ---------------------------------------------------------------------------------------------------------

   testTemplate_text: function () {
      var t = ffM_template.analyzeTemplate("textonly");
      pT_assertEquals("literalRule('textonly')",t.toString());
   },
   testTemplate_inside: function () {
      var t = ffM_template.analyzeTemplate("beg&VAR;end");
      pT_assertEquals("literalRule('beg'),lookUpRule('VAR':[onFound:copyValue()]:[onMissing:reject()]),literalRule('end')",t.toString());
   },
   testTemplate_start: function () {
      var t = ffM_template.analyzeTemplate("&VAR;atend");
      pT_assertEquals("lookUpRule('VAR':[onFound:copyValue()]:[onMissing:reject()]),literalRule('atend')",t.toString());
   },
   testTemplate_end: function () {
      var t = ffM_template.analyzeTemplate("atbeg&VAR;");
      pT_assertEquals("literalRule('atbeg'),lookUpRule('VAR':[onFound:copyValue()]:[onMissing:reject()])",t.toString());
   },
   testTemplate_alone: function () {
      var t = ffM_template.analyzeTemplate("&VAR;");
      pT_assertEquals("lookUpRule('VAR':[onFound:copyValue()]:[onMissing:reject()])",t.toString());
   },
   testTemplate_two: function () {
      var t = ffM_template.analyzeTemplate("beg&VAR1;middle&VAR2;end");
      pT_assertEquals("literalRule('beg'),lookUpRule('VAR1':[onFound:copyValue()]:[onMissing:reject()]),literalRule('middle'),lookUpRule('VAR2':[onFound:copyValue()]:[onMissing:reject()]),literalRule('end')",t.toString());
   },
   testTemplate_together: function () {
      var t = ffM_template.analyzeTemplate("beg&VAR1;&VAR2;end");
      pT_assertEquals("literalRule('beg'),lookUpRule('VAR1':[onFound:copyValue()]:[onMissing:reject()]),lookUpRule('VAR2':[onFound:copyValue()]:[onMissing:reject()]),literalRule('end')",t.toString());
   },

   testTemplate_present: function () {
      var t = ffM_template.analyzeTemplate("beg&VAR:present;end");
      pT_assertEquals("literalRule('beg'),lookUpRule('VAR':[onFound:formatValueAs('present')]:[onMissing:reject()]),literalRule('end')",t.toString());
   },
   testTemplate_missing: function () {
      var t = ffM_template.analyzeTemplate("beg&VAR?missing;end");
      pT_assertEquals("literalRule('beg'),lookUpRule('VAR':[onFound:copyValue()]:[onMissing:copyLiteral('undefined')]),literalRule('end')",t.toString());
   },
   testTemplate_present_missing: function () {
      var t = ffM_template.analyzeTemplate("beg&VAR:present?missing;end");
      pT_assertEquals("literalRule('beg'),lookUpRule('VAR':[onFound:formatValueAs('present')]:[onMissing:copyLiteral('present')]),literalRule('end')",t.toString());
   },
   testTemplate_none: function () {
      var t = ffM_template.analyzeTemplate("beg&NONE:;end");
      pT_assertEquals("literalRule('beg'),lookUpRule('NONE':[onFound:copyLiteral('')]:[onMissing:reject()]),literalRule('end')",t.toString());
   },
   testTemplate_opt: function () {
      var t = ffM_template.analyzeTemplate("beg&OPT?;end");
      pT_assertEquals("literalRule('beg'),lookUpRule('OPT':[onFound:copyValue()]:[onMissing:copyLiteral('')]),literalRule('end')",t.toString());
   },

   testResolve_text: function () {
      var errors = [];
      var t = ffM_template.analyzeTemplate("textonly");
      pT_assertEquals("textonly",t.expandTemplate(errors,null));
      pT_assertEquals(0, errors.length);
   },
   testResolve_var: function () {
      var errors = [];
      var t = ffM_template.analyzeTemplate("&var;");
      pT_assertEquals("value",t.expandTemplate(errors,ffM_rv({'var':'value'})));
      pT_assertEquals(0, errors.length);
   },
   testResolve_var_found: function () {
      var errors = [];
      var t = ffM_template.analyzeTemplate("&var:found;");
      pT_assertEquals("found",t.expandTemplate(errors,ffM_rv({'var':'value'})));
      pT_assertEquals(0, errors.length);
   },
   testResolve_var_found_2: function () {
      var errors = [];
      var t = ffM_template.analyzeTemplate("&var:found?notfound;");
      pT_assertEquals("found",t.expandTemplate(errors,ffM_rv({'var':'value'})));
      pT_assertEquals(0, errors.length);
   },
   testResolve_var_found_empty: function () {
      var errors = [];
      var t = ffM_template.analyzeTemplate("&var:;");
      pT_assertEquals("",t.expandTemplate(errors,ffM_rv({'var':'value'})));
      pT_assertEquals(0, errors.length);
   },
   testResolve_var_not_found: function () {
      var errors = [];
      var t = ffM_template.analyzeTemplate("&var?notfound;");
      pT_assertEquals("notfound",t.expandTemplate(errors,ffM_rv({'NOPE':'value'})));
      pT_assertEquals(0, errors.length);
   },
   testResolve_var_optional: function () {
      var errors = [];
      var t = ffM_template.analyzeTemplate("&var?;");
      pT_assertEquals("",t.expandTemplate(errors,ffM_rv({'NOPE':'value'})));
      pT_assertEquals(0, errors.length);
   },
   testResolve_var_complex: function () {
      var errors = [];
      var t = ffM_template.analyzeTemplate("AB,&v1;,CD,&v2:v2fmt?v2missing;,EF,&v3?;,GH,&v4?v4opt;,IJ,&v5;,KL");
      pT_assertEquals("AB,v1val,CD,v2fmt,EF,,GH,v4opt,IJ,v5val,KL",
            t.expandTemplate(errors,ffM_rv({v1:'v1val', v2:'NO', v5:'v5val'})));
      pT_assertEquals(0, errors.length);
   },

   testResolve_error: function () {
      var errors = [];
      var t = ffM_template.analyzeTemplate("abc&required;def");
      t.expandTemplate(errors,ffM_rv({v1:'v1val', v2:'NO', v5:'v5val'}));
      pT_assertEquals("", errors.join(""));
   },


}




// ---------------------------------------------------------------------------------------------------------
// Auto execute tests on load
// ---------------------------------------------------------------------------------------------------------



pT_executeTests(ffM_allTests);

