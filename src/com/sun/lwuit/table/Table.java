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
package com.sun.lwuit.table;

import com.sun.lwuit.Component;
import com.sun.lwuit.Container;
import com.sun.lwuit.Display;
import com.sun.lwuit.Form;
import com.sun.lwuit.Graphics;
import com.sun.lwuit.Label;
import com.sun.lwuit.TextArea;
import com.sun.lwuit.TextField;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.events.DataChangedListener;
import com.sun.lwuit.geom.Rectangle;
import com.sun.lwuit.plaf.Border;
import com.sun.lwuit.plaf.Style;

/**
 * The table class represents a grid of data that can be used for rendering a grid
 * of components/labels. The table reflects and updates the underlying model data.
 *
 * @author Shai Almog
 */
public class Table extends Container {
    private TableModel model;
    private Listener listener = new Listener();
    private boolean drawBorder = true;
    private boolean includeHeader = true;

    /**
     * Indicates the alignment of the title see label alignment for details
     * 
     * @see com.sun.lwuit.Label#setAlignment(int) 
     */
    private int titleAlignment = Label.CENTER;

    /**
     * Indicates the alignment of the cells see label alignment for details
     * 
     * @see com.sun.lwuit.Label#setAlignment(int)
     */
    private int cellAlignment = Label.LEFT;

    /**
     * This flag allows us to workaround issue 275 without incuring too many updateModel calls
     */
    private boolean potentiallyDirtyModel;

    /**
     * Create a table with a new model
     *
     * @param model the model underlying this table
     */
    public Table(TableModel model) {
        this.model = model;
        updateModel();
        setUIID("Table");
    }

    /**
     * Create a table with a new model
     *
     * @param model the model underlying this table
     * @param includeHeader Indicates whether the table should render a table header as the first row
     */
    public Table(TableModel model, boolean includeHeader) {
        setUIID("Table");
        this.includeHeader = includeHeader;
        this.model = model;
        updateModel();
    }

    /**
     * Returns the selected row in the table
     *
     * @return the offset of the selected row in the table if a selection exists
     */
    public int getSelectedRow() {
        Form f = getComponentForm();
        if(f != null) {
            Component c = f.getFocused();
            if(c != null) {
                return getCellRow(c);
            }
        }
        return -1;
    }

    /**
     * Returns the selected column in the table
     *
     * @return the offset of the selected column in the table if a selection exists
     */
    public int getSelectedColumn() {
        Form f = getComponentForm();
        if(f != null) {
            Component c = f.getFocused();
            if(c != null) {
                return getCellColumn(c);
            }
        }
        return -1;
    }

    private void updateModel() {
        int selectionRow = -1, selectionColumn = -1;
        Form f = getComponentForm();
        if(f != null) {
            Component c = f.getFocused();
            if(c != null) {
                selectionRow = getCellRow(c);
                selectionColumn = getCellColumn(c);
            }
        }
        removeAll();
        int columnCount = model.getColumnCount();

        // another row for the table header
        if(includeHeader) {
            setLayout(new TableLayout(model.getRowCount() + 1, columnCount));
            for(int iter = 0 ; iter < columnCount ; iter++) {
                String name = model.getColumnName(iter);
                Component header = createCellImpl(name, -1, iter, false);
                TableLayout.Constraint con = createCellConstraint(name, -1, iter);
                addComponent(con, header);
            }
        } else {
            setLayout(new TableLayout(model.getRowCount(), columnCount));
        }

        for(int r = 0 ; r < model.getRowCount() ; r++) {
            for(int c = 0 ; c < columnCount ; c++) {
                Object value = model.getValueAt(r, c);

                // null should be returned for spanned over values
                if(value != null) {
                    boolean e = model.isCellEditable(r, c);
                    Component cell = createCellImpl(value, r, c, e);
                    if(cell != null) {
                        TableLayout.Constraint con = createCellConstraint(value, r, c);

                        // returns the current row we iterate about
                        int currentRow = ((TableLayout)getLayout()).getNextRow();
                        if(currentRow > model.getRowCount()) {
                            return;
                        }
                        addComponent(con, cell);
                        if(r == selectionRow && c == selectionColumn) {
                            cell.requestFocus();
                        }
                    }
                }
            }
        }
    }

    /**
     * @inheritDoc
     */
    protected void paintGlass(Graphics g) {
        if(drawBorder) {
            int xPos = getAbsoluteX();
            int yPos = getAbsoluteY();
            g.translate(xPos, yPos);
            int rows = model.getRowCount();
            int cols = model.getColumnCount();
            if(includeHeader) {
                rows++;
            }
            g.setColor(getStyle().getFgColor());
            TableLayout t = (TableLayout)getLayout();
            int actualWidth = Math.max(getWidth(), getScrollDimension().getWidth());
            if(t.hasVerticalSpanning()) {
                // iterate over the components and draw a line at the bottom of all
                // the components other than the ones that are at the last row.
                int compCount = getComponentCount();
                int padd = getStyle().getPadding(LEFT) + getStyle().getMargin(RIGHT);
                for(int iter = 0 ; iter < compCount ; iter++) {
                    Component c = getComponentAt(iter);
                    if(getCellRow(c) != rows - 1) {
                        int y = c.getY() + c.getHeight();
                        int left = c.getStyle().getMargin(LEFT);
                        int right = c.getStyle().getMargin(RIGHT);
                        g.drawLine(c.getX() - left - padd, y, c.getX() + c.getWidth() + left + right - padd, y);
                    }
                }
            } else {
                // this is much faster since we don't need to check spanning
                for(int row = 1 ; row < rows; row++) {
                    int y = t.getRowPosition(row);
                    g.drawLine(0, y, actualWidth, y);
                }
            }
            int actualHeight = Math.max(getHeight(), getScrollDimension().getHeight());
            if(t.hasHorizontalSpanning()) {
                // iterate over the components and draw a line on the side of all
                // the components other than the ones that are at the last column.
                int compCount = getComponentCount();
                for(int iter = 0 ; iter < compCount ; iter++) {
                    Component c = getComponentAt(iter);
                    int cellColumn = getCellColumn(c);
                    int cellRow = getCellRow(c);
                    // if this isn't the last column
                    if(cellColumn != cols - 1 && cellColumn + t.getCellHorizontalSpan(cellRow, cellColumn) - 1 != cols - 1) {
                        int x = t.getColumnPosition(cellColumn);
                        int y = t.getRowPosition(cellRow);
                        int rowHeight;
                        int columnWidth = t.getColumnPosition(cellColumn + 1) - x;
                        if(cellRow < getModel().getRowCount() - 1) {
                            rowHeight = t.getRowPosition(cellRow + 1) - y;
                        } else {
                            rowHeight = getHeight() - y;
                        }
                        
                        g.drawLine(x + columnWidth, y, x + columnWidth, y + rowHeight);
                    }
                }
            } else {
                for(int col = 1 ; col < cols ; col++) {
                    int x = t.getColumnPosition(col);
                    g.drawLine(x, 0, x, actualHeight);
                }
            }
            g.translate(-xPos, -yPos);
        }
    }

    private Component createCellImpl(Object value, final int row, final int column, boolean editable) {
        Component c = createCell(value, row, column, editable);
        c.putClientProperty("row", new Integer(row));
        c.putClientProperty("column", new Integer(column));
        
        // we do this here to allow subclasses to return a text area or its subclass
        if(c instanceof TextArea) {
            ((TextArea)c).addActionListener(listener);
        } 

        Style s = c.getSelectedStyle();
        s.setMargin(0, 0, 0, 0);
        if(drawBorder) {
            s.setBorder(null);
            s = c.getUnselectedStyle();
            s.setBorder(null);
        } else {
            s = c.getUnselectedStyle();
        }
        s.setBgTransparency(0);
        s.setMargin(0, 0, 0, 0);
        return c;
    }

    /**
     * Creates a cell based on the given value
     *
     * @param value the new value object
     * @param row row number, -1 for the header rows
     * @param column column number
     * @param editable true if the cell is editable
     * @return cell component instance
     */
    protected Component createCell(Object value, int row, int column, boolean editable) {
        if(row == -1) {
            Label header = new Label((String)value);
            header.setUIID("TableHeader");
            header.setAlignment(titleAlignment);
            header.setFocusable(true);
            return header;
        }
        if(editable) {
            TextField cell = new TextField("" + value, -1);
            cell.setLeftAndRightEditingTrigger(false);
            cell.setUIID("TableCell");
            return cell;
        }
        Label cell = new Label("" + value);
        cell.setUIID("TableCell");
        cell.setAlignment(cellAlignment);
        cell.setFocusable(true);
        return cell;
    }

    /**
     * @inheritDoc
     */
    public void initComponent() {
        // this can happen if deinitialize is invoked due to a menu command which modifies
        // the content of the table while the listener wasn't bound
        if(potentiallyDirtyModel) {
            updateModel();
            potentiallyDirtyModel = false;
        }
        model.addDataChangeListener(listener);
    }

    /**
     * @inheritDoc
     */
    public void deinitialize() {
        // we unbind the listener to prevent a memory leak for the use case of keeping
        // the model while discarding the component
        potentiallyDirtyModel = true;
        model.removeDataChangeListener(listener);
    }

    /**
     * Replaces the underlying model
     *
     * @param model the new model
     */
    public void setModel(TableModel model) {
        this.model = model;
        updateModel();
        revalidate();
    }

    /**
     * Returns the model instance
     *
     * @return the model instance
     */
    public TableModel getModel() {
        return model;
    }

    /**
     * Indicates whether the table border should be drawn
     *
     * @return the drawBorder
     */
    public boolean isDrawBorder() {
        return drawBorder;
    }

    /**
     * Indicates whether the table border should be drawn
     *
     * @param drawBorder the drawBorder to set
     */
    public void setDrawBorder(boolean drawBorder) {
        this.drawBorder = drawBorder;
        repaint();
    }

    /**
     * Indicates the alignment of the title see label alignment for details
     *
     * @return the title alignment
     * @see com.sun.lwuit.Label#setAlignment(int)
     */
    public int getTitleAlignment() {
        return titleAlignment;
    }

    /**
     * Indicates the alignment of the title see label alignment for details
     *
     * @param titleAlignment the title alignment
     * @see com.sun.lwuit.Label#setAlignment(int)
     */
    public void setTitleAlignment(int titleAlignment) {
        this.titleAlignment = titleAlignment;
        repaint();
    }


    /**
     * Returns the column in which the given cell is placed
     * 
     * @param cell the component representing the cell placed in the table
     * @return the column in which the cell was placed in the table
     */
    public int getCellColumn(Component cell) {
        Integer i = ((Integer)cell.getClientProperty("column"));
        if(i != null) {
            return i.intValue();
        }
        return -1;
    }

    /**
     * Returns the row in which the given cell is placed
     * 
     * @param cell the component representing the cell placed in the table
     * @return the row in which the cell was placed in the table
     */
    public int getCellRow(Component cell) {
        Integer i = ((Integer)cell.getClientProperty("row"));
        if(i != null) {
            return i.intValue();
        }
        return -1;
    }

    /**
     * Indicates the alignment of the cells see label alignment for details
     *
     * @see com.sun.lwuit.Label#setAlignment(int)
     * @return the cell alignment
     */
    public int getCellAlignment() {
        return cellAlignment;
    }

    /**
     * Indicates the alignment of the cells see label alignment for details
     *
     * @param cellAlignment the table cell alignment
     * @see com.sun.lwuit.Label#setAlignment(int)
     */
    public void setCellAlignment(int cellAlignment) {
        this.cellAlignment = cellAlignment;
        repaint();
    }

    /**
     * Indicates whether the table should render a table header as the first row
     *
     * @return the includeHeader
     */
    public boolean isIncludeHeader() {
        return includeHeader;
    }

    /**
     * Indicates whether the table should render a table header as the first row
     * 
     * @param includeHeader the includeHeader to set
     */
    public void setIncludeHeader(boolean includeHeader) {
        this.includeHeader = includeHeader;
    }

    /**
     * Creates the table cell constraint for the given cell, this method can be overriden for
     * the purposes of modifying the table constraints.
     *
     * @param value the value of the cell
     * @param row the table row
     * @param column the table column
     * @return the table constraint
     */
    protected TableLayout.Constraint createCellConstraint(Object value, int row, int column) {
        if(includeHeader) {
            row++;
        }
        TableLayout t = (TableLayout)getLayout();
        return t.createConstraint(row, column);
    }

    class Listener implements DataChangedListener, ActionListener {
        /**
         * @inheritDoc
         */
        public final void dataChanged(int row, int column) {
            Object value = model.getValueAt(row, column);
            boolean e = model.isCellEditable(row, column);
            Component cell = createCellImpl(value, row, column, e);

            TableLayout t = (TableLayout)getLayout();
            TableLayout.Constraint con = createCellConstraint(value, row, column);
            if(includeHeader) {
                row++;
            }

            removeComponent(t.getComponentAt(row, column));
            addComponent(con, cell);
            layoutContainer();
            cell.requestFocus();
            revalidate();
        }

        public void actionPerformed(ActionEvent evt) {
            TextArea t = (TextArea)evt.getSource();
            int row = getCellRow(t);
            int column = getCellColumn(t);
            getModel().setValueAt(row, column, t.getText());
        }
    }
}
