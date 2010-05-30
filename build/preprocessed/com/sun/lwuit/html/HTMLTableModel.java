/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package com.sun.lwuit.html;

import com.sun.lwuit.Component;
import com.sun.lwuit.events.DataChangedListener;
import com.sun.lwuit.table.TableModel;
import com.sun.lwuit.util.EventDispatcher;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * HTMLTableModel is a TableModel that is adapted to HTMLTables and their creation. The difference from other models is:<br>
 * - There are no column names, on the other hand each cell anywhere can be defined as a header (which merely changes it style).<br>
 * - There is no prior declaration of the number of columns and rows in the table.<br>
 * - Cells can be added only using the addCell method which adds them to the end of the current row.<br>
 * - When a row ends, the commitRow method is called which results in creating a new empty row.<br>
 * - Since a cell in an HTML table can be practically everthing (From a simple string to a whole document), the objects added to the table are in fact LWUIT components.
 *
 * @author Ofir Leitner
 */
class HTMLTableModel implements TableModel{

    Vector rows=new Vector();
    Vector headers=new Vector();
    int maxColumn;
    Vector currentRow = new Vector();
    Hashtable constraints = new Hashtable();
    private EventDispatcher dispatcher = new EventDispatcher();

    /**
     * Adds the given component as a cell to the end of the current row of the table
     *
     * @param cell The component to add
     * @param isHeader true if this is a header cell (Element.TAG_TH), false otherwise
     * @param constraint Specific constraints for this cell (alignment, spanning)
     */
    void addCell(Component cell,boolean isHeader,CellConstraint constraint) {
        if (isHeader) {
            headers.addElement(cell);
        }
        currentRow.addElement(cell);
        if (currentRow.size()>maxColumn) {
            maxColumn=currentRow.size();
        }
        if (constraint!=null) {
            constraints.put(cell,constraint);
        }
    }

    /**
     * Sets the given alignment as a constraint to all cells in the table
     * 
     * @param isHorizontal true to set horizontal alignment, false for vertical
     * @param align The requested alignment
     */
    void setAlignToAll(boolean isHorizontal,int align) {
        for (Enumeration e=constraints.elements();e.hasMoreElements();) {
            CellConstraint cc=(CellConstraint)e.nextElement();
            if (isHorizontal) {
                cc.setHorizontalAlign(align);
            } else {
                cc.setVerticalAlign(align);
            }
        }
    }

    /**
     * Returns the constraint for the specified object/cell
     * 
     * @param object The object/cell 
     * @return the constraint for the specified object/cell
     */
    CellConstraint getConstraint(Object object) {
        return (CellConstraint)constraints.get(object);
    }

    /**
     * Checks if the object is a header
     * 
     * @param object The object/cell in question
     * @return true if object is a header, false otherwise
     */
    boolean isHeader(Object object) {
        return headers.contains(object);
    }

    /**
     * Commits the current row. This opens a new empty row.
     */
    void commitRow() {
        rows.addElement(currentRow);
        currentRow=new Vector();
    }

   /**
    *  Commits the current row only if it is not empty
    */
    void commitRowIfNotEmpty() {
        if (currentRow.size()>0) {
            commitRow();
        }
    }


    // TableModel methods:

    /**
     * {@inheritDoc}
     */
    public int getRowCount() {
        return rows.size();
    }

    /**
     * {@inheritDoc}
     */
    public int getColumnCount() {
        return maxColumn;
    }

    /**
     * {@inheritDoc}
     */
    public String getColumnName(int i) {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Object getValueAt(int row, int column) {
        if (row>=rows.size()) {
            return null;
        }
        Vector columns=(Vector)rows.elementAt(row);
        if (column>=columns.size()) {
            return null;
        }
        return columns.elementAt(column);
    }

    /**
     * {@inheritDoc}
     */
    public void setValueAt(int row, int column, Object o) {
        Vector columns=(Vector)rows.elementAt(row);
        columns.removeElementAt(column);
        columns.setElementAt(o, column);
        dispatcher.fireDataChangeEvent(column, row);
    }

    /**
     * @inheritDoc
     */
    public void addDataChangeListener(DataChangedListener d) {
        dispatcher.addListener(d);
    }

    /**
     * @inheritDoc
     */
    public void removeDataChangeListener(DataChangedListener d) {
        dispatcher.removeListener(d);
    }

}

    /**
     * CellConstraint is very similar to com.sun.lwuit.table.TableLayout.Constraint in the sense it holds about the same data.
     * It is used to store the constraints of each cell, and allows modifying and readind them - then, when the actual table is drawn
     * They are converted to TableLayout.Constraint in HTMLTable.createCellConstraint.
     *
     * This is needed because:
     * 1. Once a TableLayout.Constraint is used to draw a table it is assigned a parent and can't be reused again (See TableLayout.addLayoutComponent)
     * 2. TableLayout.Constraint does not allow reading its values, so cloning the constraint (without the parent) is impossible
     *
     * @author Ofir Leitner
     */
    class CellConstraint {
        int width = -1;//defaultColumnWidth;
        int height = -1;//defaultRowHeight;
        int spanHorizontal = 1;
        int spanVertical = 1;
        int align = -1;
        int valign = -1;

        /**
         * Sets the cells to span vertically, this number must never be smaller than 1
         *
         * @param span a number larger than 1
         */
        public void setVerticalSpan(int span) {
            if(span < 1) {
                throw new IllegalArgumentException("Illegal span");
            }
            spanVertical = span;
        }

        /**
         * Sets the cells to span horizontally, this number must never be smaller than 1
         *
         * @param span a number larger than 1
         */
        public void setHorizontalSpan(int span) {
            if(span < 1) {
                throw new IllegalArgumentException("Illegal span");
            }
            spanHorizontal = span;
        }

        /**
         * Sets the column width based on percentage of the parent
         *
         * @param width negative number indicates ignoring this member
         */
        public void setWidthPercentage(int width) {
            this.width = width;
        }

        /**
         * Sets the row height based on percentage of the parent
         *
         * @param height negative number indicates ignoring this member
         */
        public void setHeightPercentage(int height) {
            this.height = height;
        }

        /**
         * Sets the horizontal alignment of the table cell
         *
         * @param align Component.LEFT/RIGHT/CENTER
         */
        public void setHorizontalAlign(int align) {
            this.align = align;
        }

        /**
         * Sets the vertical alignment of the table cell
         *
         * @param valign Component.TOP/BOTTOM/CENTER
         */
        public void setVerticalAlign(int valign) {
            this.valign = valign;
        }
    }

