/*******************************************************************************
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     DisJ Development Group
 *******************************************************************************/

package distributed.plugin.ui.actions;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IFileEditorInput;

import distributed.plugin.core.DisJException;
import distributed.plugin.core.Graph;
import distributed.plugin.core.IConstants;
import distributed.plugin.random.IRandom;
import distributed.plugin.runtime.adversary.AgentControl;
import distributed.plugin.runtime.adversary.MsgPassingControl;
import distributed.plugin.runtime.engine.BoardAgent;
import distributed.plugin.runtime.engine.Entity;
import distributed.plugin.runtime.engine.SimulatorEngine;
import distributed.plugin.runtime.engine.TokenAgent;
import distributed.plugin.ui.IGraphEditorConstants;
import distributed.plugin.ui.dialogs.AdversaryClassInputDialog;
import distributed.plugin.ui.dialogs.ClientClassInputDialog;
import distributed.plugin.ui.dialogs.SpeedDialog;
import distributed.plugin.ui.editor.GraphEditor;
import distributed.plugin.ui.models.GraphElement;
import distributed.plugin.ui.models.NodeElement;
import distributed.plugin.ui.validators.ClassNameValidator;

/**
 * An action that will be processed by a processor
 */
public class ProcessActions extends WorkbenchPartAction {
	
	private ClassLoader loader;
	
	private GraphEditor editor;
	
	private SimulatorEngine engine;
	
	/**
	 * 
	 * 
	 * @param part A source of request an action
	 * @param actionType A type of action that will be processed
	 */
	public ProcessActions(GraphEditor part, String actionType) {
		super(part);
		
		this.setId(actionType);
		this.editor = (GraphEditor) getWorkbenchPart();
		this.engine = this.editor.getEngine();
		this.loader = this.editor.getLoader();
		
		// set class loader to editor
		if(this.loader == null){				
			this.loader = this.getClassLoader();
			this.editor.setLoader(this.loader);
		}			
	}
	
	private IJavaProject getClientProject(){
		IFileEditorInput input = (IFileEditorInput) this.editor.getEditorInput();
		IProject project = input.getFile().getProject();
		String usrProjectName = project.getName();

		IJavaModel javaModel = JavaCore.create(ResourcesPlugin.getWorkspace()
				.getRoot());
		IJavaProject javaProject = javaModel.getJavaProject(usrProjectName);

		return javaProject;
	}
	
	/*
	 * @param javaProject @return
	 */
	private ClassLoader getClassLoader() {
		ClassLoader loader = null;
		IJavaProject javaProject = this.getClientProject();		
		try {
			IClasspathEntry[] entries = javaProject.getRawClasspath();
			List<URL> urls = new ArrayList<URL>(entries.length);
			for (int i = 0; i < entries.length; i++) {
				IPath classpathEntryPath = entries[i].getPath();
				File classpathEntryFile = null;
				switch (entries[i].getEntryKind()) {
					case IClasspathEntry.CPE_SOURCE:
						IWorkspaceRoot root = ResourcesPlugin.getWorkspace()
								.getRoot();
						IPath out = root.getProject(
								classpathEntryPath.lastSegment()).getLocation();
						if (out != null)
							classpathEntryFile = out.toFile();
						else
							classpathEntryFile = root.getFolder(
									javaProject.getOutputLocation()).getLocation()
									.toFile();
	
						try {
							URI uri = classpathEntryFile.toURI();
							urls.add(uri.toURL());
						} catch (MalformedURLException e) {
							e.printStackTrace();
						}
						break;
					case IClasspathEntry.CPE_CONTAINER:
						break;
	
					// FIXME Must handle 2 more cases to handle the location of 
					// client source code
				}
			}
			// set default output (replay) file location w.r.t user bin location
			this.engine.setOutputLocation((URL) urls.get(0));
			
			// create class loader
			loader = new URLClassLoader((URL[]) urls.toArray(new URL[urls
					.size()]), ProcessActions.class.getClassLoader());
			
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		return loader;
	}

	/*
	 * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
	 */
	protected boolean calculateEnabled() {
		return this.canPerformAction();
	}

	/*
	 * Validate the action
	 */
	private boolean canPerformAction() {
		// if (getWorkbenchPart() instanceof GraphEditor) {
		// GraphEditor editor = (GraphEditor) getWorkbenchPart();
		// if (editor.getGraphElement() != null
		// && (editor.getClientObject() != null || this.execType
		// .equals(IGraphEditorConstants.LOAD_ID)))
		// return true;
		// }
		// return false;
		return true;
	}

	void missUseActionMsg(String msg) {
		MessageDialog.openError(this.editor.getSite().getShell(),
				"Miss use of action", msg);
	}

	boolean validateAction(String commandType) {
		Graph graph = editor.getGraphElement().getGraph();

		if (commandType == IGraphEditorConstants.ACTION_STOP
				|| commandType == IGraphEditorConstants.ACTION_SUSPEND) {
			
			if (editor.getClientObject() == null
					&& graph.getProtocol().equals("")) {
				MessageDialog.openError(
						getWorkbenchPart().getSite().getShell(),
						"Algoithm has not been loaded",
						"Try to load algorithm before execute this action");
				return false;
			}
		}

		if (commandType == IGraphEditorConstants.ACTION_RESUME) {
			
			if (editor.getClientObject() == null
					&& graph.getProtocol().equals("")) {
				MessageDialog.openError(
						getWorkbenchPart().getSite().getShell(),
						"Algoithm has not been loaded",
						"Try to load algorithm before execute this action");
				return false;
			} else {
				if (!graph.getProtocol().equals("")) {
					loadClientProtocol(graph.getModelId(), graph.getProtocol());
				}
			}
		} 
		
		// default return is true;
		return true;
	}
	
	boolean validateSavedGraph(GraphEditor editor) {
		if (editor.isDirty()) {
			MessageDialog messageBox;
			Shell parent = getWorkbenchPart().getSite().getShell();
			String[] btnText = { "No", "Cancel", "Yes" };
			messageBox = new MessageDialog(parent, "Save", null,
					"Graph has been modified. Do you want to save it?",
					MessageDialog.QUESTION, btnText, 2);
			messageBox.open();
			int ans = messageBox.getReturnCode();
			if (ans == 0)
				return true;
			else if (ans == 1)
				return false;
			else if (ans == 2) {
				// TODO might need to put progress monitor
				IProgressMonitor monitor = null;
				editor.doSave(monitor);
				return true;
			} else
				return false;

		} else
			return true;
	}


	public void run() {
		// execute action based on request
		String actionType = this.getId();		
		if (actionType.equals(IGraphEditorConstants.ACTION_RESUME)) {
			this.executeRun();
			
		} else if (actionType.equals(IGraphEditorConstants.ACTION_LOAD)) {
			this.executeLoadClient();
			
		} else if (actionType.equals(IGraphEditorConstants.ACTION_LOAD_RANDOM)) {
			this.executeLoadRandom();
			
		} else if (actionType.equals(IGraphEditorConstants.ACTION_LOAD_ADVERSARY)) {
			this.executeLoadAdversary();
			
		} else if (actionType.equals(IGraphEditorConstants.ACTION_SUSPEND)) {
			this.executeSuspend();
			
		} else if (actionType.equals(IGraphEditorConstants.ACTION_STOP)) {
			this.executeStop();
			
		} else if (actionType.equals(IGraphEditorConstants.ACTION_STEP_NEXT)) {
			this.executeStepNext();
			
		} else if (actionType.equals(IGraphEditorConstants.ACTION_SET_SPEED)) {
			this.executeSpeed();
			
		} else if (actionType.equals(IGraphEditorConstants.ACTION_REPLAY_RECORD)) {
			this.executeReplay();			
		}
//		else if (actionType.equals(IGraphEditorConstants.ACTION_SAVE_RECORD)) {
//			this.executeSaveRecord();
//		}

	}
	
	/*
	 * Run replay record
	 */
	private void executeReplay(){
		
		// Open replay dialog that contains
		// file record input dialog
		Shell shell = getWorkbenchPart().getSite().getShell();		
		FileDialog dialog = new FileDialog(shell, SWT.OPEN);
		dialog.setFilterExtensions(new String[]{"*.rec"});		
		String fileName = dialog.open();	

		// validate input
		if(fileName != null){
			// double check graph instance
			Graph graph = this.doubleCheck();
			this.engine.replay(graph, fileName);
		}
	}
	
	private void executeSaveRecord(){
/*
		Shell shell = this.editor.getSite().getShell();		
		FileDialog dialog = new FileDialog(shell, SWT.SAVE);
		String filename = dialog.open();
		editor.setRecFileNameForSaving(filename);
		*/
	}
	
	/*
	 * FIXME Double check and validate an input graph
	 */
	private Graph doubleCheck(){
		
		this.validateSavedGraph(editor);
		
		GraphElement ge = editor.getGraphElement();
		List<NodeElement> nList = ge.getNodeElements();
		Graph graph = ge.getGraph();
		
		// FIXME Double assignment: sometime graph is empty
		// it should not happen !!
/*		Node node;
		for (NodeElement n : nList) {
			try {
				node = n.getNode();
				graph.addNode(n.getNodeId(), node);
			} catch (DisJException e) {
				System.err.println("@ProcessAction.doubleCheck()[WARNING] try to add duplicated node");
			}
		}
		List <LinkElement> eList = ge.getLinkElements();
		Edge edge;
		for (LinkElement le : eList) {
			try {
				edge = le.getEdge();
				graph.addEdge(le.getEdgeId(), edge);
			} catch (DisJException e) {
				System.err.println("@ProcessAction.doubleCheck()[WARNING] try to add duplicated edge");
			}
		}
*/		
		return graph;
	}
	
	/*
	 * Run use algorithm live.
	 */
	private void executeRun() {
		// first run
		if (this.engine.isStarted() == false) {
			if (!this.validateAction(IGraphEditorConstants.ACTION_RESUME)){
				return;
			}
			
			Graph graph = this.doubleCheck();
			
			// FIXME a cheated way to refresh the node color
			// to user setting state
//			Map<String, Node> nodes = graph.getNodes();
//			Iterator its = nodes.keySet().iterator();
//			for (Node node = null; its.hasNext();) {
//				node = (Node) nodes.get(its.next());
//				node.resetState(node.getCurState());
//			}			
			// FIXME why we need this copy???
			// editor.getGraphElement().copyGraphElement();
			
			// get clients objects
			Class client = editor.getClientObject();
			Class<IRandom> clientRan = editor.getClientRandomObject();
			Class clientAdv = editor.getClientAdversaryObject();
			
			// validate client objects matching
			boolean pass = false;
			if(clientAdv != null){
				int model = editor.getGraphElement().getGraph().getModelId();
				if(model == IConstants.MODEL_MESSAGE_PASSING){
					 if(MsgPassingControl.class.isAssignableFrom(clientAdv)){
						 pass = true;
					 }
				} else if (model == IConstants.MODEL_AGENT_WHITEBOARD){
					if(AgentControl.class.isAssignableFrom(clientAdv)){
						 pass = true;
					 }
				} else if (model == IConstants.MODEL_AGENT_TOKEN){
					if(AgentControl.class.isAssignableFrom(clientAdv)){
						 pass = true;
					 }
				}
				if(pass){
					this.engine.execute(graph, client, clientRan, clientAdv);
					
				}else{
					this.missUseActionMsg("Client Protocol Model does NOT match " +
							"with Adversary Protocol Model");
				}
			}else{
				this.engine.execute(graph, client, clientRan, null);
			}
			
		} else {
			try {
				if ((!this.engine.isRunning()) && this.engine.isStarted()
						&& this.engine.isSuspend()) {
					this.engine.resume();
				} else {
					this.missUseActionMsg("Engine is not suspened or stoped.");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void executeLoadClient(){
		if (this.engine.isStarted() == true) {
			MessageDialog.openError(
					getWorkbenchPart().getSite().getShell(),
					"Engine is running",
					"Stop the engine and reload algorithm again");
			return;
		}
		Shell parent = this.editor.getSite().getShell();
		String className = null;

		// open up class name input dialog
		ClientClassInputDialog classNameDialog = new ClientClassInputDialog(parent);
		classNameDialog.open();
		
		if(classNameDialog.getReturnCode() < 0){
			// user press cancel
			return;
		}else {
			// user pressed OK
			className = classNameDialog.getClassName();
			if(className != null){
				this.loadClientProtocol(classNameDialog.getReturnCode(), className);				
			}
		}
	}
	
	private void loadClientProtocol(int model, String protocol) {
		// open Dialog
		Shell parent = this.editor.getSite().getShell();
		try {
			// kill current running or pending process if exist
			// FIXME one engine must maps to many processors
			this.engine.terminate();
		} catch (DisJException ignore) {
		}

		try {
			// load client class
			Class client = this.loader.loadClass(protocol);

			if(model == IConstants.MODEL_MESSAGE_PASSING){
				if (!Entity.class.isAssignableFrom(client)) {
					MessageDialog.openError(parent, "Cannot load Client Class",
							"Class must extends Entity");
					return;
				}
			}else if (model == IConstants.MODEL_AGENT_WHITEBOARD){
				if (!BoardAgent.class.isAssignableFrom(client)) {
					MessageDialog.openError(parent, "Cannot load Client Class",
							"Class must extends BoardAgent");
					return;
				}
			}else {
				if (!TokenAgent.class.isAssignableFrom(client)) {
					MessageDialog.openError(parent, "Cannot load Client Class",
							"Class must extends TokenAgent");
					return;
				}
			}
			
			// set client's class to editor			
			editor.setClientObject(client);
			editor.getGraphElement().getGraph().setProtocol(protocol);
			editor.getGraphElement().getGraph().setModelId(model);
			
		} catch (ClassNotFoundException e) {
			MessageDialog.openError(parent, "Unable to load client Classd", e
					.getMessage()
					+ " not found");

		} catch (Exception g) {
			// unexpected error
			String msg = g + "";
			MessageDialog.openError(parent,
					"Unexpected Load Client Class Error", msg);
		}
	}



	private void executeLoadRandom() {
		if (this.engine.isStarted() == true) {
			MessageDialog.openError(
					getWorkbenchPart().getSite().getShell(),
					"Engine is running",
					"Stop the engine and reload random library again");
			return;
		}
		// open Dialog
		Shell parent = this.editor.getSite().getShell();
		String className = "Fully Qualified Class Name";

		if (editor.getClientRandomObject() != null)
			className = editor.getClientRandomObject().getName();

		InputDialog classNameDialog = new InputDialog(parent,
				"Client Random generator Input Dialog", "Class Name",
				className, new ClassNameValidator());
		classNameDialog.open();

		// user pressed OK
		if (classNameDialog.getReturnCode() == 0) {
			className = classNameDialog.getValue();
			try {
				// kill current running or pending process if exist
				this.engine.terminate();

			} catch (DisJException ignore) {
			}
			try {
				Class<IRandom> clientRandom = this.loadClientRandomGenerator(parent,
						className);

				if (clientRandom == null)
					return;

				// load client's class into editor
				editor.setClientRandomObject(clientRandom);

			} catch (ClassNotFoundException e) {
				MessageDialog.openError(parent, "Unable to load client random Classd",
						e.getMessage() + " not found");

			} catch (Exception g) {
				// unexpected error
				String msg = g + "";
				MessageDialog.openError(parent,
						"Unexpected Load Client Random Class Error", msg);
			}

		} else {
			// user press cancel
			return;
		}
	}

	private Class<IRandom> loadClientRandomGenerator(Shell parent,
			String className) throws ClassNotFoundException {
		
		// load class
		Class clientRandom = this.loader.loadClass(className);
		if (!IRandom.class.isAssignableFrom(clientRandom)) {
			MessageDialog.openError(parent,
					"Cannot load user random generator Class ",
					"Class must implements IRandom");
			clientRandom = null;
		}
		return clientRandom;
	}

	


	private void executeLoadAdversary(){
		if (this.engine.isStarted() == true) {
			MessageDialog.openError(
					getWorkbenchPart().getSite().getShell(),
					"Engine is running",
					"Stop the engine and reload algorithm again");
			return;
		}
		Shell parent = this.editor.getSite().getShell();
		String className = null;

		// open up class name input dialog
		AdversaryClassInputDialog classNameDialog = new AdversaryClassInputDialog(parent);
		classNameDialog.open();
		
		if(classNameDialog.getReturnCode() < 0){
			// user press cancel
			return;
		}else {
			// user pressed OK
			className = classNameDialog.getClassName();
			if(className != null){
				this.loadClientAdversary(classNameDialog.getReturnCode(), className);
			}			
		}
	}

	private void loadClientAdversary(int model, String protocol) {
		
		// open Dialog
		Shell parent = this.editor.getSite().getShell();
		try {
			// kill current running or pending process if exist
			// FIXME one engine must maps to many processors
			this.engine.terminate();
		} catch (DisJException ignore) {
		}

		try {
			// load client class
			Class client = this.loader.loadClass(protocol);

			if(model == IConstants.MODEL_MESSAGE_PASSING){
				if (!MsgPassingControl.class.isAssignableFrom(client)) {
					MessageDialog.openError(parent, "Cannot load Adversary Class",
							"Class must extends MsgPassingControl");
					return;
				}
			}else if (model == IConstants.MODEL_AGENT_WHITEBOARD){
				if (!AgentControl.class.isAssignableFrom(client)) {
					MessageDialog.openError(parent, "Cannot load Adversary Class",
							"Class must extends AgentControl");
					return;
				}
			}else {
				if (!AgentControl.class.isAssignableFrom(client)) {
					MessageDialog.openError(parent, "Cannot load Adversary Class",
							"Class must extends AgentControl");
					return;
				}
			}
			// set adversary client's class to editor
			editor.setClientAdversaryObject(client);

		} catch (ClassNotFoundException e) {
			MessageDialog.openError(parent, "Unable to load adversary client Classd", e
					.getMessage()
					+ " not found");

		} catch (Exception g) {
			// unexpected error
			String msg = g + "";
			MessageDialog.openError(parent,
					"Unexpected Load Adversary Client Class Error", msg);
		}
	}
	
	private void executeStop() {
		if (!this.validateAction(IGraphEditorConstants.ACTION_STOP)){
			return;
		}
		if (this.engine.isStarted() == false) {
			return;
		}
		try {
			if (this.engine.isStarted()){
				this.engine.terminate();
			}

			if (this.engine.getCurGraphId() != null) {
				// reset the states and data of a graph
				editor.getGraphElement().resetGraphElement();
			} else {
				this.missUseActionMsg("Engine is not running");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void executeSuspend() {
		if (!this.validateAction(IGraphEditorConstants.ACTION_SUSPEND)){
			return;
		}
		if (this.engine.isStarted() == false) {
			return;
		}
		try {
			if (!this.engine.isSuspend())
				this.engine.suspend();
			else {
				this.missUseActionMsg("Engine already suspended");
			}
		} catch (Exception e) {
			e.printStackTrace();			
		}

	}

	private void executeStepNext() {
		if (this.engine.isStarted() == false) {
			return;
		}
		try {
			if (this.engine.isSuspend()) {
				this.engine.stepForward();
			}
		} catch (DisJException e) {
			e.printStackTrace();
		}
	}

	private void executeSpeed() {
		int speed = IConstants.SPEED_DEFAULT_RATE;
		SpeedDialog dialog = null;
		try{
			if(!this.engine.isStarted()){
				this.missUseActionMsg("No processor is running");
				
			}else if(this.engine.isSuspend()){
				speed = this.engine.getSpeed();
				Shell shell = this.getWorkbenchPart().getSite().getShell();
				dialog = new SpeedDialog(shell, speed);	
				dialog.open();
				
				speed = dialog.getSpeedInput();			
				this.engine.setSpeed(speed);
			}else{
				this.missUseActionMsg("Need to pause processor before setting speed");
			}			
		}catch(Exception e){
			this.missUseActionMsg("Error!! @executeSpeed() " + e);			
		}
			
	}


}
