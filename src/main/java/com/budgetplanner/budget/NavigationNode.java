package com.budgetplanner.budget;

import java.time.Month;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class NavigationNode {
    private String label;
    private NodeType type;
    private Year year;
    private Month month;
    private List<NavigationNode> children;
    private NavigationNode parent;
    private boolean expanded;
    private boolean selected;

    public enum NodeType {
        YEAR, MONTH
    }

    // Constructor for Year node
    public NavigationNode(Year year) {
        this.year = year;
        this.label = year.toString();
        this.type = NodeType.YEAR;
        this.children = new ArrayList<>();
        this.expanded = false;
        this.selected = false;
    }

    // Constructor for Month node
    public NavigationNode(Year year, Month month) {
        this.year = year;
        this.month = month;
        this.label = getShortMonthName(month);
        this.type = NodeType.MONTH;
        this.children = new ArrayList<>();
        this.expanded = false;
        this.selected = false;
    }

    private String getShortMonthName(Month month) {
        return switch (month) {
            case JANUARY -> "Jan";
            case FEBRUARY -> "Feb";
            case MARCH -> "Mar";
            case APRIL -> "Apr";
            case MAY -> "May";
            case JUNE -> "Jun";
            case JULY -> "Jul";
            case AUGUST -> "Aug";
            case SEPTEMBER -> "Sep";
            case OCTOBER -> "Oct";
            case NOVEMBER -> "Nov";
            case DECEMBER -> "Dec";
        };
    }

    public void addChild(NavigationNode child) {
        child.setParent(this);
        this.children.add(child);
    }

    // Getters and setters
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public NodeType getType() {
        return type;
    }

    public void setType(NodeType type) {
        this.type = type;
    }

    public Year getYear() {
        return year;
    }

    public void setYear(Year year) {
        this.year = year;
    }

    public Month getMonth() {
        return month;
    }

    public void setMonth(Month month) {
        this.month = month;
    }

    public List<NavigationNode> getChildren() {
        return children;
    }

    public void setChildren(List<NavigationNode> children) {
        this.children = children;
    }

    public NavigationNode getParent() {
        return parent;
    }

    public void setParent(NavigationNode parent) {
        this.parent = parent;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }

    @Override
    public String toString() {
        return label;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        NavigationNode that = (NavigationNode) obj;
        return type == that.type && 
               year.equals(that.year) && 
               (month == null ? that.month == null : month.equals(that.month));
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + year.hashCode();
        result = 31 * result + (month != null ? month.hashCode() : 0);
        return result;
    }
}
