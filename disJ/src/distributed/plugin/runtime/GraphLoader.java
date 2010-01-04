/*******************************************************************************
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     DisJ Development Group
 *******************************************************************************/

package distributed.plugin.runtime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import distributed.plugin.core.DisJException;
import distributed.plugin.core.Edge;
import distributed.plugin.core.IConstants;
import distributed.plugin.core.Node;
import distributed.plugin.runtime.engine.Entity;

/**
 * @author Me
 * 
 * A utilities API for loading and accessing internal data of a given graph
 */
public class GraphLoader {

    // caching storages; for a long operation and using more than one time

    private static Map initNodeTable = new HashMap();

    /**
     * Get all the initializer node of a given graph
     * 
     * @param graph
     * @return
     */
    public static List getInitNodes(Graph graph) {

        if (graph == null)
            throw new NullPointerException(IConstants.RUNTIME_ERROR_0);

        List initList = (List) initNodeTable.get(graph);
        if (initList == null) {
            Map nodes = graph.getNodes();
            Iterator itr = nodes.keySet().iterator();
            initList = new ArrayList();
            while (itr.hasNext()) {
                Object nodeName = itr.next();
                Node initNode = (Node) nodes.get(nodeName);
                if (initNode.isInitializer()) {
                    initList.add(initNode);
                }
            }
            initNodeTable.put(graph, initList);
        }
        return initList;
    }

    /**
     * TODO Clean up all the caches
     * 
     */
    public static void cleanAllCaches() {

    }

    /**
     * Create a client entity object by reflection with empty constructor
     * 
     * @param client
     *            a client class object that need to be created
     * @return
     * @throws Exception
     *             All the exception that caused by reflection processes
     */
    public static Entity createEntityObject(Class client) throws Exception {
        if (client == null)
            throw new NullPointerException(IConstants.RUNTIME_ERROR_0);

        Entity entity = null;
        try {
            entity = (Entity) client.newInstance();
        } catch (InstantiationException e) {
            throw new DisJException(IConstants.ERROR_8, e.toString());
        }
        return entity;
    }

    /**
     * Get a local port's label of a given edge on a given node
     * 
     * @param node
     * @param edge
     * @return
     */
    public static String getEdgeLabel(Node node, Edge edge) {

        Map edges = node.getEdges();
        Iterator it = edges.keySet().iterator();
        while (it.hasNext()) {
            Object label = it.next();
            if (edges.get(label).equals(edge))
                return (String) label;
        }
        return null;
    }

    /**
     * A deep clone graph object by making use of serialiazable
     * 
     * @param graph
     * @return
     */
    public static Graph cloneGraph(Graph graph) throws IOException {
        try {
            return (Graph) GraphLoader.deepClone(graph);
        } catch (DisJException e) {
            e.printStackTrace();
            throw new IOException(e.toString());
        }
    }

    /**
     * Make a deep clone of the given object by making use of serializable
     * 
     * @param object @return
     */
    public static Object deepClone(Object object) throws DisJException,
            IOException {

        if (object == null)
            return null;

        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(object);
            oos.flush();
            ByteArrayInputStream bin = new ByteArrayInputStream(bos
                    .toByteArray());
            ois = new ObjectInputStream(bin);
            return ois.readObject();
        } catch (Exception ie) {
            throw new DisJException(ie);
        } finally {
            if (oos != null)
                oos.close();
            if (ois != null)
                ois.close();
        }
    }

}