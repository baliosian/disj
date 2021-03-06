/*******************************************************************************
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     DisJ Development Group
 *******************************************************************************/

package distributed.plugin.ui.models.topologies;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.swt.widgets.Shell;

import distributed.plugin.core.Node;
import distributed.plugin.ui.IGraphEditorConstants;
import distributed.plugin.ui.dialogs.TorusDialog;
import distributed.plugin.ui.models.GraphElementFactory;
import distributed.plugin.ui.models.LinkElement;
import distributed.plugin.ui.models.NodeElement;

/**
 * @author Me
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Torus extends AbstractGraph {

    private static final int GAP = IGraphEditorConstants.NODE_SIZE * 3;

    private boolean isOriented;
    
    private int cols;

    private int rows;

    private String type;
    
    private String linkType;

    private NodeElement[][] nodes;

    /**
     * Constructor
     */
    public Torus(GraphElementFactory factory, Shell shell, String type) {
    	super(factory, shell); 
    	this.isOriented = false;
        this.type = type;
        this.rows = 0;
        this.cols = 0;
        this.linkType = IGraphEditorConstants.BI;
        this.nodes = null;      
    }

    /**
     * @see distributed.plugin.ui.models.topologies.ITopology#getName()
     */
    public String getName() {
        return IGraphEditorConstants.CREATE_TORUS_COMD;
    }

    /**
     * @see distributed.plugin.ui.models.topologies.ITopology#createTopology()
     */
    public void createTopology() {

		TorusDialog dialog = new TorusDialog(this.shell);
		dialog.open();
		
		if (!dialog.isCancel()) {
			this.rows = dialog.getNumRows();
			this.cols = dialog.getNumCols();
			this.linkType = dialog.getLinkType();
			this.numInit = dialog.getNumInit();
			this.isOriented = dialog.isOriented();
			this.nodes = new NodeElement[this.rows][this.cols];
						
			for (int i = 0; i < this.rows; i++) {
				for (int k = 0; k < this.cols; k++) {
					this.nodes[i][k] = this.factory.createNodeElement();
				}
			}

			// links for horizontal directions
			for (int i = 0; i < this.rows; i++) {
				for (int k = 0; k < this.cols; k++) {
					LinkElement link;
					if (this.linkType.equals(IGraphEditorConstants.UNI))
						link = this.factory.createUniLinkElement();
					else
						link = this.factory.createBiLinkElement();
					this.links.add(link);
				}
			}

			// links for vertical directions
			for (int i = 0; i < this.cols; i++) {
				for (int j = 0; j < this.rows; j++) {
					LinkElement link;
					if (this.linkType.equals(IGraphEditorConstants.UNI))
						link = this.factory.createUniLinkElement();
					else
						link = this.factory.createBiLinkElement();
					this.links.add(link);
				}
			}

			// Torus2: a bottom end will have a repeated link for both
			// horizontal and vertical connection
			if (this.type.equals(IGraphEditorConstants.TORUS_2)) {
				this.links.remove(links.size() - 1);
			}
			
			// set init nodes
			super.nodes = this.getAllNodes();
			super.numNode = super.nodes.size();
	        this.setInitNodes();
		}
	}

    /**
     * @see distributed.plugin.ui.models.topologies.ITopology#getAllNodes()
     */
    public List<NodeElement> getAllNodes() {
        List<NodeElement> tmp = new ArrayList<NodeElement>();
        for (int i = 0; i < this.rows; i++) {
            for (int k = 0; k < this.cols; k++) {
                tmp.add(this.nodes[i][k]);
            }
        }
        return tmp;
    }
    
    /**
     * @see distributed.plugin.ui.models.topologies.ITopology#getConnectionType()
     */
    public String getConnectionType() {
        return this.linkType;
    }

    /**
     * @see distributed.plugin.ui.models.topologies.ITopology#applyLocation(org.eclipse.draw2d.geometry.Point)
     */
    public void applyLocation(Point point) {
        int x = point.x;
        int y = point.y;
        for (int i = 0; i < this.rows; i++) {
            for (int k = 0; k < this.cols; k++) {
                Point p = new Point(x + (k * GAP), y + (i * GAP));
                this.nodes[i][k].setLocation(p);
                this.nodes[i][k].setSize(new Dimension(
                        IGraphEditorConstants.NODE_SIZE,
                        IGraphEditorConstants.NODE_SIZE));
            }
        }
    }

    /**
     * @see distributed.plugin.ui.models.topologies.ITopology#setConnections()
     */
    public void setConnections() {
    	if(this.rows <= 0){
        	return;
        }
        if(this.type.equals(IGraphEditorConstants.TORUS_1))
            this.connectType1();
        else
            this.connectType2();
    }
    
    private void connectType1(){
        int count = -1;
        // horizontal connections
        for (int i = 0; i < this.rows; i++) {
            for (int k = 0; k < this.cols; k++) {
                LinkElement link = (LinkElement) this.links.get(++count);
                link.setSource(this.nodes[i][k]);
                link.attachSource();
                link.setTarget(this.nodes[i][(k + 1)%this.cols]);
                link.attachTarget();
                
                if(this.isOriented){
                	try{
    	            	Node s = this.nodes[i][k].getNode();
    	            	Node t = this.nodes[i][(k + 1)%this.cols].getNode();
    	            	s.setPortLable("east", link.getEdge());
    	            	t.setPortLable("west", link.getEdge());
    	            	
                	}catch(Exception e){
                		System.err.println("@Torus.setConnections1() Cannot do oriented horizontal " + e);
                	}
                }
            }
        }

        // vertical connections
        for (int i = 0; i < this.cols; i++) {
            for (int k = 0; k < this.rows; k++) {
                LinkElement link = (LinkElement) this.links.get(++count);
                link.setSource(this.nodes[k][i]);
                link.attachSource();
                link.setTarget(this.nodes[(k + 1)%this.rows][i]);
                link.attachTarget();
                
                if(this.isOriented){
                	try{
    	            	Node s = this.nodes[k][i].getNode();
    	            	Node t = this.nodes[(k + 1)%this.rows][i].getNode();
    	            	s.setPortLable("north", link.getEdge());
    	            	t.setPortLable("south", link.getEdge());
    	            	
                	}catch(Exception e){
                		System.err.println("@Torus.setConnections1() Cannot do oriented vertical " + e);
                	}
                }
            }
        }
    }

    private void connectType2(){
        int count = -1;
        // horizontal connections
        for (int i = 0; i < this.rows; i++) {
            for (int k = 0; k < this.cols; k++) {
                LinkElement link = (LinkElement) this.links.get(++count);
                link.setSource(this.nodes[i][k]);
                link.attachSource();
                if(k == this.cols-1){
                    link.setTarget(this.nodes[(i+1)%this.rows][0]);
                }else{
                    link.setTarget(this.nodes[i][k + 1]);
                }
                link.attachTarget();
                
                if(this.isOriented){
                	try{
    	            	Node s = this.nodes[i][k].getNode();
    	            	Node t = null;
    	            	if(k == this.cols-1){
    	            		t = this.nodes[(i+1)%this.rows][0].getNode();
    	            	}else{
    	            		t = this.nodes[i][k+1].getNode();
    	            	}
    	            	s.setPortLable("east", link.getEdge());
    	            	t.setPortLable("west", link.getEdge());
    	            	
                	}catch(Exception e){
                		System.err.println("@Torus.setConnections2() Cannot do oriented horizontal " + e);
                	}
                }
            }
        }

        // vertical connections
        for (int i = 0; i < this.cols; i++) {
            for (int k = 0; k < this.rows; k++) {
                // ignore a repeated link at a bottom end 
                if(i == this.cols-1 && k == this.rows -1){
                    break;
                }
                
                LinkElement link = (LinkElement) this.links.get(++count);
                link.setSource(this.nodes[k][i]);
                link.attachSource();
                if(k == this.rows -1){
                    link.setTarget(this.nodes[0][(i+1)%this.cols]);
                }else{
                    link.setTarget(this.nodes[(k + 1)%this.rows][i]);
                }
                link.attachTarget();
                
                if(this.isOriented){
                	try{
    	            	Node s = this.nodes[k][i].getNode();
    	            	Node t = null;
    	            	if(k == this.rows -1){
    	            		t = this.nodes[0][(i+1)%this.cols].getNode();
    	            	}else{
    	            		t = this.nodes[(k + 1)%this.rows][i].getNode();
    	            	}
    	            	s.setPortLable("south", link.getEdge());
    	            	t.setPortLable("north", link.getEdge());
    	            	
                	}catch(Exception e){
                		System.err.println("@Torus.setConnections2() Cannot do oriented vertical " + e);
                	}
                }
            }
        }
    }
    
}
