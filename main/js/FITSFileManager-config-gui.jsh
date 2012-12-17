// FITSFileManager-config-gui.js

// This file is part of FITSFileManager, see copyrigh in FITSFileManager.js




// ========================================================================================================================
// Model
// ========================================================================================================================

// There is a 'pure data' representation of the rules (so they can be serialized)
// and the Model complement this representation with convenience methods

var ffM_RuleSet_Model = (function(){

   // Model object wrap data object and behavior
   var makeRuleModel = function(name, description) {
      // Data objects (must be serializable to JSon)
      // Rules:
      // {name, description, variableList}
      var rule = {
         name: name,
         description: description,
         variableList: [],
      };

      return {
         // Operations on the variable list
         addVariable: function(variable) {
            rule.variableList.push(variable);
         },
#ifdef NOTUSED
         insertVariable: function(variable, index) {
            rule.variableList.splice(index, 0, variable);
         },
         removeVariable: function(index) {
            rule.variableList.splice(index,1);
         },
#endif
         variableList: rule.variableList,
      }

   }



   // Define variable definition common data
   var defineVariable = function(name, description, type) {
      return {
         name: name,
         description: description,
         type: type,
      }
   }


   // Default definition for test
   var aRuleModel = makeRuleModel('Default', 'Common FITS mapping');

   aRuleModel.addVariable(defineVariable('type','Type of image (flat, bias, ...)','typeParser'));
   aRuleModel.addVariable(defineVariable('filter','Filter (clear, red, ...)','filterParser'));
   aRuleModel.addVariable(defineVariable('exposure','Exposure in seconds','exposureParser'));
   aRuleModel.addVariable(defineVariable('temp','Temperature in C','tempParser'));
   aRuleModel.addVariable(defineVariable('binning','Binning as 1x1, 2x2, ...','binningParser'));
   aRuleModel.addVariable(defineVariable('night','night (experimental)','nightParser'));
   aRuleModel.addVariable(defineVariable('filename','Input file name','filenameParser'));
   aRuleModel.addVariable(defineVariable('extension','Input file extension','extensionParser'));


   return {
      defineVariable: defineVariable,
      ruleModel: aRuleModel,
   }
}) ();


// ========================================================================================================================
// GUI support methods
// ========================================================================================================================


var ffM_GUI_support = (function (){


var STRETCH = Object.create(null);

var makeVerticalSection = function() {
   var i;
   var sizer = new VerticalSizer;
   sizer.margin = 4;
   sizer.spacing = 4;
   for (i=0; i<arguments.length; i++) {
      if (STRETCH===arguments[i]) {
         sizer.addStretch();
      } else {
         sizer.add( arguments[i] );
      }
   }
   return sizer;
}
var makeHorizontalSection = function() {
   var i;
   var sizer = new HorizontalSizer;
   sizer.margin = 4;
   sizer.spacing = 4;
   for (i=0; i<arguments.length; i++) {
      if (STRETCH===arguments[i]) {
         sizer.addStretch();
      } else {
         sizer.add( arguments[i] );
      }
   }
   return sizer;
}

var makeVerticalBox = function(parent) {
   var i;
   var c = new Control(parent);
   c.sizer = new VerticalSizer;
   c.sizer.margin = 4;
   c.sizer.spacing = 4;
   for (i=0; i<arguments.length; i++) {
      if (STRETCH===arguments[i]) {
         c.sizer.addStretch();
      } else {
         c.sizer.add( arguments[i] );
      }
   }
   return c;
}

var makeHorizontalBox = function(parent) {
   var i;
   var c = new Control(parent);
   c.sizer = new HorizontalSizer;
   c.sizer.margin = 4;
   c.sizer.spacing = 4;
   for (i=0; i<arguments.length; i++) {
      if (STRETCH===arguments[i]) {
         c.sizer.addStretch();
      } else {
         c.sizer.add( arguments[i] );
      }
   }
   return c;
}


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

function ManagedList_Box(parent, listModel, elementFactory, toolTip, selectionCallback) {
   this.__base__ = Control;
   this.__base__(parent);


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


   for (var i=0; i<listModel.length; i++) {
      // Just making the nodes add them to the treeBox
      makeNode(treeBox, listModel[i], i);
   }


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
   STRETCH: STRETCH,
   makeHorizontalBox: makeHorizontalBox,
   makeVerticalBox: makeVerticalBox,
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

function TextEntryRow(parent, minLabelWidth, name) {
   this.__base__ = Control;
   this.__base__(parent);
   this.sizer = new HorizontalSizer;

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


function VariableUIControl(parent) {
   this.__base__ = Control;
   this.__base__(parent);

   this.sizer = new HorizontalSizer;

   // Left side
   var elementFactory = function() {
        return ffM_RuleSet_Model.defineVariable('new',Date.now().toString(),'xxx');
   }
   var selectionCallback = function(variableDefinition) {
      Console.writeln("Variable selected: " + Log.pp(variableDefinition));
   }

   var variableListSelection_GroupBox = new GroupBox(this);
   this.sizer.add(variableListSelection_GroupBox);
   variableListSelection_GroupBox.title = "Select variable";
   variableListSelection_GroupBox.sizer = new VerticalSizer; // Any sizer

   var variableListSelection_Box = new ffM_GUI_support.ManagedList_Box(
         variableListSelection_GroupBox,
         ffM_RuleSet_Model.ruleModel.variableList,
         elementFactory,
         "Variable definitions",
         selectionCallback
   );
   variableListSelection_GroupBox.sizer.add(variableListSelection_Box);

   // Right side
   var variableDetails_GroupBox = new GroupBox(this);
   variableDetails_GroupBox.title = "Parameters of variable";
   this.sizer.add(variableDetails_GroupBox);

   variableDetails_GroupBox.sizer = new VerticalSizer;

   var labelWidth1 = this.font.width( "MMMMMMMMMMMM: " );

   var variableNameRow = new TextEntryRow(variableDetails_GroupBox, labelWidth1, "name");
   variableDetails_GroupBox.sizer.add(variableNameRow);

   var formatRow = new TextEntryRow(variableDetails_GroupBox, labelWidth1, "format");
   variableDetails_GroupBox.sizer.add(formatRow);

   variableDetails_GroupBox.sizer.addStretch();

}

VariableUIControl.prototype = new Control;

// ---------------------------------------------------------------------------------------------------------

function ConfigurationDialog( parentDialog, ruleSet) {
   this.__base__ = Dialog;
   this.__base__();

   this.ruleSet = ruleSet;
   var ruleSetNames = ["aaa","bbb","ccc"];

   this.windowTitle = Text.T.REMAPPING_SECTION_PART_TEXT;

   this.sizer = new VerticalSizer;

   var ruleSetSelection_ComboBox = makeRuleSetSelection_ComboBox(this, ruleSetNames);
   this.sizer.add(ruleSetSelection_ComboBox);


   var variableUI = new VariableUIControl(this);
   this.sizer.add(variableUI);

   // Buttons
   var okCancelButtons = makeOKCancel(this);
   this.sizer.add(okCancelButtons);

   this.setVariableSize();
   //this.adjustToContents();

   // FOR TESTS AT END
   this.listModel = ffM_RuleSet_Model.ruleModel.variableList;

}
ConfigurationDialog.prototype = new Dialog;

return {
   makeDialog: function(parent, ruleSet) {
      return new ConfigurationDialog(parent, ruleSet);
   }

}

}) ();
