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

// The resolvers are fixed, they are defined as:
//   {name: aString, description: aString, control: aControl}
// (more information may be added later).
// The resolver control manage its parameters, it has a 'populate()' method

   var i;

   // Describe the resolver types
   // The initial value will be deep copied to the parameter
   // The control will be populated when they are created
   var resolvers = [
      {name: 'RegExpList', description: 'Type of image (flat, bias, ...)',
            initial:{key: '?', reChecks: [{regexp: /.*/, replacement: '?'}]},  control: null},
      {name: 'Constant', description: 'Constant value',
            initial:{value: ''}, control: null},
      {name: 'Integer', description: 'Integer value',
            initial:{key: '?', format:'%4.4d'}, control: null},
      {name: 'IntegerPair', description: 'Pair of integers (binning)',
            initial:{key1: '?', key2: '?', format:'%dx%d'}, control: null}
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
      var initialValues = deepCopyData(resolverByName(resolver).initial);
      var initialParameters = {};
      initialParameters[resolver] = initialValues;
      return {
         name: name,
         description: description,
         resolver: resolver,
         parameters: initialParameters,
      }
   }

   // Get the rule by name
   var variableByName = function(variableList, name) {
      for (var i=0; i<variableList.length; i++) {
         if (variableList[i].name === name) return variableList[i];
      }
      return null;
   }


   return {
      ruleNames: ruleNames,
      ruleByName: ruleByName,
      defineVariable: defineVariable,

      resolverNames: resolverNames,
      resolverByName: resolverByName,
      resolvers: resolvers,

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
// Create a list selection box, possibly with editing capability
//    parent: UI COntrol
//    modeldescription: An array of element model descriptions (one for each column),
//         each description is an object with the properties:
//            propertyName: name of the propery to use in the element model for the corresponding column
//   intialListModel: A (usually empty) initial list model (an array of object with the properties described by modelDescription)
//                    Usually the model is specified by the modelListChange call (including for initial populate)
//   elementFactory: A function() that return a new element model (called when new is created)
//   toolTip: The tool tip text
//   selectionCallBack: a function(elementModel) that return the element model currently selected. an be null if
//                      no element is selected (for example last one is deleted).
function ManagedList_Box(parent, modelDescription, initialListModel, elementFactory, toolTip, selectionCallback, sorted) {
   this.__base__ = Control;
   this.__base__(parent);

   var i;
   typeof sorted === 'undefined' && (sorted = false);

   // -- Model
   var listModel = [];


   // -- Support methods
   // Create a node based on the model described in modelDescription
   var makeNode = function(treeBox, nodeModel, index) {
      //Log.debug('ManagedList_Box: makeNode -',Log.pp(nodeModel),Log.pp(modelDescription));
      var node = new TreeBoxNode( treeBox, index);
      for (var i=0; i<modelDescription.length;i++){
         node.setText(i, nodeModel[modelDescription[i].propertyName].toString());
      }
      return node;
   }



   // -- UI

   this.sizer = new VerticalSizer;

   var treeBox = new TreeBox(this);
   this.sizer.add(treeBox);

   treeBox.rootDecoration = false;
   treeBox.numberOfColumns = 2;
   treeBox.multipleSelection = false;
   treeBox.headerVisible = false;
   treeBox.headerSorting = false;

   treeBox.sort(0,sorted); // DO NOT SEEMS TO WORK

   //treeBox.setMinSize( 700, 200 );
   // DO not seem to have any effect
   //treeBox.lineWidth = 1;
   treeBox.style = Frame.FrameStyleSunken;

   treeBox.toolTip = toolTip;


   // -- Model update methods
   // A new model must be used for the listModel
   this.modelListChanged = function(newModelList) {

      // Clear current list display
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

   // The current values of the current row model changed
   this.currentModelElementChanged = function() {
      if (treeBox.selectedNodes.length>0) {
         var selectedNode = treeBox.selectedNodes[0];
         var selectedIndex = treeBox.childIndex(selectedNode);
         if (selectedIndex>=0 && selectedIndex<listModel.length) {
            for (var i=0; i<modelDescription.length;i++){
               selectedNode.setText(i, listModel[selectedIndex][modelDescription[i].propertyName].toString());
            }
          }
      }
    }


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




var propertyTypes = {
   FREE_TEXT: {
      name: "FREE_TEXT",
      propertyToText: function(value) {return value.toString()},
      textToProperty: function(text) {return text},
   },
   REG_EXP: {
      name: "REG_EXP",
      propertyToText: function(value) {return regExpToString(value)},
      textToProperty: function(text) {return regExpFromUserString(text)},
   }
}



// Utility pane - A Label - Labelled text field of a property of an object
//  parent: UI parent control
//  style: Style related properties (minLabelWidth, minDataWidth)
//  name: Text of the label of this text field
//  property: Name of property of the target that will be used as source of text and destination of text
//  propertyType: An object with two functions:
//     propertyToText: that format the property as text
//     textToProperty: that parse the text and return the property value, throw an exception in case of error
//  valueChangedCallback(): function that will be called if the text is successfuly updated - if null there is no callback
// The target (the object containing the property to edit) is specified dynamically (including at initialization)
function TextEntryRow(parent, style, name, property, propertyType, valueChangedCallback) {
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
   the_Label.minWidth = style.minLabelWidth;
   the_Label.text = name + ": ";

   var name_Edit = new Edit(this);
   name_Edit.minWidth = style.minDataWidth;
   this.sizer.add(name_Edit)

   name_Edit.onTextUpdated = function() {
      var value;
      if (that.target !== null) {
         //Log.debug("TextEntryRow: onTextUpdated:",property,this.text);
         try {
            value =  propertyType.textToProperty(this.text);
            // Next lines will only execute if value was correctly parsed
            name_Edit.textColor = 0x000000;
            that.target[property] = value;
            if (valueChangedCallback != null) {
               valueChangedCallback();
            }
         } catch (error) {
            name_Edit.textColor = 0xFF0000;
         }
      }
   }
   // Define the target object (that must have the property defined originally), null disables input
   this.updateTarget = function(target) {
      that.target = target;
      if (target === null) {
         name_Edit.text = '';
         name_Edit.enabled = false;
      } else {
         if (! target.hasOwnProperty(property)) {
            throw "Entry '" + name + "' does not have property '" + property + "': " + Log.pp(target);
         }
         name_Edit.text = propertyType.propertyToText(target[property]);
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
   comboBox.enabled = false;

   comboBox.onItemSelected = function() {
      if (this.currentItem>=0) {
         mappingSelectionCallback(mappingNames[this.currentItem]);
      }
   }

   comboBox.selectResolver = function(name) {
      if (name == null) {
            comboBox.enabled = false;
      } else {
         for (i=0; i<mappingNames.length;i++) {
            if (name === mappingNames[i]) {
               comboBox.currentItem = i;
               comboBox.enabled = true;
               break;
            }
         }
      }
   }

   return comboBox;
}

function ResolverSelectionRow(parent, rowStyle, name, mappingNames, mappingSelectionCallback) {
   this.__base__ = Control;
   this.__base__(parent);
   var that=this;

   this.sizer = new HorizontalSizer;
   this.sizer.margin = 2;
   this.sizer.spacing = 2;

   var the_Label = new Label( this );
   this.sizer.add(the_Label);
   the_Label.textAlignment = TextAlign_Right|TextAlign_VertCenter;
   the_Label.minWidth = rowStyle.minLabelWidth;
   the_Label.text = name + ": ";

   var resolver_ComboBox = makeResolverSelection_ComboBox (parent, mappingNames, mappingSelectionCallback);
   resolver_ComboBox.minWidth = rowStyle.minDataWidth;
   this.sizer.add(resolver_ComboBox);

   this.selectResolver = function(name) {
      resolver_ComboBox.selectResolver(name);
   }
}
ResolverSelectionRow.prototype = new Control;


// ..............................................................................................
// -- Controls for resolver
// ..............................................................................................

function MapFirstRegExpControl(parent, resolverName, rowStyle) {
   this.__base__ = Control;
   this.__base__(parent);
   var that = this;

   this.resolverName = resolverName;

   this.sizer = new VerticalSizer;

   // FITS Key
   var keyRow = new TextEntryRow(this, rowStyle, "FITS key", "key", propertyTypes.FREE_TEXT, null);
   this.sizer.add(keyRow);


   var transformationDefinitionFactory = function() {
      return {regexp: /.*/, replacement: '?'}
   }

   var transformationSelectionCallback = function(transformationModel) {
      that.selectTransformationToEdit(transformationModel);
   }


   var regExpListSelection_Box = new ffM_GUI_support.ManagedList_Box(
         this,
         [{propertyName: 'regexp'},{propertyName: 'replacement'}],
         [], // Its model will be initialized dynamically
         transformationDefinitionFactory,
         "Transformation",
         transformationSelectionCallback,
         false // Keep in order
   );
   this.sizer.add(regExpListSelection_Box);


   var currentRegExpRow = new TextEntryRow(this, rowStyle, "regexp", "regexp",
      propertyTypes.REG_EXP,
      function() {regExpListSelection_Box.currentModelElementChanged()});
   this.sizer.add(currentRegExpRow);
   var currentReplacementRow = new TextEntryRow(this,rowStyle, "replacement", "replacement",
      propertyTypes.FREE_TEXT,
      function() {regExpListSelection_Box.currentModelElementChanged()});
   this.sizer.add(currentReplacementRow);


   this.selectTransformationToEdit = function(transformationModel) {
      currentRegExpRow.updateTarget(transformationModel);
      currentReplacementRow.updateTarget(transformationModel);
   }

   this.initialize = function(variableDefinition) {
      if (!variableDefinition.parameters.hasOwnProperty(resolverName)) {
         variableDefinition.parameters[resolverName] = deepCopyData(ffM_RuleSet_Model.resolverByName(resolverName).initial);
      }
   }

   this.populate = function(variableDefinition) {
      // Should probably be somewhere else
      this.initialize(variableDefinition);
      keyRow.updateTarget(variableDefinition.parameters[resolverName]);
      regExpListSelection_Box.modelListChanged(variableDefinition.parameters[resolverName].reChecks);
   }
}
MapFirstRegExpControl.prototype = new Control;

// ..............................................................................................

function ConstantValueResolverControl(parent, resolverName, rowStyle) {
   this.__base__ = Control;
   this.__base__(parent);

   this.resolverName = resolverName;

   this.sizer = new VerticalSizer;
   var constantValueRow = new TextEntryRow(this, rowStyle, "value", "value", propertyTypes.FREE_TEXT, null);
   this.sizer.add(constantValueRow);

   this.initialize = function(variableDefinition) {
      if (!variableDefinition.parameters.hasOwnProperty(resolverName)) {
         variableDefinition.parameters[resolverName] = deepCopyData(ffM_RuleSet_Model.resolverByName(resolverName).initial);
      }
   }

   this.populate = function(variableDefinition) {
      // Should probably be somewhere else
      this.initialize(variableDefinition);
      constantValueRow.updateTarget(variableDefinition.parameters[resolverName]);
   }
}
ConstantValueResolverControl.prototype = new Control;

// ..............................................................................................

function IntegerValueResolverControl(parent, resolverName, rowStyle) {
   this.__base__ = Control;
   this.__base__(parent);

   this.resolverName = resolverName;

   this.sizer = new VerticalSizer;

   // FITS Key
   var keyRow = new TextEntryRow(this, rowStyle, "FITS key", "key", propertyTypes.FREE_TEXT, null);
   this.sizer.add(keyRow);

   var formatRow = new TextEntryRow(this, rowStyle, "Format", "format", propertyTypes.FREE_TEXT, null);
   this.sizer.add(formatRow);

   this.initialize = function(variableDefinition) {
      if (!variableDefinition.parameters.hasOwnProperty(resolverName)) {
         variableDefinition.parameters[resolverName] = deepCopyData(ffM_RuleSet_Model.resolverByName(resolverName).initial);
      }
   }

   this.populate = function(variableDefinition) {
      // Should probably be somewhere else
      this.initialize(variableDefinition);
      keyRow.updateTarget(variableDefinition.parameters[resolverName]);
      formatRow.updateTarget(variableDefinition.parameters[resolverName]);
   }
}
IntegerValueResolverControl.prototype = new Control;


// ..............................................................................................

function IntegerPairValueResolverControl(parent, resolverName, rowStyle) {
   this.__base__ = Control;
   this.__base__(parent);

   this.resolverName = resolverName;

   this.sizer = new VerticalSizer;

   // FITS Key 1
   var key1Row = new TextEntryRow(this, rowStyle, "FITS key 1", "key1", propertyTypes.FREE_TEXT, null);
   this.sizer.add(key1Row);

   var key2Row = new TextEntryRow(this, rowStyle, "FITS key 2", "key2", propertyTypes.FREE_TEXT, null);
   this.sizer.add(key2Row);

   var formatRow = new TextEntryRow(this, rowStyle, "Format", "format", propertyTypes.FREE_TEXT, null);
   this.sizer.add(formatRow);

   this.initialize = function(variableDefinition) {
      if (!variableDefinition.parameters.hasOwnProperty(resolverName)) {
         variableDefinition.parameters[resolverName] = deepCopyData(ffM_RuleSet_Model.resolverByName(resolverName).initial);
      }
   }

   this.populate = function(variableDefinition) {
      // Should probably be somewhere else
      this.initialize(variableDefinition);
      key1Row.updateTarget(variableDefinition.parameters[resolverName]);
      key2Row.updateTarget(variableDefinition.parameters[resolverName]);
      formatRow.updateTarget(variableDefinition.parameters[resolverName]);
   }
}
IntegerPairValueResolverControl.prototype = new Control;




// ..............................................................................................


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
   var dataWidth = this.font.width( "MMMMMMMMMMMMMMMMMMMMMMM" );
   var rowStyle = {
      minLabelWidth: labelWidth,
      minDataWidth: dataWidth,
   }

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
        [{propertyName: 'name'},{propertyName: 'description'}],
        [], // Its model will be initialized dynamically
         variableDefinitionFactory,
         "Variable definitions",
         variableSelectionCallback,
         true // Sort by variable name
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



   //this.resolverSelection_GroupBox =  makeResolverSelection_ComboBox(this, resolverNames, resolverSelectionCallback);
   this.resolverSelectionRow =  new ResolverSelectionRow(this, rowStyle, "type", ffM_RuleSet_Model.resolverNames, resolverSelectionCallback);
   variableDetails_GroupBox.sizer.add(this.resolverSelectionRow);

   this.variableNameRow = new TextEntryRow(this, rowStyle, "name","name",
      propertyTypes.FREE_TEXT,
      function() {variableListSelection_Box.currentModelElementChanged()});
   variableDetails_GroupBox.sizer.add(this.variableNameRow);
   this.descriptionRow = new TextEntryRow(this, rowStyle, "description","description",
      propertyTypes.FREE_TEXT,
      function() {variableListSelection_Box.currentModelElementChanged()});
   variableDetails_GroupBox.sizer.add(this.descriptionRow);

   var addNewResolverControl = function(resolverControl){
      variableDetails_GroupBox.sizer.add(resolverControl);
      ffM_RuleSet_Model.resolverByName(resolverControl.resolverName).control = resolverControl;
      resolverControl.hide();
   }

   // Make all resolver controls, only the selected one will be shown
   addNewResolverControl(new ConstantValueResolverControl(variableDetails_GroupBox, 'Constant', rowStyle));

   addNewResolverControl(new IntegerValueResolverControl(variableDetails_GroupBox, 'Integer', rowStyle));

   addNewResolverControl(new IntegerPairValueResolverControl(variableDetails_GroupBox, 'IntegerPair', rowStyle));

   addNewResolverControl(new MapFirstRegExpControl(variableDetails_GroupBox,'RegExpList', rowStyle));


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
         var resolver = ffM_RuleSet_Model.resolverByName(resolverName);
         if (resolver === null) {
            throw "Invalid resolver '" + resolverName + "' for variable '"+ variableDefinition.name+"'";
         }
         if (that.currentVariableDefinition !== null) {
            // record the new resolver
            that.currentVariableDefinition.resolver = resolverName;
            // Populate and show it
            that.currentResolver = resolver;
            resolver.control.populate(that.currentVariableDefinition );
            resolver.control.show();
         }
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
      this.resolverSelectionRow.selectResolver(resolverName);
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

function ConfigurationDialog( parentDialog, originalRuleSet, currentRuleSetName) {
   this.__base__ = Dialog;
   this.__base__();
   var that = this;

   // Model -
   // Keeps track of rule set and current rule set selected
   // in a copy, so in case of cancel nothing is changed
   this.ruleSet = deepCopyData(originalRuleSet);
   this.currentRuleSetName = currentRuleSetName;

   // For the selection of the current rule set
   var ruleSetNames = ffM_RuleSet_Model.ruleNames(this.ruleSet);

   this.newVariableCounter = 0;

   this.windowTitle = Text.T.REMAPPING_SECTION_PART_TEXT;

   this.sizer = new VerticalSizer;
   this.sizer.margin = 6;
   this.sizer.spacing = 4;

   // -- GUI actions callbacks
   // Rule set changed (also used in initialization)
   var ruleSetSelectedCallback = function(ruleSetName) {
      Log.debug("ConfigurationDialog: ruleSetSelectedCallback - RuleSet selected:",ruleSetName);
      var selectedRule = ffM_RuleSet_Model.ruleByName(that.ruleSet, ruleSetName);
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
