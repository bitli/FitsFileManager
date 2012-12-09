// FITSFileManager-config-gui.js

// This file is part of FITSFileManager, see copyrigh in FITSFileManager.js





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
         insertVariable: function(variable, index) {
            rule.variableList.splice(index, 0, variable);
         },
         removeVariable: function(index) {
            rule.variableList.splice(index,1);
         },

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



var ffM_GUI_RuleSet = (function (){

var makeVerticalSection = function() {
   var i
   var sizer = new VerticalSizer;
   sizer.margin = 4;
   sizer.spacing = 4;
   for (i=0; i<arguments.length; i++) {
      sizer.add( arguments[i] );
   }
   return sizer;
}
var makeHorizontalSection = function() {
   var i
   var sizer = new HorizontalSizer;
   sizer.margin = 4;
   sizer.spacing = 4;
   for (i=0; i<arguments.length; i++) {
      sizer.add( arguments[i] );
   }
   return sizer;
}


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

// buttons is an array of {icon, toolTip, action}
var makeIconButtonBar = function(parent, buttons) {
   var i, button;
   var horizontalSizer = new HorizontalSizer;
   horizontalSizer.margin = 6;
   horizontalSizer.spacing = 4;

   for (i=0; i<buttons.length;i ++) {
      var button = buttons[i];
      //Log.debug(i,button.icon, button.toolTip);
      var toolButton = new ToolButton( parent );
      toolButton.icon = new Bitmap( button.icon );
      toolButton.toolTip = button.toolTip;
      toolButton.onClick = button.action;
      horizontalSizer.add(toolButton);
   }
   horizontalSizer.addStretch();
   return horizontalSizer;

}


function makeManagedListTreeBox(parent, listModel, toolTip) {
   var i;

   var makeNode = function(treeNode, nodeModel, index) {
      var node = new TreeBoxNode( treeBox, index);
      node.setText( 0, nodeModel.name );
      node.setText( 1, nodeModel.description );
      return node;
   }

   var treeBox = new TreeBox( parent);

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
      // SHOULD USE FACTORY METHOD
      var element = ffM_RuleSet_Model.defineVariable('new',Date.now().toString(),'xxx');

      // insert node
      listModel.splice(nodeIndex, 0, element);
      newNode = makeNode(treeBox, element, nodeIndex);
      treeBox.currentNode = newNode;
      treeBox.onNodeSelectionUpdated();
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
         Log.debug("onNodeSelectionUpdated" , selectedIndex);
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


   var iconButtonBar = makeIconButtonBar(parent, buttons);

   var sizer = makeVerticalSection(treeBox, iconButtonBar);
   return sizer;

}




// ========================================================================================================================
// Configuration dialog
// ========================================================================================================================
// ---------------------------------------------------------------------------------------------------------
function ConfigurationDialog( parentDialog, ruleSet) {
   this.__base__ = Dialog;
   this.__base__();

   this.ruleSet = ruleSet;

   var ruleSetNames = ["aaa","bbb","ccc"];



   this.windowTitle = Text.T.REMAPPING_SECTION_PART_TEXT;

   var ruleSetSelection_ComboBox = makeRuleSetSelection_ComboBox(this, ruleSetNames);


   var variableListSelection = makeManagedListTreeBox(this, ffM_RuleSet_Model.ruleModel.variableList, "Variable definitions");

   this.sizer = makeVerticalSection(ruleSetSelection_ComboBox, variableListSelection);

   this.setVariableSize();
   this.adjustToContents();

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
