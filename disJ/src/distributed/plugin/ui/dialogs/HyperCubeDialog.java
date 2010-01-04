/*******************************************************************************
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     DisJ Development Group
 *******************************************************************************/

package distributed.plugin.ui.dialogs;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import distributed.plugin.ui.IGraphEditorConstants;

/**
 * @author Me
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class HyperCubeDialog extends Dialog {

    private int dimension;
    private String linkType;
    
    /**
     * @param arg0
     */
    public HyperCubeDialog(Shell arg0) {
        super(arg0);
        setText("HyperCube Dialog");
        this.dimension = 0;
        this.linkType = null;
    }

    public void open() {

        final Shell shell = new Shell(getParent(), SWT.DIALOG_TRIM
                | SWT.APPLICATION_MODAL);
        shell.setText(getText());
        shell.setSize(350, 150);

        // number of node
        Label numNodeQs = new Label(shell, SWT.NONE);
        numNodeQs.setLocation(25, 10);
        numNodeQs.setSize(160, 25);
        numNodeQs.setText("Number of Dimension: ");

        final Text txtResponse = new Text(shell, SWT.BORDER);
        txtResponse.setLocation(190, 10);
        txtResponse.setSize(50, 25);

        // link type
        Label direct = new Label(shell, SWT.NONE);
        direct.setLocation(25, 40);
        direct.setSize(100, 25);
        direct.setText("Type of Link: ");

        final Combo type = new Combo(shell, SWT.DROP_DOWN | SWT.READ_ONLY);
        type.setItems(new String[] {IGraphEditorConstants.UNI, IGraphEditorConstants.BI});
        type.select(1);
        type.setLocation(130, 40);
        type.setSize(150, 25);
        
        // final button
        final Button btnOkay = new Button(shell, SWT.PUSH);
        btnOkay.setText("Ok");
        btnOkay.setLocation(40, 100);
        btnOkay.setSize(100, 30);

        final Button btnCancel = new Button(shell, SWT.PUSH);
        btnCancel.setText("Cancel");
        btnCancel.setLocation(160, 100);
        btnCancel.setSize(100, 30);
        btnCancel.setSelection(true);

        Listener listener = new Listener() {
            public void handleEvent(Event event) {
                if(event.widget == btnOkay){
                    String res = validateInput(txtResponse.getText());
                    if(res != null){
                        MessageDialog.openError(getParent(), "Invalid Input",
                                res);
                        return;
                    } else {
                        dimension = Integer.parseInt(txtResponse.getText().trim());
                    }
                    linkType = type.getText();
                }                
                shell.close();
            }
        };
        
        btnOkay.addListener(SWT.Selection, listener);
        btnCancel.addListener(SWT.Selection, listener);

        shell.open();
        Display display = getParent().getDisplay();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
    }
    
    private String validateInput(Object param){
        try{
            int i = Integer.parseInt(((String)param).trim());
            if(i < 2)
                return "The size of ring should more than 1";
            else
                return null;
        }catch(NumberFormatException n){
            return "Input must be an integer number";
        }
        
    }
    

    public String getLinkType() {
        return linkType;
    }
    public int getDimension() {
        return dimension;
    }
}