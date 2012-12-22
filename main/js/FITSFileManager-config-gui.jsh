// FITSFileManager-config-gui.js

// This file is part of FITSFileManager, see copyrigh in FITSFileManager.js




// ========================================================================================================================
// Model
// ========================================================================================================================

// There is a 'pure data' representation of the rules (so they can be serialized)
// and the Model complement this representation with convenience methods

// RuleSet data (ordered list of rules, the order does not matter for the semantic)
//    [rule]

// Rule data (the variables is an ordered list)
//    {name: aString, description: aString, variableList: []}

// Variable: (the parameters dictionary depends on the resolver)
//    {name: aString, description: aString, resolver: aName, parameters: {}}


// Part of Model (rules to handle the 'pure data'):
// The resolvers are fixed, they are defined as:
//   {name: aString, description: aString, control: aControl}
// (more information may be added later).

var ffM_RuleSet_Model = (function(){

   // -- RuleSet support functions
   // Get the names of the rules
   var ruleNames = function(ruleSet) {
      var names = [];
      for (var i=0; i<ruleSet.length; i++) {
         names.push(ruleSet[i].name);
      }
      return names;
   }



   // Define specific rule parameters
   // First RegExp match value
   var makeFirstRegExpMapping = function(aFITSKey) {
      return {
         key: aFITSKey,
         mappings: [],
      }
   }

  var currentVariable = '';


   // ========================================================================================================================
   // SUPPORT FOR TESTS

   // Model of variable - define a new variable
   var defineVariable = function(name, description, resolver) {
      return {
         name: name,
         description: description,
         resolver: resolver,
      }
   }

   // Model object wrap data object and behavior
   var newRuleData = function(name, description) {
      var rule = {
         name: name,
         description: description,
         variableList: [],
      };
      var builder = {
         // Operations on the variable list
         addVariable: function(variable) {
            rule.variableList.push(variable);
            return builder;
         },
         build: function() {
            return rule;
         },
      }

      return  builder;

   }

   // The type consist of a name and type specific parameters.
   // Each type may have another UI matching its parameters

   var defaultTypes = [
      {name: 'Type', parameters: {key:'?'}},
      {name: 'Filter', parameters: {key:'?'}},
      {name: 'Exposure', parameters: {key:'?'}},
      {name: 'Temperature', parameters: {key:'?'}},
      {name: 'Binning', parameters: {key:'?'}},
      {name: 'Night', parameters: {key:'?'}},
      {name: 'FileName', parameters: {key:'?'}},
      {name: 'Extension', parameters: {key:'?'}},
   ]

   var typeMapping = {};
   for (var i in defaultTypes) {
      typeMapping[defaultTypes[i].name] = defaultTypes[i];
   }


   // Default definition for test
   var defaultRule = newRuleData('Default', 'Common FITS rules')
   .addVariable(defineVariable('type','Type of image (flat, bias, ...)','Type'))
   .addVariable(defineVariable('filter','Filter (clear, red, ...)','Filter'))
   .addVariable(defineVariable('exposure','Exposure in seconds','Exposure'))
   .addVariable(defineVariable('temp','Temperature in C','Temperature'))
   .addVariable(defineVariable('binning','Binning as 1x1, 2x2, ...','Binning'))
   .addVariable(defineVariable('night','night (experimental)','Night'))
   .addVariable(defineVariable('filename','Input file name','FileName'))
   .addVariable(defineVariable('extension','Input file extension','Extension'))
   .build();

   var testRule = newRuleData('Test', 'A test rule')
   .addVariable(defineVariable('object','Object','Object'))
   .build();

   // Test Rules
   var testRules = [];
   testRules.push(defaultRule);
   testRules.push(testRule);


   return {
      ruleNames: ruleNames,
      // For tests
      testRules: testRules,
      currentVariable: currentVariable,
   }
}) ();


// ========================================================================================================================
// GUI support methods
// ========================================================================================================================


var ffM_GUI_support = (function (){



// ---------------------------------------------------------------------------------------------------------

// buttons is an array of objects with {icon, toolTip, action}
var IconButtonBar = function(parent, buttons) {
   this.__base__ = Control;
   this.__base__(parent);
   var i, button;
   this.sizer =  new HorizontalSizer;
   this.sizer.margin = 6;
   this.sizer.spacing = 4;

   for (i=0; i<buttons.length;i ++) {
      var button = buttons[i];
      //Log.debug(i,button.icon, button.toolTip);
      var toolButton = new ToolButton( parent );
      this.sizer.add(toolButton);
      toolButton.icon = new Bitmap( button.icon );
      toolButton.toolTip = button.toolTip;
      toolButton.onClick = button.action;
    }
   this.sizer.addStretch();
   // Required ?
   //this.adjustToContents();
}
IconButtonBar.prototype = new Control;


var makeTabBox = function(parent, boxes) {
   var tabBox = new TabBox(parent);
   for (i=0; i<boxes.length;i ++) {
      var box = boxes[i];
      tabBox.add(box);
   }
   return tabBox;
}


// ---------------------------------------------------------------------------------------------------------

function ManagedList_Box(parent, initialListModel, elementFactory, toolTip, selectionCallback) {
   this.__base__ = Control;
   this.__base__(parent);

   var listModel = [];


   // nodeMode is {node, description}
   var makeNode = function(treeBox, nodeModel, index) {
      var node = new TreeBoxNode( treeBox, index);
      node.setText( 0, nodeModel.name );
      node.setText( 1, nodeModel.description );
      return node;
   }



   var i;

   this.sizer = new VerticalSizer;

   var treeBox = new TreeBox(this);
   this.sizer.add(treeBox);

   treeBox.rootDecoration = false;
   treeBox.numberOfColumns = 2;
   treeBox.multipleSelection = false;
   treeBox.headerVisible = false;
   treeBox.headerSorting = false;
   treeBox.sort(0,true);
   //treeBox.setMinSize( 700, 200 );
   treeBox.toolTip = toolTip;



   this.modelListChanged = function(newModelList) {

      var i;
      var nmbNodes = treeBox.numberOfChildren;
      for (i=nmbNodes; i>0; i--) {
         treeBox.remove(i-1);
      }

      listModel = newModelList;

      for (i=0; i<listModel.length; i++) {
         // Just making the nodes add them to the treeBox
         makeNode(treeBox, listModel[i], i);
      }
   }
   //this.modelListChanged(initialListModel);


   var upAction = function() {
      var nodeToMove,  nodeIndex, element1, element2;
      if (treeBox.selectedNodes.length>0) {
         nodeToMove = treeBox.selectedNodes[0];
         nodeIndex = treeBox.childIndex(nodeToMove);
         if (nodeIndex>0) {
            // Update model
            element1 = listModel[nodeIndex-1];
            element2 = listModel[nodeIndex];
            listModel.splice(nodeIndex-1, 2, element2, element1);
            // update UI
            treeBox.remove(nodeIndex);
            treeBox.insert(nodeIndex-1,nodeToMove);
            treeBox.currentNode = nodeToMove;
         }
      }
   }
   var downAction = function() {
      var nodeToMove,  nodeIndex, element1, element2;
      if (treeBox.selectedNodes.length>0) {
         nodeToMove = treeBox.selectedNodes[0];
         nodeIndex = treeBox.childIndex(nodeToMove);
         if (nodeIndex<treeBox.numberOfChildren-1) {
            // Update model
            element1 = listModel[nodeIndex];
            element2 = listModel[nodeIndex+1];
            listModel.splice(nodeIndex, 2, element2, element1);
            // update UI
            treeBox.remove(nodeIndex);
            treeBox.insert(nodeIndex+1,nodeToMove);
            treeBox.currentNode = nodeToMove;
         }
      }
   }
   var addAction = function() {
      var nodeBeforeNew, newNode;
      var nodeIndex = treeBox.numberOfChildren;
      if (treeBox.selectedNodes.length>0) {
         nodeBeforeNew = treeBox.selectedNodes[0];
         nodeIndex = treeBox.childIndex(nodeBeforeNew) + 1;
      }
      // Create node
      var element = elementFactory();
      if (element !== null) {
         // insert node in model then ui
         listModel.splice(nodeIndex, 0, element);
         newNode = makeNode(treeBox, element, nodeIndex);
         treeBox.currentNode = newNode;
         treeBox.onNodeSelectionUpdated();
      }
   }

   var deleteAction = function() {
      var nodeToDelete,  nodeIndex;
      if (treeBox.selectedNodes.length>0) {
         nodeToDelete = treeBox.selectedNodes[0];
         nodeIndex = treeBox.childIndex(nodeToDelete);
         listModel.splice(nodeIndex,1);
         treeBox.remove(nodeIndex);
      }
   }

   treeBox.onNodeSelectionUpdated  = function() {
      // There could be an empty selection (last element removed)
      if (treeBox.selectedNodes.length>0) {
         var selectedNode = treeBox.selectedNodes[0];
         var selectedIndex = treeBox.childIndex(selectedNode);
         selectionCallback(listModel[selectedIndex]);
      }
   }


   var buttons = [
      {
         icon: ":/images/interface/prevButton.png",
         toolTip: "Move item up",
         action: upAction
         },
      {
         icon: ":/images/interface/nextButton.png",
         toolTip:  "Move item down",
         action: downAction
      },
      {
         icon: ":/images/icons/add.png",
         toolTip:  "Add new item",
         action: addAction
      },
      {
         icon: ":/images/icons/cancel.png",
         toolTip:  "Delete item",
         action: deleteAction
      },
   ];


   var iconButtonBar = new IconButtonBar(this, buttons);
   this.sizer.add(iconButtonBar);

   //this.adjustToContents();


}
ManagedList_Box.prototype = new Control;


return {
   IconButtonBar: IconButtonBar,
   ManagedList_Box: ManagedList_Box,
}

}) ();


// ========================================================================================================================
// Configuration dialog
// ========================================================================================================================
var ffM_GUI_RuleSet = (function (){

function makeOKCancel(parentDialog) {
   var cancel_Button, ok_Button;

   // TODO Add container first
   var c = new Control(parentDialog);
   c.sizer = new HorizontalSizer;
   c.sizer.margin = 6;
   c.sizer.spacing = 4;

   cancel_Button = new PushButton( c );
   cancel_Button.text = "Cancel";
   cancel_Button.enabled = true;
   cancel_Button.onClick = function() {
      debug("cancel");
      parentDialog.cancel();
   }
   ok_Button = new PushButton( c );
   ok_Button.text = "OK";
   ok_Button.enabled = true;
   ok_Button.onClick = function() {
      debug("ok");
      parentDialog.ok();
   }

   c.sizer.addStretch();
   c.sizer.add(cancel_Button);
   c.sizer.add(ok_Button);

   return c;

}

// ---------------------------------------------------------------------------------------------------------

// Top pane - Selection of ruleset
var makeRuleSetSelection_ComboBox = function(parent, ruleSetNames) {
   var i;
   var comboBox = new ComboBox( parent );
   comboBox.toolTip = Text.H.GROUP_TEMPLATE_TOOLTIP;
   comboBox.enabled = true;
   comboBox.editEnabled = false;
   for (i=0; i<ruleSetNames.length;i++) {
      comboBox.addItem(ruleSetNames[i]);
   }
   comboBox.currentItem = 0;

   comboBox.onItemSelected = function() {
      Console.writeln("SELECTED " + this.currentItem);
   }

   return comboBox;
}

// Utility pane - A Label - Text row to put in a list
function TextEntryRow(parent, minLabelWidth, name) {
   this.__base__ = Control;
   this.__base__(parent);
   this.sizer = new HorizontalSizer;
   this.sizer.margin = 2;
   this.sizer.spacing = 2;

   var the_Label = new Label( this );
   this.sizer.add(the_Label);
   the_Label.textAlignment = TextAlign_Right|TextAlign_VertCenter;
   the_Label.minWidth = minLabelWidth;
   the_Label.text = name + ": ";

   var name_Edit = new Edit(this);
   this.sizer.add(name_Edit);
   name_Edit.text = name;
}
TextEntryRow.prototype = new Control;



// -- Various middle right panes (entry of variabel values).
var makeMappingSetSelection_ComboBox = function(parent, mappingNames, mappingSelectionCallback) {
   var i;
   var comboBox = new ComboBox( parent );
   comboBox.toolTip = "Mapping";
   comboBox.enabled = true;
   comboBox.editEnabled = false;
   for (i=0; i<mappingNames.length;i++) {
      comboBox.addItem(mappingNames[i]);
   }
   comboBox.currentItem = 0;

   comboBox.onItemSelected = function() {
      if (this.currentItem>=0) {
         mappingSelectionCallback(mappingNames[this.currentItem]);
      }
   }

   return comboBox;
}

function MapFirstRegExpControl(parent) {
   this.__base__ = Control;
   this.__base__(parent);

   this.sizer = new VerticalSizer;

   var labelWidth1 = this.font.width( "MMMMMMMMMMMM: " );

   var variableNameRow = new TextEntryRow(this, labelWidth1, "name");
   this.sizer.add(variableNameRow);

   var formatRow = new TextEntryRow(this, labelWidth1, "format");
   this.sizer.add(formatRow);


}
MapFirstRegExpControl.prototype = new Control;


function MapFirstRegExpControlTEST(parent) {
   this.__base__ = Control;
   this.__base__(parent);

   this.sizer = new VerticalSizer;

   var labelWidth1 = this.font.width( "MMMMMMMMMMMM: " );

   var variableNameRow = new TextEntryRow(this, labelWidth1, "name");
   this.sizer.add(variableNameRow);

   var formatRow = new TextEntryRow(this, labelWidth1, "TEST");
   this.sizer.add(formatRow);


}
MapFirstRegExpControlTEST.prototype = new Control;



// var temp data
var mappingUIs = {}



// -- Middle pane - Variable definitions
function VariableUIControl(parent) {
   this.__base__ = Control;
   this.__base__(parent);

   this.sizer = new HorizontalSizer;
   this.sizer.margin = 6;
   this.sizer.spacing = 4;

   // ---------- TEMPROARY call backs and definitions
   var elementFactory = function() {
        return ffM_RuleSet_Model.defineVariable('new',Date.now().toString(),'xxx');
   }

   var variableSelectionCallback = function(variableDefinition) {
      Console.writeln("Variable selected: " + Log.pp(variableDefinition));
   }
   // TODO Must be dynamic
   var currentRule = ffM_RuleSet_Model.testRules[0];
   var currentVariableList = currentRule.variableList



   // -- Left side - select variables

   var variableListSelection_GroupBox = new GroupBox(this);
   this.sizer.add(variableListSelection_GroupBox);
   variableListSelection_GroupBox.title = "Select variable";
   variableListSelection_GroupBox.sizer = new VerticalSizer; // Any sizer

   var variableListSelection_Box = new ffM_GUI_support.ManagedList_Box(
         variableListSelection_GroupBox,
         [], //currentVariableList,
         elementFactory,
         "Variable definitions",
         variableSelectionCallback
   );
   variableListSelection_GroupBox.sizer.add(variableListSelection_Box);

   //--  Right side - Enter parameters corresponding to selected variables
   var mappingSelectionCallback = function(mapping) {
      Console.writeln("Mapping selected: " + Log.pp(mapping));
      mappingUIs[mapping].hide();
   }

   var variableDetails_GroupBox = new GroupBox(this);
   variableDetails_GroupBox.title = "Parameters of variable";
   this.sizer.add(variableDetails_GroupBox);

   variableDetails_GroupBox.sizer = new VerticalSizer;

   var cb =  makeMappingSetSelection_ComboBox(this,["map1","map2"], mappingSelectionCallback);
   variableDetails_GroupBox.sizer.add(cb);

   // Make all section windows
   var map1 = new MapFirstRegExpControl(variableDetails_GroupBox);
   variableDetails_GroupBox.sizer.add(map1);
   mappingUIs['map1'] = map1;

   var map2 = new MapFirstRegExpControlTEST(variableDetails_GroupBox);
   variableDetails_GroupBox.sizer.add(map2);
   mappingUIs['map2'] = map2;

   variableDetails_GroupBox.sizer.addStretch();

   // -- Update of the model
   this.updateVariableList = function(newVariableList) {
      variableListSelection_Box.modelListChanged(newVariableList);
   }

}

VariableUIControl.prototype = new Control;



// ---------------------------------------------------------------------------------------------------------

function ConfigurationDialog( parentDialog, ruleSet) {
   this.__base__ = Dialog;
   this.__base__();

   this.ruleSet = ruleSet;
   var ruleSetNames = ffM_RuleSet_Model.ruleNames(ruleSet);

   this.windowTitle = Text.T.REMAPPING_SECTION_PART_TEXT;

   this.sizer = new VerticalSizer;
   this.sizer.margin = 6;
   this.sizer.spacing = 4;

   // Top pane
   var ruleSetSelection_ComboBox = makeRuleSetSelection_ComboBox(this, ruleSetNames);
   this.sizer.add(ruleSetSelection_ComboBox);

   // Middle pane
   var variableUI = new VariableUIControl(this);
   this.sizer.add(variableUI);

   // Bottom pane - buttons
   var okCancelButtons = makeOKCancel(this);
   this.sizer.add(okCancelButtons);

   this.setVariableSize();
   //this.adjustToContents();

   // FOR TESTS AT END
   var currentVariableList = ffM_RuleSet_Model.testRules[0].variableList;
   variableUI.updateVariableList(currentVariableList);

}
ConfigurationDialog.prototype = new Dialog;

return {
   makeDialog: function(parent, ruleSet) {
      return new ConfigurationDialog(parent, ruleSet);
   }

}

}) ();
