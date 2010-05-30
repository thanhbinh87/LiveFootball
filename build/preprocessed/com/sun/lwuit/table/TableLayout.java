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

import com.sun.lwuit.layouts.*;
import com.sun.lwuit.Component;
import com.sun.lwuit.Container;
import com.sun.lwuit.Display;
import com.sun.lwuit.geom.Dimension;
import com.sun.lwuit.plaf.Style;

/**
 * Layout manager similar in spirit to HTML tables allowing rows and columns 
 * of varying width/height.
 *
 * @author Shai Almog
 */
public class TableLayout extends Layout {
    private int currentRow;
    private int currentColumn;

    private static int minimumSizePerColumn = 10;

    private Constraint[][] tablePositions;

    private int[] columnSizes;
    private int[] columnPositions;
    private int[] rowPositions;
    private boolean[] modifableColumnSize;

    /**
     * Special case marker SPAN constraint reserving place for other elements
     */
    private static final Constraint SPAN_CONSTRAINT = new Constraint();

    private static int defaultColumnWidth = -1;
    private static int defaultRowHeight = -1;
    private boolean horizontalSpanningExists;
    private boolean verticalSpanningExists;

    /**
     * A table must declare the amount of rows and columns in advance
     *
     * @param rows rows of the table
     * @param columns columns of the table
     */
    public TableLayout(int rows, int columns) {
        tablePositions = new Constraint[rows][columns];
    }

    /**
     * Returns the component at the given row/column
     * 
     * @param row the row of the component
     * @param column the column of the component
     * @return the component instance
     */
    public Component getComponentAt(int row, int column) {
        return tablePositions[row][column].parent;
    }

    /**
     * @inheritDoc
     */
    public void layoutContainer(Container parent) {
        verticalSpanningExists = false;
        horizontalSpanningExists = false;

        // column and row size in pixels
        Style s = parent.getStyle();
        int top = s.getPadding(false, Component.TOP);
        int left = s.getPadding(parent.isRTL(), Component.LEFT);
        int bottom = s.getPadding(false, Component.BOTTOM);
        int right = s.getPadding(parent.isRTL(), Component.RIGHT);

        boolean rtl = parent.isRTL();

        columnSizes = new int[tablePositions[0].length];
        if(modifableColumnSize == null) {
            modifableColumnSize = new boolean[tablePositions[0].length];
        }
        columnPositions = new int[tablePositions[0].length];
        int[] rowSizes = new int[tablePositions.length];
        rowPositions = new int[tablePositions.length];

        int pWidth = parent.getLayoutWidth() - parent.getSideGap() - left - right;
        int pHeight = parent.getLayoutHeight() - parent.getBottomGap() - top - bottom;

        int currentX = left;
        for(int iter = 0 ; iter < columnSizes.length ; iter++) {
            columnSizes[iter] = getColumnWidthPixels(iter, pWidth, pWidth);
        }

        // try to recalculate the columns for none horizontally scrollable tables
        // so they are distributed sensibly if no room is available
        if(!parent.isScrollableX()) {
            int totalWidth = 0;
            int totalModifyablePixels = 0;

            // check how many columns we can modify (the user hasn't requested a specific size for those)
            for(int iter = 0 ; iter < modifableColumnSize.length ; iter++) {
                if(modifableColumnSize[iter]) {
                    totalModifyablePixels += columnSizes[iter];
                }
                totalWidth += columnSizes[iter];
            }
            if(pWidth < totalWidth) {
                int totalPixelsToRemove = totalWidth - pWidth;

                int totalPixelsNecessary = totalModifyablePixels - totalPixelsToRemove;

                // Go over the modifyable columns and remove the right pixels according to the ratio
                for(int iter = 0 ; iter < modifableColumnSize.length ; iter++) {
                    if(modifableColumnSize[iter]) {
                        columnSizes[iter] = (int)(((float)columnSizes[iter]) / ((float)totalModifyablePixels) * totalPixelsNecessary);
                    }
                }
            }
        }

        for(int iter = 0 ; iter < columnSizes.length ; iter++) {
            if(rtl) {
                currentX += columnSizes[iter];
                columnPositions[iter] = pWidth - currentX;
            } else {
                columnPositions[iter] = currentX;
                currentX += columnSizes[iter];
            }
        }

        int currentY = top;
        for(int iter = 0 ; iter < rowSizes.length ; iter++) {
            if(parent.isScrollableY()) {
                rowSizes[iter] = getRowHeightPixels(iter, pHeight, -1);
            } else {
                rowSizes[iter] = getRowHeightPixels(iter, pHeight, pHeight - currentY);
            }
            rowPositions[iter] = currentY;
            currentY += rowSizes[iter];
        }


        for(int r = 0 ; r < rowSizes.length ; r++) {
            for(int c = 0 ; c < columnSizes.length ; c++) {
                Constraint con = tablePositions[r][c];
                int conX, conY, conW, conH;
                if(con != null && con != SPAN_CONSTRAINT) {
                    Style componentStyle = con.parent.getStyle();
                    int leftMargin = componentStyle.getMargin(parent.isRTL(), Component.LEFT);
                    int topMargin = componentStyle.getMargin(false, Component.TOP);
                    conX = left + leftMargin + columnPositions[c];
                    conY = top + topMargin + rowPositions[r];
                    if(con.spanHorizontal > 1) {
                        horizontalSpanningExists = true;
                        int w = columnSizes[c];
                        for(int sh = 1 ; sh < con.spanHorizontal ; sh++) {
                            w += columnSizes[c + sh];
                        }

                        // for RTL we need to move the component to the side so spanning will work
                        if(rtl) {
                            conX = left + leftMargin + columnPositions[c + con.spanHorizontal - 1];
                        }
                        conW = w - leftMargin - componentStyle.getMargin(parent.isRTL(), Component.RIGHT);
                    } else {
                        conW = columnSizes[c] - leftMargin - componentStyle.getMargin(parent.isRTL(), Component.RIGHT);
                    }
                    if(con.spanVertical > 1) {
                        verticalSpanningExists = true;
                        int h = rowSizes[r];
                        for(int sv = 1 ; sv < con.spanVertical ; sv++) {
                            h += rowSizes[r + sv];
                        }
                        conH = h - topMargin - componentStyle.getMargin(false, Component.BOTTOM);
                    } else {
                        conH = rowSizes[r] - topMargin - componentStyle.getMargin(false, Component.BOTTOM);
                    }
                    placeComponent(rtl, con, conX, conY, conW, conH);
                }
            }
        }
    }

    /**
     * Returns the position of the given table row. A valid value is only returned after the
     * layout occurred.
     *
     * @param row the row in the table
     * @return the Y position in pixels or -1 if layout hasn't occured/row is too large etc.
     */
    public int getRowPosition(int row) {
        if(rowPositions != null && rowPositions.length > row) {
            return rowPositions[row];
        }
        return -1;
    }

    /**
     * Returns the position of the given table column. A valid value is only returned after the 
     * layout occurred.
     * 
     * @param col the column in the table
     * @return the X position in pixels or -1 if layout hasn't occured/column is too large etc.
     */
    public int getColumnPosition(int col) {
        if(columnPositions != null && columnPositions.length > col) {
            return columnPositions[col];
        }
        return -1;
    }

    /**
     * Places the component/constraint in the proper alignment within the cell whose bounds are given
     */
    private void placeComponent(boolean rtl, Constraint con, int x, int y, int width, int height) {
        con.parent.setX(x);
        con.parent.setY(y);
        con.parent.setWidth(width);
        con.parent.setHeight(height);
        Dimension pref = con.parent.getPreferredSize();
        int pWidth = pref.getWidth();
        int pHeight = pref.getHeight();
        if(pWidth < width) {
            int d = (width - pWidth);
            int a = con.align;
            if(rtl) {
                switch(a) {
                    case Component.LEFT:
                        a = Component.RIGHT;
                        break;
                    case Component.RIGHT:
                        a = Component.LEFT;
                        break;
                }
            }
            switch(a) {
                case Component.LEFT:
                    con.parent.setX(x);
                    con.parent.setWidth(width - d);
                    break;
                case Component.RIGHT:
                    con.parent.setX(x + d);
                    con.parent.setWidth(width - d);
                    break;
                case Component.CENTER:
                    con.parent.setX(x + d / 2);
                    con.parent.setWidth(width - d);
                    break;
            }
        }
        if(pHeight < height) {
            int d = (height - pHeight);
            switch(con.valign) {
                case Component.TOP:
                    con.parent.setY(y);
                    con.parent.setHeight(height - d);
                    break;
                case Component.BOTTOM:
                    con.parent.setY(y + d);
                    con.parent.setHeight(height - d);
                    break;
                case Component.CENTER:
                    con.parent.setY(y + d / 2);
                    con.parent.setHeight(height - d);
                    break;
            }
        }
    }

    private int getColumnWidthPixels(int column, int percentageOf, int available) {
        int current = 0;
        if(modifableColumnSize == null) {
            modifableColumnSize = new boolean[tablePositions[0].length];
        }

        for(int iter = 0 ; iter < tablePositions.length ; iter++) {
            Constraint c = tablePositions[iter][column];

            if(c == null || c == SPAN_CONSTRAINT || c.spanHorizontal > 1) {
                continue;
            }

            // width in percentage of the parent container
            if(c.width > 0 && available > -1) {
                current = Math.max(current, c.width * percentageOf / 100);
                modifableColumnSize[column] = false;
            } else {
                Style s = c.parent.getStyle();
                current = Math.max(current, c.parent.getPreferredW() + s.getMargin(false, Component.LEFT) + s.getMargin(false, Component.RIGHT));
                modifableColumnSize[column] = true;
            }
            if(available > -1) {
                current = Math.min(available, current);
            }
        }
        return current;
    }

    private int getRowHeightPixels(int row, int percentageOf, int available) {
        int current = 0;
        for(int iter = 0 ; iter < tablePositions[row].length ; iter++) {
            Constraint c = tablePositions[row][iter];

            if(c == null || c == SPAN_CONSTRAINT || c.spanVertical > 1) {
                continue;
            }

            // height in percentage of the parent container
            if(c.height > 0) {
                current = Math.max(current, c.height * percentageOf / 100);
            } else {
                Style s = c.parent.getStyle();
                current = Math.max(current, c.parent.getPreferredH() + s.getMargin(false, Component.BOTTOM) + s.getMargin(false, Component.TOP));
            }
            if(available > -1) {
                current = Math.min(available, current);
            }
        }
        return current;
    }

    /**
     * @inheritDoc
     */
    public Dimension getPreferredSize(Container parent) {
        Style s = parent.getStyle();
        int w = s.getPadding(false, Component.LEFT) + s.getPadding(false, Component.RIGHT);
        int h = s.getPadding(false, Component.TOP) + s.getPadding(false, Component.BOTTOM);

        for(int iter = 0 ; iter < tablePositions[0].length ; iter++) {
            w += getColumnWidthPixels(iter, Integer.MAX_VALUE, -1);
        }

        for(int iter = 0 ; iter < tablePositions.length ; iter++) {
            h += getRowHeightPixels(iter, Integer.MAX_VALUE, -1);
        }

        return new Dimension(w, h);
    }

    /**
     * Returns the row where the next operation of add will appear
     *
     * @return the row where the next operation of add will appear
     */
    public int getNextRow() {
        return currentRow;
    }

    /**
     * Returns the column where the next operation of add will appear
     *
     * @return the column where the next operation of add will appear
     */
    public int getNextColumn() {
        return currentColumn;
    }

    /**
     * @inheritDoc
     */
    public void addLayoutComponent(Object value, Component comp, Container c) {
        Constraint con = (Constraint)value;
        if(con == null) {
            con = createConstraint();
        } else {
            if(con.parent != null) {
                throw new IllegalArgumentException("Constraint already associated with component!");
            }
        }
        if(con.row < 0) {
            con.row = currentRow;
        }
        if(con.column < 0) {
            con.column = currentColumn;
        }
        con.parent = comp;
        if(tablePositions[con.row][con.column] != null) {
            throw new IllegalArgumentException("Row: " + con.row + " and column: " + con.column + " already occupied");
        }
        tablePositions[con.row][con.column] = con;
        if(con.spanHorizontal > 1 || con.spanVertical > 1) {
            for(int sh = 0 ; sh < con.spanHorizontal ; sh++) {
                for(int sv = 0 ; sv < con.spanVertical ; sv++) {
                    if(sh > 0 || sv > 0) {
                        if(tablePositions[con.row + sv][con.column + sh] == null) {
                            tablePositions[con.row + sv][con.column + sh] = SPAN_CONSTRAINT;
                        }
                    }
                }
            }
        }

        updateRowColumn();
    }

    private void updateRowColumn() {
        if(currentRow >= tablePositions.length) {
            return;
        }
        while(tablePositions[currentRow][currentColumn] != null) {
            currentColumn++;
            if(currentColumn >= tablePositions[0].length) {
                currentColumn = 0;
                currentRow++;
                if(currentRow >= tablePositions.length) {
                    return;
                }
            }
        }
    }

    /**
     * Returns the spanning for the table cell at the given coordinate
     * 
     * @param row row in the table
     * @param column column within the table
     * @return the amount of spanning 1 for no spanning
     */
    public int getCellHorizontalSpan(int row, int column) {
        return tablePositions[row][column].spanHorizontal;
    }

    /**
     * Returns the spanning for the table cell at the given coordinate
     *
     * @param row row in the table
     * @param column column within the table
     * @return the amount of spanning 1 for no spanning
     */
    public int getCellVerticalSpan(int row, int column) {
        return tablePositions[row][column].spanVertical;
    }

    /**
     * Indicates whether there is spanning within this layout
     *
     * @return true if the layout makes use of spanning
     */
    public boolean hasVerticalSpanning() {
        return verticalSpanningExists;
    }

    /**
     * Indicates whether there is spanning within this layout
     * 
     * @return true if the layout makes use of spanning
     */
    public boolean hasHorizontalSpanning() {
        return horizontalSpanningExists;
    }

    /**
     * @inheritDoc
     */
    public void removeLayoutComponent(Component comp) {
        for(int r = 0 ; r < tablePositions.length ; r++) {
            for(int c = 0 ; c < tablePositions[r].length ; c++) {
                if(tablePositions[r][c] != null && tablePositions[r][c].parent == comp) {
                    tablePositions[r][c] = null;
                    return;
                }
            }
        }
    }

    /**
     * @inheritDoc
     */
    public Object getComponentConstraint(Component comp) {
        for(int r = 0 ; r < tablePositions.length ; r++) {
            for(int c = 0 ; c < tablePositions[r].length ; c++) {
                if(tablePositions[r][c] != null && tablePositions[r][c].parent == comp) {
                    return tablePositions[r][c];
                }
            }
        }
        return null;
    }

    /**
     * Creates a new Constraint instance to add to the layout
     *
     * @return the default constraint
     */
    public Constraint createConstraint() {
        return new Constraint();
    }

    /**
     * Creates a new Constraint instance to add to the layout
     *
     * @param row the row for the table starting with 0
     * @param column the column for the table starting with 0
     * @return the new constraint
     */
    public Constraint createConstraint(int row, int column) {
        Constraint c = createConstraint();
        c.row = row;
        c.column = column;
        return c;
    }

    /**
     * Sets the minimum size for a column in the table, this is applicable for tables that are
     * not scrollable on the X axis. This will force the earlier columns to leave room for
     * the latter columns.
     *
     * @param minimumSize the minimum width of the column
     */
    public static void setMinimumSizePerColumn(int minimumSize) {
        minimumSizePerColumn = minimumSize;
    }

    /**
     * Indicates the minimum size for a column in the table, this is applicable for tables that are
     * not scrollable on the X axis. This will force the earlier columns to leave room for
     * the latter columns.
     *
     * @return  the minimum width of the column
     */
    public static int getMinimumSizePerColumn() {
        return minimumSizePerColumn;
    }

    /**
     * Indicates the default (in percentage) for the column width, -1 indicates
     * automatic sizing
     *
     * @param w width in percentage
     */
    public static void setDefaultColumnWidth(int w) {
        defaultColumnWidth = w;
    }


    /**
     * Indicates the default (in percentage) for the column width, -1 indicates
     * automatic sizing
     *
     * @return width in percentage
     */
    public static int getDefaultColumnWidth() {
        return defaultColumnWidth;
    }


    /**
     * Indicates the default (in percentage) for the row height, -1 indicates
     * automatic sizing
     *
     * @param h height in percentage
     */
    public static void setDefaultRowHeight(int h) {
        defaultRowHeight = h;
    }

    /**
     * Indicates the default (in percentage) for the row height, -1 indicates
     * automatic sizing
     *
     * @return height in percentage
     */
    public static int getDefaultRowHeight() {
        return defaultRowHeight;
    }

    /**
     * Represents the layout constraint for an entry within the table indicating
     * the desired position/behavior of the component.
     */
    public static class Constraint {
        private Component parent;
        private int row = -1;
        private int column = -1;
        private int width = defaultColumnWidth;
        private int height = defaultRowHeight;
        private int spanHorizontal = 1;
        private int spanVertical = 1;
        private int align = -1;
        private int valign = -1;

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
}
