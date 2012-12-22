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

// Variable: (the parameters dictionary depends on the resolver, the parameters of a resolver
//            are qualified by the resolver name allowing multiple resolvers at the same
//            time while in memory)
//    {name: aString, description: aString, resolver: aName, parameters: {resolverName: {}}}



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

   // Get the rule by name
   var ruleByName = function(ruleSet, name) {
      for (var i=0; i<ruleSet.length; i++) {
         if (ruleSet[i].name === name) return ruleSet[i];
      }
      return null;
   }


   // Model of variable - define a new variable
   var defineVariable = function(name, description, resolver) {
      return {
         name: name,
         description: description,
         resolver: resolver,
         parameters: {},
      }
   }

   // Get the rule by name
   var variableByName = function(variableList, name) {
      for (var i=0; i<variableList.length; i++) {
         if (variableList[i].name === name) return variableList[i];
      }
      return null;
   }



   // Define specific rule parameters
   // First RegExp match value
   var makeFirstRegExpMapping = function(aFITSKey) {
      return {
         key: aFITSKey,
         mappings: [],
      }
   }




   // ========================================================================================================================
   // SUPPORT FOR TESTSTextEntryRow


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

   // --------------------------
   // Default definition for test
   var defaultRule = newRuleData('Default', 'Common FITS rules')
   .addVariable(defineVariable('type','Type of image (flat, bias, ...)','RegExpList'))
   .addVariable(defineVariable('filter','Filter (clear, red, ...)','RegExpList'))
   .addVariable(defineVariable('exposure','Exposure in seconds','Constant'))
   .addVariable(defineVariable('temp','Temperature in C','Constant'))
   .addVariable(defineVariable('binning','Binning as 1x1, 2x2, ...','Constant'))
   .addVariable(defineVariable('night','night (experimental)','Constant'))
   .addVariable(defineVariable('filename','Input file name','Constant'))
   .addVariable(defineVariable('extension','Input file extension','Constant'))
   .build();

#ifdef TESTRULESETS
   var testRule = newRuleData('Test', 'A test rule')
   .addVariable(defineVariable('object','Object','Constant'))
   .build();

   var emptyRule = newRuleData('Empty', 'An empty rule')
   .build();
   var largeRule = newRuleData('Large', 'Many variables')
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
   var testRules = [];
   testRules.push(defaultRule);
#ifdef TESTRULESETS
   testRules.push(testRule);
   testRules.push(emptyRule);
   testRules.push(largeRule);
#endif

   return {
      ruleNames: ruleNames,
      ruleByName: ruleByName,
      defineVariable: defineVariable,

      // For tests
      testRules: testRules,
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


   // -- Model update action
   this.modelListChanged = function(newModelList) {

      // Cleanr current list
      var i;
      var nmbNodes = treeBox.numberOfChildren;
      for (i=nmbNodes; i>0; i--) {
         treeBox.remove(i-1);
      }
      // Update variable tracking current model
      listModel = newModelList;

      // Add new nodes
      for (i=0; i<listModel.length; i++) {
         // Just making the nodes add them to the treeBox
         makeNode(treeBox, listModel[i], i);
      }
   }
   this.modelListChanged(initialListModel);

   // -- Internal actions
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
         if (selectedIndex>=0 && selectedIndex<listModel.length) {
            selectionCallback(listModel[selectedIndex]);
         }
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

// The resolvers are fixed, they are defined as:
//   {name: aString, description: aString, control: aControl}
// (more information may be added later).
// The resolver control manage its parameters, it has a 'populate()' method

   var i;

   // The control will be populated when they are created
   var resolvers = [
      {name: 'RegExpList', description: 'Type of image (flat, bias, ...)', control: null},
      {name: 'Constant', description: 'Constant value', control: null}
   ];

   var resolverByName = function(name) {
      for (var i=0; i<resolvers.length; i++) {
         if (resolvers[i].name === name) return resolvers[i];
      }
      return null;
   }
   var resolverNames = [];
   for ( i=0; i<resolvers.length; i++) {
      resolverNames.push(resolvers[i].name);
   }



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
var makeRuleSetSelection_ComboBox = function(parent, ruleSetNames, ruleSetSelectedCallback) {
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
      if (this.currentItem>=0) {
         ruleSetSelectedCallback(ruleSetNames[this.currentItem]);
      }
   }

   return comboBox;
}

// Utility pane - A Label - Labelled text field of a property of an object
function TextEntryRow(parent, minLabelWidth, name, property) {
   this.__base__ = Control;
   this.__base__(parent);
   var that = this;

   this.sizer = new HorizontalSizer;
   this.sizer.margin = 2;
   this.sizer.spacing = 2;

   this.property = property;
   this.target = null;

   var the_Label = new Label( this );
   this.sizer.add(the_Label);
   the_Label.textAlignment = TextAlign_Right|TextAlign_VertCenter;
   the_Label.minWidth = minLabelWidth;
   the_Label.text = name + ": ";

   var name_Edit = new Edit(this);
   this.sizer.add(name_Edit)

   name_Edit.onTextUpdated = function() {
      if (that.target !== null) {
         Log.debug("TextEntryRow: onTextUpdated:",property,this.text);
         that.target[property] = this.text;
      }
   }

   this.updateTarget = function(target) {
      that.target = target;
      if (target === null) {
         name_Edit.text = '';
         name_Edit.enabled = false;
      } else {
         if (! target.hasOwnProperty(property)) {
            throw "Entry '" + name + "' does not have property '" + property + "': " + Log.pp(target);
         }
         name_Edit.text = target[property];
         name_Edit.enabled = true;
      }
   }
   this.updateTarget(null);

}
TextEntryRow.prototype = new Control;



// -- Middle right sub-panes (components to edit variables)
var makeResolverSelection_ComboBox = function(parent, mappingNames, mappingSelectionCallback) {
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

   comboBox.selectResolver = function(name) {
   for (i=0; i<mappingNames.length;i++) {
         if (name === mappingNames[i]) {
            comboBox.currentItem = i;
            break;
         }
      }
   }

   return comboBox;
}

// -- Controls for resolver

function MapFirstRegExpControl(parent, resolverName, labelWidth) {
   this.__base__ = Control;
   this.__base__(parent);

   this.sizer = new VerticalSizer;

   var variableNameRow = new TextEntryRow(this, labelWidth, "key", "key");
   this.sizer.add(variableNameRow);

   var formatRow = new TextEntryRow(this, labelWidth, "format", "format");
   this.sizer.add(formatRow);

   this.initialize = function(variableDefinition) {
      if (!variableDefinition.parameters.hasOwnProperty(resolverName)) {
         variableDefinition.parameters[resolverName] = {key: '', format: ''};
      }
   }

   this.populate = function(variableDefinition) {
      // Should probably be somewhere else
      this.initialize(variableDefinition);
      variableNameRow.updateTarget(variableDefinition.parameters[resolverName]);
      formatRow.updateTarget(variableDefinition.parameters[resolverName]);
   }
}
MapFirstRegExpControl.prototype = new Control;



function ConstantValueResolverControl(parent, resolverName, labelWidth) {
   this.__base__ = Control;
   this.__base__(parent);

   this.sizer = new VerticalSizer;
   var constantValueRow = new TextEntryRow(this, labelWidth, "value", "value");
   this.sizer.add(constantValueRow);

   this.initialize = function(variableDefinition) {
      if (!variableDefinition.parameters.hasOwnProperty(resolverName)) {
         variableDefinition.parameters[resolverName] = {value: ''};
      }
   }

   this.populate = function(variableDefinition) {
      // Should probably be somewhere else
      this.initialize(variableDefinition);
      constantValueRow.updateTarget(variableDefinition.parameters[resolverName]);
   }
}
ConstantValueResolverControl.prototype = new Control;






// -- Middle pane - Variable definitions (two panes: add/remove variables and edit selected variables)
function VariableUIControl(parent, variableDefinitionFactory ) {
   this.__base__ = Control;
   this.__base__(parent);
   var that = this;

   // -- Model data - set by selectVariable()
   this.currentVariableDefinition = null;

   // -- UI data
   // this.resolverSelection_GroupBox = null; // defined later
   this.currentResolver = null;

   this.sizer = new HorizontalSizer;
   this.sizer.margin = 6;
   this.sizer.spacing = 4;

   var labelWidth = this.font.width( "MMMMMMMMMMMM: " );

   // -- GUI action callbacks
   // Variable selected in current rule, forward to the model handling later in this object
   var variableSelectionCallback = function(variableDefinition) {
      //Log.debug("VariableUIControl: variableSelectionCallback - Variable selected: " +variableDefinition.name);
      that.selectVariable(variableDefinition);
   }




   // -- Left side - select variable being edited, add/remove variables

   var variableListSelection_GroupBox = new GroupBox(this);
   this.sizer.add(variableListSelection_GroupBox);
   variableListSelection_GroupBox.title = "Select variable";
   variableListSelection_GroupBox.sizer = new VerticalSizer; // Any sizer

   var variableListSelection_Box = new ffM_GUI_support.ManagedList_Box(
         variableListSelection_GroupBox,
         [], // Its model will be initialized dynamically
         variableDefinitionFactory,
         "Variable definitions",
         variableSelectionCallback
   );
   variableListSelection_GroupBox.sizer.add(variableListSelection_Box);



   //--  Right side - Enter parameters corresponding to selected variables
   var resolverSelectionCallback = function(resolverName) {
      Log.debug("VariableUIControl: resolverSelectionCallback - selected:",resolverName);
      that.updateResolver(resolverName);
   }

   var variableDetails_GroupBox = new GroupBox(this);
   variableDetails_GroupBox.title = "Parameters of variable";
   this.sizer.add(variableDetails_GroupBox);

   variableDetails_GroupBox.sizer = new VerticalSizer;

   this.resolverSelection_GroupBox =  makeResolverSelection_ComboBox(this, resolverNames, resolverSelectionCallback);
   variableDetails_GroupBox.sizer.add(this.resolverSelection_GroupBox);

   this.variableNameRow = new TextEntryRow(this, labelWidth, "name","name");
   variableDetails_GroupBox.sizer.add(this.variableNameRow);
   this.descriptionRow = new TextEntryRow(this, labelWidth, "description","description");
   variableDetails_GroupBox.sizer.add(this.descriptionRow);

   // Make all resolver controls, only the selected one will be shown
   var resolverRegExpList = new MapFirstRegExpControl(variableDetails_GroupBox,'RegExpList', labelWidth);
   variableDetails_GroupBox.sizer.add(resolverRegExpList);
   resolverByName('RegExpList').control = resolverRegExpList;
   resolverRegExpList.hide();

   var resolverConstantValue = new ConstantValueResolverControl(variableDetails_GroupBox, 'Constant', labelWidth);
   variableDetails_GroupBox.sizer.add(resolverConstantValue);
   resolverByName('Constant').control = resolverConstantValue;
   resolverConstantValue.hide();

   variableDetails_GroupBox.sizer.addStretch();



   // -- Update the model
   // The resolver name was updated (by select box, by changing variable or initially)
   // null if no resolver (like an empty variable list)
   this.updateResolver = function(resolverName) {
      if (that.currentResolver != null) {
         that.currentResolver.control.hide();
         that.currentResolver =null;
      }
      if (resolverName != null) {
         var resolver = resolverByName(resolverName);
         if (resolver === null) {
            throw "Invalid resolver '" + resolverName + "' for variable '"+ variableDefinition.name+"'";
         }
         // record the new resolver
         that.currentVariableDefinition.resolver = resolverName;
         // Populate and show it
         that.currentResolver = resolver;
         resolver.control.populate(that.currentVariableDefinition );
         resolver.control.show();
      }
   }

   // The variable to edit was selected
   this.selectVariable = function(variableDefinition) {
      //Log.debug("VariableUIControl: selectVariable - Variable selected ",variableDefinition.name );
      var resolverName;

      that.currentVariableDefinition = variableDefinition;

      // populate the common fields
      this.variableNameRow.updateTarget(variableDefinition);
      this.descriptionRow.updateTarget(variableDefinition);

      // Find new resolver, populate and show it
      resolverName = that.currentVariableDefinition.resolver;
      that.updateResolver(resolverName);

      // Update the UI
      this.resolverSelection_GroupBox.selectResolver(resolverName);
    }

   // The list of variables was changed externally (initial or change or rule set)
   this.updateVariableList = function(newVariableList) {
      // Update UI
      variableListSelection_Box.modelListChanged(newVariableList);
   }

}

VariableUIControl.prototype = new Control;



// ---------------------------------------------------------------------------------------------------------
// This Dialog controls the update of a ruleSet, starting at a current rule set.
// The rule set may be modified or not, at the end the caller must get
// the ruleset and current rule set name properties from the dialog to get the current
// state.

function ConfigurationDialog( parentDialog, ruleSet, currentRuleSetName) {
   this.__base__ = Dialog;
   this.__base__();
   var that = this;

   // Model -
   // Keeps track of rule set and current rule set selected
   // This will be the updated rule set at the end
   this.ruleSet = ruleSet;
   this.currentRuleSetName = currentRuleSetName;

   // For the selection of the current rule set
   var ruleSetNames = ffM_RuleSet_Model.ruleNames(ruleSet);

   this.newVariableCounter = 0;

   this.windowTitle = Text.T.REMAPPING_SECTION_PART_TEXT;

   this.sizer = new VerticalSizer;
   this.sizer.margin = 6;
   this.sizer.spacing = 4;

   // -- GUI actions callbacks
   // Rule set changed (also used in initialization)
   var ruleSetSelectedCallback = function(ruleSetName) {
      Log.debug("ConfigurationDialog: ruleSetSelectedCallback - RuleSet selected:",ruleSetName);
      var selectedRule = ffM_RuleSet_Model.ruleByName(ruleSet, ruleSetName);
      if (selectedRule == null) {
         throw "Invalid rule set name '" + ruleSetName +"'";
      }
      // Update model
      that.currentRuleSetName = ruleSetName;
      // Update UI
      that.variableUI.updateVariableList(selectedRule.variableList);
   }

   // -- Model call backs
   // New variable requested in current rule, define one with default values
   var variableDefinitionFactory = function() {
        Log.debug("ConfigurationDialog: variableDefinitionFactory - create new variable");
        that.newVariableCounter++;
        return ffM_RuleSet_Model.defineVariable("new_" + that.newVariableCounter,'','Constant');
   }

   // -- Build the top level pane

   // Top pane - select rule set
   var ruleSetSelection_ComboBox = makeRuleSetSelection_ComboBox(this, ruleSetNames, ruleSetSelectedCallback);
   this.sizer.add(ruleSetSelection_ComboBox);

   // Middle pane - variabel rules
   this.variableUI = new VariableUIControl(this, variableDefinitionFactory);
   this.sizer.add(this.variableUI);

   // Bottom pane - buttons
   var okCancelButtons = makeOKCancel(this);
   this.sizer.add(okCancelButtons);

   this.setVariableSize();
   //this.adjustToContents();


   // Initialize content
   ruleSetSelectedCallback(currentRuleSetName);
}
ConfigurationDialog.prototype = new Dialog;



return {
   makeDialog: function(parent, ruleSet, ruleSetName) {
      return new ConfigurationDialog(parent, ruleSet, ruleSetName);
   }

}

}) ();
